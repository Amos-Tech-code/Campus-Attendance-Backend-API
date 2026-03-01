package com.amos_tech_code.data.database

import com.amos_tech_code.config.AppConfig
import com.amos_tech_code.data.database.entities.AdminRefreshTokensTable
import com.amos_tech_code.data.database.entities.AdminsTable
import com.amos_tech_code.data.database.entities.NotificationsTable
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

    fun connect() {
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
    }

    fun createSchema() {

            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)

            transaction {
                // Create tables if they don't exist
                SchemaUtils.createMissingTablesAndColumns(
                    // Admin Tables
                    AdminsTable,
                    AdminRefreshTokensTable,

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

                    // Notification
                    NotificationsTable
                )
            }
    }

}