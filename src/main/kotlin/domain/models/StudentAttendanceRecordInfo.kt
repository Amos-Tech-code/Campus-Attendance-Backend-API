package com.amos_tech_code.domain.models

import java.util.*

data class StudentAttendanceRecordInfo(
    val id: UUID,
    val attendedAt: String,
    val isSuspicious: Boolean = false,
    val suspiciousReason: String? = null
)