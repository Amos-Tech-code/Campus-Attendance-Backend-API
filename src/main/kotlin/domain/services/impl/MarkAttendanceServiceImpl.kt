package domain.services.impl

import data.repository.AttendanceSessionRepository
import data.repository.StudentEnrollmentRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.dtos.requests.LecturerMarkAttendanceRequest
import com.amos_tech_code.domain.dtos.requests.MarkAttendanceRequest
import api.dtos.response.AttendanceFlag
import api.dtos.response.AttendanceMarkedEventDto
import api.dtos.response.LiveAttendanceEvent
import api.dtos.response.LiveAttendanceStudentDto
import api.dtos.response.MarkAttendanceResponse
import api.dtos.response.ProgrammeInfoResponse
import api.dtos.response.SessionDetailsResponse
import api.dtos.response.VerificationResult
import domain.models.AttendanceMethod
import com.amos_tech_code.domain.models.AttendanceSession
import domain.models.FlagType
import com.amos_tech_code.domain.models.LocationVerification
import com.amos_tech_code.domain.models.ProgrammeResolution
import domain.models.SeverityLevel
import domain.models.StudentEnrollmentSource
import com.amos_tech_code.domain.models.VerificationOutcome
import com.amos_tech_code.domain.services.AttendanceEventBus
import com.amos_tech_code.services.MarkAttendanceService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.BackgroundTaskScope
import com.amos_tech_code.utils.ConflictException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ValidationException
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MarkAttendanceServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
    private val studentRepository: StudentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
   // private val attendanceEventPublisher: AttendanceEventPublisher,
    private val attendanceEventBus: AttendanceEventBus,
    private val backgroundTaskScope: BackgroundTaskScope
) : MarkAttendanceService {

    private val logger = LoggerFactory.getLogger(MarkAttendanceServiceImpl::class.java)

    override suspend fun lecturerSignAttendance(
        lecturerId: UUID,
        request: LecturerMarkAttendanceRequest
    ): Boolean {

        try {
            // Request validation
            request.validate()

            val sessionId = UUID.fromString(request.sessionId)
            // 1. Verify session exists
            if (!attendanceSessionRepository.existsByIdAndLecturerId(sessionId, lecturerId))
                throw ValidationException("You do not own this session")

            // 3. Load student
            val student = studentRepository.findByRegistrationNumber(request.studentRegNo)
                ?: throw ValidationException("Student not found")

            // 4. Already marked?
            if (attendanceSessionRepository.hasExistingAttendance(student.id, sessionId)) {
                throw ConflictException("Attendance already recorded for this student")
            }

            // 5. Resolve enrollment
            val sessionProgrammes = attendanceSessionRepository.getSessionProgrammes(sessionId)

            if (sessionProgrammes.isEmpty()) {
                throw ValidationException("Session has no linked programmes")
            }

            val activeEnrollment =
                studentEnrollmentRepository.findActiveEnrollment(student.id)
                    ?: throw ValidationException("Student has no active enrollment")

            val matches = sessionProgrammes.any {
                it.programmeId == activeEnrollment.programmeId
            }

            if (!matches) {
                throw ValidationException(
                    "Student is enrolled in a different programme: ${activeEnrollment.programmeName}"
                )
            }

            // 6. Create attendance (OVERRIDE)
            val record = attendanceSessionRepository.createAttendanceRecord(
                studentId = student.id,
                sessionId = sessionId,
                deviceId = null,
                studentLat = null,
                studentLng = null,
                distance = null,
                isDeviceVerified = true,
                isLocationVerified = true,
                attendanceMethod = AttendanceMethod.LECTURER_MANUAL,
                isSuspicious = false,
                suspiciousReason = null,
            )

            // PUBLISH ATTENDANCE MARKED EVENT
            val event = AttendanceMarkedEventDto(
                sessionId = sessionId.toString(),
                programmeId = activeEnrollment.programmeId.toString(),
                student = LiveAttendanceStudentDto(
                    studentId = student.id.toString(),
                    regNo = student.registrationNumber,
                    name = student.fullName,
                    attendedAt = record.attendedAt,
                    isSuspicious = record.isSuspicious,
                    suspiciousReason = record.suspiciousReason
                )
            )

            backgroundTaskScope.scope.launch {
                try {
                    //attendanceEventPublisher.publishAttendanceMarked(event)
                    attendanceEventBus.publish(
                        LiveAttendanceEvent.AttendanceMarked(data = event)                    )
                } catch (e: Exception) {
                    // NEVER propagate
                    logger.error("Failed to publish attendance event: $event", e)
                }
            }

            return true


        } catch (ex: Exception) {
            logger.error("Failed to mark attendance for student: $ex")
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to mark attendance for student registration number: ${request.studentRegNo}")
            }
        }

    }

    override suspend fun processAttendanceMarking(
        studentId: UUID,
        request: MarkAttendanceRequest
    ): MarkAttendanceResponse {

        try {
            validateMarkAttendanceRequest(request)

            val session = attendanceSessionRepository
                .getActiveSessionBySessionCodeAndUnitCode(
                    request.sessionCode,
                    request.unitCode
                ) ?: throw ValidationException("Invalid or inactive session")

            if (attendanceSessionRepository.hasExistingAttendance(studentId, session.id)) {
                throw ConflictException("Attendance already recorded")
            }

            val programmeResolution = resolveProgramme(
                studentId, session, request.programmeId
            )

            if (programmeResolution.requiresSelection) {
                return programmeResolution.selectionResponse!!
            }

            val programmeId = programmeResolution.programmeId!!

            val flags = mutableListOf<AttendanceFlag>()

            // Method (hard)
            val methodVerified = verifyMethod(session.allowedMethod, request.methodUsed)
            if (!methodVerified) {
                throw ValidationException("Attendance method not allowed")
            }

            // Schedule (grace)
            val scheduleResult = verifySchedule(session)
            scheduleResult.flag?.let { flags += it }

            // Device (hard)
            val deviceVerified = verifyDevice(studentId, request.deviceId)
            if (!deviceVerified) {
                throw ValidationException("Unrecognized device")
            }

            // Location (grace)
            val locationResult = verifyLocation(session, request.studentLat, request.studentLng)
            locationResult.flag?.let { flags += it }

            val overallVerified = true // all hard rules passed

            val record = attendanceSessionRepository.createAttendanceRecord(
                studentId = studentId,
                sessionId = session.id,
                deviceId = request.deviceId,
                studentLat = request.studentLat,
                studentLng = request.studentLng,
                distance = locationResult.distance,
                isDeviceVerified = deviceVerified,
                isLocationVerified = locationResult.verified,
                attendanceMethod = request.methodUsed,
                isSuspicious = flags.isNotEmpty(),
                suspiciousReason = flags.joinToString { it.type.name }
            )

            val response = MarkAttendanceResponse(
                success = true,
                sessionId = session.id.toString(),
                programmeId = programmeId.toString(),
                verification = VerificationResult(
                    locationVerified = locationResult.verified,
                    deviceVerified = deviceVerified,
                    methodVerified = methodVerified,
                    attendanceTimeVerified = scheduleResult.verified,
                    overallVerified = overallVerified
                ),
                flags = flags,
                sessionDetails = SessionDetailsResponse(
                    sessionStatus = session.sessionStatus,
                    attendanceMethod = session.allowedMethod,
                    sessionType = session.sessionType,
                    sessionTitle = session.sessionTitle,
                    unitCode = session.unitCode,
                    unitName = session.unitName,
                ),
                attendedAt = record.attendedAt,
                message = if (flags.isEmpty())
                    "Attendance marked successfully"
                else
                    "Attendance marked with warnings"
            )

            // PUBLISH ATTENDANCE MARKED EVENT
            backgroundTaskScope.scope.launch {
                try {
                    val student = studentRepository.findById(studentId)
                        ?: throw InternalServerException("Student not found after attendance record")

                    val event = AttendanceMarkedEventDto(
                        sessionId = session.id.toString(),
                        programmeId = programmeId.toString(),
                        student = LiveAttendanceStudentDto(
                            studentId = student.id.toString(),
                            regNo = student.registrationNumber,
                            name = student.fullName,
                            attendedAt = record.attendedAt,
                            isSuspicious = flags.isNotEmpty(),
                            suspiciousReason = flags.joinToString { it.type.name }.ifBlank { null }
                        )
                    )

                    attendanceEventBus.publish(
                        LiveAttendanceEvent.AttendanceMarked(data = event)
                    )
                } catch (e: Exception) {
                    // NEVER propagate
                    logger.error("Failed to publish attendance event:", e)
                }
            }

            return response

        } catch (ex: Exception) {
            logger.error("Failed to mark attendance: $ex")
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to mark attendance")
            }
        }
    }

    private suspend fun resolveProgramme(
        studentId: UUID,
        session: AttendanceSession,
        requestedProgrammeId: String?
    ): ProgrammeResolution {

        val sessionProgrammes =
            attendanceSessionRepository.getSessionProgrammes(session.id)

        if (sessionProgrammes.isEmpty()) {
            throw ValidationException("No programmes linked to this session")
        }

        // 1. Check active enrollment FIRST
        val activeEnrollment =
            studentEnrollmentRepository.findActiveEnrollment(studentId)

        if (activeEnrollment != null) {

            val enrolledProgrammeId = activeEnrollment.programmeId

            val allowed = sessionProgrammes.any {
                it.programmeId == enrolledProgrammeId
            }

            if (!allowed) {
                throw ValidationException(
                    "You are enrolled in ${activeEnrollment.programmeName}, " +
                            "but this session is for a different programme"
                )
            }

            return ProgrammeResolution(programmeId = enrolledProgrammeId)
        }

        // 2. No active enrollment — auto-enroll if only one programme
        if (sessionProgrammes.size == 1) {
            val programme = sessionProgrammes.first()

            studentEnrollmentRepository.createEnrollment(
                studentId = studentId,
                universityId = session.universityId,
                programmeId = programme.programmeId,
                academicTermId = session.academicTermId,
                yearOfStudy = programme.yearOfStudy,
                enrollmentSource = StudentEnrollmentSource.ATTENDANCE
            )

            return ProgrammeResolution(programmeId = programme.programmeId)
        }

        // 3. Multiple programmes — require explicit selection
        if (requestedProgrammeId == null) {
            return ProgrammeResolution(
                requiresSelection = true,
                selectionResponse = MarkAttendanceResponse(
                    success = false,
                    sessionId = session.id.toString(),
                    requiresProgrammeSelection = true,
                    availableProgrammes = sessionProgrammes.map {
                        ProgrammeInfoResponse(
                            it.programmeId.toString(),
                            it.programmeName,
                            it.departmentName,
                            it.yearOfStudy
                        )
                    },
                    verification = VerificationResult(
                        false, false, false, false, false
                    ),
                    flags = emptyList(),
                    attendedAt = LocalDateTime.now().toString(),
                    message = "Please select your programme"
                )
            )
        }

        // 4. Validate selected programme
        val parsedProgrammeId = UUID.fromString(requestedProgrammeId)

        val valid = sessionProgrammes.any {
            it.programmeId == parsedProgrammeId
        }

        if (!valid) {
            throw ValidationException("Invalid programme selection")
        }

        val selectedProgramme = sessionProgrammes.first {
            it.programmeId == parsedProgrammeId
        }

        studentEnrollmentRepository.createEnrollment(
            studentId = studentId,
            universityId = session.universityId,
            programmeId = parsedProgrammeId,
            academicTermId = session.academicTermId,
            yearOfStudy = selectedProgramme.yearOfStudy,
            enrollmentSource = StudentEnrollmentSource.ATTENDANCE
        )

        return ProgrammeResolution(programmeId = parsedProgrammeId)
    }

    private fun verifyMethod(
        allowed: AttendanceMethod,
        used: AttendanceMethod
    ): Boolean {
        return when {
            used == AttendanceMethod.LECTURER_MANUAL -> true
            allowed == AttendanceMethod.ANY -> true
            allowed == used -> true
            else -> false
        }
    }

    private fun verifySchedule(session: AttendanceSession): VerificationOutcome {
        val now = LocalDateTime.now()

        if (now.isBefore(session.scheduledStartTime)) {
            throw ValidationException("Attendance has not started yet")
        }

        val hardClose = session.scheduledEndTime.plusMinutes(10)
        val graceClose = session.scheduledEndTime.plusMinutes(5)

        return when {
            now.isAfter(hardClose) ->
                throw ValidationException("Attendance window closed")

            now.isAfter(graceClose) ->
                VerificationOutcome(
                    verified = true,
                    flag = AttendanceFlag(
                        FlagType.OUTSIDE_SCHEDULE_WINDOW,
                        "Attendance marked slightly outside scheduled time",
                        SeverityLevel.LOW
                    )
                )

            else -> VerificationOutcome(true)
        }
    }

    private suspend fun verifyDevice(studentId: UUID, deviceId: String): Boolean {
        val registeredDevice = studentRepository.findDeviceByStudentId(studentId)
        return registeredDevice?.deviceId == deviceId
    }

    private fun verifyLocation(
        session: AttendanceSession,
        studentLat: Double?,
        studentLng: Double?
    ): LocationVerification {

        if (!session.isLocationRequired) {
            return LocationVerification(true, 0.0)
        }

        if (studentLat == null || studentLng == null) {
            throw ValidationException("Location details are required")
        }

        val distance = calculateDistance(
            session.lecturerLatitude!!,
            session.lecturerLongitude!!,
            studentLat,
            studentLng
        )

        val allowed = session.locationRadius!!
        val graceRadius = allowed + 20 // meters

        return when {
            distance > graceRadius ->
                throw ValidationException("You are too far from the attendance location")

            distance > allowed ->
                LocationVerification(
                    verified = false,
                    distance = distance,
                    flag = AttendanceFlag(
                        FlagType.LOCATION_MISMATCH,
                        "Slightly outside allowed location radius",
                        SeverityLevel.MEDIUM
                    )
                )

            else ->
                LocationVerification(true, distance)
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun validateMarkAttendanceRequest(request: MarkAttendanceRequest) {
        if (request.sessionCode.isBlank()) {
            throw ValidationException("Session code is required")
        }
        if (request.sessionCode.length != 6) {
            throw ValidationException("Session code must be 6 digits")
        }
        if (!request.sessionCode.matches(Regex("\\d{6}"))) {
            throw ValidationException("Session code must contain only digits")
        }
        if (request.unitCode.isBlank()) {
            throw ValidationException("Unit code is required")
        }

        if (request.deviceId.isBlank()) {
            throw ValidationException("Device ID is required")
        }

        request.studentLat?.let { lat ->
            if (lat < -90 || lat > 90) throw ValidationException("Invalid latitude")
        }
        request.studentLng?.let { lng ->
            if (lng < -180 || lng > 180) throw ValidationException("Invalid longitude")
        }

        require(request.methodUsed.name.isNotBlank()) {
            throw ValidationException("Method used is required")
        }
    }

    private fun LecturerMarkAttendanceRequest.validate() {
        require(sessionId.isNotBlank()) {
            throw ValidationException("Session Id is required")
        }
        require(studentRegNo.isNotBlank()) {
            throw ValidationException("Student Registration Number is required.")
        }
    }

}
