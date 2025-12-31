package com.amos_tech_code.domain.dtos.response

import com.amos_tech_code.domain.models.AttendanceMethod
import com.amos_tech_code.domain.models.AttendanceSessionStatus
import kotlinx.serialization.Serializable

@Serializable
data class SessionResponse(
    val sessionId: String,
    val sessionCode: String, // 6-digit code
    val qrCodeUrl: String?,
    val method: AttendanceMethod,
    val universityId: String,
    val programmes: List<ProgrammeInfo>,
    val unit: UnitInfo,
    val location: LocationInfo?,
    val timeInfo: TimeInfo,
    val status: AttendanceSessionStatus
)

@Serializable
data class ProgrammeInfo(
    val id: String,
    val name: String,
    val department: String,
)

@Serializable
data class UnitInfo(
    val id: String,
    val name: String,
    val code: String
)

@Serializable
data class LocationInfo(
    val latitude: Double?,
    val longitude: Double?,
    val radiusMeters: Int
)

@Serializable
data class TimeInfo(
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int
)