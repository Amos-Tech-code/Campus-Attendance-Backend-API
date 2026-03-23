package com.amos_tech_code.api.dtos.admin

import domain.models.AttendanceMethod
import domain.models.DeviceChangeStatus
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

@Serializable
data class CreateUniversityRequest(
    val name: String
)

@Serializable
data class UpdateUniversityRequest(
    val name: String
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

@Serializable
data class CreateDepartmentRequest(
    val universityId: String,
    val name: String
)

@Serializable
data class UpdateDepartmentRequest(
    val name: String
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
data class AdminDeviceChangeApprovalRequest(
    val approve: Boolean,
    val rejectionReason: String? = null
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
data class ReviewSuspiciousActivityRequest(
    val isSuspicious: Boolean,
    val notes: String? = null
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
data class CleanupRequest(
    val cleanupType: String, // "orphaned", "expired", "all", "manual"
    val fileIds: List<String>? = null,
    val olderThanDays: Int? = 30
)