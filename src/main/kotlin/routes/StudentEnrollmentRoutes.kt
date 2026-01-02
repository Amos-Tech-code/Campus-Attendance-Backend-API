package com.amos_tech_code.routes

import com.amos_tech_code.domain.dtos.requests.StudentEnrollmentRequest
import com.amos_tech_code.domain.dtos.requests.UpdateYearRequest
import com.amos_tech_code.services.StudentEnrollmentService
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.respondBadRequest
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.studentEnrollmentRoutes(
    enrollmentService: StudentEnrollmentService
) {
    route("api/v1/students/enrollments") {
        // Enroll in a programme
        post {

            val studentId = call.getUserIdFromJWT()
                ?: return@post call.respondBadRequest("Student ID is required")

            val request = call.receive<StudentEnrollmentRequest>()

            val response = enrollmentService.enrollStudent(
                studentId = studentId,
                request = request
            )

            call.respond(response)
        }

        // Get all active enrollment for student
        get {
            val studentId = call.getUserIdFromJWT()
                ?: return@get call.respondBadRequest("Student ID is required.")

            val enrollment = enrollmentService.getStudentEnrollment(
                studentId = studentId
            )

            call.respond(enrollment)
        }

        // Deactivate an enrollment
        delete("/{enrollmentId}") {

            val studentId = call.getUserIdFromJWT()
                ?: return@delete call.respondBadRequest("Student ID is required.")

            val enrollmentId = call.parameters["enrollmentId"]
                ?: return@delete call.respondBadRequest("Enrollment ID is required")

            val response = enrollmentService.deactivateEnrollment(
                studentId = studentId,
                enrollmentId = UUID.fromString(enrollmentId)
            )

            call.respond(response)
        }

        // Update year of study for an enrollment
        patch("/{enrollmentId}/year") {

            val studentId = call.getUserIdFromJWT()
                ?: return@patch call.respondBadRequest("Student ID is required.")

            val enrollmentId = call.parameters["enrollmentId"]
                ?: return@patch call.respondBadRequest("Enrollment ID is required")

            val request = call.receive<UpdateYearRequest>()

            val response = enrollmentService.updateEnrollmentYear(
                studentId = studentId,
                enrollmentId = UUID.fromString(enrollmentId),
                newYearOfStudy = request.newYearOfStudy
            )

            call.respond(response)
        }
    }
}