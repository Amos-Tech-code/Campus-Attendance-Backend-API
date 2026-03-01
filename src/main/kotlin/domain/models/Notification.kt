package com.amos_tech_code.domain.models

import domain.models.NotificationType
import java.time.LocalDateTime
import java.util.UUID


data class Notification(
    val id: UUID,
    val recipientId: UUID,
    val title: String,
    val body: String,
    val type: NotificationType,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)