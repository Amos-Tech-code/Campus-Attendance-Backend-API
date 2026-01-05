package domain.services

import api.dtos.response.AttendanceMarkedEventDto

interface AttendanceEventPublisher {

    suspend fun publishAttendanceMarked(
        event: AttendanceMarkedEventDto
    )

}
