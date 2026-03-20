package domain.models

import java.time.LocalDateTime
import java.util.UUID

data class DeviceChangeDomainRequest(
    val id: UUID,
    val studentId: UUID,
    val oldDeviceId: String,
    val newDeviceId: String,
    val newDeviceModel: String,
    val newDeviceOS: String,
    val newFcmToken: String?,
    val reason: String?,
    val status: DeviceChangeStatus,
    val requestedAt: LocalDateTime,
    val reviewedBy: UUID? = null, // Lecturer who reviewed
    val reviewedAt: LocalDateTime? = null,
    val rejectionReason: String? = null
)