package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.admin.AdminResponse
import com.amos_tech_code.api.dtos.admin.CreateAdminRequest
import com.amos_tech_code.api.dtos.admin.UpdateAdminRequest
import com.amos_tech_code.data.database.entities.AdminRefreshTokensTable
import com.amos_tech_code.data.database.entities.AdminsTable
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Admin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.*

class AdminRepository {

    // ========== AUTH METHODS ==========

    suspend fun findByEmail(email: String): Admin? = exposedTransaction {
        AdminsTable
            .selectAll()
            .where { AdminsTable.email eq email }
            .map { it.toAdmin()}
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

    // ========== MANAGEMENT METHODS ==========

    suspend fun getAllAdmins(): List<AdminResponse> = exposedTransaction {
        AdminsTable
            .selectAll()
            .orderBy(AdminsTable.createdAt to SortOrder.DESC)
            .map { it.toAdminResponse() }
    }

    suspend fun getAdminByIdResponse(id: UUID): AdminResponse? = exposedTransaction {
        AdminsTable
            .selectAll()
            .where { AdminsTable.id eq id }
            .map { it.toAdminResponse() }
            .singleOrNull()
    }

    suspend fun createAdmin(request: CreateAdminRequest): AdminResponse? = exposedTransaction {
        // Check if email already exists
        val existing = AdminsTable
            .selectAll()
            .where { AdminsTable.email eq request.email }
            .map { it[AdminsTable.email] }
            .singleOrNull()

        if (existing != null) return@exposedTransaction null

        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        AdminsTable.insert {
            it[AdminsTable.id] = id
            it[AdminsTable.email] = request.email
            it[AdminsTable.passwordHash] = hashedPassword
            it[AdminsTable.fullName] = request.fullName
            it[AdminsTable.role] = request.role
            it[AdminsTable.createdAt] = now
            it[AdminsTable.updatedAt] = now
        }

        AdminResponse(
            id = id.toString(),
            email = request.email,
            fullName = request.fullName,
            role = request.role,
            isActive = true
        )
    }

    suspend fun updateAdmin(id: UUID, request: UpdateAdminRequest): Boolean = exposedTransaction {
        val updateCount = AdminsTable.update({ AdminsTable.id eq id }) {
            request.fullName?.let { fullName -> it[AdminsTable.fullName] = fullName }
            request.role?.let { role -> it[AdminsTable.role] = role }
            request.isActive?.let { isActive -> it[AdminsTable.isActive] = isActive }
            it[AdminsTable.updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    suspend fun deleteAdmin(id: UUID): Boolean = exposedTransaction {
        // Check if this is the last admin (prevent deleting all admins)
        val adminCount = AdminsTable.selectAll().count()
        if (adminCount <= 1) return@exposedTransaction false

        val deleteCount = AdminsTable.deleteWhere { AdminsTable.id eq id }
        deleteCount > 0
    }

    suspend fun resetAdminPassword(id: UUID, newPassword: String): Boolean = exposedTransaction {
        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        val updateCount = AdminsTable.update({ AdminsTable.id eq id }) {
            it[AdminsTable.passwordHash] = hashedPassword
            it[AdminsTable.updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    // ========== MAPPERS ==========

    private fun ResultRow.toAdmin(): Admin = Admin(
        id = this[AdminsTable.id],
        email = this[AdminsTable.email],
        fullName = this[AdminsTable.fullName],
        role = this[AdminsTable.role],
        lastLoginAt = this[AdminsTable.lastLoginAt]?.toString(),
        isActive = this[AdminsTable.isActive]
    )

    private fun ResultRow.toAdminResponse(): AdminResponse = AdminResponse(
        id = this[AdminsTable.id].toString(),
        email = this[AdminsTable.email],
        fullName = this[AdminsTable.fullName],
        role = this[AdminsTable.role],
        lastLoginAt = this[AdminsTable.lastLoginAt]?.toString(),
        isActive = this[AdminsTable.isActive]
    )
}