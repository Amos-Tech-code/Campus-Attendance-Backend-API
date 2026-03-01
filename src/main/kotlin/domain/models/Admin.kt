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

data class DashboardStats(
    val totalStudents: Long,
    val totalLecturers: Long,
    val totalUniversities: Long,
    val totalProgrammes: Long,
    val totalSessions: Long,
    val todaySessions: Long,
    val totalAttendance: Long,
    val recentActivities: List<ActivityLog>
)

data class ActivityLog(
    val id: String,
    val type: String,
    val description: String,
    val timestamp: String,
    val performedBy: String
)