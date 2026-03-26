package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.admin.NotificationHistoryResponse
import com.amos_tech_code.data.database.entities.NotificationsTable
import com.amos_tech_code.data.database.utils.exposedTransaction
import data.database.entities.LecturersTable
import data.database.entities.StudentsTable
import domain.models.NotificationType
import org.jetbrains.exposed.sql.*

class NotificationManagementRepository {

    suspend fun getNotificationHistory(
        page: Int = 1,
        pageSize: Int = 20,
        recipientType: String? = null,
        notificationType: NotificationType? = null,
        search: String? = null
    ): Triple<List<NotificationHistoryResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = NotificationsTable
            .selectAll()

        // Join with students or lecturers to get recipient names
        // We'll do this in a separate step

        if (recipientType != null) {
            // We'll filter after getting results
        }

        if (notificationType != null) {
            query = query.andWhere { NotificationsTable.type eq notificationType }
        }

        val total = query.count()

        val notifications = query
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                val recipientId = row[NotificationsTable.recipientId]

                // Try to find recipient in students table first
                val student = StudentsTable
                    .select(StudentsTable.fullName)
                    .where { StudentsTable.id eq recipientId }
                    .singleOrNull()

                val (recipientName, recipientTypeStr) = if (student != null) {
                    student[StudentsTable.fullName] to "STUDENT"
                } else {
                    val lecturer = LecturersTable
                        .select(LecturersTable.fullName)
                        .where { LecturersTable.id eq recipientId }
                        .singleOrNull()
                    lecturer?.let {
                        it[LecturersTable.fullName] to "LECTURER"
                    } ?: ("Unknown" to "UNKNOWN")
                }

                NotificationHistoryResponse(
                    id = row[NotificationsTable.id].toString(),
                    recipientId = recipientId.toString(),
                    recipientName = recipientName ?: "UNKNOWN",
                    recipientType = recipientTypeStr,
                    title = row[NotificationsTable.title],
                    message = row[NotificationsTable.message],
                    type = row[NotificationsTable.type].name,
                    isRead = row[NotificationsTable.isRead],
                    createdAt = row[NotificationsTable.createdAt].toString()
                )
            }

        Triple(notifications, total, ((total + pageSize - 1) / pageSize).toInt())
    }


    suspend fun getNotificationStats(): Map<String, Long> = exposedTransaction {
        NotificationsTable
            .selectAll()
            .groupBy(NotificationsTable.type)
            .associate {
                val type = it[NotificationsTable.type].name
                val count = it[NotificationsTable.id.count()]
                type to count
            }
    }
}