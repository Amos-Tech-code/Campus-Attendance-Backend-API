package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.*
import data.database.entities.*
import domain.models.AttendanceSessionStatus
import domain.models.AttendanceSessionType
import domain.models.ExportFormat
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class AttendanceExportRepository {

    // ========== Core Export Methods ==========

    suspend fun getActiveAcademicTerm(universityId: String): UUID? = exposedTransaction {
        AcademicTermsTable
            .selectAll()
            .where {
                (AcademicTermsTable.universityId eq UUID.fromString(universityId)) and
                        (AcademicTermsTable.isActive eq true)
            }
            .orderBy(AcademicTermsTable.academicYear to SortOrder.DESC, AcademicTermsTable.semester to SortOrder.DESC)
            .limit(1)
            .map { it[AcademicTermsTable.id] }
            .firstOrNull()
    }

    suspend fun getAttendanceData(
        teachingAssignmentId: UUID,
        academicTermId: UUID,
        weekRange: String,
        sessionType: AttendanceSessionType?
    ): List<AttendanceReportData> = exposedTransaction {
        val (startWeek, endWeek) = parseWeekRange(weekRange)

        // Get the teaching assignment details with related data
        val teachingAssignment = getTeachingAssignmentDetails(teachingAssignmentId)
            ?: return@exposedTransaction emptyList()

        // Get all sessions for this teaching assignment within week range
        val allSessions = AttendanceSessionsTable
            .selectAll()
            .where {
                (AttendanceSessionsTable.lecturerId eq teachingAssignment.lecturerId) and
                        (AttendanceSessionsTable.unitId eq teachingAssignment.unitId) and
                        (AttendanceSessionsTable.academicTermId eq academicTermId) and
                        (AttendanceSessionsTable.weekNumber greaterEq startWeek) and
                        (AttendanceSessionsTable.weekNumber lessEq endWeek) and
                        (AttendanceSessionsTable.status neq AttendanceSessionStatus.CANCELLED)
            }.let { query ->
                if (sessionType != null) {
                    query.andWhere { AttendanceSessionsTable.attendanceSessionType eq sessionType }
                } else query
            }
            .orderBy(AttendanceSessionsTable.weekNumber to SortOrder.ASC, AttendanceSessionsTable.sessionNumber to SortOrder.ASC)
            .map { row ->
                SessionInfoData(
                    sessionId = row[AttendanceSessionsTable.id],
                    weekNumber = row[AttendanceSessionsTable.weekNumber],
                    sessionNumber = row[AttendanceSessionsTable.sessionNumber],
                    sessionTitle = row[AttendanceSessionsTable.sessionTitle],
                    scheduledStartTime = row[AttendanceSessionsTable.scheduledStartTime],
                    sessionType = row[AttendanceSessionsTable.attendanceSessionType]
                )
            }

        if (allSessions.isEmpty()) return@exposedTransaction emptyList()

        // Separate sessions by type
        val regularSessions = allSessions.filter { it.sessionType == AttendanceSessionType.REGULAR }
        val specialSessions = allSessions.filter { it.sessionType == AttendanceSessionType.SPECIAL }
        val makeupSessions = allSessions.filter { it.sessionType == AttendanceSessionType.MAKEUP }

        // Get all enrolled students for this programme and year of study
        val students = StudentEnrollmentsTable
            .innerJoin(StudentsTable)
            .select(
                StudentsTable.id,
                StudentsTable.registrationNumber,
                StudentsTable.fullName
            )
            .where {
                (StudentEnrollmentsTable.programmeId eq teachingAssignment.programmeId) and
                        (StudentEnrollmentsTable.academicTermId eq academicTermId) and
                        (StudentEnrollmentsTable.yearOfStudy eq teachingAssignment.yearOfStudy) and
                        (StudentEnrollmentsTable.isActive eq true)
            }
            .orderBy(StudentsTable.registrationNumber to SortOrder.ASC)
            .map { row ->
                StudentInfoData(
                    studentId = row[StudentsTable.id],
                    regNo = row[StudentsTable.registrationNumber],
                    fullName = row[StudentsTable.fullName]
                )
            }
            .distinctBy { it.studentId }

        if (students.isEmpty()) return@exposedTransaction emptyList()

        // Get all session IDs
        val allSessionIds = allSessions.map { it.sessionId }

        // Get attendance records for these sessions and students
        val attendanceRecords = AttendanceRecordsTable
            .selectAll()
            .where {
                (AttendanceRecordsTable.sessionId inList allSessionIds) and
                        (AttendanceRecordsTable.studentId inList students.map { it.studentId })
            }
            .map { row ->
                AttendanceRecordData(
                    sessionId = row[AttendanceRecordsTable.sessionId],
                    studentId = row[AttendanceRecordsTable.studentId],
                    attendedAt = row[AttendanceRecordsTable.attendedAt]
                )
            }
            .groupBy { it.sessionId }

        // Create a map of session ID to its type for quick lookup
        val sessionTypeMap = allSessions.associate { it.sessionId to it.sessionType }

        // Build the enhanced report data with session type information
        students.map { student ->
            // Group attendance by session type
            val regularAttendance = regularSessions.map { session ->
                val attended = attendanceRecords[session.sessionId]
                    ?.any { it.studentId == student.studentId } ?: false
                WeeklyAttendanceData(
                    weekNumber = session.weekNumber,
                    sessionNumber = session.sessionNumber,
                    attended = attended,
                    attendedAt = attendanceRecords[session.sessionId]
                        ?.find { it.studentId == student.studentId }
                        ?.attendedAt,
                    sessionType = AttendanceSessionType.REGULAR
                )
            }

            val specialAttendance = specialSessions.map { session ->
                val attended = attendanceRecords[session.sessionId]
                    ?.any { it.studentId == student.studentId } ?: false
                WeeklyAttendanceData(
                    weekNumber = session.weekNumber,
                    sessionNumber = session.sessionNumber,
                    attended = attended,
                    attendedAt = attendanceRecords[session.sessionId]
                        ?.find { it.studentId == student.studentId }
                        ?.attendedAt,
                    sessionType = AttendanceSessionType.SPECIAL
                )
            }

            val makeupAttendance = makeupSessions.map { session ->
                val attended = attendanceRecords[session.sessionId]
                    ?.any { it.studentId == student.studentId } ?: false
                WeeklyAttendanceData(
                    weekNumber = session.weekNumber,
                    sessionNumber = session.sessionNumber,
                    attended = attended,
                    attendedAt = attendanceRecords[session.sessionId]
                        ?.find { it.studentId == student.studentId }
                        ?.attendedAt,
                    sessionType = AttendanceSessionType.MAKEUP
                )
            }

            // Combine all attendance records
            val allAttendance = regularAttendance + specialAttendance + makeupAttendance

            // Calculate totals by session type
            val regularTotal = regularAttendance.count { it.attended }
            val specialTotal = specialAttendance.count { it.attended }
            val makeupTotal = makeupAttendance.count { it.attended }

            // Overall attendance (all session types)
            val totalAttended = allAttendance.count { it.attended }
            val totalSessions = allSessions.size
            val attendancePercentage = if (totalSessions > 0)
                (totalAttended * 100.0 / totalSessions) else 0.0

            AttendanceReportData(
                studentId = student.studentId,
                regNo = student.regNo,
                fullName = student.fullName,
                weeklyAttendance = allAttendance,
                regularAttendance = regularAttendance,
                specialAttendance = specialAttendance,
                makeupAttendance = makeupAttendance,
                regularTotal = regularTotal,
                specialTotal = specialTotal,
                makeupTotal = makeupTotal,
                totalSessions = totalSessions,
                attendedSessions = totalAttended,
                attendancePercentage = attendancePercentage
            )
        }
    }

    suspend fun saveExportRecord(
        lecturerId: UUID,
        teachingAssignmentId: UUID,
        exportType: ExportFormat,
        academicTermId: UUID,
        weekRange: String,
        fileUrl: String,
        fileSize: Long,
        fileName: String,
        unitName: String,
        unitCode: String,
        programmeName: String,
        academicTermName: String,
        expiryDays: Int
    ): UUID = exposedTransaction {
        val id = UUID.randomUUID()
        val now = Instant.now()

        AttendanceExportsTable.insert {
            it[AttendanceExportsTable.id] = id
            it[AttendanceExportsTable.lecturerId] = lecturerId
            it[AttendanceExportsTable.teachingAssignmentId] = teachingAssignmentId
            it[AttendanceExportsTable.exportType] = exportType.name
            it[AttendanceExportsTable.academicTermId] = academicTermId
            it[AttendanceExportsTable.weekRange] = weekRange
            it[AttendanceExportsTable.fileUrl] = fileUrl
            it[AttendanceExportsTable.fileSize] = fileSize
            it[AttendanceExportsTable.fileName] = fileName
            it[AttendanceExportsTable.unitName] = unitName
            it[AttendanceExportsTable.unitCode] = unitCode
            it[AttendanceExportsTable.programmeName] = programmeName
            it[AttendanceExportsTable.academicTermName] = academicTermName
            it[createdAt] = now.atOffset(ZoneOffset.UTC).toLocalDateTime()
            it[expiresAt] = now.plusSeconds(expiryDays * 24L * 60 * 60)
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime()
        }
        id
    }


    suspend fun findByLecturerUnitAndProgrammeAndTerm(
        lecturerId: UUID,
        unitId: UUID,
        programmeId: UUID,
        academicTermId: UUID
    ): LecturerTeachingAssignment? = exposedTransaction {
        LecturerTeachingAssignmentsTable
            .selectAll()
            .where {
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.unitId eq unitId) and
                        (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                        (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId) and
                        (LecturerTeachingAssignmentsTable.isActive eq true)
            }
            .map { row ->
                LecturerTeachingAssignment(
                    id = row[LecturerTeachingAssignmentsTable.id],
                    lecturerId = row[LecturerTeachingAssignmentsTable.lecturerId],
                    universityId = row[LecturerTeachingAssignmentsTable.universityId],
                    programmeId = row[LecturerTeachingAssignmentsTable.programmeId],
                    unitId = row[LecturerTeachingAssignmentsTable.unitId],
                    academicTermId = row[LecturerTeachingAssignmentsTable.academicTermId],
                    yearOfStudy = row[LecturerTeachingAssignmentsTable.yearOfStudy],
                    lectureDay = row[LecturerTeachingAssignmentsTable.lectureDay],
                    lectureTime = row[LecturerTeachingAssignmentsTable.lectureTime],
                    lectureVenue = row[LecturerTeachingAssignmentsTable.lectureVenue],
                    expectedStudents = row[LecturerTeachingAssignmentsTable.expectedStudents],
                    isActive = row[LecturerTeachingAssignmentsTable.isActive],
                    createdAt = row[LecturerTeachingAssignmentsTable.createdAt]
                )
            }
            .singleOrNull()
    }

    suspend fun getUnitWithDetails(unitId: UUID): UnitWithDetails? = exposedTransaction {
        (UnitsTable
            .innerJoin(DepartmentsTable, { UnitsTable.departmentId }, { DepartmentsTable.id })
            .innerJoin(UniversitiesTable, { DepartmentsTable.universityId }, { UniversitiesTable.id }))
            .select(
                UnitsTable.id,
                UnitsTable.code,
                UnitsTable.name,
                UnitsTable.departmentId,
                DepartmentsTable.name,
                UniversitiesTable.id,
                UniversitiesTable.name
            )
            .where { UnitsTable.id eq unitId }
            .map { row ->
                UnitWithDetails(
                    unitId = row[UnitsTable.id],
                    unitCode = row[UnitsTable.code],
                    unitName = row[UnitsTable.name],
                    departmentId = row[UnitsTable.departmentId],
                    departmentName = row[DepartmentsTable.name],
                    universityId = row[UniversitiesTable.id],
                    universityName = row[UniversitiesTable.name]
                )
            }
            .singleOrNull()
    }

    suspend fun getProgrammeWithDetails(programmeId: UUID): ProgrammeWithDetails? = exposedTransaction {
        (ProgrammesTable
            .innerJoin(DepartmentsTable, { ProgrammesTable.departmentId }, { DepartmentsTable.id })
            .innerJoin(UniversitiesTable, { DepartmentsTable.universityId }, { UniversitiesTable.id }))
            .select(
                ProgrammesTable.id,
                ProgrammesTable.name,
                ProgrammesTable.departmentId,
                DepartmentsTable.name,
                UniversitiesTable.id,
                UniversitiesTable.name
            )
            .where { ProgrammesTable.id eq programmeId }
            .map { row ->
                ProgrammeWithDetails(
                    programmeId = row[ProgrammesTable.id],
                    programmeName = row[ProgrammesTable.name],
                    departmentId = row[ProgrammesTable.departmentId],
                    departmentName = row[DepartmentsTable.name],
                    universityId = row[UniversitiesTable.id],
                    universityName = row[UniversitiesTable.name]
                )
            }
            .singleOrNull()
    }

    suspend fun getTeachingAssignmentDetails(teachingAssignmentId: UUID): TeachingAssignmentDetails? = exposedTransaction {
        (LecturerTeachingAssignmentsTable
            .innerJoin(UnitsTable, { LecturerTeachingAssignmentsTable.unitId }, { UnitsTable.id })
            .innerJoin(ProgrammesTable, { LecturerTeachingAssignmentsTable.programmeId }, { ProgrammesTable.id }))
            .select(
                LecturerTeachingAssignmentsTable.id,
                LecturerTeachingAssignmentsTable.lecturerId,
                LecturerTeachingAssignmentsTable.universityId,
                LecturerTeachingAssignmentsTable.programmeId,
                LecturerTeachingAssignmentsTable.unitId,
                LecturerTeachingAssignmentsTable.yearOfStudy,
                UnitsTable.code,
                UnitsTable.name,
                ProgrammesTable.name
            )
            .where { LecturerTeachingAssignmentsTable.id eq teachingAssignmentId }
            .map {
                TeachingAssignmentDetails(
                    id = it[LecturerTeachingAssignmentsTable.id],
                    lecturerId = it[LecturerTeachingAssignmentsTable.lecturerId],
                    universityId = it[LecturerTeachingAssignmentsTable.universityId],
                    programmeId = it[LecturerTeachingAssignmentsTable.programmeId],
                    programmeName = it[ProgrammesTable.name],
                    unitId = it[LecturerTeachingAssignmentsTable.unitId],
                    unitCode = it[UnitsTable.code],
                    unitName = it[UnitsTable.name],
                    yearOfStudy = it[LecturerTeachingAssignmentsTable.yearOfStudy]
                )
            }
            .singleOrNull()
    }

    // ========== Export Record Methods ==========
    suspend fun findExportById(exportId: String): AttendanceExportRecord? = exposedTransaction {
        AttendanceExportsTable
            .selectAll()
            .where { AttendanceExportsTable.id eq UUID.fromString(exportId) }
            .map { toExportRecord(it) }
            .singleOrNull()
    }

    suspend fun findExportsByLecturer(
        lecturerId: UUID,
        limit: Int,
        offset: Int
    ): List<AttendanceExportRecord> = exposedTransaction {
        AttendanceExportsTable
            .selectAll()
            .where { AttendanceExportsTable.lecturerId eq lecturerId }
            .orderBy(AttendanceExportsTable.createdAt to SortOrder.DESC)
            .limit(limit).offset(offset.toLong())
            .map { toExportRecord(it) }
    }

    // ========== Helper Methods ==========

    suspend fun getAcademicTermById(academicTermId: UUID): AcademicTerm? = exposedTransaction {
        AcademicTermsTable
            .selectAll()
            .where { AcademicTermsTable.id eq academicTermId }
            .map {
                AcademicTerm(
                    id = it[AcademicTermsTable.id],
                    universityId = it[AcademicTermsTable.universityId],
                    academicYear = it[AcademicTermsTable.academicYear],
                    semester = it[AcademicTermsTable.semester],
                    weekCount = it[AcademicTermsTable.weekCount],
                    isActive = it[AcademicTermsTable.isActive],
                    createdAt = it[AcademicTermsTable.createdAt]
                )
            }
            .singleOrNull()
    }

    suspend fun countExportsByLecturer(lecturerId: UUID): Int = exposedTransaction {
        AttendanceExportsTable
            .select(AttendanceExportsTable.id)
            .where { AttendanceExportsTable.lecturerId eq lecturerId }
            .count()
            .toInt()
    }

    private fun parseWeekRange(weekRange: String): Pair<Int, Int> {
        return if (weekRange.equals("ALL", ignoreCase = true)) {
            1 to 52
        } else {
            val parts = weekRange.split("-")
            require(parts.size == 2) { "Invalid week range format. Use 'start-end' or 'ALL'" }
            parts[0].toInt() to parts[1].toInt()
        }
    }

    private fun toExportRecord(row: ResultRow): AttendanceExportRecord {
        return AttendanceExportRecord(
            id = row[AttendanceExportsTable.id],
            lecturerId = row[AttendanceExportsTable.lecturerId],
            teachingAssignmentId = row[AttendanceExportsTable.teachingAssignmentId],
            exportType = ExportFormat.valueOf(row[AttendanceExportsTable.exportType]),
            academicTermId = row[AttendanceExportsTable.academicTermId],
            weekRange = row[AttendanceExportsTable.weekRange],
            fileUrl = row[AttendanceExportsTable.fileUrl],
            fileSize = row[AttendanceExportsTable.fileSize],
            fileName = row[AttendanceExportsTable.fileName],
            unitName = row[AttendanceExportsTable.unitName],
            unitCode = row[AttendanceExportsTable.unitCode],
            programmeName = row[AttendanceExportsTable.programmeName],
            academicTermName = row[AttendanceExportsTable.academicTermName],
            createdAt = row[AttendanceExportsTable.createdAt],
            expiresAt = row[AttendanceExportsTable.expiresAt]
        )
    }
}