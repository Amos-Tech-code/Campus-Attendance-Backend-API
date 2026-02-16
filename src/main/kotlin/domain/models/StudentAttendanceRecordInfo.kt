package com.amos_tech_code.domain.models

import java.time.LocalDateTime
import java.util.UUID

data class StudentAttendanceRecordInfo(
    val id: UUID,
    val attendedAt: LocalDateTime,
    val isSuspicious: Boolean = false,
    val suspiciousReason: String? = null
)