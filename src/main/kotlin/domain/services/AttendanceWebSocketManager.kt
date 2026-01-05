package com.amos_tech_code.domain.services

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AttendanceWebSocketManager {

    private val sessions =
        ConcurrentHashMap<UUID, MutableSet<DefaultWebSocketServerSession>>()

    fun register(sessionId: UUID, socket: DefaultWebSocketServerSession) {
        sessions.computeIfAbsent(sessionId) { mutableSetOf() }.add(socket)
    }

    fun unregister(sessionId: UUID, socket: DefaultWebSocketServerSession) {
        sessions[sessionId]?.remove(socket)
        if (sessions[sessionId].isNullOrEmpty()) {
            sessions.remove(sessionId)
        }
    }

    suspend fun broadcast(sessionId: UUID, message: String) {
        sessions[sessionId]?.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }

}
