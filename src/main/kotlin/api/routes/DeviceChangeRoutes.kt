package api.routes

import api.dtos.requests.DeviceChangeApprovalRequest
import api.dtos.requests.StudentDeviceChangeRequest
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.respondUnauthorized
import domain.services.impl.DeviceChangeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.deviceChangeRoutes(deviceChangeService: DeviceChangeService) {

    route("/student/device") {

        // Student requests device change
        post("/change-request") {

            val userId = call.getUserIdFromJWT() ?: return@post call.respondUnauthorized()
            val request = call.receive<StudentDeviceChangeRequest>()

            val response = deviceChangeService.requestDeviceChange(
                studentId = userId,
                request = request
            )

            call.respond(HttpStatusCode.OK, response)
        }

        // Get student's device change history
        get("/history") {

            val userId = call.getUserIdFromJWT() ?: return@get call.respondUnauthorized()

            val history = deviceChangeService.getStudentDeviceHistory(
                studentId = userId
            )

            call.respond(HttpStatusCode.OK,history)
        }
    }

    route("/lecturer/device-requests") {

        // Get all pending device change requests for this lecturer
        get("/pending") {

            val userId = call.getUserIdFromJWT() ?: return@get call.respondUnauthorized()

            val pendingRequests = deviceChangeService.getPendingRequests(
                lecturerId = userId
            )

            call.respond(
                HttpStatusCode.OK,
                pendingRequests
            )
        }

        // Approve or reject a device change request
        post("/review") {
            val userId = call.getUserIdFromJWT() ?: return@post call.respondUnauthorized()
            val request = call.receive<DeviceChangeApprovalRequest>()

            val response = deviceChangeService.reviewDeviceRequest(
                lecturerId = userId,
                request = request
            )

            call.respond(HttpStatusCode.OK,response)
        }
    }
}