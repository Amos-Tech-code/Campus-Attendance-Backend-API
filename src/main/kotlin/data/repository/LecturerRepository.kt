package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Lecturer
import com.amos_tech_code.domain.models.ResolvedUniversity
import com.amos_tech_code.domain.models.TeachingUnit
import data.database.entities.LecturerTeachingAssignmentsTable
import data.database.entities.LecturerUniversitiesTable
import data.database.entities.LecturersTable
import data.database.entities.UnitsTable
import data.database.entities.UniversitiesTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
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

    suspend fun getAllWithFcmTokens(): List<Lecturer> = exposedTransaction {
        LecturersTable
            .selectAll()
            .where {
                (LecturersTable.fcmToken.isNotNull()) and
                        (LecturersTable.isActive eq true)
            }
            .map { it.toLecturer() }
    }

    suspend fun getLecturersWithFcmTokens(
        lecturerIds: List<UUID>,
    ): List<Lecturer> = exposedTransaction {
        LecturersTable
            .selectAll()
            .where {
                (LecturersTable.id inList lecturerIds) and
                (LecturersTable.fcmToken.isNotNull()) and
                        (LecturersTable.isActive eq true)
            }
            .map { it.toLecturer() }
    }

    suspend fun updateFcmToken(lecturerId: UUID, fcmToken: String): Boolean = exposedTransaction {
        LecturersTable.update({ LecturersTable.id eq lecturerId }) {
            it[LecturersTable.fcmToken] = fcmToken
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    // In LecturerRepository.kt
    suspend fun getLecturerUniversities(lecturerId: UUID): List<ResolvedUniversity> = exposedTransaction {
        LecturerUniversitiesTable
            .innerJoin(UniversitiesTable) {
                LecturerUniversitiesTable.universityId eq UniversitiesTable.id
            }
            .select(UniversitiesTable.columns)
            .where { LecturerUniversitiesTable.lecturerId eq lecturerId }
            .map { row ->
                ResolvedUniversity(
                    id = row[UniversitiesTable.id],
                    name = row[UniversitiesTable.name]
                )
            }
    }

    suspend fun getTeachingUnits(
        lecturerId: UUID,
        academicTermId: UUID
    ): List<TeachingUnit> = exposedTransaction {
        LecturerTeachingAssignmentsTable
            .innerJoin(UnitsTable) {
                LecturerTeachingAssignmentsTable.unitId eq UnitsTable.id
            }
            .select(
                LecturerTeachingAssignmentsTable.unitId,
                UnitsTable.code,
                UnitsTable.name
            )
            .where {
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId) and
                        (LecturerTeachingAssignmentsTable.isActive eq true)
            }
            .map { row ->
                TeachingUnit(
                    unitId = row[LecturerTeachingAssignmentsTable.unitId],
                    unitCode = row[UnitsTable.code],
                    unitName = row[UnitsTable.name]
                )
            }
    }

    // Helper functions
    fun ResultRow.toLecturer(): Lecturer {
        return Lecturer(
            id = this[LecturersTable.id],
            email = this[LecturersTable.email],
            name = this[LecturersTable.fullName],
            fcmToken = this[LecturersTable.fcmToken],
            isProfileComplete = this[LecturersTable.isProfileComplete],
            lastLoginAt = this[LecturersTable.lastLoginAt],
            createdAt = this[LecturersTable.createdAt],
            updatedAt = this[LecturersTable.updatedAt]
        )
    }

}