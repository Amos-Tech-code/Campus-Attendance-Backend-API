package com.amos_tech_code.services.impl

import com.amos_tech_code.data.repository.AttendanceSessionRepository
import com.amos_tech_code.data.repository.ProgrammeRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.dtos.requests.MarkAttendanceRequest
import com.amos_tech_code.domain.dtos.response.AttendanceFlag
import com.amos_tech_code.domain.dtos.response.MarkAttendanceResponse
import com.amos_tech_code.domain.dtos.response.ProgrammeInfoResponse
import com.amos_tech_code.domain.dtos.response.VerificationResult
import com.amos_tech_code.domain.models.AttendanceSession
import com.amos_tech_code.domain.models.FlagType
import com.amos_tech_code.domain.models.SeverityLevel
import com.amos_tech_code.domain.models.StudentEnrollmentSource
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
    private val programmeRepository: ProgrammeRepository
) : MarkAttendanceService {

    override suspend fun processIntelligentAttendance(studentId: UUID, request: MarkAttendanceRequest): MarkAttendanceResponse {
        try {
            validateMarkAttendanceRequest(request)

            // Verify session exists and is active
            val session = attendanceSessionRepository.getActiveSessionBySessionCodeAndUnitCode(
                request.sessionCode,
                request.unitCode
            ) ?: return createErrorResponse("Invalid session or session has ended")

            // Simple duplicate check
            if (attendanceSessionRepository.hasExistingAttendance(studentId, session.id)) {
                throw ConflictException("You have already marked attendance for this session")
            }

            val isFirstAttendance = attendanceSessionRepository.isFirstAttendance(studentId, session.id)

            return if (isFirstAttendance) {
                handleFirstTimeAttendance(studentId, session, request)
            } else {
                handleSubsequentAttendance(studentId, session, request)
            }
        } catch (ex: Exception) {
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to process attendance: ${ex.message}")
            }
        }
    }

    private suspend fun handleFirstTimeAttendance(
        studentId: UUID,
        session: AttendanceSession,
        request: MarkAttendanceRequest
    ): MarkAttendanceResponse {
        val sessionProgrammes = attendanceSessionRepository.getSessionProgrammes(session.id)

        if (sessionProgrammes.isEmpty()) {
            return createErrorResponse("No programmes associated with this session")
        }

        val academicTermId = UUID.randomUUID() // TODO(): Remove Random UUID * Replace with real impl

        return when {

            // Case 1: Session has only one programme - auto-link student to it
            sessionProgrammes.size == 1 -> {
                val programme = sessionProgrammes.first()
                // Link student to programme on first attendance
                programmeRepository.linkStudentToProgramme(
                    studentId = studentId,
                    programmeId = programme.programmeId,
                    unitId = session.unitId,
                    universityId = session.universityId,
                    academicTermId = academicTermId,
                    enrollmentSource = StudentEnrollmentSource.ATTENDANCE,
                )
                createAttendanceRecord(studentId, session, request, programme.programmeId)
            }

            // Case 2: Session has multiple programmes - student must select one
            else -> {
                // Multiple programmes - check if programmeId provided
                request.programmeId?.let { programmeIdStr ->
                    val programmeId = UUID.fromString(programmeIdStr)
                    if (sessionProgrammes.any { it.programmeId == programmeId }) {
                        // Link student to selected programme
                        programmeRepository.linkStudentToProgramme(
                            studentId,
                            programmeId,
                            session.unitId,
                            session.universityId,
                            academicTermId = academicTermId,
                            enrollmentSource = StudentEnrollmentSource.ATTENDANCE
                        )
                        return createAttendanceRecord(studentId, session, request, programmeId)
                    }
                }

                // No valid programme selected
                MarkAttendanceResponse(
                    success = false,
                    sessionId = session.id.toString(),
                    verification = VerificationResult(
                        locationVerified = false,
                        deviceVerified = false,
                        methodVerified = false,
                        overallVerified = false
                    ),
                    requiresProgrammeSelection = true,
                    availableProgrammes = sessionProgrammes.map { programme ->
                        ProgrammeInfoResponse(
                            id = programme.programmeId.toString(),
                            name = programme.programmeName,
                            department = programme.departmentName,
                            yearOfStudy = programme.yearOfStudy
                        )
                    },
                    attendedAt = LocalDateTime.now().toString(),
                    message = "Please select your programme"
                )
            }
        }
    }

    private suspend fun handleSubsequentAttendance(
        studentId: UUID,
        session: AttendanceSession,
        request: MarkAttendanceRequest
    ): MarkAttendanceResponse {
        // Get student's linked programme
        val studentProgramme = programmeRepository.getStudentActiveProgramme(studentId, session.universityId)
            ?: return createErrorResponse("No programme linked to student")

        // Verify session programme matches student's linked programme
        val sessionProgrammes = attendanceSessionRepository.getSessionProgrammes(session.id)
        val isProgrammeValid = sessionProgrammes.any { it.programmeId == studentProgramme.programmeId }

        if (!isProgrammeValid) {
            return createErrorResponse("Session is not available for your linked programme")
        }

        return createAttendanceRecord(studentId, session, request, studentProgramme.programmeId)
    }

    private suspend fun createAttendanceRecord(
        studentId: UUID,
        session: AttendanceSession,
        request: MarkAttendanceRequest,
        programmeId: UUID
    ): MarkAttendanceResponse {
        val verificationResult = performVerificationChecks(studentId, session, request)
        val flags = mutableListOf<AttendanceFlag>()

        if (!verificationResult.locationVerified) {
            flags.add(AttendanceFlag(
                type = FlagType.LOCATION_MISMATCH,
                message = "Location verification failed",
                severity = SeverityLevel.MEDIUM
            ))
        }

        if (!verificationResult.deviceVerified) {
            flags.add(AttendanceFlag(
                type = FlagType.DEVICE_MISMATCH,
                message = "Device verification failed",
                severity = SeverityLevel.HIGH
            ))
        }

        val attendanceRecord = attendanceSessionRepository.createAttendanceRecord(
            studentId = studentId,
            sessionId = session.id,
            programmeId = programmeId,
            sessionCode = request.sessionCode,
            deviceId = request.deviceId,
            studentLat = request.studentLat,
            studentLng = request.studentLng,
            isLocationVerified = verificationResult.locationVerified,
            isDeviceVerified = verificationResult.deviceVerified
        )

        return MarkAttendanceResponse(
            success = true,
            sessionId = session.id.toString(),
            programmeId = programmeId.toString(),
            verification = verificationResult,
            flags = flags,
            attendedAt = attendanceRecord.attendedAt.toString(),
            message = if (flags.isEmpty()) "Attendance marked successfully" else "Attendance marked with warnings"
        )
    }

    private suspend fun performVerificationChecks(
        studentId: UUID,
        session: AttendanceSession,
        request: MarkAttendanceRequest
    ): VerificationResult {
        val locationVerified = verifyLocation(
            lecturerLat = session.lecturerLatitude ?: 0.0,
            lecturerLng = session.lecturerLongitude ?: 0.0,
            studentLat = request.studentLat,
            studentLng = request.studentLng,
            radiusMeters = session.locationRadius
        )

        val deviceVerified = verifyDevice(studentId, request.deviceId)

        return VerificationResult(
            locationVerified = locationVerified,
            deviceVerified = deviceVerified,
            methodVerified = true,
            overallVerified = locationVerified && deviceVerified
        )
    }

    private fun verifyLocation(
        lecturerLat: Double,
        lecturerLng: Double,
        studentLat: Double?,
        studentLng: Double?,
        radiusMeters: Int
    ): Boolean {
        if (studentLat == null || studentLng == null) return false

        val distance = calculateDistance(lecturerLat, lecturerLng, studentLat, studentLng)
        return distance <= radiusMeters
    }

    private suspend fun verifyDevice(studentId: UUID, deviceId: String): Boolean {
        val registeredDevice = studentRepository.findDeviceByStudentId(studentId)
        return registeredDevice?.deviceId == deviceId
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
            throw ValidationException("Secret key is required")
        }
        if (request.unitCode.length != 8) {
            throw ValidationException("Secret key must be 8 characters")
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
    }

    private fun createErrorResponse(message: String): MarkAttendanceResponse {
        return MarkAttendanceResponse(
            success = false,
            sessionId = "",
            verification = VerificationResult(
                locationVerified = false,
                deviceVerified = false,
                methodVerified = false,
                overallVerified = false
            ),
            attendedAt = LocalDateTime.now().toString(),
            message = message
        )
    }
}