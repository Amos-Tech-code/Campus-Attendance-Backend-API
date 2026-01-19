package com.amos_tech_code.api.dtos.response

@Serializable
data class StudentAttendanceHistoryResponse(
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
    val records: List<StudentAttendanceRecordDto>
)

@Serializable
data class StudentAttendanceRecordDto(
    val sessionId: String,
    val unitCode: String,
    val unitName: String,
    val sessionTitle: String?,
    val sessionType: AttendanceSessionType,
    val attendanceMethodUsed: AttendanceMethod,
    val status: AttendanceSessionStatus,
    val attendedAt: Long,
    val isSuspicious: Boolean,
    val suspiciousReason: String?
)
