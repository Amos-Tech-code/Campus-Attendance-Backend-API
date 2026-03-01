package utils

import api.dtos.response.AttendanceMarkedEventDto
import api.dtos.response.LiveAttendanceSnapshot
import com.amos_tech_code.utils.ValidationException
import domain.models.LiveAttendanceEventType
import kotlinx.serialization.json.Json
import java.io.Writer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Convert LocalDateTime to ISO String
 */
fun LocalDateTime.toIsoString(): String =
    this.atOffset(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/**
 * Convert nullable LocalDateTime to ISO String
 */
fun LocalDateTime?.toIsoStringOrNull(): String? =
    this?.atOffset(ZoneOffset.UTC)
        ?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/**
 * Converts String to LocalDateTime
 * @throws ValidationException
 */
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

