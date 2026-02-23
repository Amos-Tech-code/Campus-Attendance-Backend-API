package com.amos_tech_code.data.database

import com.amos_tech_code.config.AppConfig
import data.database.entities.AcademicTermsTable
import data.database.entities.AttendanceExportsTable
import data.database.entities.AttendanceRecordsTable
import data.database.entities.AttendanceSessionsTable
import data.database.entities.DepartmentsTable
import data.database.entities.DevicesTable
import data.database.entities.LecturerTeachingAssignmentsTable
import data.database.entities.LecturerUniversitiesTable
import data.database.entities.LecturersTable
import data.database.entities.ProgrammeUnitsTable
import data.database.entities.ProgrammesTable
import data.database.entities.SessionProgrammesTable
import data.database.entities.StudentEnrollmentsTable
import data.database.entities.StudentsTable
import data.database.entities.SuspiciousLoginsTable
import data.database.entities.UnitsTable
import data.database.entities.UniversitiesTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
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