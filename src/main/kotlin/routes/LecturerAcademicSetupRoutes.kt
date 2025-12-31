package com.amos_tech_code.routes

import com.amos_tech_code.domain.dtos.requests.AcademicSetUpRequest
import com.amos_tech_code.domain.dtos.requests.DepartmentSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.ProgrammeSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UnitSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UniversitySuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UpdateAcademicSetupRequest
import com.amos_tech_code.domain.models.UserRole
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.utils.ValidationException
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.getUserRoleFromJWT
import com.amos_tech_code.utils.respondBadRequest
import com.amos_tech_code.utils.respondForbidden
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.lecturerAcademicSetupRoutes(
    lecturerAcademicService : LecturerAcademicService
) {

    route("api/v1/lecturer/academic-setup") {

        post {

            val lecturerId = call.getUserIdFromJWT() ?: return@post call.respondBadRequest("Lecturer ID is required")

            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@post call.respondForbidden()

            val request = call.receive<AcademicSetUpRequest>()

            val academicSetup = lecturerAcademicService.saveAcademicSetup(lecturerId, request)

            call.respond(
                HttpStatusCode.OK,
                academicSetup
            )

        }

        get {

            val lecturerId = call.getUserIdFromJWT() ?: return@get call.respondBadRequest("Lecturer ID is required")

            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@get call.respondForbidden()

            // Optional universityId parameter
            val universityId = call.parameters["universityId"]

            val academicSetup = lecturerAcademicService.getLecturerAcademicSetup(lecturerId, universityId)

            call.respond(
                HttpStatusCode.OK,
                academicSetup
            )
        }

        put {

            val lecturerId = call.getUserIdFromJWT() ?: return@put call.respondBadRequest("Lecturer ID is required")

            if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@put call.respondForbidden()

            val request = call.receive<UpdateAcademicSetupRequest>()

            val updatedSetup = lecturerAcademicService.updateLecturerAcademicSetup(lecturerId, request)

            call.respond(HttpStatusCode.OK, updatedSetup)

        }

        route("/suggestions") {

            get("/universities") {
                val query = call.request.queryParameters["query"] ?: ""
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

                val request = UniversitySuggestionRequest(query = query, limit = limit)
                val suggestions = lecturerAcademicService.suggestUniversities(request)

                call.respond(HttpStatusCode.OK, suggestions)
            }

            get("/departments") {
                val universityId = call.request.queryParameters["universityId"]
                    ?: throw ValidationException("University ID is required")
                val query = call.request.queryParameters["query"] ?: ""
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

                val request = DepartmentSuggestionRequest(
                    universityId = universityId,
                    query = query,
                    limit = limit
                )

                val suggestions = lecturerAcademicService.suggestDepartments(request)

                call.respond(HttpStatusCode.OK, suggestions)
            }

            get("/programmes") {
                val universityId = call.request.queryParameters["universityId"]
                    ?: throw ValidationException("University ID is required")
                val departmentId = call.request.queryParameters["departmentId"]
                val query = call.request.queryParameters["query"] ?: ""
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

                val request = ProgrammeSuggestionRequest(
                    universityId = universityId,
                    departmentId = departmentId,
                    query = query,
                    limit = limit
                )

                val suggestions = lecturerAcademicService.suggestProgrammes(request)

                call.respond(HttpStatusCode.OK, suggestions)
            }

            get("/units") {
                val universityId = call.request.queryParameters["universityId"]
                    ?: throw ValidationException("University ID is required")
                val departmentId = call.request.queryParameters["departmentId"]
                val programmeId = call.request.queryParameters["programmeId"]
                val query = call.request.queryParameters["query"] ?: ""
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

                val request = UnitSuggestionRequest(
                    universityId = universityId,
                    departmentId = departmentId,
                    programmeId = programmeId,
                    query = query,
                    limit = limit
                )

                val suggestions = lecturerAcademicService.suggestUnits(request)

                call.respond(HttpStatusCode.OK, suggestions)
            }
        }

    }
}