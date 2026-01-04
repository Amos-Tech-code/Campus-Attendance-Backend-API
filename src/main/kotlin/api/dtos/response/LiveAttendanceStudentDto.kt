package api.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class LiveAttendanceSnapshot(
    val sessionId: String,
    val programmes: List<ProgrammeAttendanceDto>
)

@Serializable
data class ProgrammeAttendanceDto(
    val programmeId: String,
    val programmeName: String,
    val yearOfStudy: Int,
    val students: List<LiveAttendanceStudentDto>
)

@Serializable
data class LiveAttendanceStudentDto(
    val studentId: String,
    val regNo: String,
    val name: String,
    val attendedAt: String,
    val isSuspicious: Boolean,
    val suspiciousReason: String?
)

@Serializable
data class AttendanceMarkedEventDto(
    val sessionId: String,
    val programmeId: String,
    val student: LiveAttendanceStudentDto
)