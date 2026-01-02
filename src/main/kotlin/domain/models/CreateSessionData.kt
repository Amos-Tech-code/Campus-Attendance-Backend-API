package com.amos_tech_code.domain.models

import java.time.LocalDateTime
import java.util.UUID

data class CreateSessionData(
    val lecturerId: UUID,
    val universityId: UUID,
    val academicTermId: UUID,
    val title: String?,
    val attendanceSessionType: AttendanceSessionType,
    val weekNumber: Int,
    val unitId: UUID,
    val sessionCode: String,
    val qrCodeUrl: String?,
    val isLocationRequired: Boolean,
    val allowedMethod: AttendanceMethod,
    val lecturerLatitude: Double?,
    val lecturerLongitude: Double?,
    val locationRadius: Int?,
    val scheduledStartTime: LocalDateTime,
    val scheduledEndTime: LocalDateTime,
    val durationMinutes: Int,
    val sessionStatus: AttendanceSessionStatus
)
