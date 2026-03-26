package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.NotificationHistoryListResponse
import com.amos_tech_code.api.dtos.admin.NotificationTemplateResponse
import com.amos_tech_code.api.dtos.admin.SendBroadcastRequest
import com.amos_tech_code.api.dtos.admin.UpdateNotificationTemplateRequest
import com.amos_tech_code.data.repository.NotificationManagementRepository
import domain.models.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class NotificationManagementService(
    private val repository: NotificationManagementRepository,
    private val notificationService: NotificationService,
) {

    private val logger = LoggerFactory.getLogger(NotificationManagementService::class.java)

    // Define all notification templates with defaults
    val notificationTemplates = listOf(
        NotificationTemplateResponse(
            type = "ATTENDANCE_MARKED",
            title = "Attendance Marked",
            defaultTitle = "Attendance Marked",
            body = "Your attendance for {unitCode} - {sessionTitle} has been recorded",
            defaultBody = "Your attendance for {unitCode} - {sessionTitle} has been recorded",
            isEnabled = true,
            persistToDatabase = false,
            recipientType = "STUDENT"
        ),
        NotificationTemplateResponse(
            type = "ATTENDANCE_REVOKED",
            title = "⚠️ Attendance Revoked",
            defaultTitle = "⚠️ Attendance Revoked",
            body = "Your attendance for {unitCode} - {sessionTitle} has been revoked. Reason: {reason}",
            defaultBody = "Your attendance for {unitCode} - {sessionTitle} has been revoked. Reason: {reason}",
            isEnabled = true,
            persistToDatabase = true,
            recipientType = "STUDENT"
        ),
        NotificationTemplateResponse(
            type = "DEVICE_APPROVED",
            title = "✅ Device Change Approved",
            defaultTitle = "✅ Device Change Approved",
            body = "Your new device ({deviceModel}) has been approved",
            defaultBody = "Your new device ({deviceModel}) has been approved",
            isEnabled = true,
            persistToDatabase = true,
            recipientType = "STUDENT"
        ),
        NotificationTemplateResponse(
            type = "DEVICE_REJECTED",
            title = "❌ Device Change Rejected",
            defaultTitle = "❌ Device Change Rejected",
            body = "Your device change request for {deviceModel} was rejected. Reason: {reason}",
            defaultBody = "Your device change request for {deviceModel} was rejected. Reason: {reason}",
            isEnabled = true,
            persistToDatabase = true,
            recipientType = "STUDENT"
        ),
        NotificationTemplateResponse(
            type = "SESSION_STARTED",
            title = "📢 Session Started",
            defaultTitle = "📢 Session Started",
            body = "Your session {unitCode} - {sessionTitle} is now active. Code: {sessionCode}",
            defaultBody = "Your session {unitCode} - {sessionTitle} is now active. Code: {sessionCode}",
            isEnabled = true,
            persistToDatabase = false,
            recipientType = "LECTURER"
        ),
        NotificationTemplateResponse(
            type = "SESSION_ENDED",
            title = "✅ Session Ended",
            defaultTitle = "✅ Session Ended",
            body = "Session {unitCode} - {sessionTitle} has ended. Total attendance: {attendanceCount} students",
            defaultBody = "Session {unitCode} - {sessionTitle} has ended. Total attendance: {attendanceCount} students",
            isEnabled = true,
            persistToDatabase = false,
            recipientType = "LECTURER"
        ),
        NotificationTemplateResponse(
            type = "SUSPICIOUS_ACTIVITY",
            title = "⚠️ Suspicious Activity Detected",
            defaultTitle = "⚠️ Suspicious Activity Detected",
            body = "Student {studentName} - {reason} in {unitCode} session",
            defaultBody = "Student {studentName} - {reason} in {unitCode} session",
            isEnabled = true,
            persistToDatabase = false,
            recipientType = "LECTURER"
        ),
        NotificationTemplateResponse(
            type = "SUPPORT_RESPONSE",
            title = "📧 Support Response",
            defaultTitle = "📧 Support Response",
            body = "Your support ticket #{ticketId} has been responded to",
            defaultBody = "Your support ticket #{ticketId} has been responded to",
            isEnabled = true,
            persistToDatabase = true,
            recipientType = "BOTH"
        ),
        NotificationTemplateResponse(
            type = "SYSTEM_ALERT",
            title = "System Alert",
            defaultTitle = "System Alert",
            body = "{message}",
            defaultBody = "{message}",
            isEnabled = true,
            persistToDatabase = true,
            recipientType = "BOTH"
        ),
        NotificationTemplateResponse(
            type = "ADMIN_ALERT",
            title = "Admin Alert",
            defaultTitle = "Admin Alert",
            body = "{message}",
            defaultBody = "{message}",
            isEnabled = true,
            persistToDatabase = true,
            recipientType = "ADMIN"
        )
    )

    suspend fun getNotificationHistory(
        page: Int = 1,
        pageSize: Int = 20,
        recipientType: String? = null,
        notificationType: NotificationType? = null,
        search: String? = null
    ): NotificationHistoryListResponse = withContext(Dispatchers.IO) {
        val (notifications, total, totalPages) = repository.getNotificationHistory(
            page = page,
            pageSize = pageSize,
            recipientType = recipientType,
            notificationType = notificationType,
            search = search
        )

        NotificationHistoryListResponse(
            notifications = notifications,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    suspend fun getNotificationTemplates(): List<NotificationTemplateResponse> = withContext(Dispatchers.IO) {
        notificationTemplates
    }

    suspend fun updateNotificationTemplate(
        type: String,
        request: UpdateNotificationTemplateRequest
    ): Boolean = withContext(Dispatchers.IO) {
        val template = notificationTemplates.find { it.type == type }
            ?: return@withContext false

        // In a real implementation, you'd store these in a database
        // For now, we'll just simulate the update
        logger.info("Updating template $type: $request")
        true
    }

    suspend fun sendBroadcastNotification(request: SendBroadcastRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            when (request.recipientType) {
                "STUDENTS" -> {
                    notificationService.notifyAllStudents(
                        title = request.title,
                        body = request.message,
                        data = mapOf("type" to request.type.name),
                        persist = request.persistToDatabase
                    )
                }
                "LECTURERS" -> {
                    notificationService.notifyAllLecturers(
                        title = request.title,
                        body = request.message,
                        data = mapOf("type" to request.type.name),
                        persist = request.persistToDatabase
                    )
                }
                "ALL" -> {
                    notificationService.notifyAllStudents(
                        title = request.title,
                        body = request.message,
                        data = mapOf("type" to request.type.name),
                        persist = request.persistToDatabase
                    )
                    notificationService.notifyAllLecturers(
                        title = request.title,
                        body = request.message,
                        data = mapOf("type" to request.type.name),
                        persist = request.persistToDatabase
                    )
                }
            }
            true
        } catch (e: Exception) {
            logger.error("Failed to send broadcast notification", e)
            false
        }
    }

    suspend fun getNotificationStats(): Map<String, Long> = withContext(Dispatchers.IO) {
        repository.getNotificationStats()
    }
}