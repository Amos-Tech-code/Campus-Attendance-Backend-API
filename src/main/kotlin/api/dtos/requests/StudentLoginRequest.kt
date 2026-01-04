package com.amos_tech_code.domain.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class StudentLoginRequest(
    val registrationNumber: String,
    val deviceInfo: DeviceInfo
)