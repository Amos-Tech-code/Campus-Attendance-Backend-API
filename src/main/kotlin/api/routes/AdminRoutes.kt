package com.amos_tech_code.api.routes

import com.amos_tech_code.api.dtos.response.AdminLoginRequest
import com.amos_tech_code.api.dtos.response.RefreshTokenRequest
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.adminRoutes(
    adminAuthService: AdminAuthService,        // Only auth
    adminDashboardService: AdminDashboardService  // Only dashboard
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

            // Auth service
            post("/logout") {
                val request = call.receive<RefreshTokenRequest>()
                adminAuthService.logout(request.refreshToken)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
            }
        }
    }
}