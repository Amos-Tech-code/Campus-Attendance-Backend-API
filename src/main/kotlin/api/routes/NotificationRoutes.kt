package com.amos_tech_code.api.routes

import api.dtos.response.GenericResponseDto
import com.amos_tech_code.api.dtos.response.PaginatedNotificationsDto
import com.amos_tech_code.domain.services.NotificationService
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.respondUnauthorized
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.notificationRoutes(
    notificationService: NotificationService
) {
    route("/notifications") {

        // Get unread notifications
        get("/unread") {
            val userId = call.getUserIdFromJWT()
                ?: return@get call.respondUnauthorized()

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            val notifications = notificationService.getUnreadNotifications(userId, limit)

            call.respond(
                HttpStatusCode.OK,
                notifications
            )
        }

        // Get notification history with pagination
        get("/history") {
            val userId = call.getUserIdFromJWT()
                ?: return@get call.respondUnauthorized()

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val notifications = notificationService.getNotificationHistory(
                recipientId = userId,
                page = page,
                pageSize = pageSize
            )

            val hasMore = notifications.size == pageSize

            val paginatedResponse = PaginatedNotificationsDto(
                notifications = notifications,
                page = page,
                pageSize = pageSize,
                hasMore = hasMore
            )

            call.respond(
                HttpStatusCode.OK,
                paginatedResponse
            )
        }

        // Get specific notification by ID
        get("/{id}") {
            val userId = call.getUserIdFromJWT()
                ?: return@get call.respondUnauthorized()

            val notificationId = runCatching { UUID.fromString(call.parameters["id"]) }
                .getOrElse {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.BadRequest.value,
                            message = "Invalid notification ID format"
                        )
                    )
                }

            val notification = notificationService.getNotificationById(notificationId, userId)

            call.respond(HttpStatusCode.OK, notification)
        }

        // Mark notification as read
        patch("/{id}/read") {
            val userId = call.getUserIdFromJWT()
                ?: return@patch call.respondUnauthorized()

            val notificationId = runCatching { UUID.fromString(call.parameters["id"]) }
                .getOrElse {
                    return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.BadRequest.value,
                            message = "Invalid notification ID format"
                        )
                    )
                }

            notificationService.markNotificationAsRead(notificationId, userId)

            call.respond(
                HttpStatusCode.OK,
                GenericResponseDto(
                    statusCode = HttpStatusCode.OK.value,
                    message = "Notification marked as read"
                )
            )
        }

        // Mark all notifications as read
        patch("/read-all") {
            val userId = call.getUserIdFromJWT()
                ?: return@patch call.respondUnauthorized()

            val count = notificationService.markAllNotificationsAsRead(userId)

            call.respond(
                HttpStatusCode.OK,
                GenericResponseDto(
                    statusCode = HttpStatusCode.OK.value,
                    message = "$count notifications marked as read"
                )
            )
        }

        // Delete a notification
        delete("/{id}") {
            val userId = call.getUserIdFromJWT()
                ?: return@delete call.respondUnauthorized()

            val notificationId = runCatching { UUID.fromString(call.parameters["id"]) }
                .getOrElse {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.BadRequest.value,
                            message = "Invalid notification ID format"
                        )
                    )
                }

            notificationService.deleteNotification(notificationId, userId)

            call.respond(
                HttpStatusCode.OK,
                GenericResponseDto(
                    statusCode = HttpStatusCode.OK.value,
                    message = "Notification deleted successfully"
                )
            )
        }

        // Get notification counts
        get("/count") {
            val userId = call.getUserIdFromJWT()
                ?: return@get call.respondUnauthorized()

            val counts = notificationService.getNotificationCounts(userId)

            call.respond(
                HttpStatusCode.OK,
                counts
            )
        }
    }
}