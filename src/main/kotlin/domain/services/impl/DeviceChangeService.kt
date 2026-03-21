package domain.services.impl

import api.dtos.requests.*
import com.amos_tech_code.data.repository.LecturerRepository
import com.amos_tech_code.domain.models.Device
import com.amos_tech_code.domain.services.NotificationService
import com.amos_tech_code.utils.*
import data.repository.DeviceChangeRequestRepository
import data.repository.StudentEnrollmentRepository
import data.repository.StudentRepository
import domain.models.DeviceChangeDomainRequest
import domain.models.DeviceChangeStatus
import domain.models.DeviceStatus
import domain.models.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class DeviceChangeService(
    private val studentRepository: StudentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val lecturerRepository: LecturerRepository,
    private val deviceChangeRequestRepository: DeviceChangeRequestRepository,
    private val notificationService: NotificationService,
    private val backgroundTaskScope: BackgroundTaskScope
) {

    private val logger = LoggerFactory.getLogger(DeviceChangeService::class.java)
    /**
     * Student requests to change their device
     */
    suspend fun requestDeviceChange(
        studentId: UUID,
        request: StudentDeviceChangeRequest
    ): DeviceChangeRequestResponse = withContext(Dispatchers.IO) {
        try {
            // Validate student exists
            studentRepository.findById(studentId)
                ?: throw ResourceNotFoundException("Student not found")

            // Validate student has active enrollment
            studentEnrollmentRepository.findActiveEnrollment(studentId)
                ?: throw ValidationException("You must be enrolled in a programme to request device change")

            // Get current device
            val currentDevice = studentRepository.findDeviceByStudentId(studentId)
                ?: throw ValidationException("No current device found")

            // Check if new device is already used by another student
            val existingDevice = studentRepository.findActiveDeviceByDeviceId(request.deviceInfo.deviceId)
            if (existingDevice != null && existingDevice.studentId != studentId) {
                throw ConflictException("This device is already registered to another student")
            }

            // Check if there's already a pending request
            val existingPending = deviceChangeRequestRepository.findRequestsByStudent(studentId)
                .firstOrNull { it.status == DeviceChangeStatus.PENDING }
            if (existingPending != null) {
                throw ConflictException("You already have a pending device change request")
            }

            // Create device change request
            val changeRequest = DeviceChangeDomainRequest(
                id = UUID.randomUUID(),
                studentId = studentId,
                oldDeviceId = currentDevice.deviceId,
                newDeviceId = request.deviceInfo.deviceId,
                newDeviceModel = request.deviceInfo.model,
                newDeviceOS = request.deviceInfo.os,
                newFcmToken = request.deviceInfo.fcmToken,
                reason = request.reason,
                status = DeviceChangeStatus.PENDING,
                requestedAt = LocalDateTime.now()
            )

            deviceChangeRequestRepository.create(changeRequest)

            // Notify lecturers in background
            backgroundTaskScope.scope.launch {
                try {
                    notifyLecturersAboutDeviceRequest(studentId)
                } catch (e: Exception) {
                    logger.error("Failed to notify lecturers about device request", e)
                }
            }

            DeviceChangeRequestResponse(
                requestId = changeRequest.id.toString(),
                status = DeviceChangeStatus.PENDING,
                message = "Device change request submitted successfully. Waiting for lecturer approval.",
                requestedAt = changeRequest.requestedAt.toString()
            )

        } catch (e: Exception) {
            logger.error("Device change request failed", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to process device change request")
            }
        }
    }


    /**
     * Get device change history for a student
     */
    suspend fun getStudentDeviceHistory(studentId: UUID): List<DeviceChangeHistoryDto> = withContext(Dispatchers.IO) {
        try {
            val requests = deviceChangeRequestRepository.findRequestsByStudent(studentId)
            requests.map { request ->
                DeviceChangeHistoryDto(
                    requestId = request.id.toString(),
                    status = request.status,
                    oldDeviceId = request.oldDeviceId,
                    newDeviceInfo = DeviceInfoDto(
                        deviceId = request.newDeviceId,
                        model = request.newDeviceModel,
                        os = request.newDeviceOS,
                        fcmToken = request.newFcmToken
                    ),
                    reason = request.reason,
                    requestedAt = request.requestedAt.toString(),
                    reviewedBy = request.reviewedBy?.toString(),
                    reviewedAt = request.reviewedAt?.toString(),
                    rejectionReason = request.rejectionReason
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to get device history")
            emptyList()
        }
    }


    /**
     * Student cancels their pending device change request
     */
    suspend fun cancelDeviceChangeRequest(
        studentId: UUID,
        requestId: String
    ): Boolean {
        try {
            val requestUUID = UUID.fromString(requestId)

            // Verify student exists
            studentRepository.findById(studentId)
                ?: throw ResourceNotFoundException("Student not found")

            // Cancel the request
            val cancelled = deviceChangeRequestRepository.cancelRequest(requestUUID, studentId)

            if (!cancelled) {
                throw ValidationException("Request not found or already processed")
            }

            return true

        } catch (e: Exception) {
            logger.error("Failed to cancel device change request", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to cancel device change request")
            }
        }
    }


    /**
     * Lecturer approves or rejects a device change request
     */
    suspend fun reviewDeviceRequest(
        lecturerId: UUID,
        request: DeviceChangeApprovalRequest
    ): DeviceChangeRequestResponse = withContext(Dispatchers.IO) {
        try {
            val requestId = UUID.fromString(request.requestId)

            // Get the change request
            val changeRequest = deviceChangeRequestRepository.findById(requestId)
                ?: throw ResourceNotFoundException("Device change request not found")

            // Verify lecturer can approve this request
            val canApprove = deviceChangeRequestRepository.canLecturerApproveRequest(
                lecturerId, changeRequest.studentId
            )
            if (!canApprove) {
                throw AuthorizationException("You are not authorized to review this device change request")
            }

            // Verify request is still pending
            if (changeRequest.status != DeviceChangeStatus.PENDING) {
                throw ConflictException("This request has already been ${changeRequest.status.name.lowercase()}")
            }

            if (request.approve) {
                // APPROVE - Update DevicesTable with the new device

                // 1. Update request status
                deviceChangeRequestRepository.approveRequest(requestId, lecturerId)

                // 2. Find the pending device in DevicesTable
                val pendingDevice = studentRepository.findDeviceByStudentIdAndDeviceId(
                    changeRequest.studentId, changeRequest.newDeviceId
                )

                if (pendingDevice != null) {
                    // 3. Mark old active device as INACTIVE/REJECTED
                    val oldActiveDevice = studentRepository.findActiveDeviceByStudentId(changeRequest.studentId)
                    oldActiveDevice?.let { device ->
                        studentRepository.updateDeviceStatus(device.id, DeviceStatus.REJECTED)
                    }

                    // 4. Update the pending device to ACTIVE
                    studentRepository.updateDeviceStatus(pendingDevice.id, DeviceStatus.ACTIVE)
                } else {
                    // If device doesn't exist in DevicesTable yet (shouldn't happen), create it
                    val newDevice = Device(
                        id = UUID.randomUUID(),
                        studentId = changeRequest.studentId,
                        deviceId = changeRequest.newDeviceId,
                        model = changeRequest.newDeviceModel,
                        os = changeRequest.newDeviceOS,
                        fcmToken = changeRequest.newFcmToken,
                        status = DeviceStatus.ACTIVE,
                        lastSeen = LocalDateTime.now(),
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                    studentRepository.createDevice(newDevice)
                }

                // Send notification in background
                backgroundTaskScope.scope.launch {
                    try {
                        notificationService.notifyStudentDeviceChangeApproved(
                            studentId = changeRequest.studentId,
                            newDeviceModel = changeRequest.newDeviceModel
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to send approval notification", e)
                    }
                }

                DeviceChangeRequestResponse(
                    requestId = requestId.toString(),
                    status = DeviceChangeStatus.APPROVED,
                    message = "Device change request approved successfully",
                    requestedAt = changeRequest.requestedAt.toString()
                )
            } else {
                // REJECT - Just update request and device status, don't touch active device
                val rejectionReason = request.rejectionReason ?: "No reason provided"

                // 1. Update request status
                deviceChangeRequestRepository.rejectRequest(
                    requestId, lecturerId, rejectionReason
                )

                // 2. Update the pending device status to REJECTED in DevicesTable
                val pendingDevice = studentRepository.findDeviceByStudentIdAndDeviceId(
                    changeRequest.studentId, changeRequest.newDeviceId
                )
                pendingDevice?.let { device ->
                    studentRepository.updateDeviceStatus(device.id, DeviceStatus.REJECTED)
                }

                // Send notification in background
                backgroundTaskScope.scope.launch {
                    try {
                        notificationService.notifyStudentDeviceChangeRejected(
                            studentId = changeRequest.studentId,
                            deviceModel = changeRequest.newDeviceModel,
                            reason = rejectionReason
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to send rejection notification", e)
                    }
                }

                DeviceChangeRequestResponse(
                    requestId = requestId.toString(),
                    status = DeviceChangeStatus.REJECTED,
                    message = "Device change request rejected",
                    requestedAt = changeRequest.requestedAt.toString()
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to review device request", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to process device change review")
            }
        }
    }

    /**
     * Get pending device change requests for a lecturer
     */
    suspend fun getPendingRequests(lecturerId: UUID): List<PendingDeviceChangeDto> = withContext(Dispatchers.IO) {
        try {
            lecturerRepository.findById(lecturerId)
                ?: throw ResourceNotFoundException("Lecturer not found")

            val pendingRequests = deviceChangeRequestRepository.findPendingRequestsForLecturer(lecturerId)

            pendingRequests.map { request ->
                val student = studentRepository.findById(request.studentId)
                PendingDeviceChangeDto(
                    requestId = request.id.toString(),
                    studentId = request.studentId.toString(),
                    studentName = student?.fullName ?: "Unknown",
                    studentRegNo = student?.registrationNumber ?: "Unknown",
                    oldDeviceId = request.oldDeviceId,
                    newDeviceInfo = DeviceInfoDto(
                        deviceId = request.newDeviceId,
                        model = request.newDeviceModel,
                        os = request.newDeviceOS,
                        fcmToken = request.newFcmToken
                    ),
                    reason = request.reason,
                    requestedAt = request.requestedAt.toString()
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to get pending requests", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to get pending requests")
            }
        }
    }


    /**
     * Notify all eligible lecturers about a device change request
     */
    private suspend fun notifyLecturersAboutDeviceRequest(
        studentId: UUID
    ) {
        // Get all lecturers who teach this student
        val lecturers = studentEnrollmentRepository.getLecturersForStudent(studentId)

        lecturers.forEach { _ ->
            notificationService.notifyLecturers(
                lecturerIds = lecturers.map { it.id },
                persist = true,
                title = "New Device Change Request",
                body = "Student requested to change device. Review in dashboard.",
                data = mapOf(
                    "type" to NotificationType.DEVICE_REQUEST.name,
                    "studentId" to studentId.toString()
                )
            )
        }
    }
}