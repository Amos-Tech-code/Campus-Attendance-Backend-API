package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.entities.AdminRefreshTokensTable
import com.amos_tech_code.data.database.entities.AdminsTable
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.ActivityLog
import com.amos_tech_code.domain.models.Admin
import com.amos_tech_code.domain.models.DashboardStats
import data.database.entities.*
import org.jetbrains.exposed.sql.*
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.*

class AdminRepository {

    suspend fun createAdmin(email: String, password: String, fullName: String): Admin? = exposedTransaction {
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val id = UUID.randomUUID()

        AdminsTable.insert {
            it[AdminsTable.id] = id
            it[AdminsTable.email] = email
            it[AdminsTable.passwordHash] = hashedPassword
            it[AdminsTable.fullName] = fullName
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }

        findById(id)
    }

    suspend fun findByEmail(email: String): Admin? = exposedTransaction {
        AdminsTable
            .selectAll()
            .where { AdminsTable.email eq email }
            .map { it.toAdmin() }
            .singleOrNull()
    }

    suspend fun findById(id: UUID): Admin? = exposedTransaction {
        AdminsTable
            .selectAll()
            .where { AdminsTable.id eq id }
            .map { it.toAdmin() }
            .singleOrNull()
    }

    suspend fun validatePassword(admin: Admin, password: String): Boolean = exposedTransaction {
        val hash = AdminsTable
            .select(AdminsTable.passwordHash)
            .where { AdminsTable.id eq admin.id }
            .map { it[AdminsTable.passwordHash] }
            .singleOrNull()

        hash?.let { BCrypt.checkpw(password, it) } ?: false
    }

    suspend fun saveRefreshToken(adminId: UUID, token: String, expiresAt: LocalDateTime): Boolean = exposedTransaction {
        val result = AdminRefreshTokensTable.insert {
            it[id] = UUID.randomUUID()
            it[this.adminId] = adminId
            it[this.token] = token
            it[this.expiresAt] = expiresAt
        }

        result.insertedCount > 0
    }

    suspend fun findRefreshToken(token: String): UUID? = exposedTransaction {
        AdminRefreshTokensTable
            .select(AdminRefreshTokensTable.adminId)
            .where {
                (AdminRefreshTokensTable.token eq token) and
                        (AdminRefreshTokensTable.expiresAt greaterEq LocalDateTime.now()) and
                        (AdminRefreshTokensTable.revokedAt.isNull())
            }
            .map { it[AdminRefreshTokensTable.adminId] }
            .singleOrNull()
    }

    suspend fun revokeRefreshToken(token: String): Boolean = exposedTransaction {
        AdminRefreshTokensTable.update({ AdminRefreshTokensTable.token eq token }) {
            it[revokedAt] = LocalDateTime.now()
        } > 0
    }

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

        // Get recent activities (simplified - you can expand this)
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

        DashboardStats(
            totalStudents = totalStudents,
            totalLecturers = totalLecturers,
            totalUniversities = totalUniversities,
            totalProgrammes = totalProgrammes,
            totalSessions = totalSessions,
            todaySessions = todaySessions,
            totalAttendance = totalAttendance,
            recentActivities = recentActivities
        )
    }

    private fun ResultRow.toAdmin(): Admin = Admin(
        id = this[AdminsTable.id],
        email = this[AdminsTable.email],
        fullName = this[AdminsTable.fullName],
        role = this[AdminsTable.role],
        lastLoginAt = this[AdminsTable.lastLoginAt]?.toString(),
        isActive = this[AdminsTable.isActive]
    )
}