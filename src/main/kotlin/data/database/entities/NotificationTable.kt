package com.amos_tech_code.data.database.entities

import domain.models.NotificationType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

object NotificationsTable : Table("notifications") {
    val id = uuid("id").autoGenerate()

    val recipientId = uuid("recipient_id") // Can be student_id or lecturer_id

    val title = varchar("title", 255)
    val message = varchar("message", 1000)
    val type = enumerationByName<NotificationType>("type", 50)
    val isRead = bool("is_read").default(false)

    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, recipientId)
        index(false, createdAt)
    }

}