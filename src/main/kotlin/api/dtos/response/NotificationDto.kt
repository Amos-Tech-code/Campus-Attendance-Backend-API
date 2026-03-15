package com.amos_tech_code.api.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val recipientId: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String,  // ISO format
    val updatedAt: String   // ISO format
)

@Serializable
data class NotificationCountsDto(
    val total: Int,
    val unread: Int
)

@Serializable
data class PaginatedNotificationsDto(
    val notifications: List<NotificationDto>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)