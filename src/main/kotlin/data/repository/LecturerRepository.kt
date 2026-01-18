package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Lecturer
import data.database.entities.LecturersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.*

class LecturerRepository() {

    suspend fun findByEmail(email: String): Lecturer? {
        return exposedTransaction {
            LecturersTable
                .selectAll().where { LecturersTable.email eq email }
                .map { it.toLecturer() }
                .singleOrNull()
        }
    }

    suspend fun findById(id: UUID): Lecturer? {
        return exposedTransaction {
            LecturersTable
                .selectAll().where { LecturersTable.id eq id }
                .map { it.toLecturer() }
                .singleOrNull()
        }
    }

    suspend fun create(lecturer: Lecturer): Lecturer {
        return exposedTransaction {
            LecturersTable.insert {
                it[id] = lecturer.id
                it[email] = lecturer.email
                it[fullName] = lecturer.name
                it[isProfileComplete] = lecturer.isProfileComplete
                it[createdAt] = lecturer.createdAt
                it[updatedAt] = lecturer.updatedAt
            }.resultedValues?.single()?.toLecturer() ?: throw Exception("Failed to create lecturer")
        }
    }

    suspend fun updateProfileComplete(lecturerId: UUID, complete: Boolean): Boolean {
        return exposedTransaction {
            LecturersTable.update({ LecturersTable.id eq lecturerId }) {
                it[isProfileComplete] = complete
                it[updatedAt] = LocalDateTime.now()
            } > 0
        }
    }

    suspend fun updateLastLogin(
        lecturerId: UUID,
        timestamp: LocalDateTime
    ): Boolean {
        return exposedTransaction {
            LecturersTable.update({ LecturersTable.id eq lecturerId }) {
                it[lastLoginAt] = timestamp
                it[updatedAt] = LocalDateTime.now()
            } > 0
        }
    }

    suspend fun updateName(
        lecturerId: UUID,
        fullName: String
    ) : Boolean {
        return exposedTransaction {
            LecturersTable
                .update({ LecturersTable.id eq lecturerId}) {
                    it[this.fullName] = fullName
                    it[updatedAt] = LocalDateTime.now()
                } > 0
        }
    }


    fun ResultRow.toLecturer(): Lecturer {
        return Lecturer(
            id = this[LecturersTable.id],
            email = this[LecturersTable.email],
            name = this[LecturersTable.fullName],
            isProfileComplete = this[LecturersTable.isProfileComplete],
            lastLoginAt = this[LecturersTable.lastLoginAt],
            createdAt = this[LecturersTable.createdAt],
            updatedAt = this[LecturersTable.updatedAt]
        )
    }

}