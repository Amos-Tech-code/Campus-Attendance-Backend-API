package com.amos_tech_code.api.dtos.admin

import kotlinx.serialization.Serializable

@Serializable
data class CreateAdminRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String = "ADMIN"
)

@Serializable
data class UpdateAdminRequest(
    val fullName: String?,
    val role: String?,
    val isActive: Boolean?
)

@Serializable
data class UpdateStudentRequest(
    val fullName: String?,
    val isActive: Boolean?
)

@Serializable
data class UpdateLecturerRequest(
    val fullName: String?,
    val isActive: Boolean?
)