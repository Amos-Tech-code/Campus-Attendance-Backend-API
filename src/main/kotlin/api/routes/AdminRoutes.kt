package com.amos_tech_code.api.routes

import com.amos_tech_code.api.dtos.admin.AdminLoginRequest
import com.amos_tech_code.api.dtos.admin.CreateAdminRequest
import com.amos_tech_code.api.dtos.admin.RefreshTokenRequest
import com.amos_tech_code.api.dtos.admin.UpdateAdminRequest
import com.amos_tech_code.api.dtos.admin.UpdateLecturerRequest
import com.amos_tech_code.api.dtos.admin.UpdateStudentRequest
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import com.amos_tech_code.domain.services.impl.LecturerStudentManagementService
import com.amos_tech_code.utils.getUserIdFromJWT
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID


fun Route.adminRoutes(
    adminAuthService: AdminAuthService,        // Only auth
    adminDashboardService: AdminDashboardService,  // Only dashboard
    adminManagementService: AdminManagementService,
    lecturerStudentManagementService: LecturerStudentManagementService
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

    // API routes
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

        }
    }
}