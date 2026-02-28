package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.response.AttendanceStatsResponse
import com.amos_tech_code.api.dtos.response.StudentAttendanceRecordDto
import com.amos_tech_code.data.database.utils.exposedTransaction
import data.database.entities.AcademicTermsTable
import data.database.entities.AttendanceRecordsTable
import data.database.entities.AttendanceSessionsTable
import data.database.entities.SessionProgrammesTable
import data.database.entities.StudentEnrollmentsTable
import data.database.entities.UnitsTable
import domain.models.AttendanceSessionStatus
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class AttendanceRecordRepository {

    suspend fun findBySessionAndStudent(
        sessionId: UUID,
        studentId: UUID
    ): UUID? = exposedTransaction {
        AttendanceRecordsTable
            .select(AttendanceRecordsTable.id)
            .where {
                (AttendanceRecordsTable.sessionId eq sessionId) and
                        (AttendanceRecordsTable.studentId eq studentId)
            }
            .limit(1)
            .singleOrNull()
            ?.get(AttendanceRecordsTable.id)
    }

    suspend fun deleteById(attendanceId: UUID) = exposedTransaction {
        AttendanceRecordsTable.deleteWhere {
            AttendanceRecordsTable.id eq attendanceId
        }
    }

    suspend fun fetchStudentAttendanceHistory(
        studentId: UUID,
        limit: Int,
        offset: Int,
        sortDesc: Boolean
    ): List<StudentAttendanceRecordDto> = exposedTransaction {

        (AttendanceRecordsTable
            .innerJoin(AttendanceSessionsTable)
            .innerJoin(UnitsTable)
                )
            .select(
                AttendanceSessionsTable.id,
                UnitsTable.code,
                UnitsTable.name,
                AttendanceSessionsTable.sessionTitle,
                AttendanceSessionsTable.attendanceSessionType,
                AttendanceRecordsTable.attendanceMethodUsed,
                AttendanceSessionsTable.status,
                AttendanceRecordsTable.attendedAt,
                AttendanceRecordsTable.isSuspicious,
                AttendanceRecordsTable.suspiciousReason
            )
            .where {
                AttendanceRecordsTable.studentId eq studentId
            }
            .orderBy(
                AttendanceRecordsTable.attendedAt to
                        if (sortDesc) SortOrder.DESC else SortOrder.ASC
            )
            .limit(limit).offset(offset.toLong())
            .map { row ->

                val attendedAt = row[AttendanceRecordsTable.attendedAt]

                StudentAttendanceRecordDto(
                    sessionId = row[AttendanceSessionsTable.id].toString(),
                    unitCode = row[UnitsTable.code],
                    unitName = row[UnitsTable.name],
                    sessionTitle = row[AttendanceSessionsTable.sessionTitle],
                    sessionType = row[AttendanceSessionsTable.attendanceSessionType],
                    attendanceMethodUsed = row[AttendanceRecordsTable.attendanceMethodUsed],
                    status = row[AttendanceSessionsTable.status],
                    attendedAt = attendedAt
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli(),
                    isSuspicious = row[AttendanceRecordsTable.isSuspicious],
                    suspiciousReason = row[AttendanceRecordsTable.suspiciousReason]
                )
            }
    }

    suspend fun hasNextStudentAttendanceHistory(
        studentId: UUID,
        offset: Int
    ): Boolean = exposedTransaction {

        AttendanceRecordsTable
            .select(AttendanceRecordsTable.id)
            .where { AttendanceRecordsTable.studentId eq studentId }
            .limit(1).offset(offset.toLong())
            .any()
    }


    // Get student attendance stats for current active term
    suspend fun getStudentAttendanceStats(studentId: UUID): AttendanceStatsResponse = exposedTransaction {
        // 1. Get student's current active term enrollment (don't fetch all active terms)
        val enrollment = StudentEnrollmentsTable
            .innerJoin(AcademicTermsTable,
                onColumn = { StudentEnrollmentsTable.academicTermId },
                otherColumn = { AcademicTermsTable.id }
            )
            .selectAll()
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.isActive eq true) and
                        (AcademicTermsTable.isActive eq true)
            }
            .limit(1)
            .singleOrNull()

        if (enrollment == null) {
            return@exposedTransaction AttendanceStatsResponse(
                totalSessions = 0,
                attendedSessions = 0,
                currentStreak = 0,
                lastUpdated = System.currentTimeMillis()
            )
        }

        val programmeId = enrollment[StudentEnrollmentsTable.programmeId]
        val yearOfStudy = enrollment[StudentEnrollmentsTable.yearOfStudy]
        val termId = enrollment[StudentEnrollmentsTable.academicTermId]

        // 2. Get session stats (total and attended) in a single query
        val sessionStats = SessionProgrammesTable
            .innerJoin(
                AttendanceSessionsTable,
                onColumn = { SessionProgrammesTable.sessionId },
                otherColumn = { AttendanceSessionsTable.id }
            )
            .leftJoin(
                AttendanceRecordsTable,
                onColumn = { AttendanceSessionsTable.id },
                otherColumn = { AttendanceRecordsTable.sessionId }
            )
            .select(
                AttendanceSessionsTable.id.countDistinct(),
                AttendanceRecordsTable.id.countDistinct()
            )
            .where {
                (SessionProgrammesTable.programmeId eq programmeId) and
                        (SessionProgrammesTable.yearOfStudy eq yearOfStudy) and
                        (AttendanceSessionsTable.academicTermId eq termId) and
                        (AttendanceSessionsTable.status inList listOf(
                            AttendanceSessionStatus.ACTIVE,
                            AttendanceSessionStatus.ENDED
                        )) and
                        (AttendanceRecordsTable.studentId eq studentId or (AttendanceRecordsTable.studentId.isNull())) and
                        ((AttendanceRecordsTable.isSuspicious eq false) or (AttendanceRecordsTable.isSuspicious.isNull()))
            }
            .map { row ->
                Pair(
                    row[AttendanceSessionsTable.id.countDistinct()],
                    row[AttendanceRecordsTable.id.countDistinct()]
                )
            }
            .firstOrNull() ?: Pair(0L, 0L)

        val totalSessions = sessionStats.first.toInt()
        val attendedSessions = sessionStats.second.toInt()

        // 3. Calculate current streak (only for current term)
        val streak = calculateCurrentStreakForTerm(studentId, termId)

        AttendanceStatsResponse(
            totalSessions = totalSessions,
            attendedSessions = attendedSessions,
            currentStreak = streak,
            lastUpdated = System.currentTimeMillis()
        )
    }

    // Helper method to calculate streak for specific term
    private suspend fun calculateCurrentStreakForTerm(studentId: UUID, termId: UUID): Int = exposedTransaction {
        // Get attendance dates for the current term
        val attendanceDates = AttendanceRecordsTable
            .innerJoin(
                AttendanceSessionsTable,
                onColumn = { AttendanceRecordsTable.sessionId },
                otherColumn = { AttendanceSessionsTable.id }
            )
            .select(AttendanceRecordsTable.attendedAt)
            .where {
                (AttendanceRecordsTable.studentId eq studentId) and
                        (AttendanceSessionsTable.academicTermId eq termId) and
                        (AttendanceRecordsTable.isSuspicious eq false)
            }
            .orderBy(AttendanceRecordsTable.attendedAt to SortOrder.DESC)
            .map { it[AttendanceRecordsTable.attendedAt].toLocalDate() }
            .distinct()

        if (attendanceDates.isEmpty()) return@exposedTransaction 0

        val today = LocalDate.now(ZoneOffset.UTC)

        // Check if attended today
        if (!attendanceDates.contains(today)) return@exposedTransaction 0

        // Count consecutive days backwards
        var streak = 1
        var expectedDate = today.minusDays(1)

        while (attendanceDates.contains(expectedDate)) {
            streak++
            expectedDate = expectedDate.minusDays(1)
        }

        return@exposedTransaction streak
    }


}