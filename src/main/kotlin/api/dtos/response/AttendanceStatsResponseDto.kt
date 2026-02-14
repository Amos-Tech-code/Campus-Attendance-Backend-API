package com.amos_tech_code.api.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceStatsResponse(
    val totalSessions: Int,
    val attendedSessions: Int,
    val currentStreak: Int,
    val lastUpdated: Long
)
