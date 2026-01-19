package com.amos_tech_code.api.routes

import com.amos_tech_code.api.dtos.requests.RemoveAttendanceRequest
import com.amos_tech_code.domain.services.AttendanceManagementService
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.getUserRoleFromJWT
import com.amos_tech_code.utils.respondBadRequest
import com.amos_tech_code.utils.respondForbidden
import domain.models.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.attendanceManagementRoutes(
    attendanceManagementService: AttendanceManagementService
) {
    route("api/v1/attendance-manage") {

        delete("/record") {
            val lecturerId = call.getUserIdFromJWT() ?: return@delete call.respondBadRequest("Lecturer ID is required")
            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@delete call.respondForbidden()

            val request = call.receive<RemoveAttendanceRequest>()

            attendanceManagementService.removeStudentAttendance(
                lecturerId = lecturerId,
                request = request
            )

            call.respond(HttpStatusCode.NoContent)

        }

        get {
            val studentId = call.getUserIdFromJWT() ?: return@get call.respondBadRequest("Student ID is required")
            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.STUDENT.name) return@get call.respondForbidden()

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            val sort = call.request.queryParameters["sort"] ?: "desc"

            val response =
                attendanceManagementService.getStudentAttendanceHistory(
                    studentId = studentId,
                    page = page,
                    size = size,
                    sortDesc = sort.lowercase() != "asc"
                )

            call.respond(HttpStatusCode.OK, response)

        }

    }
}