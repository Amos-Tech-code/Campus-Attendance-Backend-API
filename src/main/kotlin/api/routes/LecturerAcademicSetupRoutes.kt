package api.routes

import com.amos_tech_code.domain.dtos.requests.*
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.utils.*
import domain.dtos.requests.AddAcademicTermRequest
import domain.dtos.requests.AddProgrammeWithUnitsRequest
import domain.dtos.requests.AddUnitToProgrammeRequest
import domain.dtos.requests.UpdateProgrammeDetailsRequest
import domain.models.UserRole
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

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

        /**
         * Lecturer deactivates themselves from a university
         * This removes the lecturer's teaching assignments for this university
         * Does NOT delete the university entity itself
         */
        delete("/universities/{universityId}/deactivate") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val universityId = UUID.fromString(
                call.parameters["universityId"]
                    ?: throw ValidationException("University ID required")
            )

            val response = lecturerAcademicService.deactivateUniversityForLecturer(
                lecturerId = lecturerId,
                universityId = universityId
            )

            call.respond(HttpStatusCode.OK, response)
        }
        /**
         * Add a new academic term to a university
         * Creates a new term and optionally activates it
         */
        post("/universities/{universityId}/terms") {
            val lecturerId = call.getUserIdFromJWT() ?: throw AuthenticationException("Not authenticated")
            val universityId = UUID.fromString(call.parameters["universityId"] ?: throw ValidationException("University ID required"))
            val request = call.receive<AddAcademicTermRequest>()

            val response = lecturerAcademicService.addAcademicTerm(lecturerId, universityId, request)
            call.respond(HttpStatusCode.Created, response)
        }


        /**
         * Add a new programme with at least one unit
         * Creates programme and automatically creates teaching assignments
         */
        post("/universities/{universityId}/programmes") {
            val lecturerId = call.getUserIdFromJWT() ?: throw AuthenticationException("Not authenticated")
            val universityId = UUID.fromString(call.parameters["universityId"] ?: throw ValidationException("University ID required"))
            val request = call.receive<AddProgrammeWithUnitsRequest>()

            val response = lecturerAcademicService.addProgrammeWithUnits(lecturerId, universityId, request)
            call.respond(HttpStatusCode.Created, response)
        }

        /**
         * Update programme details (name, year, student count)
         * Does NOT modify units - use separate unit endpoints for that
         */
        patch("/programmes/{programmeId}") {
            val lecturerId = call.getUserIdFromJWT() ?: throw AuthenticationException("Not authenticated")
            val programmeId = UUID.fromString(call.parameters["programmeId"] ?: throw ValidationException("Programme ID required"))
            val request = call.receive<UpdateProgrammeDetailsRequest>()

            val response = lecturerAcademicService.updateProgrammeDetails(lecturerId, programmeId, request)
            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * Deactivate a programme (soft delete)
         */
        delete("/programmes/{programmeId}") {
            val lecturerId = call.getUserIdFromJWT() ?: throw AuthenticationException("Not authenticated")
            val programmeId = UUID.fromString(call.parameters["programmeId"] ?: throw ValidationException("Programme ID required"))

            val response = lecturerAcademicService.deactivateProgramme(lecturerId, programmeId)
            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * Add a new unit to a programme
         * Creates unit, links to programme, and creates teaching assignment
         */
        post("/programmes/{programmeId}/units") {
            val lecturerId = call.getUserIdFromJWT() ?: throw AuthenticationException("Not authenticated")
            val programmeId = UUID.fromString(call.parameters["programmeId"] ?: throw ValidationException("Programme ID required"))
            val request = call.receive<AddUnitToProgrammeRequest>()

            val response = lecturerAcademicService.addUnitToProgramme(lecturerId, programmeId, request)
            call.respond(HttpStatusCode.Created, response)
        }

        /**
         * Remove a unit from a programme (soft delete)
         * Does NOT delete the unit entity, just removes from teaching assignments
         */
        delete("/programmes/{programmeId}/units/{unitId}") {
            val lecturerId = call.getUserIdFromJWT() ?: throw AuthenticationException("Not authenticated")
            val programmeId = UUID.fromString(call.parameters["programmeId"] ?: throw ValidationException("Programme ID required"))
            val unitId = UUID.fromString(call.parameters["unitId"] ?: throw ValidationException("Unit ID required"))

            val response = lecturerAcademicService.removeUnitFromProgramme(lecturerId, programmeId, unitId)
            call.respond(HttpStatusCode.OK, response)
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