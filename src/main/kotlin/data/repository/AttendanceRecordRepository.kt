package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.response.StudentAttendanceRecordDto
import com.amos_tech_code.data.database.utils.exposedTransaction
import data.database.entities.AttendanceRecordsTable
import data.database.entities.AttendanceSessionsTable
import data.database.entities.UnitsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
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


}