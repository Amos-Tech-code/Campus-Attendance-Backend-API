package com.amos_tech_code.services

import com.amos_tech_code.domain.dtos.requests.LecturerMarkAttendanceRequest
import com.amos_tech_code.domain.dtos.requests.MarkAttendanceRequest
import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.amos_tech_code.domain.dtos.response.MarkAttendanceResponse
import java.util.UUID

interface MarkAttendanceService {

    suspend fun lecturerSignAttendance(
        lecturerId: UUID,
        request: LecturerMarkAttendanceRequest
    ): Boolean

    suspend fun processAttendanceMarking(studentId: UUID, request: MarkAttendanceRequest): MarkAttendanceResponse

}
