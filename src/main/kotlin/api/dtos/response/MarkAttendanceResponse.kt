package com.amos_tech_code.domain.dtos.response

import domain.models.FlagType
import domain.models.SeverityLevel
import kotlinx.serialization.Serializable
import kotlin.Boolean

// Enhanced Response DTO
@Serializable
data class MarkAttendanceResponse(
    val success: kotlin.Boolean,
    val sessionId: String,
    val programmeId: String? = null,
    val verification: VerificationResult,
    val flags: List<AttendanceFlag> = emptyList(),
    val requiresProgrammeSelection: kotlin.Boolean = false,
    val availableProgrammes: List<ProgrammeInfoResponse> = emptyList(),
    val attendedAt: String,
    val message: String? = null
)

@Serializable
data class VerificationResult(
    val locationVerified: kotlin.Boolean,
    val deviceVerified: kotlin.Boolean,
    val methodVerified: kotlin.Boolean,
    val attendanceTimeVerified: kotlin.Boolean,
    val overallVerified: kotlin.Boolean
)

@Serializable
data class AttendanceFlag(
    val type: FlagType,
    val message: String,
    val severity: SeverityLevel
)

@Serializable
data class ProgrammeInfoResponse(
    val id: String,
    val name: String,
    val department: String,
    val yearOfStudy: Int
)

@Serializable
data class VerifyAttendanceResponse(
    val requiresProgrammeSelection: kotlin.Boolean,
    val availableProgrammes: List<ProgrammeInfoResponse>,
    val requiresLocation: Boolean,
    val sessionInfo: SessionInfo
)

@Serializable
data class SessionInfo(
    val sessionId: String,
    val unitName: String,
    val unitCode: String,
    val lecturerName: String
)
