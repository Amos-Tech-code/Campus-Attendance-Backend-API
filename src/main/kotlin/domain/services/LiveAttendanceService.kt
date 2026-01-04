package com.amos_tech_code.domain.services

import api.dtos.response.LiveAttendanceEvent
import api.dtos.response.LiveAttendanceMessage
import api.dtos.response.LiveAttendanceSnapshot
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.flow.Flow
import java.util.UUID


interface LiveAttendanceService {

    suspend fun authorize(
        lecturerId: UUID,
        sessionId: UUID
    )

    suspend fun getInitialSnapshot(
        sessionId: UUID
    ): LiveAttendanceEvent

    fun liveEvents(
        sessionId: UUID
    ): Flow<LiveAttendanceEvent>
}


/*
interface LiveAttendanceService {

    suspend fun authorizeAndConnect(
        lecturerId: UUID,
        sessionId: UUID,
        socket: DefaultWebSocketServerSession
    )

    suspend fun disconnect(
        lecturerId: UUID,
        sessionId: UUID,
        socket: DefaultWebSocketServerSession
    )

    suspend fun getInitialSnapshot(
        lecturerId: UUID,
        sessionId: UUID
    ): LiveAttendanceSnapshot

}

 */
