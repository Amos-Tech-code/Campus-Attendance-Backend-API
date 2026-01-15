package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import data.database.entities.AttendanceRecordsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
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


}