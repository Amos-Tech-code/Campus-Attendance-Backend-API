package com.amos_tech_code.data.database.entities

import com.amos_tech_code.domain.models.StudentEnrollmentSource
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
    val lastSeen = datetime("last_seen").clientDefault { now() }
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex("unique_student_device", studentId, deviceId) }
}


object SuspiciousLoginsTable : Table("suspicious_logins") {
    val id = uuid("id").autoGenerate()
    val studentId = uuid("student_id").references(StudentsTable.id, onDelete = ReferenceOption.CASCADE)
    val attemptedDeviceId = varchar("attempted_device_id", 255)
    val attemptedModel = varchar("attempted_model", 255)
    val attemptedOs = varchar("attempted_os", 255)
    val attemptedFcmToken = varchar("attempted_fcm_token", 255).nullable()
    val createdAt = datetime("created_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
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
    val enrollmentSource = customEnumeration(
        "enrollment_source", "VARCHAR(20)",
        { StudentEnrollmentSource.valueOf( it as String) }, { it.name })
    val isActive = bool("is_active").default(true)

    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("unique_student_programme_term",
            studentId, programmeId, academicTermId)
        index(false, programmeId, academicTermId, yearOfStudy)
    }

}
