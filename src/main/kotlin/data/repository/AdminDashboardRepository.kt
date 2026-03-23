package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.ActivityLog
import com.amos_tech_code.domain.models.DashboardStats
import data.database.entities.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime

class AdminDashboardRepository {

    // ========== DASHBOARD METHODS ==========

    suspend fun getDashboardStats(): DashboardStats = exposedTransaction {
        val totalStudents = StudentsTable.selectAll().count()
        val totalLecturers = LecturersTable.selectAll().count()
        val totalUniversities = UniversitiesTable.selectAll().count()
        val totalProgrammes = ProgrammesTable.selectAll().count()
        val totalSessions = AttendanceSessionsTable.selectAll().count()

        val today = LocalDateTime.now().toLocalDate().atStartOfDay()
        val todaySessions = AttendanceSessionsTable
            .selectAll()
            .where { AttendanceSessionsTable.scheduledStartTime greaterEq today }
            .count()

        val totalAttendance = AttendanceRecordsTable.selectAll().count()

        // Get recent activities
        val recentActivities = mutableListOf<ActivityLog>()

        // Last 5 student logins
        StudentsTable
            .selectAll()
            .where { StudentsTable.lastLoginAt.isNotNull() }
            .orderBy(StudentsTable.lastLoginAt to SortOrder.DESC)
            .limit(5)
            .forEach {
                recentActivities.add(
                    ActivityLog(
                        id = it[StudentsTable.id].toString(),
                        type = "STUDENT_LOGIN",
                        description = "Student ${it[StudentsTable.fullName]} logged in",
                        timestamp = it[StudentsTable.lastLoginAt].toString(),
                        performedBy = it[StudentsTable.fullName]
                    )
                )
            }

        // Last 5 lecturer logins
        LecturersTable
            .selectAll()
            .where { LecturersTable.lastLoginAt.isNotNull() }
            .orderBy(LecturersTable.lastLoginAt to SortOrder.DESC)
            .limit(5)
            .forEach {
                recentActivities.add(
                    ActivityLog(
                        id = it[LecturersTable.id].toString(),
                        type = "LECTURER_LOGIN",
                        description = "Lecturer ${it[LecturersTable.fullName]} logged in",
                        timestamp = it[LecturersTable.lastLoginAt].toString(),
                        performedBy = it[LecturersTable.fullName] ?: "Unknown"
                    )
                )
            }

        // Last 5 attendance sessions started
        AttendanceSessionsTable
            .selectAll()
            .orderBy(AttendanceSessionsTable.createdAt to SortOrder.DESC)
            .limit(5)
            .forEach {
                recentActivities.add(
                    ActivityLog(
                        id = it[AttendanceSessionsTable.id].toString(),
                        type = "SESSION_STARTED",
                        description = "Session '${it[AttendanceSessionsTable.sessionTitle]}' started",
                        timestamp = it[AttendanceSessionsTable.createdAt].toString(),
                        performedBy = "Lecturer"
                    )
                )
            }

        // Take only the latest 10 activities
        val limitedActivities = recentActivities
            .sortedByDescending { it.timestamp }
            .take(10)

        DashboardStats(
            totalStudents = totalStudents,
            totalLecturers = totalLecturers,
            totalUniversities = totalUniversities,
            totalProgrammes = totalProgrammes,
            totalSessions = totalSessions,
            todaySessions = todaySessions,
            totalAttendance = totalAttendance,
            recentActivities = limitedActivities
        )
    }

}