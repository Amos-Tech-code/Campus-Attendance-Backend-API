package com.amos_tech_code.domain.services

import api.dtos.response.LiveAttendanceEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AttendanceEventBus {

    fun subscribe(sessionId: UUID): Flow<LiveAttendanceEvent>

    suspend fun publish(event: LiveAttendanceEvent)
}
