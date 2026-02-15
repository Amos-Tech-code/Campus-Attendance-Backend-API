package com.amos_tech_code.domain.models

import api.dtos.response.AttendanceFlag
import api.dtos.response.MarkAttendanceResponse
import domain.models.AttendanceMethod
import java.time.LocalDateTime
import java.util.UUID

data class AttendanceSession(
    val id: UUID,
    val sessionCode: String,
    val unitId: UUID,
    val universityId: UUID,
    val academicTermId: UUID,
    val lecturerId: UUID,
    val allowedMethod: AttendanceMethod,
    val isLocationRequired: Boolean,
    val lecturerLatitude: Double?,
    val lecturerLongitude: Double?,
    val locationRadius: Int?,
    val unitName: String,
    val unitCode: String,
    val lecturerName: String,
    val scheduledStartTime: LocalDateTime,
    val scheduledEndTime: LocalDateTime
)

data class SessionProgramme(
    val programmeId: UUID,
    val programmeName: String,
    val departmentName: String,
    val yearOfStudy: Int
)


data class ProgrammeResolution(
    val programmeId: UUID? = null,
    val requiresSelection: Boolean = false,
    val selectionResponse: MarkAttendanceResponse? = null
)

data class VerificationOutcome(
    val verified: Boolean,
    val flag: AttendanceFlag? = null
)

data class LocationVerification(
    val verified: Boolean,
    val distance: Double,
    val flag: AttendanceFlag? = null
)