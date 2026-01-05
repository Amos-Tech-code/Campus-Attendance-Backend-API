package data.database.entities

import domain.models.AttendanceMethod
import domain.models.AttendanceSessionStatus
import domain.models.AttendanceSessionType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

// Attendance sessions started by lecturers
object AttendanceSessionsTable : Table("attendance_sessions") {
    val id = uuid("id").autoGenerate()
    val lecturerId = uuid("lecturer_id")
        .references(LecturersTable.id, onDelete = ReferenceOption.CASCADE)
    val universityId = uuid("university_id")
        .references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val unitId = uuid("unit_id")
        .references(UnitsTable.id, onDelete = ReferenceOption.CASCADE)
    val academicTermId = uuid("academic_term_id")
        .references(AcademicTermsTable.id, onDelete = ReferenceOption.CASCADE)

    // Add these fields for multi-programme support
    val sessionTitle = varchar("session_title", 255).nullable() // e.g., "Math 101 - Week 5 Lecture"
    val attendanceSessionType = enumerationByName<AttendanceSessionType>("session_type", 20).default(AttendanceSessionType.REGULAR)

    // Weekly session tracking
    val weekNumber = integer("week_number")
    val sessionNumber = integer("session_number").default(1)

    // Session codes
    val sessionCode = varchar("session_code", 6)
    val qrCodeUrl = text("qr_code_url").nullable() // Generated QR code URL

    // Configuration
    val allowedMethod = enumerationByName<AttendanceMethod>("allowed_method", 20)
    val isLocationRequired = bool("is_location_required")
    val lecturerLatitude = double("lecturer_latitude").nullable()
    val lecturerLongitude = double("lecturer_longitude").nullable()
    val locationRadius = integer("location_radius").nullable().default(50)

    // Time management
    val durationMinutes = integer("duration_minutes").default(30)
    val scheduledStartTime = datetime("scheduled_start_time")
    val scheduledEndTime = datetime("scheduled_end_time")

    // Status
    val status = enumerationByName<AttendanceSessionStatus>("status", 20)

    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
    init {
        index(false,sessionCode, status)
        index(false, lecturerId, universityId, status)
        index(false, unitId, academicTermId, weekNumber)
        uniqueIndex("unique_lecturer_unit_week_session",
            lecturerId, unitId, academicTermId, weekNumber, sessionNumber)
    }
}

// Session-Programme Mapping (Supports multiple programmes per session)
object SessionProgrammesTable : Table("session_programmes") {
    val id = uuid("id").autoGenerate()
    val sessionId = uuid("session_id").references(AttendanceSessionsTable.id, onDelete = ReferenceOption.CASCADE)
    val programmeId = uuid("programme_id").references(ProgrammesTable.id, onDelete = ReferenceOption.CASCADE)
    val yearOfStudy = integer("year_of_study") // Useful for multi-year sessions
    val createdAt = datetime("created_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init { uniqueIndex("unique_session_programme", sessionId, programmeId) }
}

// Attendance records per student
object AttendanceRecordsTable : Table("attendance_records") {
    val id = uuid("id").autoGenerate()

    val sessionId = uuid("session_id")
        .references(AttendanceSessionsTable.id, onDelete = ReferenceOption.CASCADE)

    val studentId = uuid("student_id")
        .references(StudentsTable.id, onDelete = ReferenceOption.CASCADE)

    val attendanceMethodUsed = enumerationByName<AttendanceMethod>("attendance_method_used", 20)

    val studentLatitude = double("student_latitude").nullable()
    val studentLongitude = double("student_longitude").nullable()
    val distanceFromLecturer = double("distance_from_lecturer").nullable()
    val isLocationVerified = bool("is_location_verified").default(false)

    val deviceId = varchar("device_id", 255).nullable()
    val isDeviceVerified = bool("is_device_verified").default(false)

    val isSuspicious = bool("is_suspicious").default(false)
    val suspiciousReason = text("suspicious_reason").nullable()

    val attendedAt = datetime("attended_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("unique_student_session", sessionId, studentId)
        index(false, studentId)
        index(false, sessionId)
        index(false, deviceId)
    }
}

object AttendanceExportsTable : Table("attendance_exports") {
    val id = uuid("id").autoGenerate()
    val lecturerId = uuid("lecturer_id")
        .references(LecturersTable.id, onDelete = ReferenceOption.CASCADE)
    val teachingAssignmentId = uuid("teaching_assignment_id")
        .references(LecturerTeachingAssignmentsTable.id, onDelete = ReferenceOption.CASCADE)

    val exportType = varchar("export_type", 20) // PDF, EXCEL, CSV
    val exportFormat = varchar("export_format", 50) // "Weekly", "Semester", "Custom"
    val academicTermId = uuid("academic_term_id")
        .references(AcademicTermsTable.id, onDelete = ReferenceOption.CASCADE)
    val weekRange = varchar("week_range", 50).nullable() // "Week 1-7"

    val fileUrl = text("file_url")
    val fileSize = long("file_size")
    val fileName = varchar("file_name", 255)

    val createdAt = datetime("created_at").clientDefault { now() }
    val expiresAt = datetime("expires_at").nullable() // Auto-cleanup

    override val primaryKey = PrimaryKey(id)
    init {
        index(false, lecturerId, createdAt)
        index(false, teachingAssignmentId, academicTermId)
    }
}

object AttendanceSummariesTable : Table("attendance_summaries") {
    val id = uuid("id").autoGenerate()
    val teachingAssignmentId = uuid("teaching_assignment_id")
        .references(LecturerTeachingAssignmentsTable.id, onDelete = ReferenceOption.CASCADE)
    val studentId = uuid("student_id")
        .references(StudentsTable.id, onDelete = ReferenceOption.CASCADE)
    val academicTermId = uuid("academic_term_id")
        .references(AcademicTermsTable.id, onDelete = ReferenceOption.CASCADE)

    val totalSessions = integer("total_sessions").default(0)
    val attendedSessions = integer("attended_sessions").default(0)
    val attendancePercentage = double("attendance_percentage").default(0.0)

    val lastCalculated = datetime("last_calculated").clientDefault { now() }
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex("unique_student_teaching_summary",
            studentId, teachingAssignmentId, academicTermId)
        index(false, teachingAssignmentId, attendancePercentage)
    }
}