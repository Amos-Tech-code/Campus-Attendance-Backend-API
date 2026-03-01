package com.amos_tech_code.data.database.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

// Add this to your entities file
object AdminsTable : Table("admins") {
    val id = uuid("id").autoGenerate()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val role = varchar("role", 50).default("ADMIN") // For future super-admin if needed
    val lastLoginAt = datetime("last_login_at").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)
}

// Refresh tokens table
object AdminRefreshTokensTable : Table("admin_refresh_tokens") {
    val id = uuid("id").autoGenerate()
    val adminId = uuid("admin_id").references(AdminsTable.id, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 512).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").clientDefault { now() }
    val revokedAt = datetime("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
    init {
        index(false, adminId)
        index(false, token)
    }
}