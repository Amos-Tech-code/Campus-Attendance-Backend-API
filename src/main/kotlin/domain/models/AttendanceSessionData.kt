package com.amos_tech_code.domain.models

import java.time.LocalDateTime
import java.util.UUID

// Data classes for the enhanced functionality
data class AttendanceSession(
    val id: UUID,
    val sessionCode: String,
    val unitId: UUID,
    val universityId: UUID,
    val lecturerId: UUID,
    val isLocationRequired: Boolean,
    val lecturerLatitude: Double?,
    val lecturerLongitude: Double?,
    val locationRadius: Int?,
    val unitName: String,
    val unitCode: String,
    val lecturerName: String
)

data class SessionProgramme(
    val programmeId: UUID,
    val programmeName: String,
    val departmentName: String,
    val yearOfStudy: Int
)

data class PreviousAttendance(
    val programmeId: UUID,
    val attendedAt: LocalDateTime
)