package com.amos_tech_code.data.database

import com.amos_tech_code.data.database.entities.AdminsTable
import com.amos_tech_code.data.repository.AdminRepository
import data.database.entities.LecturersTable
import data.database.entities.StudentsTable
import org.bouncycastle.cms.RecipientId.password
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime.now
import java.util.UUID

object SeedManager {

    /**
     * Seed defaults
     */
    fun seed() {

        transaction {

            // Seed default Lecturer if none exists
            val lecturerExists =
                LecturersTable.selectAll().limit(1).any()

            if (!lecturerExists) {
                LecturersTable.insert {
                    it[email] = "lecturer@example.com"
                    it[fullName] = "Default Lecturer"
                    it[isProfileComplete] = false
                    it[isActive] = true
                    it[lastLoginAt] = null
                    it[createdAt] = now()
                    it[updatedAt] = now()
                }

                println("✅ Default lecturer seeded")
            }

            // Seed test student if none exists
            val studentExists =
                StudentsTable.selectAll().limit(1).any()

            if (!studentExists) {
                StudentsTable.insert {
                    it[registrationNumber] = "SC211/0483/2022"
                    it[fullName] = "Amos Njega Kamau"
                    it[isActive] = true
                    it[lastLoginAt] = null
                    it[createdAt] = now()
                    it[updatedAt] = now()
                }

                println("✅ Default student seeded")
            }

            // Seed default admin if none exists
            val existingAdmin = AdminsTable.selectAll().limit(1).any()
            if (!existingAdmin) {
                println("Seeding default admin...")

                val password = "Admin123!"
                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

                AdminsTable.insert {
                    it[email] = "admin@smartattend.com"
                    it[fullName] = "System Administrator"
                    it[passwordHash] = hashedPassword
                }

                println("Default admin seeded successfully.")
                println("Email: admin@smartattend.com")
                println("Password: Admin123!")
            }
        }
    }
}