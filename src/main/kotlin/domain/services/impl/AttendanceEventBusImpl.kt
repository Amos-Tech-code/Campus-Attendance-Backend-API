package com.amos_tech_code.domain.services.impl

import api.dtos.response.LiveAttendanceEvent
import com.amos_tech_code.domain.services.AttendanceEventBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AttendanceEventBusImpl() : AttendanceEventBus {

    private val channels =
        ConcurrentHashMap<UUID, MutableSharedFlow<LiveAttendanceEvent>>()

    override fun subscribe(sessionId: UUID): Flow<LiveAttendanceEvent> {
        val flow = channels.computeIfAbsent(sessionId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 64
            )
        }
        return flow.asSharedFlow()
    }

    override suspend fun publish(event: LiveAttendanceEvent) {
        val sessionId = when (event) {
            is LiveAttendanceEvent.AttendanceMarked ->
                UUID.fromString(event.data.sessionId)

            is LiveAttendanceEvent.InitialState ->
                UUID.fromString(event.data.sessionId)
        }

        channels[sessionId]?.emit(event)
    }

}
