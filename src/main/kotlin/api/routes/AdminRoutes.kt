package com.amos_tech_code.api.routes

import com.amos_tech_code.api.dtos.admin.AdminDeviceChangeApprovalRequest
import com.amos_tech_code.api.dtos.admin.AdminLoginRequest
import com.amos_tech_code.api.dtos.admin.CleanupRequest
import com.amos_tech_code.api.dtos.admin.CreateAcademicTermRequest
import com.amos_tech_code.api.dtos.admin.CreateAdminRequest
import com.amos_tech_code.api.dtos.admin.CreateDepartmentRequest
import com.amos_tech_code.api.dtos.admin.CreateProgrammeRequest
import com.amos_tech_code.api.dtos.admin.CreateUnitRequest
import com.amos_tech_code.api.dtos.admin.CreateUniversityRequest
import com.amos_tech_code.api.dtos.admin.LinkUnitToProgrammeRequest
import com.amos_tech_code.api.dtos.admin.RefreshTokenRequest
import com.amos_tech_code.api.dtos.admin.ReviewSuspiciousActivityRequest
import com.amos_tech_code.api.dtos.admin.UpdateAcademicTermRequest
import com.amos_tech_code.api.dtos.admin.UpdateAdminRequest
import com.amos_tech_code.api.dtos.admin.UpdateDepartmentRequest
import com.amos_tech_code.api.dtos.admin.UpdateLecturerRequest
import com.amos_tech_code.api.dtos.admin.UpdateProgrammeRequest
import com.amos_tech_code.api.dtos.admin.UpdateStudentRequest
import com.amos_tech_code.api.dtos.admin.UpdateUnitRequest
import com.amos_tech_code.api.dtos.admin.UpdateUniversityRequest
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminDeviceChangeService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import com.amos_tech_code.domain.services.impl.LecturerStudentManagementService
import com.amos_tech_code.domain.services.impl.StorageManagementService
import com.amos_tech_code.domain.services.impl.SuspiciousActivityService
import com.amos_tech_code.domain.services.impl.UniversityStructureService
import com.amos_tech_code.utils.getUserIdFromJWT
import domain.models.DeviceChangeStatus
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import utils.toUUID
import java.util.UUID


