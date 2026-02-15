package com.amos_tech_code.domain.services

import com.amos_tech_code.api.dtos.requests.AttendanceExportRequest
import com.amos_tech_code.api.dtos.response.AttendanceExportRecordDto
import com.amos_tech_code.api.dtos.response.AttendanceExportResponseDto
import com.amos_tech_code.api.dtos.response.ExportsListResponseDto
import java.util.UUID

interface AttendanceExportService {

    suspend fun generateAndExportAttendance(
        lecturerId: UUID,
        request: AttendanceExportRequest
    ): AttendanceExportResponseDto

    suspend fun getExportById(exportId: String, lecturerId: UUID): AttendanceExportRecordDto

    suspend fun getExportsByLecturer(
        lecturerId: UUID,
        limit: Int,
        offset: Int
    ): ExportsListResponseDto

}