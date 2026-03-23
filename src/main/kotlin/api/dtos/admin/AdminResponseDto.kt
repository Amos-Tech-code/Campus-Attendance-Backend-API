package com.amos_tech_code.api.dtos.admin

import kotlinx.serialization.Serializable

@Serializable
data class AdminResponse(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String = "ADMIN",
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
    val adminName: String,
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


@Serializable
data class LecturerResponse(
    val id: String,
    val email: String,
    val fullName: String?,
    val isProfileComplete: Boolean,
    val isActive: Boolean,
    val lastLoginAt: String?,
    val universities: List<UniversityInfo> = emptyList(),
    val teachingAssignments: Int = 0
)

@Serializable
data class UniversityInfo(
    val id: String,
    val name: String
)


@Serializable
data class LecturerListResponse(
    val lecturers: List<LecturerResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)


@Serializable
data class StudentResponse(
    val id: String,
    val registrationNumber: String,
    val fullName: String,
    val isActive: Boolean,
    val lastLoginAt: String?,
    val enrollments: List<EnrollmentInfoAdmin> = emptyList(),
    val devices: Int = 0
)

@Serializable
data class EnrollmentInfoAdmin(
    val programmeName: String,
    val universityName: String,
    val academicTerm: String,
    val yearOfStudy: Int,
    val enrollmentDate: String
)

@Serializable
data class StudentListResponse(
    val students: List<StudentResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)