package domain.services.impl

import api.dtos.response.AttendanceMarkedEventDto
import api.dtos.response.LiveAttendanceMessage
import domain.services.AttendanceEventPublisher
import com.amos_tech_code.domain.services.AttendanceWebSocketManager
import domain.models.LiveAttendanceEventType
import kotlinx.serialization.json.Json
import java.util.UUID

class AttendanceEventPublisherImpl : AttendanceEventPublisher {

    override suspend fun publishAttendanceMarked(
        event: AttendanceMarkedEventDto
    ) {

        val message = LiveAttendanceMessage(
            type = LiveAttendanceEventType.ATTENDANCE_MARKED,
            data = event
        )

        val payload = Json.encodeToString(message)

        AttendanceWebSocketManager.broadcast(
            sessionId = UUID.fromString(event.sessionId),
            message = payload
        )
    }

}

