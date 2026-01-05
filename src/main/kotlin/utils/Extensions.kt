package utils

import api.dtos.response.AttendanceMarkedEventDto
import api.dtos.response.LiveAttendanceEvent
import api.dtos.response.LiveAttendanceSnapshot
import api.dtos.response.LiveAttendanceStudentDto
import com.amos_tech_code.utils.ValidationException
import domain.models.LiveAttendanceEventType
import kotlinx.serialization.json.Json
import java.io.Writer
import java.time.LocalDateTime

fun String.toLocalDateTimeOrThrow(): LocalDateTime =
    try {
        LocalDateTime.parse(this)
    } catch (e: Exception) {
        throw ValidationException("Scheduled start time must be a valid ISO-8601 datetime")
    }

fun Writer.writeSseSnapshot(event: LiveAttendanceEventType, data: LiveAttendanceSnapshot) {
    write("event: ${event.name}\n")
    write("data: ${Json.encodeToString(data)}\n\n")
    flush()
}

fun Writer.writeSseLiveEvent(event: LiveAttendanceEventType, data: AttendanceMarkedEventDto) {
    write("event: ${event.name}\n")
    write("data: ${Json.encodeToString(data)}\n\n")
    flush()
}

