package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.AdminDeviceChangeApprovalRequest
import com.amos_tech_code.api.dtos.admin.DeviceChangeRequestListResponse
import com.amos_tech_code.api.dtos.admin.DeviceChangeRequestResponseDto
import com.amos_tech_code.api.dtos.admin.NewDeviceInfoDto
import com.amos_tech_code.domain.models.Device
import com.amos_tech_code.domain.services.NotificationService
import com.amos_tech_code.utils.BackgroundTaskScope
import data.repository.DeviceChangeRequestRepository
import data.repository.StudentRepository
import domain.models.DeviceChangeStatus
import domain.models.DeviceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class AdminDeviceChangeService(
    private val deviceChangeRequestRepository: DeviceChangeRequestRepository,
    private val studentRepository: StudentRepository,
    private val notificationService: NotificationService,
    private val backgroundTaskScope: BackgroundTaskScope
) {

    private val logger = LoggerFactory.getLogger(AdminDeviceChangeService::class.java)

    suspend fun getAllDeviceChangeRequests(
        page: Int = 1,
        pageSize: Int = 20,
        status: DeviceChangeStatus? = null,
        studentId: UUID? = null,
        search: String? = null
    ): DeviceChangeRequestListResponse = withContext(Dispatchers.IO) {
        val (requests, total, totalPages) = deviceChangeRequestRepository.getAllDeviceChangeRequests(
            page = page,
            pageSize = pageSize,
            status = status,
            studentId = studentId,
            search = search
        )

        val requestDtos = requests.map { request ->
            // Get student details
            val student = studentRepository.findById(request.studentId)

            DeviceChangeRequestResponseDto(
                id = request.id.toString(),
                studentId = request.studentId.toString(),
                studentName = student?.fullName ?: "Unknown",
                studentRegNo = student?.registrationNumber ?: "Unknown",
                studentEmail = null,
                oldDeviceId = request.oldDeviceId,
                newDeviceInfo = NewDeviceInfoDto(
                    deviceId = request.newDeviceId,
                    model = request.newDeviceModel,
                    os = request.newDeviceOS,
                    fcmToken = request.newFcmToken
                ),
                reason = request.reason,
                status = request.status,
                requestedAt = request.requestedAt.toString(),
                reviewedBy = request.reviewedBy?.toString(),
                reviewedAt = request.reviewedAt?.toString(),
                rejectionReason = request.rejectionReason
            )
        }

        DeviceChangeRequestListResponse(
            requests = requestDtos,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    suspend fun getDeviceChangeRequestById(id: UUID): DeviceChangeRequestResponseDto? = withContext(Dispatchers.IO) {
        val pair = deviceChangeRequestRepository.getDeviceChangeRequestWithStudentDetails(id)
        pair?.let { (request, student) ->
            DeviceChangeRequestResponseDto(
                id = request.id.toString(),
                studentId = request.studentId.toString(),
                studentName = student?.fullName ?: "Unknown",
                studentRegNo = student?.registrationNumber ?: "Unknown",
                studentEmail = null,
                oldDeviceId = request.oldDeviceId,
                newDeviceInfo = NewDeviceInfoDto(
                    deviceId = request.newDeviceId,
                    model = request.newDeviceModel,
                    os = request.newDeviceOS,
                    fcmToken = request.newFcmToken
                ),
                reason = request.reason,
                status = request.status,
                requestedAt = request.requestedAt.toString(),
                reviewedBy = request.reviewedBy?.toString(),
                reviewedAt = request.reviewedAt?.toString(),
                rejectionReason = request.rejectionReason
            )
        }
    }

    suspend fun approveDeviceChangeRequest(
        adminId: UUID,
        requestId: UUID,
        approvalRequest: AdminDeviceChangeApprovalRequest
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the change request
            val changeRequest = deviceChangeRequestRepository.findById(requestId)
                ?: throw IllegalArgumentException("Device change request not found")

            // Verify request is still pending
            if (changeRequest.status != DeviceChangeStatus.PENDING) {
                throw IllegalStateException("This request has already been ${changeRequest.status.name.lowercase()}")
            }

            if (approvalRequest.approve) {
                // Check if device is already used by another student
                val deviceInUse = studentRepository.findActiveDeviceByDeviceId(changeRequest.newDeviceId)

                if (deviceInUse != null && deviceInUse.studentId != changeRequest.studentId) {
                    // Device belongs to someone else
                    deviceChangeRequestRepository.systemRejectRequest(
                        requestId,
                        "This device is already registered to another student"
                    )
                    throw IllegalStateException("This device is already registered to another student")
                }

                // APPROVE - Update request status
                val approved = deviceChangeRequestRepository.approveRequest(requestId, adminId)

                if (approved) {
                    // Update DevicesTable with the new device
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

                    studentRepository.updateDevice(newDevice)

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
                }

                approved
            } else {
                // REJECT
                val rejectionReason = approvalRequest.rejectionReason ?: "Rejected by admin"

                val rejected = deviceChangeRequestRepository.rejectRequest(
                    requestId, adminId, rejectionReason
                )

                if (rejected) {
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
                }

                rejected
            }
        } catch (e: Exception) {
            logger.error("Failed to process device change request", e)
            throw e
        }
    }
}