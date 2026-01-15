package com.amos_tech_code.api.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class RemoveAttendanceRequest(
    val sessionId: String,
    val studentId: String
)
