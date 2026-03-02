package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.requests.RemoveAttendanceRequest
import com.amos_tech_code.api.dtos.response.AttendanceStatsResponse
import com.amos_tech_code.api.dtos.response.StudentAttendanceHistoryResponse
import com.amos_tech_code.data.repository.AttendanceRecordRepository
import com.amos_tech_code.domain.services.AttendanceManagementService
import com.amos_tech_code.domain.services.NotificationService
import com.amos_tech_code.utils.*
import data.repository.AttendanceSessionRepository
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*

class AttendanceManagementServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val notificationService: NotificationService,
    private val backgroundTaskScope: BackgroundTaskScope
) : AttendanceManagementService {

    private val logger = LoggerFactory.getLogger(AttendanceManagementServiceImpl::class.java)

    override suspend fun removeStudentAttendance(
        lecturerId: UUID,
        request: RemoveAttendanceRequest
    ) {
        try {
            request.validate()

            val sessionId = UUID.fromString(request.sessionId)
            val studentId = UUID.fromString(request.studentId)
            // 1. Lecturer must own session
            val ownsSession =
                attendanceSessionRepository.existsByIdAndLecturerId(sessionId, lecturerId)

            if (!ownsSession) {
                throw AuthorizationException("You do not own this attendance session")
            }

            // 2. Get session details before deletion
            val session = attendanceSessionRepository.getSessionDetails(sessionId)
                ?: throw ResourceNotFoundException("Session not found")

            // 3. Attendance must exist
            val attendanceId =
                attendanceRecordRepository.findBySessionAndStudent(sessionId, studentId)
                    ?: throw ResourceNotFoundException("Attendance record not found")

            // 4. Delete attendance
            attendanceRecordRepository.deleteById(attendanceId)

            // 5. Fire notification in background (fire-and-forget)
            backgroundTaskScope.scope.launch {
                try {
                    notificationService.notifyStudentAttendanceRevoked(
                        studentId = studentId,
                        sessionTitle = session.title ?: "No session title",
                        unitCode = session.unit.code,
                        reason = "Your attendance was flagged"
                    )
                } catch (e: Exception) {
                    logger.error("Failed to send attendance revoked notification", e)
                }
            }
        } catch (ex: Exception) {
            logger.error("Failed to remove attendance record", ex)
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to remove attendance record")
            }
        }
    }

    override suspend fun getStudentAttendanceHistory(
        studentId: UUID,
        page: Int,
        size: Int,
        sortDesc: Boolean
    ): StudentAttendanceHistoryResponse {

        try {
            val offset = page * size

            val records = attendanceRecordRepository
                .fetchStudentAttendanceHistory(
                    studentId = studentId,
                    limit = size,
                    offset = offset,
                    sortDesc = sortDesc
                )

            val hasNext = attendanceRecordRepository
                .hasNextStudentAttendanceHistory(
                    studentId = studentId,
                    offset = offset + size
                )

            return StudentAttendanceHistoryResponse(
                page = page,
                size = size,
                hasNext = hasNext,
                records = records
            )
        } catch (ex: Exception) {
            logger.error("Failed to fetch student attendance record", ex)
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to fetch attendance record")
            }
        }
    }

    override suspend fun getStudentAttendanceStats(studentId: UUID): AttendanceStatsResponse {
        try {

            return attendanceRecordRepository.getStudentAttendanceStats(studentId)

        } catch (ex: Exception) {
            logger.error("Failed to fetch attendance stats", ex)
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to fetch attendance stats")
            }
        }
    }

    private fun RemoveAttendanceRequest.validate() {
        if (sessionId.isBlank()) throw ValidationException("Session ID is required")
        if (studentId.isBlank()) throw ValidationException("Student ID is required")
    }

}
