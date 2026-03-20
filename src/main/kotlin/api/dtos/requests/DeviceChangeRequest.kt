package api.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class StudentDeviceChangeRequest(
    val deviceInfo: DeviceInfoDto,
    val reason: String? = null
)

@Serializable
data class DeviceInfoDto(
    val deviceId: String,
    val model: String,
    val os: String,
    val fcmToken: String?
)

@Serializable
data class DeviceChangeRequestResponse(
    val requestId: String,
    val status: String,
    val message: String,
    val requestedAt: String
)

@Serializable
data class DeviceChangeApprovalRequest(
    val requestId: String,
    val approve: Boolean,
    val rejectionReason: String? = null
)

@Serializable
data class PendingDeviceChangeDto(
    val requestId: String,
    val studentId: String,
    val studentName: String,
    val studentRegNo: String,
    val oldDeviceId: String,
    val newDeviceInfo: DeviceInfoDto,
    val reason: String?,
    val requestedAt: String
)

@Serializable
data class DeviceChangeHistoryDto(
    val requestId: String,
    val status: String,
    val oldDeviceId: String,
    val newDeviceInfo: DeviceInfoDto,
    val reason: String?,
    val requestedAt: String,
    val reviewedBy: String?,
    val reviewedAt: String?,
    val rejectionReason: String?
)