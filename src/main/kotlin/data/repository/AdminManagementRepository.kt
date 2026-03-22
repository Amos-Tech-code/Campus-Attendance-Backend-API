package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.admin.AdminResponse
import com.amos_tech_code.data.database.entities.AdminsTable
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Admin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.*

class AdminManagementRepository {

    suspend fun getAllAdmins(): List<AdminResponse> = exposedTransaction {
        AdminsTable
            .selectAll()
            .orderBy(AdminsTable.createdAt to SortOrder.DESC)
            .map { it.toAdminResponse() }
    }

    suspend fun getAdminById(id: UUID): AdminResponse? = exposedTransaction {
        AdminsTable
            .selectAll()
            .where { AdminsTable.id eq id }
            .map { it.toAdminResponse() }
            .singleOrNull()
    }

    suspend fun createAdmin(
        email: String,
        password: String,
        fullName: String,
        role: String = "ADMIN"
    ): Admin? = exposedTransaction {
        // Check if email already exists
        val existing = AdminsTable
            .selectAll()
            .where { AdminsTable.email eq email }
            .map { it[AdminsTable.email] }
            .singleOrNull()

        if (existing != null) return@exposedTransaction null

        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val id = UUID.randomUUID()

        AdminsTable.insert {
            it[AdminsTable.id] = id
            it[AdminsTable.email] = email
            it[AdminsTable.passwordHash] = hashedPassword
            it[AdminsTable.fullName] = fullName
            it[AdminsTable.role] = role
            it[AdminsTable.createdAt] = LocalDateTime.now()
            it[AdminsTable.updatedAt] = LocalDateTime.now()
        }

        findById(id)
    }

    suspend fun updateAdmin(
        id: UUID,
        fullName: String? = null,
        role: String? = null,
        isActive: Boolean? = null
    ): Boolean = exposedTransaction {
        val updateMap = mutableMapOf<String, Any?>()

        fullName?.let { updateMap["full_name"] = it }
        role?.let { updateMap["role"] = it }
        isActive?.let { updateMap["is_active"] = it }
        updateMap["updated_at"] = LocalDateTime.now()

        val updateCount = AdminsTable.update({ AdminsTable.id eq id }) {
            updateMap.forEach { (key, value) ->
                when (key) {
                    "full_name" -> it[AdminsTable.fullName] = value as String
                    "role" -> it[AdminsTable.role] = value as String
                    "is_active" -> it[AdminsTable.isActive] = value as Boolean
                    "updated_at" -> it[AdminsTable.updatedAt] = value as LocalDateTime
                }
            }
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

    suspend fun findById(id: UUID): Admin? = exposedTransaction {
        AdminsTable
            .selectAll()
            .where { AdminsTable.id eq id }
            .map { it.toAdmin() }
            .singleOrNull()
    }

    private fun ResultRow.toAdminResponse(): AdminResponse = AdminResponse(
        id = this[AdminsTable.id].toString(),
        email = this[AdminsTable.email],
        fullName = this[AdminsTable.fullName],
        role = this[AdminsTable.role],
        lastLoginAt = this[AdminsTable.lastLoginAt]?.toString(),
        isActive = this[AdminsTable.isActive]
    )

    private fun ResultRow.toAdmin(): Admin = Admin(
        id = this[AdminsTable.id],
        email = this[AdminsTable.email],
        fullName = this[AdminsTable.fullName],
        role = this[AdminsTable.role],
        lastLoginAt = this[AdminsTable.lastLoginAt]?.toString(),
        isActive = this[AdminsTable.isActive]
    )
}