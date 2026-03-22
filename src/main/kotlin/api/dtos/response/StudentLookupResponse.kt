package com.amos_tech_code.api.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class StudentLookupResponse(
    val studentInfo: StudentBasicInfo,
    val deviceInfo: DeviceLookupInfo,
    val enrollmentInfo: List<EnrollmentInfo>,
    val attendanceSummary: AttendanceSummary,
    val pendingDeviceChange: PendingDeviceChangeInfo?,
    val recentActivity: List<RecentActivityInfo>
)

@Serializable
data class StudentBasicInfo(
    val studentId: String,
    val fullName: String,
    val registrationNumber: String,
    val isActive: Boolean,
    val lastLoginAt: String?
)

@Serializable
data class DeviceLookupInfo(
    val deviceId: String?,
    val deviceModel: String?,
    val deviceStatus: String?,
    val lastSeen: String?
)

@Serializable
data class EnrollmentInfo(
    val programmeName: String,
    val yearOfStudy: Int,
    val academicTerm: String,
    val enrollmentDate: String,
    val units: List<UnitInfo>
)

@Serializable
data class UnitInfo(
    val unitId: String,
    val unitCode: String,
    val unitName: String,
    val sessionsAttended: Int,
    val totalSessions: Int,
    val attendancePercentage: Double,
    val isTeaching: Boolean // Whether the requesting lecturer teaches this unit
)

@Serializable
data class AttendanceSummary(
    val overallAttendance: Double,
    val totalSessions: Int,
    val sessionsAttended: Int,
    val suspiciousActivities: Int,
    val lastAttendanceDate: String?,
    val attendanceByUnit: List<UnitAttendanceInfo>
)

@Serializable
data class UnitAttendanceInfo(
    val unitCode: String,
    val unitName: String,
    val attended: Int,
    val total: Int,
    val percentage: Double
)

@Serializable
data class PendingDeviceChangeInfo(
    val requestId: String,
    val requestedAt: String,
    val newDeviceModel: String,
    val newDeviceOS: String,
    val reason: String?
)

@Serializable
data class RecentActivityInfo(
    val activityType: String, // "ATTENDANCE", "DEVICE_CHANGE", etc.
    val description: String,
    val timestamp: String,
    val details: Map<String, String>?
)