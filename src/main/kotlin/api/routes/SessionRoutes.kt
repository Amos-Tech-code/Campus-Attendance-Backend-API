package api.routes

import com.amos_tech_code.domain.dtos.requests.EndSessionRequest
import com.amos_tech_code.domain.dtos.requests.StartSessionRequest
import com.amos_tech_code.domain.dtos.requests.UpdateSessionRequest
import com.amos_tech_code.domain.dtos.requests.VerifySessionRequest
import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.amos_tech_code.services.AttendanceSessionService
import com.amos_tech_code.utils.ResourceNotFoundException
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.getUserRoleFromJWT
import com.amos_tech_code.utils.respondBadRequest
import com.amos_tech_code.utils.respondForbidden
import domain.models.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.sessionRoutes(
    attendanceSessionService: AttendanceSessionService
) {

    route("api/v1/session") {

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
                throw ResourceNotFoundException("Session not found")
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

        post("/verify") {

            val studentId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Student ID is required")
            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.STUDENT.name) return@post call.respondForbidden()

            val request = call.receive<VerifySessionRequest>()

            val result = attendanceSessionService.verifySessionForAttendance(studentId, request)

            call.respond(HttpStatusCode.OK, result)

        }

    }
}