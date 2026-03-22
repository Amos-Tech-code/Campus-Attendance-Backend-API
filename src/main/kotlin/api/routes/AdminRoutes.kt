package com.amos_tech_code.api.routes

import com.amos_tech_code.api.dtos.admin.AdminLoginRequest
import com.amos_tech_code.api.dtos.admin.CreateAdminRequest
import com.amos_tech_code.api.dtos.admin.RefreshTokenRequest
import com.amos_tech_code.api.dtos.admin.UpdateAdminRequest
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID


fun Route.adminRoutes(
    adminAuthService: AdminAuthService,        // Only auth
    adminDashboardService: AdminDashboardService,  // Only dashboard
    adminManagementService: AdminManagementService
) {
    // Serve HTML pages
    route("/admin") {
        get("/login") {
            val htmlContent = this::class.java.classLoader
                .getResourceAsStream("templates/admin/login.html")
                ?.bufferedReader()
                .use { it?.readText() }

            if (htmlContent != null) {
                call.respondText(htmlContent, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/dashboard") {
            val htmlContent = this::class.java.classLoader
                .getResourceAsStream("templates/admin/dashboard.html")
                ?.bufferedReader()
                .use { it?.readText() }

            if (htmlContent != null) {
                call.respondText(htmlContent, ContentType.Text.Html)
            } else {
                call.respondText("Dashboard page not found", status = HttpStatusCode.NotFound)
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
                val stats = adminDashboardService.getDashboardStats()
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
                    call.respond(HttpStatusCode.BadRequest, "Email already exists")
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
                val currentAdminId = call.principal<JWTPrincipal>()?.payload?.getClaim("adminId")?.asString()
                    ?.let { UUID.fromString(it) }

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
        }
    }
}