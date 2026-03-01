package com.amos_tech_code.data.database

import data.database.entities.LecturersTable
import data.database.entities.StudentsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime.now

object SeedManager {

    /**
     * Seed defaults
     */
    fun seed() {

        transaction {

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
        }
    }
}