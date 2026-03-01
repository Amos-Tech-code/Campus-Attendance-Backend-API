package com.amos_tech_code.domain.models

import java.time.LocalDateTime
import java.util.UUID

data class Lecturer(
    val id: UUID,
    val email: String,
    val name: String? = null,
    val fcmToken: String? = null,
    val isProfileComplete: Boolean = false,
    val lastLoginAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
