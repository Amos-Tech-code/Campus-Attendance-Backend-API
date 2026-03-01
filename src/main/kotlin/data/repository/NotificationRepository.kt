package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.entities.NotificationsTable
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Notification
import domain.models.NotificationType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.*

class NotificationRepository {

    suspend fun createNotification(
        recipientId: UUID,
        title: String,
        body: String,
        type: NotificationType,
    ): Notification? = exposedTransaction {
        val id = UUID.randomUUID()

        NotificationsTable.insert {
            it[NotificationsTable.id] = id
            it[NotificationsTable.recipientId] = recipientId
            it[this.title] = title
            it[this.message] = body
            it[this.type] = type
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }

        findById(id)
    }

    suspend fun markAsRead(id: UUID): Boolean = exposedTransaction {
        NotificationsTable.update({ NotificationsTable.id eq id }) {
            it[isRead] = true
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun getUnreadNotifications(
        recipientId: UUID,
        limit: Int = 50
    ): List<Notification> = exposedTransaction {
        NotificationsTable
            .selectAll()
            .where {
                NotificationsTable.recipientId eq recipientId
            }
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toNotification() }
    }

    suspend fun getNotificationHistory(
        recipientId: UUID,
        page: Int = 0,
        pageSize: Int = 20
    ): List<Notification> = exposedTransaction {
        NotificationsTable
            .selectAll()
            .where {
                NotificationsTable.recipientId eq recipientId
            }
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
            .limit(pageSize)
            .offset(start = page.toLong() * pageSize)
            .map { it.toNotification() }
    }

    suspend fun findById(id: UUID): Notification? = exposedTransaction {
        NotificationsTable
            .selectAll()
            .where { NotificationsTable.id eq id }
            .map { it.toNotification() }
            .singleOrNull()
    }

    private fun ResultRow.toNotification(): Notification = Notification(
        id = this[NotificationsTable.id],
        recipientId = this[NotificationsTable.recipientId],
        title = this[NotificationsTable.title],
        body = this[NotificationsTable.message],
        type = this[NotificationsTable.type],
        isRead = this[NotificationsTable.isRead],
        createdAt = this[NotificationsTable.createdAt],
        updatedAt = this[NotificationsTable.updatedAt]
    )
}
