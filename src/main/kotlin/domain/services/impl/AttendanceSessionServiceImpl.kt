package domain.services.impl

import api.dtos.response.AttendanceSessionHistoryResponse
import data.repository.AttendanceSessionRepository
import com.amos_tech_code.domain.dtos.requests.StartSessionRequest
import com.amos_tech_code.domain.dtos.requests.UpdateSessionRequest
import com.amos_tech_code.domain.dtos.requests.VerifySessionRequest
import api.dtos.response.ProgrammeInfoResponse
import api.dtos.response.SessionInfo
import com.amos_tech_code.domain.dtos.response.SessionResponse
import api.dtos.response.VerifyAttendanceResponse
import data.repository.StudentEnrollmentRepository
import domain.models.AttendanceSessionStatus
import domain.models.AttendanceSessionType
import com.amos_tech_code.domain.models.CreateSessionData
import com.amos_tech_code.domain.models.UpdateSessionData
import domain.services.AttendanceSessionService
import com.amos_tech_code.services.CloudStorageService
import com.amos_tech_code.services.QRCodeService
import com.amos_tech_code.services.SessionCodeGenerator
import com.amos_tech_code.utils.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import utils.toLocalDateTimeOrThrow
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class AttendanceSessionServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val qrCodeService: QRCodeService,
    private val sessionCodeGenerator: SessionCodeGenerator,
    private val cloudStorageService: CloudStorageService
) : AttendanceSessionService {

    private val logger = LoggerFactory.getLogger(AttendanceSessionServiceImpl::class.java)

    override suspend fun startSession(lecturerId: UUID, request: StartSessionRequest): SessionResponse {

        try {
            if (attendanceSessionRepository.getActiveSession(lecturerId) != null) {
                throw ConflictException("You already have an active session.")
            }

            validateStartSessionRequest(request)

            val universityId = UUID.fromString(request.universityId)
            val unitId = UUID.fromString(request.unitId)
            val programmeIds = request.programmeIds.map { UUID.fromString(it) }

            val academicTermId = attendanceSessionRepository.resolveActiveAcademicTermId(universityId)
            // Validate lecturer authorization
            attendanceSessionRepository.validateLecturerAuthorization(lecturerId, universityId, academicTermId, unitId, programmeIds)

            // Generate unique session code
            val sessionCode = generateUniqueSessionCode()
            val unitCode = attendanceSessionRepository.findUnitCodeById(unitId)

            // Handle QR code generation
            val qrCodeUrl = generateAndUploadQrCode(sessionCode, unitCode)

            val scheduledStartTime = request.scheduledStartTime?.toLocalDateTimeOrThrow() ?: LocalDateTime.now()
            val scheduledEndTime = scheduledStartTime.plusMinutes(request.durationMinutes.toLong())

            // Create session data
            val sessionData = CreateSessionData(
                lecturerId = lecturerId,
                universityId = universityId,
                academicTermId = academicTermId,
                unitId = unitId,
                sessionCode = sessionCode,
                allowedMethod = request.allowedMethod,
                qrCodeUrl = qrCodeUrl,
                isLocationRequired = request.isLocationRequired,
                lecturerLatitude = request.location?.latitude,
                lecturerLongitude = request.location?.longitude,
                locationRadius = request.radiusMeters,
                scheduledStartTime = scheduledStartTime,
                scheduledEndTime = scheduledEndTime,
                durationMinutes = request.durationMinutes,
                sessionStatus = if (request.scheduledStartTime == null) AttendanceSessionStatus.ACTIVE else AttendanceSessionStatus.SCHEDULED,
                title = request.title,
                attendanceSessionType = AttendanceSessionType.valueOf(request.attendanceSessionType.name),
                weekNumber = request.weekNumber
            )

            var sessionId : UUID? = null
            // Create session and link programmes
            newSuspendedTransaction {

                sessionId = attendanceSessionRepository.createSession(sessionData)
                attendanceSessionRepository.linkSessionToProgrammes(
                    sessionId = sessionId,
                    programmeIds = programmeIds,
                    unitId = unitId,
                    lecturerId = lecturerId,
                    universityId = universityId
                )
            }

            // Return the created session
            if (sessionId != null) {
                return attendanceSessionRepository.getSessionDetails(sessionId) ?: throw InternalServerException("Failed to retrieve created session")
            } else {
                throw IllegalStateException("Failed to start attendance session")
            }

        } catch (ex: Exception) {
            logger.error("Failed to start attendance session: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to start attendance session.")
            }
        }
    }

    override suspend fun updateSession(
        lecturerId: UUID,
        sessionId: String,
        request: UpdateSessionRequest
    ): SessionResponse {

        return try {

            validateUpdateSessionRequest(request)

            val sessionUUID = UUID.fromString(sessionId)

            attendanceSessionRepository.updateSession(
                sessionId = sessionUUID,
                lecturerId = lecturerId,
                updateData = UpdateSessionData(
                    title = request.title,
                    attendanceSessionType = request.attendanceSessionType?.let {
                        AttendanceSessionType.valueOf(it.name)
                    },
                    weekNumber = request.weekNumber,
                    programmeIds = request.programmeIds?.map(UUID::fromString),
                    unitId = request.unitId?.let(UUID::fromString),
                    allowedMethod = request.allowedMethod,
                    isLocationRequired = request.isLocationRequired,
                    lecturerLatitude = request.location?.latitude,
                    lecturerLongitude = request.location?.longitude,
                    locationRadius = request.radiusMeters,
                    scheduledStartTime = request.scheduledStartTime?.toLocalDateTimeOrThrow(),
                    durationMinutes = request.durationMinutes
                )
            ) ?: throw ResourceNotFoundException("Session not found")

        } catch (ex: Exception) {
            logger.error("Failed to update session: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to update session: ${ex.message}")
            }
        }
    }

    override suspend fun endSession(lecturerId: UUID, sessionId: String): Boolean {
        try {
            if (sessionId.isBlank()) {
                throw ValidationException("Session ID is required")
            }

            val sessionUUID = UUID.fromString(sessionId)

            // Get QR code URL before ending session to delete from cloud storage
            val qrCodeUrl = attendanceSessionRepository.getSessionQrCodeUrl(sessionUUID)

            // End the session
            val success = attendanceSessionRepository.endSession(lecturerId, sessionUUID)

            // Delete QR code from cloud storage if session was ended successfully
            if (success && qrCodeUrl != null) {
                cloudStorageService.deleteQRCode(qrCodeUrl)
            }

            return success

        } catch (ex: Exception) {
            logger.error("Failed to end session: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to end session")
            }
        }
    }

    override suspend fun getActiveSession(lecturerId: UUID): SessionResponse {
        return attendanceSessionRepository.getActiveSession(lecturerId)
            ?: throw ResourceNotFoundException("No active session found")
    }

    override suspend fun verifySessionForAttendance(studentId: UUID, request: VerifySessionRequest): VerifyAttendanceResponse {
        try {
            // Validate request
            validateVerifySessionRequest(request)

            // Get active session
            val session = attendanceSessionRepository.getActiveSessionBySessionCodeAndUnitCode(
                request.sessionCode,
                request.unitCode
            ) ?: throw ResourceNotFoundException("Invalid session or session has ended")

            // Check if any programmes linked to the session
            val sessionProgrammes = attendanceSessionRepository.getSessionProgrammes(session.id)

            if (sessionProgrammes.isEmpty()) throw ValidationException("No programmes associated with this session")

            // Check if first attendance by checking active enrollment
            val activeEnrollment = studentEnrollmentRepository.findActiveEnrollment(studentId)

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

                // Not first time - no programme selection needed
                return VerifyAttendanceResponse(
                    requiresProgrammeSelection = false,
                    availableProgrammes = sessionProgrammes.map { programme ->
                        ProgrammeInfoResponse(
                            id = programme.programmeId.toString(),
                            name = programme.programmeName,
                            department = programme.departmentName,
                            yearOfStudy = programme.yearOfStudy
                        )
                    },
                    requiresLocation = session.isLocationRequired,
                    sessionInfo = SessionInfo(
                        sessionId = session.id.toString(),
                        unitName = session.unitName,
                        unitCode = session.unitCode,
                        lecturerName = session.lecturerName
                    )
                )
            } else {
                return VerifyAttendanceResponse(
                    requiresProgrammeSelection = sessionProgrammes.size > 1,
                    availableProgrammes = sessionProgrammes.map { programme ->
                        ProgrammeInfoResponse(
                            id = programme.programmeId.toString(),
                            name = programme.programmeName,
                            department = programme.departmentName,
                            yearOfStudy = programme.yearOfStudy
                        )
                    },
                    requiresLocation = session.isLocationRequired,
                    sessionInfo = SessionInfo(
                        sessionId = session.id.toString(),
                        unitName = session.unitName,
                        unitCode = session.unitCode,
                        lecturerName = session.lecturerName
                    )
                )
            }


        } catch (ex: Exception) {
            logger.error("Attendance verification failed: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Attendance verification failed.")
            }
        }
    }

    override suspend fun getLecturerSessionHistory(
        lecturerId: UUID,
        page: Int,
        size: Int
    ): AttendanceSessionHistoryResponse {

        try {
            require(page >= 0) { "Page must be >= 0" }
            require(size in 1..50) { "Size must be between 1 and 50" }

            val offset = page * size

            val sessions = attendanceSessionRepository.fetchSessionHistory(
                lecturerId = lecturerId,
                limit = size + 1, // fetch one extra to detect hasNext
                offset = offset
            )

            val hasNext = sessions.size > size

            return AttendanceSessionHistoryResponse(
                page = page,
                size = size,
                hasNext = hasNext,
                sessions = sessions.take(size)
            )
        } catch (ex: Exception) {
            logger.error("Failed to fetch session history", ex)
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to fetch session history")
            }
        }
    }

    private suspend fun generateUniqueSessionCode(): String {
        var attempts = 0
        val maxAttempts = 10

        while (attempts < maxAttempts) {
            val code = sessionCodeGenerator.generateSixDigitCode()
            val isUnique = attendanceSessionRepository.isSessionCodeUnique(code)

            if (isUnique) {
                return code
            }
            attempts++
        }

        throw InternalServerException("Failed to generate unique 6-digit session code after $maxAttempts attempts")
    }

    private suspend fun generateAndUploadQrCode(sessionCode: String, unitCode: String): String {
        return try {
            val sessionId = UUID.randomUUID() // Temporary ID for QR data
            val qrCodeData = qrCodeService.generateQRCodeData(sessionCode, unitCode, sessionId)
            val qrCodeImage = qrCodeService.generateQRCodeImage(qrCodeData, 300, 300)
            val fileName = "qr_${sessionId}_${System.currentTimeMillis()}.png"
            cloudStorageService.uploadQRCode(qrCodeImage, fileName)
        } catch (ex: Exception) {
            throw InternalServerException("Failed to generate and upload QR code: ${ex.message}")
        }
    }

    private fun validateStartSessionRequest(request: StartSessionRequest) {

        // --- Required IDs ---
        if (request.universityId.isBlank()) {
            throw ValidationException("University ID is required")
        }

        if (request.programmeIds.isEmpty()) {
            throw ValidationException("At least one programme ID is required")
        }

        if (request.programmeIds.size > 10) {
            throw ValidationException("Maximum number of programmes allowed is 10")
        }

        if (request.unitId.isBlank()) {
            throw ValidationException("Unit ID is required")
        }

        require (request.weekNumber in 1..20) {
            throw ValidationException("Invalid academic week number")
        }

        // --- UUID format validation ---
        try {
            UUID.fromString(request.universityId)
            UUID.fromString(request.unitId)
            request.programmeIds.forEach { UUID.fromString(it) }
        } catch (e: IllegalArgumentException) {
            throw ValidationException("One or more IDs are not valid UUIDs")
        }

        // --- Location validation (conditional) ---
        if (request.isLocationRequired) {
            val location = request.location
                ?: throw ValidationException("Location is required when location tracking is enabled")

            require (location.latitude in -90.0..90.0) {
                throw ValidationException("Invalid latitude value")
            }

            require (location.longitude in -180.0..180.0) {
                throw ValidationException("Invalid longitude value")
            }

            require (request.radiusMeters in 1..1000) {
                throw ValidationException("Location radius must be between 1 and 1000 meters")
            }
        }

        // --- Session duration ---
        require (request.durationMinutes in 1..240) {
            throw ValidationException("Duration must be between 1 and 240 minutes")
        }

        // --- Scheduled start time ---
        request.scheduledStartTime?.let {
            try {
                Instant.parse(it)
            } catch (e: Exception) {
                throw ValidationException("Scheduled start time must be a valid")
            }
        }
    }

    private fun validateUpdateSessionRequest(request: UpdateSessionRequest) {

        if (request.isLocationRequired == true) {
            val location = request.location
                ?: throw ValidationException("Location is required when location tracking is enabled")

            if (location.latitude !in -90.0..90.0) {
                throw ValidationException("Invalid latitude value")
            }

            if (location.longitude !in -180.0..180.0) {
                throw ValidationException("Invalid longitude value")
            }

            if (request.radiusMeters !in 1..1000) {
                throw ValidationException("Location radius must be between 1 and 1000 meters")
            }
        }

        request.radiusMeters?.let { radius ->
            if (radius !in 1..1000) throw ValidationException("Location radius must be between 1 and 1000 meters")
        }

        request.durationMinutes?.let { duration ->
            if (duration !in 1..240) throw ValidationException("Duration must be between 1 and 240 minutes")
        }

        // Validate UUID formats if provided
        request.unitId?.let {
            try {
                UUID.fromString(it)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid unit ID format")
            }
        }

        request.programmeIds?.size?.let {
            if (it > 10) {
                throw ValidationException("Maximum number of Programmes allowed is 10")
            }
        }

        request.programmeIds?.forEach { programmeId ->
            try {
                UUID.fromString(programmeId)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid programme ID format")
            }
        }

        // --- Scheduled start time ---
        request.scheduledStartTime?.let {
            try {
                Instant.parse(it)
            } catch (e: Exception) {
                throw ValidationException("Scheduled start time must be a valid")
            }
        }

    }

    private fun validateVerifySessionRequest(request: VerifySessionRequest) {
        if (request.sessionCode.isBlank()) {
            throw ValidationException("Invalid QR code")
        }
        if (request.sessionCode.length != 6) {
            throw ValidationException("Invalid QR code")
        }
        if (!request.sessionCode.matches(Regex("\\d{6}"))) {
            throw ValidationException("Invalid QR code")
        }
        if (request.unitCode.isBlank()) {
            throw ValidationException("Invalid QR code")
        }
    }

}