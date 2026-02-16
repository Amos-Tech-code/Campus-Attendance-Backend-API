package com.amos_tech_code.domain.services

import api.dtos.response.LiveAttendanceEvent
import api.dtos.response.LiveAttendanceSnapshot
import kotlinx.coroutines.flow.Flow
import java.util.UUID


interface LiveAttendanceService {

    suspend fun authorize(
        lecturerId: UUID,
        sessionId: UUID
    )

    suspend fun getInitialSnapshot(
        sessionId: UUID
    ): LiveAttendanceSnapshot

    fun liveEvents(
        sessionId: UUID
    ): Flow<LiveAttendanceEvent>
}
