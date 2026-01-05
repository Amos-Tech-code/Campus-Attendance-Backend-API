package data.database.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

/**
 * System Master Tables.
 * New records can be added by the lecturer or the system admin.
 */
// Universities
object UniversitiesTable : Table("universities") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255).uniqueIndex()
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
}

// Departments in a university
object DepartmentsTable : Table("departments") {
    val id = uuid("id").autoGenerate()
    val universityId = uuid("university_id").references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex("unique_department_university", universityId, name) }
}

// Programmes offered in a department
object ProgrammesTable : Table("programmes") {
    val id = uuid("id").autoGenerate()
    val universityId = uuid("university_id").references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val departmentId = uuid("department_id").references(DepartmentsTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("unique_programme_university", universityId, name)
    }
}

// Units offered in a programme (or university)
object UnitsTable : Table("units") {
    val id = uuid("id").autoGenerate()
    val universityId = uuid("university_id").references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val departmentId = uuid("department_id").references(DepartmentsTable.id, onDelete = ReferenceOption.CASCADE)
    val code = varchar("code", 50)
    val name = varchar("name", 255)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("unique_unit_code_university", universityId, code)
        index(false, departmentId)
    }
}


object ProgrammeUnitsTable : Table("programme_units") {
    val id = uuid("id").autoGenerate()
    val programmeId = uuid("programme_id")
        .references(ProgrammesTable.id, onDelete = ReferenceOption.CASCADE)
    val unitId = uuid("unit_id")
        .references(UnitsTable.id, onDelete = ReferenceOption.CASCADE)
    val yearOfStudy = integer("year_of_study")
    val semester = integer("semester")
    val createdAt = datetime("created_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("unique_programme_unit_year", programmeId, unitId, yearOfStudy)
    }
}


object AcademicTermsTable : Table("academic_terms") {
    val id = uuid("id").autoGenerate()
    val universityId = uuid("university_id")
        .references(UniversitiesTable.id, onDelete = ReferenceOption.CASCADE)
    val academicYear = varchar("academic_year", 9) // "2024-2025"
    val semester = integer("semester") // 1 or 2
    val weekCount = integer("week_count").default(14) // Typical Kenyan semester weeks
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init { uniqueIndex(universityId, academicYear, semester) }
}