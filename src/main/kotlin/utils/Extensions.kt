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
import java.util.UUID

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
    } catch (_: Exception) {
        throw ValidationException("Scheduled start time must be a valid ISO-8601 datetime")
    }


/**
 * Converts string to UUID
 */
fun String.toUUID(): UUID =
    try {
        UUID.fromString(this)
    } catch (_: Exception) {
        throw ValidationException("Invalid UUID format")
    }


/**
 * Writes a Server-Sent Events (SSE) snapshot message to the underlying [Writer].
 *
 * This function emits a complete SSE event using the standard SSE wire format:
 * - `event:` specifies the event type (derived from [LiveAttendanceEventType.name])
 * - `data:` contains a JSON-serialized [LiveAttendanceSnapshot] payload
 * - A double newline (`\n\n`) terminates the event as required by the SSE protocol
 *
 * This is typically used to send an initial or full-state snapshot of attendance
 * data to a connected lecturer.
 *
 * @param event The logical SSE event type (used as the `event:` field)
 * @param data The snapshot payload to be serialized and sent to the lecturer
 *
 * @see kotlinx.serialization.encodeToString
 */
fun Writer.writeSseSnapshot(event: LiveAttendanceEventType, data: LiveAttendanceSnapshot) {
    write("event: ${event.name}\n")
    write("data: ${Json.encodeToString(data)}\n\n")
    flush()
}

/**
 * Writes a Server-Sent Events (SSE) live update message to the underlying [Writer].
 *
 * This function emits a complete SSE event using the standard SSE wire format:
 * - `event:` specifies the event type (derived from [LiveAttendanceEventType.name])
 * - `data:` contains a JSON-serialized [AttendanceMarkedEventDto] payload
 * - A double newline (`\n\n`) terminates the event as required by the SSE protocol
 *
 * This is intended for incremental, real-time updates (e.g., when attendance
 * is marked) rather than full snapshots.
 *
 * @param event The logical SSE event type (used as the `event:` field)
 * @param data The live event payload to be serialized and sent to the client
 *
 * @see kotlinx.serialization.encodeToString
 */
fun Writer.writeSseLiveEvent(event: LiveAttendanceEventType, data: AttendanceMarkedEventDto) {
    write("event: ${event.name}\n")
    write("data: ${Json.encodeToString(data)}\n\n")
    flush()
}
