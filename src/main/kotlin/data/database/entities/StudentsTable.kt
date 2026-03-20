package data.database.entities

import domain.models.DeviceChangeStatus
import domain.models.DeviceStatus
import domain.models.StudentEnrollmentSource
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

object StudentsTable : Table("students") {
    val id = uuid("id").autoGenerate()
    val registrationNumber = varchar("reg_no", 255)
    val fullName = varchar("full_name", 255)
    val lastLoginAt = datetime("last_login_at").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex("unique_reg_no", registrationNumber) }
}

// Devices registered by students
object DevicesTable : Table("student_devices") {
    val id = uuid("id").autoGenerate()
    val studentId = uuid("student_id").references(StudentsTable.id, onDelete = ReferenceOption.CASCADE)
    val deviceId = varchar("device_id", 255)
    val deviceModel = varchar("model", 100)
    val os = varchar("os", 50)
    val fcmToken = varchar("fcm_token", 255).nullable()

    // Device status
    val status = enumerationByName<DeviceStatus>("status", 20).default(DeviceStatus.ACTIVE)

    // Timestamps
    val lastSeen = datetime("last_seen").clientDefault { now() }
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("unique_student_device", studentId, deviceId)
        index(false, studentId, status)
        index(false, deviceId)
    }
}

object DeviceChangeRequestsTable : Table("device_change_requests") {
    val id = uuid("id").autoGenerate()
    val studentId = uuid("student_id")
        .references(StudentsTable.id, onDelete = ReferenceOption.CASCADE)
    val oldDeviceId = varchar("old_device_id", 255)
    val newDeviceId = varchar("new_device_id", 255)
    val newDeviceModel = varchar("new_device_model", 100)
    val newDeviceOS = varchar("new_device_os", 50)
    val newFcmToken = varchar("new_fcm_token", 255).nullable()
    val reason = text("reason").nullable()

    val status = enumerationByName<DeviceChangeStatus>("status", 20)
        .default(DeviceChangeStatus.PENDING)

    val requestedAt = datetime("requested_at").clientDefault { now() }
    val reviewedBy = uuid("reviewed_by").references(LecturersTable.id).nullable()
    val reviewedAt = datetime("reviewed_at").nullable()
    val rejectionReason = text("rejection_reason").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, studentId, status)
        index(false, reviewedBy, status)
        index(false, requestedAt)
    }
}

object StudentEnrollmentsTable : Table("student_enrollments") {
    val id = uuid("id").autoGenerate()
    val studentId = uuid("student_id")
        .references(StudentsTable.id, onDelete = ReferenceOption.CASCADE)
    val universityId = uuid("university_id")
        .references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val programmeId = uuid("programme_id")
        .references(ProgrammesTable.id, onDelete = ReferenceOption.CASCADE)
    val academicTermId = uuid("academic_term_id")
        .references(AcademicTermsTable.id, onDelete = ReferenceOption.CASCADE)

    val yearOfStudy = integer("year_of_study")
    val enrollmentDate = datetime("enrollment_date").clientDefault { now() }
    val enrollmentSource = enumerationByName<StudentEnrollmentSource>("enrollment_source", 20)

    val isActive = bool("is_active").default(true)

    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("unique_student_programme_term",
            studentId, programmeId, academicTermId)
        index(false, programmeId, academicTermId, yearOfStudy)
    }

}
