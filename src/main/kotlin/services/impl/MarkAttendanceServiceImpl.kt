package com.amos_tech_code.services.impl

import com.amos_tech_code.data.repository.AttendanceSessionRepository
import com.amos_tech_code.data.repository.StudentEnrollmentRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.dtos.requests.MarkAttendanceRequest
import com.amos_tech_code.domain.dtos.response.AttendanceFlag
import com.amos_tech_code.domain.dtos.response.MarkAttendanceResponse
import com.amos_tech_code.domain.dtos.response.ProgrammeInfoResponse
import com.amos_tech_code.domain.dtos.response.VerificationResult
import com.amos_tech_code.domain.models.AttendanceMethod
import com.amos_tech_code.domain.models.AttendanceSession
import com.amos_tech_code.domain.models.FlagType
import com.amos_tech_code.domain.models.LocationVerification
import com.amos_tech_code.domain.models.ProgrammeResolution
import com.amos_tech_code.domain.models.SeverityLevel
import com.amos_tech_code.domain.models.StudentEnrollmentSource
import com.amos_tech_code.domain.models.VerificationOutcome
import com.amos_tech_code.services.MarkAttendanceService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.ConflictException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ValidationException
import java.time.LocalDateTime
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MarkAttendanceServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
    private val studentRepository: StudentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository
) : MarkAttendanceService {

    override suspend fun processIntelligentAttendance(
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

            return MarkAttendanceResponse(
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
                attendedAt = record.attendedAt.toString(),
                message = if (flags.isEmpty())
                    "Attendance marked successfully"
                else
                    "Attendance marked with warnings"
            )

        } catch (ex: Exception) {
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

    /*private suspend fun resolveProgramme(
        studentId: UUID,
        session: AttendanceSession,
        requestedProgrammeId: String?
    ): ProgrammeResolution {

        val sessionProgrammes =
            attendanceSessionRepository.getSessionProgrammes(session.id)

        if (sessionProgrammes.isEmpty()) {
            throw ValidationException("No programmes linked to this session")
        }

        val activeEnrollment =
            programmeRepository.getStudentActiveProgramme(
                studentId,
                session.universityId
            )

        // Already enrolled
        if (activeEnrollment != null) {
            val valid = sessionProgrammes.any {
                it.programmeId == activeEnrollment.programmeId
            }

            if (!valid) {
                throw ValidationException("Session not available for your programme")
            }

            return ProgrammeResolution(programmeId = activeEnrollment.programmeId)
        }

        // First-time — single programme
        if (sessionProgrammes.size == 1) {
            val programme = sessionProgrammes.first()

            programmeRepository.linkStudentToProgramme(
                studentId = studentId,
                programmeId = programme.programmeId,
                unitId = session.unitId,
                universityId = session.universityId,
                academicTermId = session.academicTermId,
                enrollmentSource = StudentEnrollmentSource.ATTENDANCE
            )

            return ProgrammeResolution(programmeId = programme.programmeId)
        }

        // First-time — multiple programmes
        if (requestedProgrammeId != null) {
            val parsed = UUID.fromString(requestedProgrammeId)
            val valid = sessionProgrammes.any { it.programmeId == parsed }

            if (!valid) {
                throw ValidationException("Invalid programme selection")
            }

            programmeRepository.linkStudentToProgramme(
                studentId,
                parsed,
                session.unitId,
                session.universityId,
                session.academicTermId,
                StudentEnrollmentSource.ATTENDANCE
            )

            return ProgrammeResolution(programmeId = parsed)
        }

        // Require explicit selection
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
                verification = VerificationResult(false, false, false, false,false),
                attendedAt = LocalDateTime.now().toString(),
                message = "Please select your programme first"
            )
        )
    }*/

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

}
