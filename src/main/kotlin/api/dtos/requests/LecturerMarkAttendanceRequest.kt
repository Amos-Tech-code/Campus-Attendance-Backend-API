package com.amos_tech_code.domain.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class LecturerMarkAttendanceRequest(
    val sessionId: String,
    val studentRegNo: String,
)