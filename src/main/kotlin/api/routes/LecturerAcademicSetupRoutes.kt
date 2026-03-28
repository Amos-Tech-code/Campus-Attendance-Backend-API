package api.routes

import api.dtos.response.GenericResponseDto
import com.amos_tech_code.domain.dtos.requests.AcademicSetUpRequest
import com.amos_tech_code.domain.dtos.requests.AddAcademicTermRequest
import com.amos_tech_code.domain.dtos.requests.AddProgrammeRequest
import com.amos_tech_code.domain.dtos.requests.AddTeachingAssignmentRequest
import com.amos_tech_code.domain.dtos.requests.AddUnitToProgrammeRequest
import com.amos_tech_code.domain.dtos.requests.DepartmentSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.ProgrammeSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UnitSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UniversitySuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UpdateAcademicTermRequest
import com.amos_tech_code.domain.dtos.requests.UpdateProgrammeRequest
import com.amos_tech_code.domain.dtos.requests.UpdateTeachingAssignmentRequest
import com.amos_tech_code.domain.dtos.requests.UpdateUnitRequest
import domain.models.UserRole
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.utils.AuthenticationException
import com.amos_tech_code.utils.ValidationException
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.getUserRoleFromJWT
import com.amos_tech_code.utils.respondBadRequest
import com.amos_tech_code.utils.respondForbidden
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

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

        // ============ UPDATE OPERATIONS ============

        /**
         * Deactivate university for current lecturer
         * Returns GenericResponseDto
         */
        patch("/universities/{universityId}/deactivate") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val universityId = call.parameters["universityId"]
                ?: throw ValidationException("University ID required")

            val response = lecturerAcademicService.deactivateUniversityForLecturer(
                lecturerId = lecturerId,
                universityId = UUID.fromString(universityId)
            )

            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * Reactivate university for current lecturer
         * Returns GenericResponseDto
         */
        patch("/universities/{universityId}/reactivate") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val universityId = call.parameters["universityId"]
                ?: throw ValidationException("University ID required")

            val response = lecturerAcademicService.reactivateUniversityForLecturer(
                lecturerId = lecturerId,
                universityId = UUID.fromString(universityId)
            )

            call.respond(HttpStatusCode.OK, response)
        }

        // ============ ACADEMIC TERM OPERATIONS ============

        /**
         * Add new academic term
         * Returns GenericResponseDto
         */
        post("/universities/{universityId}/terms") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val universityId = call.parameters["universityId"]
                ?: throw ValidationException("University ID required")

            val request = call.receive<AddAcademicTermRequest>()

            lecturerAcademicService.addAcademicTerm(
                lecturerId = lecturerId,
                universityId = UUID.fromString(universityId),
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Academic term added successfully"
            ))
        }

        /**
         * Update academic term
         * Returns GenericResponseDto
         */
        patch("/terms/{termId}") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val termId = call.parameters["termId"]
                ?: throw ValidationException("Term ID required")

            val request = call.receive<UpdateAcademicTermRequest>()

            lecturerAcademicService.updateAcademicTerm(
                lecturerId = lecturerId,
                termId = UUID.fromString(termId),
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Academic term updated successfully"
            ))
        }

        /**
         * Activate a specific term (deactivates all others)
         * Returns GenericResponseDto
         */
        patch("/terms/{termId}/activate") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val termId = call.parameters["termId"]
                ?: throw ValidationException("Term ID required")

            val response = lecturerAcademicService.activateTerm(
                lecturerId = lecturerId,
                termId = UUID.fromString(termId)
            )

            call.respond(HttpStatusCode.OK, response)
        }

        // ============ PROGRAMME OPERATIONS ============

        /**
         * Add new programme
         * Returns GenericResponseDto
         */
        post("/universities/{universityId}/programmes") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val universityId = call.parameters["universityId"]
                ?: throw ValidationException("University ID required")

            val request = call.receive<AddProgrammeRequest>()

            lecturerAcademicService.addProgramme(
                lecturerId = lecturerId,
                universityId = UUID.fromString(universityId),
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Programme added successfully"
            ))
        }

        /**
         * Update programme
         * Returns GenericResponseDto
         */
        patch("/programmes/{programmeId}") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val programmeId = call.parameters["programmeId"]
                ?: throw ValidationException("Programme ID required")

            val request = call.receive<UpdateProgrammeRequest>()

            lecturerAcademicService.updateProgramme(
                lecturerId = lecturerId,
                programmeId = UUID.fromString(programmeId),
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Programme updated successfully"
            ))
        }

        /**
         * Deactivate programme
         * Returns GenericResponseDto
         */
        delete("/programmes/{programmeId}") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val programmeId = call.parameters["programmeId"]
                ?: throw ValidationException("Programme ID required")

            val response = lecturerAcademicService.deactivateProgramme(
                lecturerId = lecturerId,
                programmeId = UUID.fromString(programmeId)
            )

            call.respond(HttpStatusCode.OK, response)
        }

        // ============ UNIT OPERATIONS ============

        /**
         * Add unit to programme
         * Returns GenericResponseDto
         */
        post("/programmes/{programmeId}/units") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val programmeId = call.parameters["programmeId"]
                ?: throw ValidationException("Programme ID required")

            val request = call.receive<AddUnitToProgrammeRequest>()

            lecturerAcademicService.addUnitToProgramme(
                lecturerId = lecturerId,
                programmeId = UUID.fromString(programmeId),
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Unit added successfully"
            ))
        }

        /**
         * Update unit
         * Returns GenericResponseDto
         */
        patch("/units/{unitId}") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val unitId = call.parameters["unitId"]
                ?: throw ValidationException("Unit ID required")

            val request = call.receive<UpdateUnitRequest>()

            lecturerAcademicService.updateUnit(
                lecturerId = lecturerId,
                unitId = UUID.fromString(unitId),
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Unit updated successfully"
            ))
        }

        /**
         * Deactivate unit
         * Returns GenericResponseDto
         */
        delete("/units/{unitId}") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val unitId = call.parameters["unitId"]
                ?: throw ValidationException("Unit ID required")

            val response = lecturerAcademicService.deactivateUnit(
                lecturerId = lecturerId,
                unitId = UUID.fromString(unitId)
            )

            call.respond(HttpStatusCode.OK, response)
        }

        // ============ TEACHING ASSIGNMENT OPERATIONS ============

        /**
         * Add teaching assignment
         * Returns GenericResponseDto
         */
        post("/teaching-assignments") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val request = call.receive<AddTeachingAssignmentRequest>()

            lecturerAcademicService.addTeachingAssignment(
                lecturerId = lecturerId,
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Teaching assignment added successfully"
            ))
        }

        /**
         * Update teaching assignment
         * Returns GenericResponseDto
         */
        patch("/teaching-assignments/{assignmentId}") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val assignmentId = call.parameters["assignmentId"]
                ?: throw ValidationException("Assignment ID required")

            val request = call.receive<UpdateTeachingAssignmentRequest>()

            lecturerAcademicService.updateTeachingAssignment(
                lecturerId = lecturerId,
                assignmentId = UUID.fromString(assignmentId),
                request = request
            )

            call.respond(HttpStatusCode.OK, GenericResponseDto(
                statusCode = 200,
                message = "Teaching assignment updated successfully"
            ))
        }

        /**
         * Delete teaching assignment
         * Returns GenericResponseDto
         */
        delete("/teaching-assignments/{assignmentId}") {
            val lecturerId = call.getUserIdFromJWT()
                ?: throw AuthenticationException("Not authenticated")

            val assignmentId = call.parameters["assignmentId"]
                ?: throw ValidationException("Assignment ID required")

            val response = lecturerAcademicService.deleteTeachingAssignment(
                lecturerId = lecturerId,
                assignmentId = UUID.fromString(assignmentId)
            )

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