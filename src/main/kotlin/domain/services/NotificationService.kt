package com.amos_tech_code.domain.services

import com.amos_tech_code.data.repository.LecturerRepository
import com.amos_tech_code.data.repository.NotificationRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.services.impl.FirebaseService
import domain.models.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class NotificationService(
    private val studentRepository: StudentRepository,
    private val lecturerRepository: LecturerRepository,
    private val notificationRepository: NotificationRepository
)
{

    private val logger = org.slf4j.LoggerFactory.getLogger(NotificationService::class.java)

    // Define which notification types should be persisted
    companion object {
        val PERSISTED_TYPES = setOf(
            NotificationType.ATTENDANCE_REVOKED,
            NotificationType.DEVICE_APPROVED,
            NotificationType.DEVICE_REJECTED,
            NotificationType.SUPPORT_RESPONSE,
            NotificationType.SYSTEM_ALERT,
            NotificationType.ADMIN_ALERT
        )
    }

    // =========== STUDENT NOTIFICATIONS ===========

    suspend fun notifyStudentAttendanceMarked(
        studentId: UUID,
        sessionTitle: String,
        unitCode: String
    ) {
        withContext(Dispatchers.IO) {
            val student = studentRepository.findById(studentId) ?: return@withContext
            val device = studentRepository.findDeviceByStudentId(studentId)

            device?.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = "Attendance Marked",
                    body = "Your attendance for $unitCode - $sessionTitle has been recorded",
                    data = mapOf(
                        "type" to NotificationType.ATTENDANCE_MARKED.name,
                        "studentId" to studentId.toString(),
                        "unitCode" to unitCode,
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
            }

            // ⚠️ NOT persisted - transient notification
        }
    }

    suspend fun notifyStudentAttendanceRevoked(
        studentId: UUID,
        sessionTitle: String,
        unitCode: String,
        reason: String
    ) {
        withContext(Dispatchers.IO) {
            val student = studentRepository.findById(studentId) ?: return@withContext
            val device = studentRepository.findDeviceByStudentId(studentId)

            device?.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = "⚠️ Attendance Revoked",
                    body = "Your attendance for $unitCode - $sessionTitle has been revoked. Reason: $reason",
                    data = mapOf(
                        "type" to NotificationType.ATTENDANCE_REVOKED.name,
                        "studentId" to studentId.toString(),
                        "unitCode" to unitCode,
                        "reason" to reason,
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
            }

            // ✅ PERSISTED - important notification
            persistNotification(
                recipientId = studentId,
                title = "Attendance Revoked",
                body = "Your attendance for $unitCode - $sessionTitle has been revoked. Reason: $reason",
                type = NotificationType.ATTENDANCE_REVOKED
            )
        }
    }

    suspend fun notifyStudentDeviceChangeApproved(
        studentId: UUID,
        newDeviceModel: String
    ) {
        withContext(Dispatchers.IO) {
            val student = studentRepository.findById(studentId) ?: return@withContext
            val device = studentRepository.findDeviceByStudentId(studentId)

            device?.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = "✅ Device Change Approved",
                    body = "Your new device ($newDeviceModel) has been approved",
                    data = mapOf(
                        "type" to NotificationType.DEVICE_APPROVED.name,
                        "studentId" to studentId.toString()
                    )
                )
            }

            // ✅ PERSISTED - important notification
            persistNotification(
                recipientId = studentId,
                title = "Device Change Approved",
                body = "Your new device ($newDeviceModel) has been approved",
                type = NotificationType.DEVICE_APPROVED
            )
        }
    }

    suspend fun notifyStudentDeviceChangeRejected(
        studentId: UUID,
        deviceModel: String,
        reason: String
    ) {
        withContext(Dispatchers.IO) {
            val student = studentRepository.findById(studentId) ?: return@withContext
            val device = studentRepository.findDeviceByStudentId(studentId)

            device?.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = "❌ Device Change Rejected",
                    body = "Your device change request for $deviceModel was rejected. Reason: $reason",
                    data = mapOf(
                        "type" to NotificationType.DEVICE_REJECTED.name,
                        "studentId" to studentId.toString(),
                        "reason" to reason
                    )
                )
            }

            // ✅ PERSISTED - important notification
            persistNotification(
                recipientId = studentId,
                title = "Device Change Rejected",
                body = "Your device change request for $deviceModel was rejected. Reason: $reason",
                type = NotificationType.DEVICE_REJECTED
            )
        }
    }

    // =========== LECTURER NOTIFICATIONS ===========

    suspend fun notifyLecturerSessionStarted(
        lecturerId: UUID,
        sessionTitle: String,
        unitCode: String,
        sessionCode: String
    ) {
        withContext(Dispatchers.IO) {
            val lecturer = lecturerRepository.findById(lecturerId) ?: return@withContext

            lecturer.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = "📢 Session Started",
                    body = "Your session $unitCode - $sessionTitle is now active. Code: $sessionCode",
                    data = mapOf(
                        "type" to NotificationType.SESSION_STARTED.name,
                        "lecturerId" to lecturerId.toString(),
                        "sessionCode" to sessionCode,
                        "unitCode" to unitCode
                    )
                )
            }

            // ⚠️ NOT persisted - transient notification
        }
    }

    suspend fun notifyLecturerSessionEnded(
        lecturerId: UUID,
        sessionTitle: String,
        unitCode: String,
        attendanceCount: Int
    ) {
        withContext(Dispatchers.IO) {
            val lecturer = lecturerRepository.findById(lecturerId) ?: return@withContext

            lecturer.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = "✅ Session Ended",
                    body = "Session $unitCode - $sessionTitle has ended. Total attendance: $attendanceCount students",
                    data = mapOf(
                        "type" to NotificationType.SESSION_ENDED.name,
                        "lecturerId" to lecturerId.toString(),
                        "attendanceCount" to attendanceCount.toString()
                    )
                )
            }

            // ⚠️ NOT persisted - transient notification
        }
    }

    suspend fun notifyLecturerSuspiciousActivity(
        lecturerId: UUID,
        sessionTitle: String,
        unitCode: String,
        studentName: String,
        reason: String
    ) {
        withContext(Dispatchers.IO) {
            val lecturer = lecturerRepository.findById(lecturerId) ?: return@withContext

            lecturer.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = "⚠️ Suspicious Activity Detected",
                    body = "Student $studentName - $reason in $unitCode session",
                    data = mapOf(
                        "type" to NotificationType.SUSPICIOUS_ACTIVITY.name,
                        "lecturerId" to lecturerId.toString(),
                        "sessionTitle" to sessionTitle,
                        "unitCode" to unitCode,
                        "reason" to reason
                    )
                )
            }

            // ⚠️ NOT persisted - transient notification
        }
    }

    // =========== SUPPORT & SYSTEM NOTIFICATIONS ===========

    suspend fun notifySupportResponse(
        userId: UUID,
        isStudent: Boolean,
        ticketId: String,
        responseMessage: String
    ) {
        withContext(Dispatchers.IO) {
            val token = if (isStudent) {
                studentRepository.findById(userId)?.let { student ->
                    studentRepository.findDeviceByStudentId(userId)?.fcmToken
                }
            } else {
                lecturerRepository.findById(userId)?.fcmToken
            }

            token?.let {
                FirebaseService.sendNotification(
                    token = it,
                    title = "📧 Support Response",
                    body = "Your support ticket #$ticketId has been responded to",
                    data = mapOf(
                        "type" to NotificationType.SUPPORT_RESPONSE.name,
                        "userId" to userId.toString(),
                        "ticketId" to ticketId,
                        "response" to responseMessage
                    )
                )
            }

            // ✅ PERSISTED - important notification
            persistNotification(
                recipientId = userId,
                title = "Support Response",
                body = "Your support ticket #$ticketId has been responded to",
                type = NotificationType.SUPPORT_RESPONSE
            )
        }
    }

    suspend fun notifySystemAlert(
        userId: UUID,
        isStudent: Boolean,
        alertTitle: String,
        alertMessage: String
    ) {
        withContext(Dispatchers.IO) {
            val token = if (isStudent) {
                studentRepository.findById(userId)?.let { student ->
                    studentRepository.findDeviceByStudentId(userId)?.fcmToken
                }
            } else {
                lecturerRepository.findById(userId)?.fcmToken
            }

            token?.let {
                FirebaseService.sendNotification(
                    token = it,
                    title = alertTitle,
                    body = alertMessage,
                    data = mapOf(
                        "type" to NotificationType.SYSTEM_ALERT.name,
                        "userId" to userId.toString()
                    )
                )
            }

            // ✅ PERSISTED - important notification
            persistNotification(
                recipientId = userId,
                title = alertTitle,
                body = alertMessage,
                type = NotificationType.SYSTEM_ALERT
            )
        }
    }

    suspend fun notifyAdminAlert(
        adminId: UUID,
        alertTitle: String,
        alertMessage: String
    ) {
        withContext(Dispatchers.IO) {
            val admin = lecturerRepository.findById(adminId) // Assuming admin is a lecturer type

            admin?.fcmToken?.let { token ->
                FirebaseService.sendNotification(
                    token = token,
                    title = alertTitle,
                    body = alertMessage,
                    data = mapOf(
                        "type" to NotificationType.ADMIN_ALERT.name,
                        "adminId" to adminId.toString()
                    )
                )
            }

            // ✅ PERSISTED - important notification
            persistNotification(
                recipientId = adminId,
                title = alertTitle,
                body = alertMessage,
                type = NotificationType.ADMIN_ALERT
            )
        }
    }

    // =========== BULK NOTIFICATIONS ===========

    suspend fun notifyAllStudents(
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        persist: Boolean = false  // Control persistence for bulk notifications
    ) {
        withContext(Dispatchers.IO) {
            val allDevices = studentRepository.getAllActiveDevices()
            val tokens = allDevices.mapNotNull { it.fcmToken }

            if (tokens.isNotEmpty()) {
                tokens.chunked(500).forEach { batch ->
                    FirebaseService.sendMulticast(batch, title, body, data)
                }
            }

            // Optionally persist for important announcements
            if (persist) {
                val type = data["type"]?.let {
                    try { NotificationType.valueOf(it) }
                    catch (e: Exception) { null }
                } ?: NotificationType.SYSTEM_ALERT

                allDevices.forEach { device ->
                    persistNotification(
                        recipientId = device.studentId,
                        title = title,
                        body = body,
                        type = type
                    )
                }
            }
        }
    }

    suspend fun notifyAllLecturers(
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        persist: Boolean = false  // Control persistence for bulk notifications
    ) {
        withContext(Dispatchers.IO) {
            val allLecturers = lecturerRepository.getAllWithFcmTokens()
            val tokens = allLecturers.mapNotNull { it.fcmToken }

            if (tokens.isNotEmpty()) {
                tokens.chunked(500).forEach { batch ->
                    FirebaseService.sendMulticast(batch, title, body, data)
                }
            }

            // Optionally persist for important announcements
            if (persist) {
                val type = data["type"]?.let {
                    try { NotificationType.valueOf(it) }
                    catch (e: Exception) { null }
                } ?: NotificationType.SYSTEM_ALERT

                allLecturers.forEach { lecturer ->
                    persistNotification(
                        recipientId = lecturer.id,
                        title = title,
                        body = body,
                        type = type
                    )
                }
            }
        }
    }

    // =========== PRIVATE HELPER METHODS ===========

    private suspend fun persistNotification(
        recipientId: UUID,
        title: String,
        body: String,
        type: NotificationType
    ) {
        try {
            if (type in PERSISTED_TYPES) {
                notificationRepository.createNotification(
                    recipientId = recipientId,
                    title = title,
                    body = body,
                    type = type
                )
                logger.info("Persisted notification: $type for user: $recipientId")
            }
        } catch (e: Exception) {
            logger.error("Failed to persist notification: ${e.message}", e)
            // Don't throw - we don't want to fail the notification if persistence fails
        }
    }

    // =========== GENERIC NOTIFICATION METHOD ===========

    suspend fun sendNotification(
        recipientId: UUID,
        isStudent: Boolean,
        title: String,
        body: String,
        type: NotificationType,
        additionalData: Map<String, String> = emptyMap()
    ) {
        withContext(Dispatchers.IO) {
            // Get FCM token
            val token = if (isStudent) {
                studentRepository.findById(recipientId)?.let { student ->
                    studentRepository.findDeviceByStudentId(recipientId)?.fcmToken
                }
            } else {
                lecturerRepository.findById(recipientId)?.fcmToken
            }

            // Send push notification
            token?.let {
                val data = additionalData + mapOf("type" to type.name)
                FirebaseService.sendNotification(it, title, body, data)
            }

            // Persist if needed
            if (type in PERSISTED_TYPES) {
                persistNotification(recipientId, title, body, type)
            }
        }
    }
}