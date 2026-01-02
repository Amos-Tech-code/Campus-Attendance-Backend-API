package com.amos_tech_code.domain.models

import java.time.LocalDateTime
import java.util.UUID


data class UpdateSessionData(
    val title: String? = null,
    val attendanceSessionType: AttendanceSessionType? = null,
    val weekNumber: Int? = null,
    val programmeIds: List<UUID>? = null,
    val unitId: UUID? = null,
    val allowedMethod: AttendanceMethod? = null,
    val isLocationRequired: Boolean? = null,
    val lecturerLatitude: Double? = null,
    val lecturerLongitude: Double? = null,
    val locationRadius: Int? = null,
    val scheduledStartTime: LocalDateTime? = null,
    val durationMinutes: Int? = null
)
