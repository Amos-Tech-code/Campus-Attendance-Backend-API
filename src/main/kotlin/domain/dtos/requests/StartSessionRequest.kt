package com.amos_tech_code.domain.dtos.requests

import com.amos_tech_code.domain.models.AttendanceMethod
import kotlinx.serialization.Serializable

@Serializable
data class StartSessionRequest(
    val universityId: String,

    val title: String? = null,                  // e.g. "Week 3 Lecture"
    val attendanceSessionType: SessionTypeRequest, // REGULAR, MAKEUP, SPECIAL
    val weekNumber: Int,                // Academic week number

    val programmeIds: List<String>, // Session may contain multiple programmes
    val unitId: String,

    val allowedMethod: AttendanceMethod,
    val isLocationRequired: Boolean,
    val location: AttendanceLocationRequest? = null, // If location is required this cannot be null
    val radiusMeters: Int? = 50,       // If location is required this cannot be null

    val durationMinutes: Int = 60,
    val scheduledStartTime: String? = null
)

@Serializable
enum class SessionTypeRequest {
    REGULAR,
    MAKEUP,
    SPECIAL
}

@Serializable
data class AttendanceLocationRequest(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class UpdateSessionRequest(
    val title: String? = null,                  // e.g. "Week 3 Lecture"
    val attendanceSessionType: SessionTypeRequest? = null, // REGULAR, MAKEUP, SPECIAL
    val weekNumber: Int? = null,                // Academic week number
    val programmeIds: List<String>? = null,
    val unitId: String? = null,
    val allowedMethod: AttendanceMethod? = null,
    val isLocationRequired: Boolean? = null,
    val location: AttendanceLocationRequest? = null,
    val radiusMeters: Int? = null,
    val durationMinutes: Int? = null,
    val scheduledStartTime: String? = null
)

@Serializable
data class EndSessionRequest(
    val sessionId: String
)
