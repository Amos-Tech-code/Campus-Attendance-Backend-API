package api.dtos.response

import domain.models.LiveAttendanceEventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveAttendanceMessage<T>(
    val type: LiveAttendanceEventType,
    val data: T
)

@Serializable
sealed class LiveAttendanceEvent {

//    @SerialName("eventType")
//    abstract val eventType: LiveAttendanceEventType

    @Serializable
    data class InitialState(
        val eventType: LiveAttendanceEventType = LiveAttendanceEventType.INITIAL_STATE,
        val data: LiveAttendanceSnapshot
    ) : LiveAttendanceEvent()

    @Serializable
    data class AttendanceMarked(
        val eventType: LiveAttendanceEventType = LiveAttendanceEventType.ATTENDANCE_MARKED,
        val data: AttendanceMarkedEventDto
    ) : LiveAttendanceEvent()
}

