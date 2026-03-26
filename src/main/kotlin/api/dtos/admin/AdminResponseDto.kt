package com.amos_tech_code.api.dtos.admin

import domain.models.AttendanceMethod
import domain.models.DeviceChangeStatus
import domain.models.NotificationType
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
data class AdminAuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val admin: AdminResponse,
    val expiresIn: Long = 3600 // 1 hour
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

// University DTOs
@Serializable
data class UniversityResponse(
    val id: String,
    val name: String,
    val departmentCount: Int = 0,
    val programmeCount: Int = 0,
    val unitCount: Int = 0,
    val createdAt: String
)

// Department DTOs
@Serializable
data class DepartmentResponse(
    val id: String,
    val universityId: String,
    val universityName: String,
    val name: String,
    val programmeCount: Int = 0,
    val createdAt: String
)

// Programme DTOs
@Serializable
data class ProgrammeResponse(
    val id: String,
    val universityId: String,
    val universityName: String,
    val departmentId: String?,
    val departmentName: String?,
    val name: String,
    val isActive: Boolean,
    val unitCount: Int = 0,
    val createdAt: String
)

// Unit DTOs
@Serializable
data class UnitResponse(
    val id: String,
    val universityId: String,
    val universityName: String,
    val departmentId: String?,
    val departmentName: String?,
    val code: String,
    val name: String,
    val isActive: Boolean,
    val programmes: List<ProgrammeInfo> = emptyList(),
    val createdAt: String
)

@Serializable
data class ProgrammeInfo(
    val id: String,
    val name: String,
    val yearOfStudy: Int
)

