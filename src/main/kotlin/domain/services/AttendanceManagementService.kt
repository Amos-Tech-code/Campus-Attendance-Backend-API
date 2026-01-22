package com.amos_tech_code.domain.services

import com.amos_tech_code.api.dtos.requests.RemoveAttendanceRequest
import com.amos_tech_code.api.dtos.response.StudentAttendanceHistoryResponse
import java.util.UUID

interface AttendanceManagementService {

    suspend fun removeStudentAttendance(
        lecturerId: UUID,
        request: RemoveAttendanceRequest
    )

    suspend fun getStudentAttendanceHistory(
        studentId: UUID,
        page: Int,
        size: Int,
        sortDesc: Boolean
    ): StudentAttendanceHistoryResponse
}