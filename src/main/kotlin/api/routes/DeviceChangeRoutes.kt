package api.routes

import api.dtos.requests.DeviceChangeApprovalRequest
import api.dtos.requests.StudentDeviceChangeRequest
import api.dtos.response.GenericResponseDto
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.respondBadRequest
import com.amos_tech_code.utils.respondUnauthorized
import domain.services.impl.DeviceChangeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.http.HttpStatus

fun Route.deviceChangeRoutes(deviceChangeService: DeviceChangeService) {

    route("api/v1/device-change") {

        route("/student") {

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

                call.respond(HttpStatusCode.OK, history)
            }

            // Cancel device change request
            patch("cancel-request/{requestId}") {
                val userId = call.getUserIdFromJWT() ?: return@patch call.respondUnauthorized()
                val requestId = call.parameters["requestId"]
                    ?: return@patch call.respondBadRequest("Request ID required")

                val cancelled = deviceChangeService.cancelDeviceChangeRequest(
                    studentId = userId,
                    requestId = requestId
                )

                if (cancelled) {
                    call.respond(
                        HttpStatusCode.OK,
                        GenericResponseDto(
                            HttpStatusCode.OK.value,
                            "Device change request canceled successfully"
                        )
                    )
                }
            }
        }

        route("/lecturer") {

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

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}