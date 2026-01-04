package utils

import api.dtos.response.LiveAttendanceEvent
import com.amos_tech_code.utils.ValidationException
import kotlinx.serialization.json.Json
import java.io.Writer
import java.time.LocalDateTime

fun String.toLocalDateTimeOrThrow(): LocalDateTime =
    try {
        LocalDateTime.parse(this)
    } catch (e: Exception) {
        throw ValidationException("Scheduled start time must be a valid ISO-8601 datetime")
    }


fun Writer.sendSse(event: String, data: String) {
    write("event: $event\n")
    write("data: $data\n\n")
    flush()
}

fun Writer.writeSse(event: String, data: LiveAttendanceEvent) {
    write("event: $event\n")
    write("data: ${Json.encodeToString(data)}\n\n")
    flush()
}

