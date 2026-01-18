package api.routes

import api.dtos.response.LiveAttendanceEvent
import com.amos_tech_code.domain.dtos.requests.*
import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.amos_tech_code.domain.services.LiveAttendanceService
import com.amos_tech_code.services.MarkAttendanceService
import com.amos_tech_code.utils.*
import domain.models.LiveAttendanceEventType
import domain.models.UserRole
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import utils.writeSseLiveEvent
import utils.writeSseSnapshot
import java.util.*

fun Route.attendanceRoutes(
    markAttendanceService: MarkAttendanceService,
    liveAttendanceService: LiveAttendanceService,
) {

    route("api/v1/attendance") {

        post("/mark") {

            val studentId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Student ID is required")
            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.STUDENT.name) return@post call.respondForbidden()

            val request = call.receive<MarkAttendanceRequest>()

            val result = markAttendanceService.processAttendanceMarking(studentId, request)

            call.respond(
                HttpStatusCode.OK,
                result
            )
        }

        post("/lecturer-mark") {
            val lecturerId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Lecturer ID is required")
            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@post call.respondForbidden()

            val request = call.receive<LecturerMarkAttendanceRequest>()

            val isSuccessful = markAttendanceService.lecturerSignAttendance(
                lecturerId,
                request
            )
            if (isSuccessful) {
                call.respond(HttpStatusCode.OK,
                    GenericResponseDto(
                    HttpStatusCode.OK.value,
                    "Attendance marked successfully")
                )
            }

        }

        get ("/{sessionId}/live") {

            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthorizationException("Unauthorized")

            if (call.getUserRoleFromJWT() != UserRole.LECTURER.name) {
                throw AuthorizationException("Forbidden")
            }

            val sessionId = try {
                UUID.fromString(call.parameters["sessionId"])
            } catch (e: Exception) {
                throw ValidationException("Invalid session Id")
            }

            liveAttendanceService.authorize(lecturerId, sessionId)

            call.response.cacheControl(CacheControl.NoCache(null))
            call.respondTextWriter(
                contentType = ContentType.Text.EventStream
            ) {

                // 1 Send initial snapshot
                val snapshot = liveAttendanceService.getInitialSnapshot(sessionId)
                writeSseSnapshot(
                    event = LiveAttendanceEventType.INITIAL_STATE,
                    data = snapshot
                )

                // 2 Stream live events
                liveAttendanceService.liveEvents(sessionId).collect { event ->
                    when (event) {
                        is LiveAttendanceEvent.AttendanceMarked -> {
                            writeSseLiveEvent(
                                event = LiveAttendanceEventType.ATTENDANCE_MARKED,
                                data = event.data
                            )
                        }

                        else -> {}
                    }
                }
            }
        }

    }

}