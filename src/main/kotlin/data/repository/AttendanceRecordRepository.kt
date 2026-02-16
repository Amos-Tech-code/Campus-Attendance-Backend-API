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
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
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
        // 1. Find current active academic term
        val currentTerm = AcademicTermsTable
            .selectAll()
            .where { AcademicTermsTable.isActive eq true }
            .orderBy(AcademicTermsTable.academicYear to SortOrder.DESC)
            .orderBy(AcademicTermsTable.semester to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?: return@exposedTransaction AttendanceStatsResponse(
                totalSessions = 0,
                attendedSessions = 0,
                currentStreak = 0,
                lastUpdated = System.currentTimeMillis()
            )

        // 2. Get student's enrollment for current term
        val enrollment = StudentEnrollmentsTable
            .selectAll()
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.academicTermId eq currentTerm[AcademicTermsTable.id]) and
                        (StudentEnrollmentsTable.isActive eq true)
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

        // 3. Get all sessions for this programme and year in current term
        val sessionIds = SessionProgrammesTable
            .innerJoin(
                AttendanceSessionsTable,
                onColumn = { SessionProgrammesTable.sessionId },
                otherColumn = { AttendanceSessionsTable.id }
            )
            .select(SessionProgrammesTable.sessionId)
            .where {
                (SessionProgrammesTable.programmeId eq programmeId) and
                        (SessionProgrammesTable.yearOfStudy eq yearOfStudy) and
                        (AttendanceSessionsTable.academicTermId eq termId) and
                        (AttendanceSessionsTable.status eq AttendanceSessionStatus.ENDED)
            }
            .withDistinct(true)
            .map { it[SessionProgrammesTable.sessionId] }

        val totalSessions = sessionIds.size

        // 4. Count attended sessions
        val attendedSessions = if (sessionIds.isNotEmpty()) {
            AttendanceRecordsTable
                .select(AttendanceRecordsTable.id)
                .where {
                    (AttendanceRecordsTable.studentId eq studentId) and
                            (AttendanceRecordsTable.sessionId inList sessionIds)
                }
                .count()
        } else 0

        // 5. Calculate current streak
        val streak = calculateCurrentStreak(studentId)

        AttendanceStatsResponse(
            totalSessions = totalSessions,
            attendedSessions = attendedSessions.toInt(),
            currentStreak = streak,
            lastUpdated = System.currentTimeMillis()
        )
    }

    // Helper method to calculate streak
    private suspend fun calculateCurrentStreak(studentId: UUID): Int = exposedTransaction {
        // Get recent attendance dates (last 30 days max)
        val recentAttendances = AttendanceRecordsTable
            .select(AttendanceRecordsTable.attendedAt)
            .where { AttendanceRecordsTable.studentId eq studentId }
            .orderBy(AttendanceRecordsTable.attendedAt to SortOrder.DESC)
            .limit(30)
            .map { it[AttendanceRecordsTable.attendedAt] }

        if (recentAttendances.isEmpty()) return@exposedTransaction 0

        // Convert to LocalDate for date-only comparison using UTC
        val today = LocalDate.now(ZoneOffset.UTC)

        // Group by date (ignoring time)
        val attendanceDates = recentAttendances
            .map { attendance ->
                attendance.toLocalDate()
            }
            .distinct()
            .sortedDescending()

        // Check if attended today
        val attendedToday = attendanceDates.contains(today)
        if (!attendedToday) return@exposedTransaction 0

        var streak = 1
        var expectedDate = today.minusDays(1)

        for (date in attendanceDates.drop(1)) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }

        return@exposedTransaction streak
    }


}