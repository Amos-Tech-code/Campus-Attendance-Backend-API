package com.amos_tech_code.api.routes

import com.amos_tech_code.api.dtos.requests.StudentLookupRequest
import com.amos_tech_code.domain.services.StudentLookUpService
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.getUserRoleFromJWT
import com.amos_tech_code.utils.respondBadRequest
import com.amos_tech_code.utils.respondForbidden
import domain.models.UserRole
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentLookUpRoute(studentLookupService: StudentLookUpService) {

    route("api/v1/lecturer/student") {

        // Look up student by registration number
        post("/lookup") {

            val userId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Invalid user ID")
            val role = call.getUserRoleFromJWT() ?: return@post call.respondBadRequest("Invalid request")
            if (role != UserRole.LECTURER.name) {
                return@post call.respondForbidden("You are not allowed to access this resource")
            }

            val request = call.receive<StudentLookupRequest>()

            val response = studentLookupService.lookupStudent(
                lecturerId = userId,
                request = request
            )

            call.respond(response)

        }
    }
}