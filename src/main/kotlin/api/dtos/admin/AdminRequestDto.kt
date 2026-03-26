package com.amos_tech_code.api.dtos.admin

import domain.models.NotificationType
import kotlinx.serialization.Serializable

@Serializable
data class AdminLoginRequest(
    val email: String,
    val password: String
)
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

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


@Serializable
data class CreateUniversityRequest(
    val name: String
)

@Serializable
data class UpdateUniversityRequest(
    val name: String
)
@Serializable
data class CreateDepartmentRequest(
    val universityId: String,
    val name: String
)

@Serializable
data class UpdateDepartmentRequest(
    val name: String
)

@Serializable
data class CreateProgrammeRequest(
    val universityId: String,
    val departmentId: String,
    val name: String,
    val isActive: Boolean = true
)

@Serializable
data class UpdateProgrammeRequest(
    val name: String?,
    val departmentId: String?,
    val isActive: Boolean?
)


@Serializable
data class CreateUnitRequest(
    val universityId: String,
    val departmentId: String,
    val code: String,
    val name: String,
    val isActive: Boolean = true
)

@Serializable
data class UpdateUnitRequest(
    val code: String?,
    val name: String?,
    val departmentId: String?,
    val isActive: Boolean?
)

@Serializable
data class LinkUnitToProgrammeRequest(
    val programmeId: String,
    val yearOfStudy: Int,
    val semester: Int = 1
)

@Serializable
data class CreateAcademicTermRequest(
    val universityId: String,
    val academicYear: String,
    val semester: Int,
    val weekCount: Int = 14,
    val isActive: Boolean = true
)

@Serializable
data class UpdateAcademicTermRequest(
    val academicYear: String?,
    val semester: Int?,
    val weekCount: Int?,
    val isActive: Boolean?
)


@Serializable
data class AdminDeviceChangeApprovalRequest(
    val approve: Boolean,
    val rejectionReason: String? = null
)

@Serializable
data class ReviewSuspiciousActivityRequest(
    val isSuspicious: Boolean,
    val notes: String? = null
)
@Serializable
data class CleanupRequest(
    val cleanupType: String, // "orphaned", "expired", "all", "manual"
    val fileIds: List<String>? = null,
    val olderThanDays: Int? = 30
)


@Serializable
data class SendBroadcastRequest(
    val title: String,
    val message: String,
    val type: NotificationType,
    val recipientType: String, // "STUDENTS", "LECTURERS", "ALL"
    val persistToDatabase: Boolean = true
)

@Serializable
data class UpdateNotificationTemplateRequest(
    val title: String?,
    val body: String?,
    val isEnabled: Boolean?
)


@Serializable
data class UpdateSystemSettingsRequest(
    val attendanceSettings: AttendanceSettingsDto? = null,
    val securitySettings: SecuritySettingsDto? = null,
    val deviceSettings: DeviceSettingsDto? = null,
    val systemPreferences: SystemPreferencesDto? = null,
    val notificationSettings: NotificationSettingsDto? = null,
    val reportSettings: ReportSettingsDto? = null,
    val maintenanceSettings: MaintenanceSettingsDto? = null
)