fun Route.adminRoutes(
    adminAuthService: AdminAuthService,        // Only auth
    adminDashboardService: AdminDashboardService,  // Only dashboard
    adminManagementService: AdminManagementService,
    lecturerStudentManagementService: LecturerStudentManagementService,
    universityStructureService: UniversityStructureService,
    adminDeviceChangeService: AdminDeviceChangeService,
    suspiciousActivityService: SuspiciousActivityService,
    storageManagementService: StorageManagementService,
) {
    // Serve HTML pages
    route("/admin") {

        get {
            val htmlContent = this::class.java.classLoader
                .getResourceAsStream("templates/admin/index.html")
                ?.bufferedReader()
                .use { it?.readText() }

            if (htmlContent != null) {
                call.respondText(htmlContent, ContentType.Text.Html)
            } else {
                call.respondText("Admin page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/login") {
            val htmlContent = this::class.java.classLoader
                .getResourceAsStream("templates/admin/modules/login.html")
                ?.bufferedReader()
                .use { it?.readText() }

            if (htmlContent != null) {
                call.respondText(htmlContent, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

    }


    // ========== API ROUTES ==========

    route("/admin/api") {
        // Public endpoints - Auth service only
        post("/login") {
            val request = call.receive<AdminLoginRequest>()
            val response = adminAuthService.login(request)

            if (response != null) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid email or password")
            }
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val response = adminAuthService.refreshToken(request.refreshToken)

            if (response != null) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid refresh token")
            }
        }

        // Protected endpoints
        authenticate("admin-jwt") {
            // Dashboard service
            get("/dashboard") {
                val adminId = call.getUserIdFromJWT() ?: return@get call.respondText("Authentication failed", status = HttpStatusCode.Unauthorized)
                val stats = adminDashboardService.getDashboardStats(adminId)
                call.respond(HttpStatusCode.OK, stats)
            }


            // Admin Management
            get("/admins") {
                val admins = adminManagementService.getAllAdmins()
                call.respond(HttpStatusCode.OK, admins)
            }

            get("/admins/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid admin ID")
                val admin = adminManagementService.getAdminById(id)
                if (admin != null) {
                    call.respond(HttpStatusCode.OK, admin)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Admin not found")
                }
            }

            post("/admins") {
                val request = call.receive<CreateAdminRequest>()
                val newAdmin = adminManagementService.createAdmin(request)
                if (newAdmin != null) {
                    call.respond(HttpStatusCode.Created, newAdmin)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create a new admin")
                }
            }

            put("/admins/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid admin ID")
                val request = call.receive<UpdateAdminRequest>()
                val updated = adminManagementService.updateAdmin(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Admin updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Admin not found")
                }
            }

            delete("/admins/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid admin ID")

                // Get current admin from token
                val currentAdminId = call.getUserIdFromJWT()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid admin ID")

                if (currentAdminId == id) {
                    call.respond(HttpStatusCode.BadRequest, "Cannot delete your own account")
                    return@delete
                }

                val deleted = adminManagementService.deleteAdmin(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Admin deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Cannot delete last admin")
                }
            }

            post("/admins/{id}/reset-password") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid admin ID")
                val request = call.receive<Map<String, String>>()
                val newPassword = request["newPassword"] ?: throw IllegalArgumentException("New password required")

                val reset = adminManagementService.resetAdminPassword(id, newPassword)
                if (reset) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Password reset successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Admin not found")
                }
            }

            // Auth service
            post("/logout") {
                val request = call.receive<RefreshTokenRequest>()
                adminAuthService.logout(request.refreshToken)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
            }


            get("/lecturers") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val search = call.request.queryParameters["search"]
                val status = call.request.queryParameters["status"]?.toBooleanStrictOrNull()

                val result = lecturerStudentManagementService.getAllLecturers(
                    page = page,
                    pageSize = pageSize,
                    search = search,
                    status = status
                )
                call.respond(HttpStatusCode.OK, result)
            }

            get("/lecturers/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid lecturer ID")
                val lecturer = lecturerStudentManagementService.getLecturerById(id)
                if (lecturer != null) {
                    call.respond(HttpStatusCode.OK, lecturer)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Lecturer not found")
                }
            }

            put("/lecturers/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid lecturer ID")
                val request = call.receive<UpdateLecturerRequest>()
                val updated = lecturerStudentManagementService.updateLecturer(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Lecturer updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Lecturer not found")
                }
            }

            delete("/lecturers/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid lecturer ID")
                val deleted = lecturerStudentManagementService.deleteLecturer(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Lecturer deactivated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Lecturer not found")
                }
            }

            post("/lecturers/{id}/activate") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid lecturer ID")
                val activated = lecturerStudentManagementService.activateLecturer(id)
                if (activated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Lecturer activated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Lecturer not found")
                }
            }


            get("/students") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val search = call.request.queryParameters["search"]
                val status = call.request.queryParameters["status"]?.toBooleanStrictOrNull()

                val result = lecturerStudentManagementService.getAllStudents(
                    page = page,
                    pageSize = pageSize,
                    search = search,
                    status = status
                )
                call.respond(HttpStatusCode.OK, result)
            }

            get("/students/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid student ID")
                val student = lecturerStudentManagementService.getStudentById(id)
                if (student != null) {
                    call.respond(HttpStatusCode.OK, student)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Student not found")
                }
            }

            put("/students/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid student ID")
                val request = call.receive<UpdateStudentRequest>()
                val updated = lecturerStudentManagementService.updateStudent(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Student updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Student not found")
                }
            }

            delete("/students/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid student ID")
                val deleted = lecturerStudentManagementService.deleteStudent(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Student deactivated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Student not found")
                }
            }

            post("/students/{id}/activate") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid student ID")
                val activated = lecturerStudentManagementService.activateStudent(id)
                if (activated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Student activated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Student not found")
                }
            }


            // ========== UNIVERSITY ROUTES ==========
            get("/universities") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val search = call.request.queryParameters["search"]

                val result = universityStructureService.getAllUniversities(
                    page = page,
                    pageSize = pageSize,
                    search = search
                )
                call.respond(HttpStatusCode.OK, result)
            }

            get("/universities/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid university ID")
                val university = universityStructureService.getUniversityById(id)
                if (university != null) {
                    call.respond(HttpStatusCode.OK, university)
                } else {
                    call.respond(HttpStatusCode.NotFound, "University not found")
                }
            }

            post("/universities") {
                val request = call.receive<CreateUniversityRequest>()
                val university = universityStructureService.createUniversity(request)
                if (university != null) {
                    call.respond(HttpStatusCode.Created, university)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create university")
                }
            }

            put("/universities/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid university ID")
                val request = call.receive<UpdateUniversityRequest>()
                val updated = universityStructureService.updateUniversity(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "University updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "University not found")
                }
            }

            delete("/universities/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid university ID")
                val deleted = universityStructureService.deleteUniversity(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "University deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Cannot delete university with existing departments/programmes/units")
                }
            }

            // ========== DEPARTMENT ROUTES ==========
            get("/departments") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val universityId = call.request.queryParameters["universityId"]?.let { UUID.fromString(it) }
                val search = call.request.queryParameters["search"]

                val result = universityStructureService.getAllDepartments(
                    page = page,
                    pageSize = pageSize,
                    universityId = universityId,
                    search = search
                )
                call.respond(HttpStatusCode.OK, result)
            }

            post("/departments") {
                val request = call.receive<CreateDepartmentRequest>()
                val department = universityStructureService.createDepartment(request)
                if (department != null) {
                    call.respond(HttpStatusCode.Created, department)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create department")
                }
            }

            put("/departments/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid department ID")
                val request = call.receive<UpdateDepartmentRequest>()
                val updated = universityStructureService.updateDepartment(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Department updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Department not found")
                }
            }

            delete("/departments/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid department ID")
                val deleted = universityStructureService.deleteDepartment(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Department deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Cannot delete department with existing programmes")
                }
            }

            // ========== PROGRAMME ROUTES ==========
            get("/programmes") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val universityId = call.request.queryParameters["universityId"]?.let { UUID.fromString(it) }
                val departmentId = call.request.queryParameters["departmentId"]?.let { UUID.fromString(it) }
                val search = call.request.queryParameters["search"]
                val activeOnly = call.request.queryParameters["activeOnly"]?.toBooleanStrictOrNull() ?: false

                val result = universityStructureService.getAllProgrammes(
                    page = page,
                    pageSize = pageSize,
                    universityId = universityId,
                    departmentId = departmentId,
                    search = search,
                    activeOnly = activeOnly
                )
                call.respond(HttpStatusCode.OK, result)
            }

            post("/programmes") {
                val request = call.receive<CreateProgrammeRequest>()
                val programme = universityStructureService.createProgramme(request)
                if (programme != null) {
                    call.respond(HttpStatusCode.Created, programme)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create programme")
                }
            }

            put("/programmes/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid programme ID")
                val request = call.receive<UpdateProgrammeRequest>()
                val updated = universityStructureService.updateProgramme(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Programme updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Programme not found")
                }
            }

            delete("/programmes/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid programme ID")
                val deleted = universityStructureService.deleteProgramme(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Programme deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Cannot delete programme with existing enrollments")
                }
            }

            // ========== UNIT ROUTES ==========
            get("/units") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val universityId = call.request.queryParameters["universityId"]?.let { UUID.fromString(it) }
                val departmentId = call.request.queryParameters["departmentId"]?.let { UUID.fromString(it) }
                val search = call.request.queryParameters["search"]
                val activeOnly = call.request.queryParameters["activeOnly"]?.toBooleanStrictOrNull() ?: false

                val result = universityStructureService.getAllUnits(
                    page = page,
                    pageSize = pageSize,
                    universityId = universityId,
                    departmentId = departmentId,
                    search = search,
                    activeOnly = activeOnly
                )
                call.respond(HttpStatusCode.OK, result)
            }

            post("/units") {
                val request = call.receive<CreateUnitRequest>()
                val unit = universityStructureService.createUnit(request)
                if (unit != null) {
                    call.respond(HttpStatusCode.Created, unit)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create unit")
                }
            }

            put("/units/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid unit ID")
                val request = call.receive<UpdateUnitRequest>()
                val updated = universityStructureService.updateUnit(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Unit updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Unit not found")
                }
            }

            delete("/units/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid unit ID")
                val deleted = universityStructureService.deleteUnit(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Unit deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Cannot delete unit with existing teaching assignments")
                }
            }

            post("/units/{id}/link-programme") {
                val unitId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid unit ID")
                val request = call.receive<LinkUnitToProgrammeRequest>()
                val linked = universityStructureService.linkUnitToProgramme(unitId, request)
                if (linked) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Unit linked to programme successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Unit already linked to this programme")
                }
            }

            delete("/units/{unitId}/programmes/{programmeId}") {
                val unitId = call.parameters["unitId"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid unit ID")
                val programmeId = call.parameters["programmeId"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid programme ID")
                val unlinked = universityStructureService.unlinkUnitFromProgramme(unitId, programmeId)
                if (unlinked) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Unit unlinked from programme successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Link not found")
                }
            }


            // ========== ACADEMIC TERMS ROUTES ==========
            get("/academic-terms") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val universityId = call.request.queryParameters["universityId"]?.let { UUID.fromString(it) }
                val search = call.request.queryParameters["search"]
                val activeOnly = call.request.queryParameters["activeOnly"]?.toBooleanStrictOrNull() ?: false

                val result = universityStructureService.getAllAcademicTerms(
                    page = page,
                    pageSize = pageSize,
                    universityId = universityId,
                    search = search,
                    activeOnly = activeOnly
                )
                call.respond(HttpStatusCode.OK, result)
            }

            get("/academic-terms/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid academic term ID")
                val term = universityStructureService.getAcademicTermById(id)
                if (term != null) {
                    call.respond(HttpStatusCode.OK, term)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Academic term not found")
                }
            }

            post("/academic-terms") {
                val request = call.receive<CreateAcademicTermRequest>()

                // Validate semester
                if (request.semester !in 1..2) {
                    call.respond(HttpStatusCode.BadRequest, "Semester must be 1 or 2")
                    return@post
                }

                val term = universityStructureService.createAcademicTerm(request)
                if (term != null) {
                    call.respond(HttpStatusCode.Created, term)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Academic term already exists for this university, year, and semester")
                }
            }

            put("/academic-terms/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid academic term ID")
                val request = call.receive<UpdateAcademicTermRequest>()

                // Validate semester if provided
                if (request.semester != null && request.semester !in 1..3) {
                    call.respond(HttpStatusCode.BadRequest, "Semester must be 1, 2 or 3")
                    return@put
                }

                val updated = universityStructureService.updateAcademicTerm(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Academic term updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Academic term not found")
                }
            }

            delete("/academic-terms/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid academic term ID")
                val deleted = universityStructureService.deleteAcademicTerm(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Academic term deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Cannot delete term with existing sessions or enrollments")
                }
            }

            post("/academic-terms/{id}/set-active") {
                val termId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid academic term ID")

                // Get the term to find its university
                val term = universityStructureService.getAcademicTermById(termId)
                if (term == null) {
                    call.respond(HttpStatusCode.NotFound, "Academic term not found")
                    return@post
                }

                val activated = universityStructureService.setActiveAcademicTerm(term.universityId.toUUID(), termId)
                if (activated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Academic term set as active successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to set academic term as active")
                }
            }

            get("/universities/{universityId}/active-term") {
                val universityId = call.parameters["universityId"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid university ID")
                val activeTerm = universityStructureService.getActiveAcademicTerm(universityId)
                if (activeTerm != null) {
                    call.respond(HttpStatusCode.OK, activeTerm)
                } else {
                    call.respond(HttpStatusCode.NotFound, "No active academic term found for this university")
                }
            }


            // ========== DEVICE CHANGE REQUESTS (ADMIN) ==========
            get("/device-change-requests") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val status = call.request.queryParameters["status"]?.let {
                    DeviceChangeStatus.valueOf(it)
                }
                val studentId = call.request.queryParameters["studentId"]?.let { UUID.fromString(it) }
                val search = call.request.queryParameters["search"]

                val result = adminDeviceChangeService.getAllDeviceChangeRequests(
                    page = page,
                    pageSize = pageSize,
                    status = status,
                    studentId = studentId,
                    search = search
                )
                call.respond(HttpStatusCode.OK, result)
            }

            get("/device-change-requests/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid request ID")
                val request = adminDeviceChangeService.getDeviceChangeRequestById(id)
                if (request != null) {
                    call.respond(HttpStatusCode.OK, request)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Device change request not found")
                }
            }

            post("/device-change-requests/{id}/review") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid request ID")
                val adminId = call.getUserIdFromJWT()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Authentication failed")
                val approvalRequest = call.receive<AdminDeviceChangeApprovalRequest>()

                try {
                    val result = adminDeviceChangeService.approveDeviceChangeRequest(
                        adminId = adminId,
                        requestId = id,
                        approvalRequest = approvalRequest
                    )
                    if (result) {
                        val message = if (approvalRequest.approve) "Request approved successfully" else "Request rejected successfully"
                        call.respond(HttpStatusCode.OK, mapOf("message" to message))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Failed to process request")
                    }
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request state")
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, e.message ?: "Request not found")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to process request")
                }
            }

            // ========== SUSPICIOUS ACTIVITY ROUTES ==========
            get("/suspicious-activity") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val studentId = call.request.queryParameters["studentId"]?.let { UUID.fromString(it) }
                val sessionId = call.request.queryParameters["sessionId"]?.let { UUID.fromString(it) }
                val unitId = call.request.queryParameters["unitId"]?.let { UUID.fromString(it) }
                val search = call.request.queryParameters["search"]

                val result = suspiciousActivityService.getAllSuspiciousActivities(
                    page = page,
                    pageSize = pageSize,
                    studentId = studentId,
                    sessionId = sessionId,
                    unitId = unitId,
                    search = search
                )
                call.respond(HttpStatusCode.OK, result)
            }

            get("/suspicious-activity/{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid activity ID")
                val activity = suspiciousActivityService.getSuspiciousActivityById(id)
                if (activity != null) {
                    call.respond(HttpStatusCode.OK, activity)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Suspicious activity not found")
                }
            }

            get("/suspicious-activity/stats") {
                val stats = suspiciousActivityService.getSuspiciousActivityStats()
                call.respond(HttpStatusCode.OK, stats)
            }

            post("/suspicious-activity/{id}/review") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid activity ID")
                val request = call.receive<ReviewSuspiciousActivityRequest>()

                val updated = suspiciousActivityService.reviewSuspiciousActivity(id, request)
                if (updated) {
                    val message = if (request.isSuspicious) "Activity marked as suspicious" else "Activity cleared"
                    call.respond(HttpStatusCode.OK, mapOf("message" to message))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Activity not found")
                }
            }

            // ========== STORAGE MANAGEMENT ROUTES ==========
            get("/storage/files") {
                val type = call.request.queryParameters["type"] // "all", "orphaned", "expired"
                val files = when (type) {
                    "orphaned" -> storageManagementService.getOrphanedFiles()
                    "expired" -> storageManagementService.getExpiredFiles()
                    else -> storageManagementService.getAllFiles()
                }
                call.respond(HttpStatusCode.OK, files)
            }

            get("/storage/stats") {
                val stats = storageManagementService.getStorageStats()
                call.respond(HttpStatusCode.OK, stats)
            }

            post("/storage/cleanup") {
                val request = call.receive<CleanupRequest>()
                val result = storageManagementService.cleanupFiles(request)
                call.respond(HttpStatusCode.OK, result)
            }

            delete("/storage/files/{fileId}") {
                val fileId = call.parameters["fileId"] ?: throw IllegalArgumentException("Invalid file ID")
                val deleted = storageManagementService.deleteSingleFile(fileId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "File deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "File not found or could not be deleted")
                }
            }


        }
    }
}