package com.amos_tech_code.domain.services

import com.amos_tech_code.data.repository.LecturerRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.services.impl.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class NotificationService(
    private val studentRepository: StudentRepository,
    private val lecturerRepository: LecturerRepository,
) {

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
                        "type" to "ATTENDANCE_MARKED",
                        "studentId" to studentId.toString(),
                        "unitCode" to unitCode,
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
            }
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
                        "type" to "ATTENDANCE_REVOKED",
                        "studentId" to studentId.toString(),
                        "unitCode" to unitCode,
                        "reason" to reason,
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
            }
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
                        "type" to "DEVICE_APPROVED",
                        "studentId" to studentId.toString()
                    )
                )
            }
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
                        "type" to "DEVICE_REJECTED",
                        "studentId" to studentId.toString(),
                        "reason" to reason
                    )
                )
            }
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
                        "type" to "SESSION_STARTED",
                        "lecturerId" to lecturerId.toString(),
                        "sessionCode" to sessionCode,
                        "unitCode" to unitCode
                    )
                )
            }
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
                        "type" to "SESSION_ENDED",
                        "lecturerId" to lecturerId.toString(),
                        "attendanceCount" to attendanceCount.toString()
                    )
                )
            }
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
                        "type" to "SUSPICIOUS_ACTIVITY",
                        "lecturerId" to lecturerId.toString(),
                        "sessionTitle" to sessionTitle,
                        "unitCode" to unitCode,
                        "reason" to reason
                    )
                )
            }
        }
    }


    // =========== BULK NOTIFICATIONS ===========

    suspend fun notifyAllStudents(
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        withContext(Dispatchers.IO) {
            val allDevices = studentRepository.getAllActiveDevices()
            val tokens = allDevices.mapNotNull { it.fcmToken }

            if (tokens.isNotEmpty()) {
                // Send in batches of 500 (Firebase limit)
                tokens.chunked(500).forEach { batch ->
                    FirebaseService.sendMulticast(batch, title, body, data)
                }
            }
        }
    }

    suspend fun notifyAllLecturers(
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        withContext(Dispatchers.IO) {
            val allLecturers = lecturerRepository.getAllWithFcmTokens()
            val tokens = allLecturers.mapNotNull { it.fcmToken }

            if (tokens.isNotEmpty()) {
                tokens.chunked(500).forEach { batch ->
                    FirebaseService.sendMulticast(batch, title, body, data)
                }
            }
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(NotificationService::class.java)
    }
}