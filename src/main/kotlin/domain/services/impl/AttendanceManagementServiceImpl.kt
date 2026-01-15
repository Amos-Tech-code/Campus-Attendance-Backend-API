package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.requests.RemoveAttendanceRequest
import com.amos_tech_code.data.repository.AttendanceRecordRepository
import com.amos_tech_code.domain.services.AttendanceManagementService
import com.amos_tech_code.utils.*
import data.repository.AttendanceSessionRepository
import java.util.*

class AttendanceManagementServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository
) : AttendanceManagementService {

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

            // 2. Attendance must exist
            val attendanceId =
                attendanceRecordRepository.findBySessionAndStudent(sessionId, studentId)
                    ?: throw ResourceNotFoundException("Attendance record not found")

            // 3. Delete attendance
            attendanceRecordRepository.deleteById(attendanceId)

        } catch (ex: Exception) {
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to remove attendance record")
            }
        }
    }

    private fun RemoveAttendanceRequest.validate() {
        if (sessionId.isBlank()) throw ValidationException("Session ID is required")
        if (studentId.isBlank()) throw ValidationException("Student ID is required")
    }

}
