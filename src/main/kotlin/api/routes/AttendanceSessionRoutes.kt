package api.routes

import api.dtos.response.LiveAttendanceEvent
import com.amos_tech_code.domain.dtos.requests.*
import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.amos_tech_code.domain.services.LiveAttendanceService
import com.amos_tech_code.services.AttendanceSessionService
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

fun Route.attendanceSessionRoutes(
    attendanceSessionService: AttendanceSessionService,
    markAttendanceService: MarkAttendanceService,
    liveAttendanceService: LiveAttendanceService,
) {

    route("api/v1/attendance") {

        route("/session") {

            post("/start") {
                val lecturerId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Lecturer ID is required")
                if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@post call.respondForbidden()

                val request = call.receive<StartSessionRequest>()

                val session = attendanceSessionService.startSession(lecturerId, request)

                call.respond(
                    HttpStatusCode.Created,
                    session,
                )

            }

            patch("/{sessionId}") {
                val lecturerId = call.getUserIdFromJWT() ?: return@patch call.respondBadRequest("Lecturer ID is required")
                if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@patch call.respondForbidden()

                val sessionId = call.parameters["sessionId"] ?: return@patch call.respondBadRequest("Session ID is required")
                val request = call.receive<UpdateSessionRequest>()

                val updatedSession = attendanceSessionService.updateSession(lecturerId, sessionId, request)

                call.respond(
                    HttpStatusCode.OK,
                    updatedSession
                )
            }

            post("/end") {
                val lecturerId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Lecturer ID is required")
                if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@post call.respondForbidden()

                val request = call.receive<EndSessionRequest>()

                val success = attendanceSessionService.endSession(lecturerId, request.sessionId)

                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        GenericResponseDto(
                        statusCode = HttpStatusCode.NotFound.value,
                        message = "Session not found",
                    ))
                }

            }

            get("/active") {
                val lecturerId = call.getUserIdFromJWT() ?: return@get call.respondBadRequest("Lecturer ID is required")
                if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@get call.respondForbidden()

                val activeSession = attendanceSessionService.getActiveSession(lecturerId)

                call.respond(HttpStatusCode.OK,
                    activeSession
                )

            }

        }

        post("/verify") {

            val studentId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Student ID is required")
            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.STUDENT.name) return@post call.respondForbidden()

            val request = call.receive<VerifySessionRequest>()

            val result = attendanceSessionService.verifySessionForAttendance(studentId, request)

            call.respond(HttpStatusCode.OK, result)

        }

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