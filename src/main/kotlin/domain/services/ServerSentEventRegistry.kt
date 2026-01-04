package domain.services

import utils.sendSse
import java.io.Writer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ServerSentEventRegistry {

    private val streams =
        ConcurrentHashMap<UUID, MutableSet<Writer>>()

    fun register(sessionId: UUID, writer: Writer) {
        streams.computeIfAbsent(sessionId) { mutableSetOf() }.add(writer)
    }

    fun unregister(sessionId: UUID, writer: Writer) {
        streams[sessionId]?.remove(writer)
        if (streams[sessionId].isNullOrEmpty()) {
            streams.remove(sessionId)
        }
    }

    suspend fun broadcast(sessionId: UUID, event: String, data: String) {
        streams[sessionId]?.forEach { writer ->
            try {
                writer.sendSse(event, data)
            } catch (_: Exception) {
                unregister(sessionId, writer)
            }
        }
    }
}
