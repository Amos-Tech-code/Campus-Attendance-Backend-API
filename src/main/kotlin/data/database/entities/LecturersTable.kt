package data.database.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

// Lecturer basic info
object LecturersTable : Table("lecturers") {
    val id = uuid("id").autoGenerate()
    val email = varchar("email", 255).uniqueIndex()
    val fullName = varchar("full_name", 255).nullable()
    val isProfileComplete = bool("is_profile_complete").default(false)
    val lastLoginAt = datetime("last_login_at").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
}

// Link lecturer to universities
object LecturerUniversitiesTable : Table("lecturer_universities") {
    val id = uuid("id").autoGenerate()
    val lecturerId = uuid("lecturer_id").references(LecturersTable.id, onDelete = ReferenceOption.CASCADE)
    val universityId = uuid("university_id").references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init { uniqueIndex("unique_lecturer_university", lecturerId, universityId) }
}

// Lecturer teaching assignments to units
object LecturerTeachingAssignmentsTable : Table("lecturer_teaching_assignments") {
    val id = uuid("id").autoGenerate()
    val lecturerId = uuid("lecturer_id").references(LecturersTable.id, onDelete = ReferenceOption.CASCADE)
    val universityId = uuid("university_id").references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val programmeId = uuid("programme_id").references(ProgrammesTable.id, onDelete = ReferenceOption.CASCADE)
    val unitId = uuid("unit_id").references(UnitsTable.id, onDelete = ReferenceOption.CASCADE)
    val academicTermId = uuid("academic_term_id").references(AcademicTermsTable.id, onDelete = ReferenceOption.CASCADE)
    val yearOfStudy = integer("year_of_study")
    val lectureDay = varchar("lecture_day", 20).nullable()
    val lectureTime = varchar("lecture_time", 20).nullable()
    val lectureVenue = varchar("lecture_venue", 100).nullable()
    val expectedStudents = integer("expected_students").default(0)

    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(
            "unique_lecturer_assignment",
            lecturerId, programmeId, unitId, academicTermId, yearOfStudy
        )
    }
}
