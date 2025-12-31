package com.amos_tech_code.data.database

import com.amos_tech_code.config.AppConfig
import com.amos_tech_code.data.database.entities.AcademicTermsTable
import com.amos_tech_code.data.database.entities.AttendanceExportsTable
import com.amos_tech_code.data.database.entities.AttendanceRecordsTable
import com.amos_tech_code.data.database.entities.AttendanceSessionsTable
import com.amos_tech_code.data.database.entities.AttendanceSummariesTable
import com.amos_tech_code.data.database.entities.DepartmentsTable
import com.amos_tech_code.data.database.entities.DevicesTable
import com.amos_tech_code.data.database.entities.LecturerTeachingAssignmentsTable
import com.amos_tech_code.data.database.entities.LecturerUniversitiesTable
import com.amos_tech_code.data.database.entities.LecturersTable
import com.amos_tech_code.data.database.entities.ProgrammeUnitsTable
import com.amos_tech_code.data.database.entities.ProgrammesTable
import com.amos_tech_code.data.database.entities.SessionProgrammesTable
import com.amos_tech_code.data.database.entities.StudentEnrollmentsTable
import com.amos_tech_code.data.database.entities.StudentsTable
import com.amos_tech_code.data.database.entities.SuspiciousLoginsTable
import com.amos_tech_code.data.database.entities.UnitsTable
import com.amos_tech_code.data.database.entities.UniversitiesTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime.now

object DatabaseFactory {

    val config = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl =
            "jdbc:postgresql://${AppConfig.DB_HOST}:${AppConfig.DB_PORT}/${AppConfig.DB_NAME}?sslmode=require"
        username = AppConfig.DB_USER
        password = AppConfig.DB_PASSWORD

        // Connection pool settings
        maximumPoolSize = 20

        // Transactions
        isAutoCommit = false  // Let's data.repository layer manage transactions
    }

    fun init() {
        try {
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)

            transaction {
                // Create tables if they don't exist
                SchemaUtils.createMissingTablesAndColumns(
                    // System Master Tables
                    UniversitiesTable,
                    AcademicTermsTable,
                    ProgrammesTable,
                    DepartmentsTable,
                    UnitsTable,
                    ProgrammeUnitsTable,

                    // Students
                    StudentsTable,
                    DevicesTable,
                    SuspiciousLoginsTable,
                    StudentEnrollmentsTable,

                    // Lecturers
                    LecturersTable,
                    LecturerUniversitiesTable,
                    LecturerTeachingAssignmentsTable,

                    // Attendance
                    AttendanceSessionsTable,
                    SessionProgrammesTable,
                    AttendanceRecordsTable,
                    AttendanceExportsTable,
                    AttendanceSummariesTable

                )

                // updateAdminPassword()
            }
        } catch (e: Exception) {
            println("Database initialization failed: ${e.message}")
            throw e
        }
    }

}


fun Application.migrateDatabase() {
    // This function can be used for future database migrations

}

fun Application.seedDatabase() {
    environment.monitor.subscribe(ApplicationStarted) {
        transaction {
            // Seed admin user if none exist
            // Check if any lecturer exists
            val existingLecturer = LecturersTable
                .select(LecturersTable.id)
                .any()

            val existingStudent = StudentsTable
                .select(StudentsTable.id)
                .any()

            if (!existingLecturer) {
                println("Seeding default lecturer...")

                LecturersTable.insert {
                    it[email] = "lecturer@example.com"
                    it[fullName] = "Default Lecturer"
                    it[isProfileComplete] = false
                    it[isActive] = true
                    it[lastLoginAt] = null
                    it[createdAt] = now()
                    it[updatedAt] = now()
                }
                println("Default lecturer seeded successfully.")
            } else {
                println("Lecturer already exist, skipping seeding.")
            }

            if (!existingStudent) {
                println("Seeding default student...")

                StudentsTable.insert {
                    it[registrationNumber] = "SC211/0483/2022"
                    it[fullName] = "Amos Njega Kamau"
                    it[isActive] = true
                    it[lastLoginAt] = null
                    it[createdAt] = now()
                    it[updatedAt] = now()
                }
                println("Default student seeded successfully.")
            } else {
                println("Student already exist, skipping seeding.")
            }
        }
    }

}