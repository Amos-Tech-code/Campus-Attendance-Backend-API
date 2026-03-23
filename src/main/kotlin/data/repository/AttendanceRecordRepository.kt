package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.admin.SuspiciousActivityResponse
import com.amos_tech_code.api.dtos.admin.SuspiciousActivityStatsResponse
import com.amos_tech_code.api.dtos.response.AttendanceStatsResponse
import com.amos_tech_code.api.dtos.response.StudentAttendanceRecordDto
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.AttendanceStats
import com.amos_tech_code.domain.models.RecentAttendance
import com.amos_tech_code.domain.models.UnitAttendance
import data.database.entities.AcademicTermsTable
import data.database.entities.AttendanceRecordsTable
import data.database.entities.AttendanceSessionsTable
import data.database.entities.LecturersTable
import data.database.entities.SessionProgrammesTable
import data.database.entities.StudentEnrollmentsTable
import data.database.entities.StudentsTable
import data.database.entities.UnitsTable
import domain.models.AttendanceSessionStatus
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.LocalDateTime
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

    suspend fun getOverallAttendanceStats(studentId: UUID): AttendanceStats = exposedTransaction {
        val attended = AttendanceRecordsTable
            .select(AttendanceRecordsTable.id)
            .where { AttendanceRecordsTable.studentId eq studentId }
            .count()
            .toInt()

        val total = AttendanceSessionsTable
            .innerJoin(AttendanceRecordsTable) {
                AttendanceSessionsTable.id eq AttendanceRecordsTable.sessionId
            }
            .select(AttendanceSessionsTable.id)
            .where { AttendanceRecordsTable.studentId eq studentId }
            .count()
            .toInt()

        AttendanceStats(attended = attended, total = total)
    }

    suspend fun getAttendanceByUnit(studentId: UUID): List<UnitAttendance> = exposedTransaction {
        // Get attended sessions per unit with proper joins
        val attendedPerUnit = AttendanceRecordsTable
            .innerJoin(AttendanceSessionsTable) {
                AttendanceRecordsTable.sessionId eq AttendanceSessionsTable.id
            }
            .innerJoin(UnitsTable) {
                AttendanceSessionsTable.unitId eq UnitsTable.id
            }
            .select(
                AttendanceSessionsTable.unitId,
                UnitsTable.code,
                UnitsTable.name,
                AttendanceRecordsTable.id.count()
            )
            .where { AttendanceRecordsTable.studentId eq studentId }
            .groupBy(
                AttendanceSessionsTable.unitId,
                UnitsTable.code,
                UnitsTable.name
            )
            .map { row ->
                val unitId = row[AttendanceSessionsTable.unitId]
                val unitCode = row[UnitsTable.code]
                val unitName = row[UnitsTable.name]
                val attended = row[AttendanceRecordsTable.id.count()].toInt()
                Triple(unitId, unitCode, unitName) to attended
            }
            .toMap()

        // Get total sessions per unit
        val totalPerUnit = AttendanceSessionsTable
            .select(
                AttendanceSessionsTable.unitId,
                AttendanceSessionsTable.id.count()
            )
            .groupBy(AttendanceSessionsTable.unitId)
            .map { row ->
                row[AttendanceSessionsTable.unitId] to row[AttendanceSessionsTable.id.count()].toInt()
            }
            .toMap()

        // Combine results
        attendedPerUnit.map { (key, attended) ->
            val (unitId, unitCode, unitName) = key
            val total = totalPerUnit[unitId] ?: 0

            UnitAttendance(
                unitId = unitId,
                unitCode = unitCode,
                unitName = unitName,
                attended = attended,
                total = total
            )
        }
    }

    suspend fun getRecentAttendance(
        studentId: UUID,
        limit: Int = 10
    ): List<RecentAttendance> = exposedTransaction {
        AttendanceRecordsTable
            .innerJoin(AttendanceSessionsTable) {
                AttendanceRecordsTable.sessionId eq AttendanceSessionsTable.id
            }
            .innerJoin(UnitsTable) {
                AttendanceSessionsTable.unitId eq UnitsTable.id
            }
            .selectAll()
            .where { AttendanceRecordsTable.studentId eq studentId }
            .orderBy(AttendanceRecordsTable.attendedAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                RecentAttendance(
                    sessionId = row[AttendanceSessionsTable.id],
                    sessionTitle = row[AttendanceSessionsTable.sessionTitle] ?: "Untitled",
                    unitCode = row[UnitsTable.code],
                    unitName = row[UnitsTable.name],
                    attendedAt = row[AttendanceRecordsTable.attendedAt],
                    attendanceMethod = row[AttendanceRecordsTable.attendanceMethodUsed].name,
                    isSuspicious = row[AttendanceRecordsTable.isSuspicious]
                )
            }
    }

    /**
     * Get count of suspicious activities for a student
     */
    suspend fun getSuspiciousActivityCount(studentId: UUID): Int = exposedTransaction {
        AttendanceRecordsTable
            .select(AttendanceRecordsTable.id)
            .where {
                (AttendanceRecordsTable.studentId eq studentId) and
                        (AttendanceRecordsTable.isSuspicious eq true)
            }
            .count()
            .toInt()
    }

    /**
     * Get attendance statistics for a specific unit
     */
    suspend fun getAttendanceStatsForUnit(
        studentId: UUID,
        unitId: UUID,
        academicTermId: UUID
    ): AttendanceStats = exposedTransaction {
        // Get attended sessions for this unit
        val attended = AttendanceRecordsTable
            .innerJoin(AttendanceSessionsTable) {
                AttendanceRecordsTable.sessionId eq AttendanceSessionsTable.id
            }
            .select(AttendanceRecordsTable.id)
            .where {
                (AttendanceRecordsTable.studentId eq studentId) and
                        (AttendanceSessionsTable.unitId eq unitId) and
                        (AttendanceSessionsTable.academicTermId eq academicTermId)
            }
            .count()
            .toInt()

        // Get total sessions for this unit in this academic term
        val total = AttendanceSessionsTable
            .select(AttendanceSessionsTable.id)
            .where {
                (AttendanceSessionsTable.unitId eq unitId) and
                        (AttendanceSessionsTable.academicTermId eq academicTermId)
            }
            .count()
            .toInt()

        AttendanceStats(
            attended = attended,
            total = total
        )
    }

    /**
     * Get the date of the student's last attendance
     */
    suspend fun getLastAttendanceDate(studentId: UUID): LocalDateTime? = exposedTransaction {
        AttendanceRecordsTable
            .select(AttendanceRecordsTable.attendedAt)
            .where { AttendanceRecordsTable.studentId eq studentId }
            .orderBy(AttendanceRecordsTable.attendedAt to SortOrder.DESC)
            .limit(1)
            .map { it[AttendanceRecordsTable.attendedAt] }
            .singleOrNull()
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


    /*---------------------------------
          ADMIN METHODS
    -------------------------------- */
    suspend fun getAllSuspiciousActivities(
        page: Int = 1,
        pageSize: Int = 20,
        studentId: UUID? = null,
        sessionId: UUID? = null,
        unitId: UUID? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        search: String? = null
    ): Triple<List<SuspiciousActivityResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = AttendanceRecordsTable
            .innerJoin(StudentsTable, { AttendanceRecordsTable.studentId }, { StudentsTable.id })
            .innerJoin(AttendanceSessionsTable, { AttendanceRecordsTable.sessionId }, { AttendanceSessionsTable.id })
            .innerJoin(UnitsTable, { AttendanceSessionsTable.unitId }, { UnitsTable.id })
            .leftJoin(LecturersTable, { AttendanceSessionsTable.lecturerId }, { LecturersTable.id })
            .select(
                AttendanceRecordsTable.id,
                AttendanceRecordsTable.studentId,
                StudentsTable.fullName,
                StudentsTable.registrationNumber,
                AttendanceRecordsTable.sessionId,
                AttendanceSessionsTable.sessionTitle,
                UnitsTable.code,
                UnitsTable.name,
                AttendanceSessionsTable.lecturerId,
                LecturersTable.fullName,
                AttendanceRecordsTable.attendanceMethodUsed,
                AttendanceRecordsTable.attendedAt,
                AttendanceRecordsTable.isSuspicious,
                AttendanceRecordsTable.suspiciousReason,
                AttendanceRecordsTable.studentLatitude,
                AttendanceRecordsTable.studentLongitude,
                AttendanceRecordsTable.distanceFromLecturer,
                AttendanceRecordsTable.isLocationVerified,
                AttendanceRecordsTable.deviceId,
                AttendanceRecordsTable.isDeviceVerified
            )
            .where { AttendanceRecordsTable.isSuspicious eq true }

        // Apply filters
        studentId?.let {
            query = query.andWhere { AttendanceRecordsTable.studentId eq it }
        }

        sessionId?.let {
            query = query.andWhere { AttendanceRecordsTable.sessionId eq it }
        }

        unitId?.let {
            query = query.andWhere { AttendanceSessionsTable.unitId eq it }
        }

        if (!search.isNullOrBlank()) {
            query = query.andWhere {
                (StudentsTable.fullName like "%$search%") or
                        (StudentsTable.registrationNumber like "%$search%") or
                        (UnitsTable.name like "%$search%") or
                        (UnitsTable.code like "%$search%")
            }
        }

        val total = query.count()

        val activities = query
            .orderBy(AttendanceRecordsTable.attendedAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                SuspiciousActivityResponse(
                    id = row[AttendanceRecordsTable.id].toString(),
                    studentId = row[AttendanceRecordsTable.studentId].toString(),
                    studentName = row[StudentsTable.fullName],
                    studentRegNo = row[StudentsTable.registrationNumber],
                    sessionId = row[AttendanceRecordsTable.sessionId].toString(),
                    sessionTitle = row[AttendanceSessionsTable.sessionTitle],
                    unitCode = row[UnitsTable.code],
                    unitName = row[UnitsTable.name],
                    lecturerId = row[AttendanceSessionsTable.lecturerId].toString(),
                    lecturerName = row[LecturersTable.fullName],
                    attendanceMethodUsed = row[AttendanceRecordsTable.attendanceMethodUsed],
                    attendedAt = row[AttendanceRecordsTable.attendedAt].toString(),
                    isSuspicious = row[AttendanceRecordsTable.isSuspicious],
                    suspiciousReason = row[AttendanceRecordsTable.suspiciousReason],
                    studentLatitude = row[AttendanceRecordsTable.studentLatitude],
                    studentLongitude = row[AttendanceRecordsTable.studentLongitude],
                    distanceFromLecturer = row[AttendanceRecordsTable.distanceFromLecturer],
                    isLocationVerified = row[AttendanceRecordsTable.isLocationVerified],
                    deviceId = row[AttendanceRecordsTable.deviceId],
                    isDeviceVerified = row[AttendanceRecordsTable.isDeviceVerified]
                )
            }

        Triple(activities, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun getSuspiciousActivityById(id: UUID): SuspiciousActivityResponse? = exposedTransaction {
        val row = AttendanceRecordsTable
            .innerJoin(StudentsTable, { AttendanceRecordsTable.studentId }, { StudentsTable.id })
            .innerJoin(AttendanceSessionsTable, { AttendanceRecordsTable.sessionId }, { AttendanceSessionsTable.id })
            .innerJoin(UnitsTable, { AttendanceSessionsTable.unitId }, { UnitsTable.id })
            .leftJoin(LecturersTable, { AttendanceSessionsTable.lecturerId }, { LecturersTable.id })
            .select(
                AttendanceRecordsTable.id,
                AttendanceRecordsTable.studentId,
                StudentsTable.fullName,
                StudentsTable.registrationNumber,
                AttendanceRecordsTable.sessionId,
                AttendanceSessionsTable.sessionTitle,
                UnitsTable.code,
                UnitsTable.name,
                AttendanceSessionsTable.lecturerId,
                LecturersTable.fullName,
                AttendanceRecordsTable.attendanceMethodUsed,
                AttendanceRecordsTable.attendedAt,
                AttendanceRecordsTable.isSuspicious,
                AttendanceRecordsTable.suspiciousReason,
                AttendanceRecordsTable.studentLatitude,
                AttendanceRecordsTable.studentLongitude,
                AttendanceRecordsTable.distanceFromLecturer,
                AttendanceRecordsTable.isLocationVerified,
                AttendanceRecordsTable.deviceId,
                AttendanceRecordsTable.isDeviceVerified
            )
            .where { (AttendanceRecordsTable.id eq id) and (AttendanceRecordsTable.isSuspicious eq true) }
            .singleOrNull()

        row?.let {
            SuspiciousActivityResponse(
                id = it[AttendanceRecordsTable.id].toString(),
                studentId = it[AttendanceRecordsTable.studentId].toString(),
                studentName = it[StudentsTable.fullName],
                studentRegNo = it[StudentsTable.registrationNumber],
                sessionId = it[AttendanceRecordsTable.sessionId].toString(),
                sessionTitle = it[AttendanceSessionsTable.sessionTitle],
                unitCode = it[UnitsTable.code],
                unitName = it[UnitsTable.name],
                lecturerId = it[AttendanceSessionsTable.lecturerId].toString(),
                lecturerName = it[LecturersTable.fullName],
                attendanceMethodUsed = it[AttendanceRecordsTable.attendanceMethodUsed],
                attendedAt = it[AttendanceRecordsTable.attendedAt].toString(),
                isSuspicious = it[AttendanceRecordsTable.isSuspicious],
                suspiciousReason = it[AttendanceRecordsTable.suspiciousReason],
                studentLatitude = it[AttendanceRecordsTable.studentLatitude],
                studentLongitude = it[AttendanceRecordsTable.studentLongitude],
                distanceFromLecturer = it[AttendanceRecordsTable.distanceFromLecturer],
                isLocationVerified = it[AttendanceRecordsTable.isLocationVerified],
                deviceId = it[AttendanceRecordsTable.deviceId],
                isDeviceVerified = it[AttendanceRecordsTable.isDeviceVerified]
            )
        }
    }

    suspend fun getSuspiciousActivityStats(): SuspiciousActivityStatsResponse = exposedTransaction {

        val totalSuspicious = AttendanceRecordsTable
            .selectAll()
            .where { AttendanceRecordsTable.isSuspicious eq true }
            .count()

        val pendingReview = totalSuspicious
        val reviewedAndConfirmed = 0L
        val reviewedAndCleared = 0L

        // Define count expression
        val countExpr = AttendanceRecordsTable.id.count()

        val reasons = AttendanceRecordsTable
            .select(AttendanceRecordsTable.suspiciousReason, countExpr)
            .where { AttendanceRecordsTable.isSuspicious eq true }
            .groupBy(AttendanceRecordsTable.suspiciousReason)
            .associate { row ->
                val reason = row[AttendanceRecordsTable.suspiciousReason] ?: "UNKNOWN"
                val count = row[countExpr]

                reason to count
            }

        SuspiciousActivityStatsResponse(
            totalSuspicious = totalSuspicious,
            pendingReview = pendingReview,
            reviewedAndConfirmed = reviewedAndConfirmed,
            reviewedAndCleared = reviewedAndCleared,
            bySeverity = emptyMap(),
            byFlagType = reasons
        )
    }

    suspend fun updateSuspiciousFlag(
        id: UUID,
        isSuspicious: Boolean,
        notes: String?
    ): Boolean = exposedTransaction {
        val updateCount = AttendanceRecordsTable.update({ AttendanceRecordsTable.id eq id }) {
            it[this.isSuspicious] = isSuspicious
            it[suspiciousReason] = notes ?: (if (isSuspicious) "Marked as suspicious by admin" else "Cleared by admin")
        }
        updateCount > 0
    }

}