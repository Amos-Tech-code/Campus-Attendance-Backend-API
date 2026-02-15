package com.amos_tech_code.domain.models

import domain.models.AttendanceSessionType
import domain.models.ExportFormat
import java.time.LocalDateTime
import java.util.UUID

data class AttendanceReportData(
    val studentId: UUID,
    val regNo: String,
    val fullName: String,
    val weeklyAttendance: List<WeeklyAttendanceData>,
    val totalSessions: Int,
    val attendedSessions: Int,
    val attendancePercentage: Double
)

data class WeeklyAttendanceData(
    val weekNumber: Int,
    val sessionNumber: Int,
    val attended: Boolean,
    val attendedAt: LocalDateTime?
)

data class SessionInfoData(
    val sessionId: UUID,
    val weekNumber: Int,
    val sessionNumber: Int,
    val sessionTitle: String?,
    val scheduledStartTime: LocalDateTime,
    val sessionType: AttendanceSessionType
)

data class StudentInfoData(
    val studentId: UUID,
    val regNo: String,
    val fullName: String
)

data class AttendanceRecordData(
    val sessionId: UUID,
    val studentId: UUID,
    val attendedAt: LocalDateTime
)

// Additional data classes
data class TeachingAssignmentDetails(
    val id: UUID,
    val lecturerId: UUID,
    val universityId: UUID,
    val programmeId: UUID,
    val programmeName: String,
    val unitId: UUID,
    val unitCode: String,
    val unitName: String,
    val yearOfStudy: Int
)

data class AcademicTerm(
    val id: UUID,
    val universityId: UUID,
    val academicYear: String,
    val semester: Int,
    val weekCount: Int,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)

data class AttendanceExportRecord(
    val id: UUID,
    val lecturerId: UUID,
    val teachingAssignmentId: UUID,
    val exportType: ExportFormat,
    val exportFormat: String,
    val academicTermId: UUID,
    val weekRange: String?,
    val fileUrl: String,
    val fileSize: Long,
    val fileName: String,

    val unitName: String,
    val unitCode: String,
    val programmeName: String,
    val academicTermName: String,

    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?
)

// Teaching Assignment domain model
data class LecturerTeachingAssignment(
    val id: UUID,
    val lecturerId: UUID,
    val universityId: UUID,
    val programmeId: UUID,
    val unitId: UUID,
    val academicTermId: UUID,
    val yearOfStudy: Int,
    val lectureDay: String?,
    val lectureTime: String?,
    val lectureVenue: String?,
    val expectedStudents: Int,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)

// Unit with details (including department and university names)
data class UnitWithDetails(
    val unitId: UUID,
    val unitCode: String,
    val unitName: String,
    val departmentId: UUID,
    val departmentName: String,
    val universityId: UUID,
    val universityName: String
)

// Programme with details (including department and university names)
data class ProgrammeWithDetails(
    val programmeId: UUID,
    val programmeName: String,
    val departmentId: UUID,
    val departmentName: String,
    val universityId: UUID,
    val universityName: String
)