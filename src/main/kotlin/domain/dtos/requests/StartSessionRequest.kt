package com.amos_tech_code.domain.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class StartSessionRequest(
    val universityId: String,
    val programmeIds: List<String>,
    val unitId: String,
    val allowedMethod: AttendanceMethodRequest,
    val isLocationRequired: Boolean,
    val location: AttendanceLocationRequest? = null, // If location is required location cannot be null
    val radiusMeters: Int = 50,
    val durationMinutes: Int = 60,
    val scheduledStartTime: String? = null
)

@Serializable
data class AttendanceLocationRequest(
    val latitude: Double,
    val longitude: Double
)

@Serializable
enum class AttendanceMethodRequest {
    QR_CODE,       // Students scan QR projected/displayed by lecturer
    MANUAL_CODE,   // Students manually type in a generated session code
    ANY            // Students can use both methods
}


@Serializable
data class UpdateSessionRequest(
    val programmeIds: List<String>? = null,
    val unitId: String? = null,
    val allowedMethod: AttendanceMethodRequest? = null,
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
