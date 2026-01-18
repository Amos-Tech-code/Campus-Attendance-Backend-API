package com.amos_tech_code.api.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class UpdateStudentProfileRequest(
    val fullName: String,
    val registrationNumber: String
)

@Serializable
data class UpdateLecturerProfileRequest(
    val fullName: String
)
