package com.amos_tech_code.api.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class AdminResponse(
    val id: String,
    val email: String,
    val fullName: String,
    val lastLoginAt: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class AdminLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AdminAuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val admin: AdminResponse,
    val expiresIn: Long = 3600 // 1 hour
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class DashboardStatsDto(
    val totalStudents: Long,
    val totalLecturers: Long,
    val totalUniversities: Long,
    val totalProgrammes: Long,
    val totalSessions: Long,
    val todaySessions: Long,
    val totalAttendance: Long,
    val recentActivities: List<ActivityLogDto>
)

@Serializable
data class ActivityLogDto(
    val id: String,
    val type: String, // "STUDENT_LOGIN", "LECTURER_LOGIN", "SESSION_STARTED", "ATTENDANCE_MARKED"
    val description: String,
    val timestamp: String,
    val performedBy: String
)