package com.amos_tech_code.domain.models

import domain.models.DeviceStatus
import java.time.LocalDateTime
import java.util.UUID

data class Student(
    val id: UUID,
    val registrationNumber: String,
    val fullName: String,
    val device: Device?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null,
    val lastLogin: LocalDateTime? = null
)


data class Device(
    val id: UUID,
    val studentId: UUID,
    val deviceId: String,
    val model: String,
    val os: String,
    val fcmToken: String? = null,
    val status: DeviceStatus,
    val lastSeen: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)