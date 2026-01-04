package com.amos_tech_code.domain.dtos.response

import domain.models.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class StudentAuthResponse(
    val token: String,
    val fullName: String,
    val regNumber: String,
    val userType: UserRole,
)
