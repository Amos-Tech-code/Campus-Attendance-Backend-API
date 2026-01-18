package com.amos_tech_code.domain.services

import com.amos_tech_code.api.dtos.requests.RemoveAttendanceRequest
import java.util.UUID

interface AttendanceManagementService {

    suspend fun removeStudentAttendance(
        lecturerId: UUID,
        request: RemoveAttendanceRequest
    )
}