package com.amos_tech_code.domain.models

import java.util.UUID

data class Admin(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: String = "ADMIN",
    val lastLoginAt: String? = null,
    val isActive: Boolean = true
)