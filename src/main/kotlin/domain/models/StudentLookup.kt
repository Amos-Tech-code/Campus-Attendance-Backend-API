package com.amos_tech_code.domain.models

import java.time.LocalDateTime
import java.util.UUID

data class TeachingUnit(
    val unitId: UUID,
    val unitCode: String,
    val unitName: String
)


data class StudentEnrollment(
    val id: UUID,
    val studentId: UUID,
    val universityId: UUID,
    val universityName: String,
    val programmeId: UUID,
    val programmeName: String,
    val academicTermId: UUID,
    val academicYear: String,
    val semester: Int,
    val yearOfStudy: Int,
    val enrollmentDate: LocalDateTime,
    val isActive: Boolean
)

data class StudentUnit(
    val unitId: UUID,
    val unitCode: String,
    val unitName: String
)

data class AttendanceStats(
    val attended: Int,
    val total: Int
)

data class UnitAttendance(
    val unitId: UUID,
    val unitCode: String,
    val unitName: String,
    val attended: Int,
    val total: Int
)

data class RecentAttendance(
    val sessionId: UUID,
    val sessionTitle: String,
    val unitCode: String,
    val unitName: String,
    val attendedAt: LocalDateTime,
    val attendanceMethod: String,
    val isSuspicious: Boolean
)