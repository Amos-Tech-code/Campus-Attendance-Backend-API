package com.amos_tech_code.api.dtos.requests

import domain.models.AttendanceSessionType
import domain.models.ExportFormat
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceExportRequest(
    val universityId: String,
    val programmeId: String,
    val unitId: String,
    val weekRange: String, // e.g., "1-7" or "ALL"
    val sessionType: AttendanceSessionType? = null, // null means ALL
    val yearOfStudy: Int,
    val semester: Int,
    val exportFormat: ExportFormat
)