// List Responses
@Serializable
data class UniversityListResponse(
    val universities: List<UniversityResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class DepartmentListResponse(
    val departments: List<DepartmentResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class ProgrammeListResponse(
    val programmes: List<ProgrammeResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class UnitListResponse(
    val units: List<UnitResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)


@Serializable
data class AcademicTermResponse(
    val id: String,
    val universityId: String,
    val universityName: String,
    val academicYear: String,
    val semester: Int,
    val weekCount: Int,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class AcademicTermListResponse(
    val terms: List<AcademicTermResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class DeviceChangeRequestResponseDto(
    val id: String,
    val studentId: String,
    val studentName: String,
    val studentRegNo: String,
    val studentEmail: String?,
    val oldDeviceId: String,
    val newDeviceInfo: NewDeviceInfoDto,
    val reason: String?,
    val status: DeviceChangeStatus,
    val requestedAt: String,
    val reviewedBy: String?,
    val reviewedAt: String?,
    val rejectionReason: String?
)

@Serializable
data class NewDeviceInfoDto(
    val deviceId: String,
    val model: String,
    val os: String,
    val fcmToken: String?
)

@Serializable
data class DeviceChangeRequestListResponse(
    val requests: List<DeviceChangeRequestResponseDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class SuspiciousActivityResponse(
    val id: String,
    val studentId: String,
    val studentName: String,
    val studentRegNo: String,
    val sessionId: String,
    val sessionTitle: String?,
    val unitCode: String,
    val unitName: String,
    val lecturerId: String?,
    val lecturerName: String?,
    val attendanceMethodUsed: AttendanceMethod,
    val attendedAt: String,
    val isSuspicious: Boolean,
    val suspiciousReason: String?,
    val studentLatitude: Double?,
    val studentLongitude: Double?,
    val distanceFromLecturer: Double?,
    val isLocationVerified: Boolean,
    val deviceId: String?,
    val isDeviceVerified: Boolean
)

@Serializable
data class SuspiciousActivityListResponse(
    val activities: List<SuspiciousActivityResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class SuspiciousActivityStatsResponse(
    val totalSuspicious: Long,
    val pendingReview: Long,
    val reviewedAndConfirmed: Long,
    val reviewedAndCleared: Long,
    val bySeverity: Map<String, Long>,
    val byFlagType: Map<String, Long>
)


@Serializable
data class StoredFileResponse(
    val id: String,
    val fileName: String,
    val fileType: String,
    val fileUrl: String,
    val fileSize: Long?,
    val createdAt: String?,
    val expiresAt: String?,
    val associatedWith: String?,
    val associatedId: String?,
    val isActive: Boolean
)

@Serializable
data class StorageStatsResponse(
    val totalFiles: Long,
    val totalQRCodes: Long,
    val totalReports: Long,
    val totalSizeBytes: Long,
    val orphanedFiles: Long,
    val expiredFiles: Long,
    val storageUsedMB: Double,
    val byType: Map<String, Long>
)

@Serializable
data class CleanupResultResponse(
    val deletedCount: Int,
    val failedCount: Int,
    val freedSpaceMB: Double,
    val details: List<CleanupDetail>
)

@Serializable
data class CleanupDetail(
    val fileName: String,
    val fileType: String,
    val fileUrl: String,
    val reason: String,
    val success: Boolean,
    val error: String?
)


@Serializable
data class NotificationTemplateResponse(
    val type: String,
    val title: String,
    val defaultTitle: String,
    val body: String,
    val defaultBody: String,
    val isEnabled: Boolean,
    val persistToDatabase: Boolean,
    val recipientType: String // "STUDENT", "LECTURER", "BOTH"
)

@Serializable
data class NotificationHistoryResponse(
    val id: String,
    val recipientId: String,
    val recipientName: String,
    val recipientType: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String
)

@Serializable
data class NotificationHistoryListResponse(
    val notifications: List<NotificationHistoryResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class SystemSettingsResponse(
    // Attendance Settings
    val defaultAttendanceDuration: Int = 30,
    val defaultLocationRadius: Int = 50,
    val allowedAttendanceMethods: List<String> = listOf("QR_CODE", "MANUAL_CODE", "LECTURER_MANUAL"),
    val requireLocationForAttendance: Boolean = true,
    val requireDeviceVerification: Boolean = true,

    // Session Settings
    val sessionCodeLength: Int = 6,
    val sessionCodeExpiryMinutes: Int = 30,
    val maxActiveSessionsPerLecturer: Int = 3,

    // Security Settings
    val maxLoginAttempts: Int = 5,
    val lockoutDurationMinutes: Int = 30,
    val passwordExpiryDays: Int = 90,
    val requireStrongPasswords: Boolean = true,
    val sessionTimeoutMinutes: Int = 60,

    // Device Management
    val maxDevicesPerStudent: Int = 2,
    val deviceChangeRequiresApproval: Boolean = true,
    val autoApproveTrustedDevices: Boolean = false,

    // System Preferences
    val systemName: String = "Campus Attendance System",
    val systemEmail: String = "admin@campus.edu",
    val timezone: String = "Africa/Nairobi",
    val dateFormat: String = "dd/MM/yyyy",
    val timeFormat: String = "24h",

    // Notification Settings
    val enablePushNotifications: Boolean = true,
    val enableEmailNotifications: Boolean = false,
    val notificationRetentionDays: Int = 30,

    // Report Settings
    val defaultReportFormat: String = "PDF",
    val autoGenerateReports: Boolean = false,
    val reportRetentionDays: Int = 90,

    // Maintenance Settings
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val lastBackup: String? = null
)

@Serializable
data class AttendanceSettingsDto(
    val defaultAttendanceDuration: Int? = null,
    val defaultLocationRadius: Int? = null,
    val allowedAttendanceMethods: List<String>? = null,
    val requireLocationForAttendance: Boolean? = null,
    val requireDeviceVerification: Boolean? = null
)

@Serializable
data class SecuritySettingsDto(
    val maxLoginAttempts: Int? = null,
    val lockoutDurationMinutes: Int? = null,
    val passwordExpiryDays: Int? = null,
    val requireStrongPasswords: Boolean? = null,
    val sessionTimeoutMinutes: Int? = null
)

@Serializable
data class DeviceSettingsDto(
    val maxDevicesPerStudent: Int? = null,
    val deviceChangeRequiresApproval: Boolean? = null,
    val autoApproveTrustedDevices: Boolean? = null
)

@Serializable
data class SystemPreferencesDto(
    val systemName: String? = null,
    val systemEmail: String? = null,
    val timezone: String? = null,
    val dateFormat: String? = null,
    val timeFormat: String? = null
)

@Serializable
data class NotificationSettingsDto(
    val enablePushNotifications: Boolean? = null,
    val enableEmailNotifications: Boolean? = null,
    val notificationRetentionDays: Int? = null
)

@Serializable
data class ReportSettingsDto(
    val defaultReportFormat: String? = null,
    val autoGenerateReports: Boolean? = null,
    val reportRetentionDays: Int? = null
)

@Serializable
data class MaintenanceSettingsDto(
    val maintenanceMode: Boolean? = null,
    val maintenanceMessage: String? = null
)