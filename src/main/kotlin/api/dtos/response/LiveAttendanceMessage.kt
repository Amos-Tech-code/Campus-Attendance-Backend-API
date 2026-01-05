package api.dtos.response

import domain.models.LiveAttendanceEventType
import kotlinx.serialization.Serializable

@Serializable
data class LiveAttendanceMessage<T>(
    val type: LiveAttendanceEventType,
    val data: T
)


sealed class LiveAttendanceEvent {

    data class InitialState(
        val data: LiveAttendanceSnapshot
    ) : LiveAttendanceEvent()

    data class AttendanceMarked(
        val data: AttendanceMarkedEventDto
    ) : LiveAttendanceEvent()
}

