package com.amos_tech_code.api.dtos.response

import domain.models.ExportFormat
import kotlinx.serialization.Serializable


@Serializable
data class AttendanceExportResponseDto(
    val exportId: String,
    val fileUrl: String,
    val fileName: String,
    val fileSize: Long,
    val exportFormat: ExportFormat,
    val expiresAt: String?,
    val message: String
)

@Serializable
data class ExportsListResponseDto(
    val exports: List<AttendanceExportRecordDto>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class AttendanceExportRecordDto(
    val exportId: String,
    val fileName: String,
    val fileUrl: String,
    val fileSize: Long,
    val exportFormat: String,
    val weekRange: String?,
    val createdAt: String,
    val expiresAt: String?,
    val unitName: String?,
    val unitCode: String?,
    val programmeName: String?,
    val academicTerm: String?
)