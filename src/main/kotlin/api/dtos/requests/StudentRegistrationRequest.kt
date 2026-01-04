package com.amos_tech_code.domain.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class StudentRegistrationRequest(
    val registrationNumber: String,
    val fullName: String,
    val deviceInfo: DeviceInfo
)

@Serializable
data class DeviceInfo(
    val deviceId: String,
    val model: String,
    val os: String,
    val fcmToken: String? = null
)
