package com.amos_tech_code.api.routes

import com.amos_tech_code.api.dtos.requests.FcmTokenRequest
import com.amos_tech_code.api.dtos.requests.UpdateLecturerProfileRequest
import com.amos_tech_code.api.dtos.requests.UpdateStudentProfileRequest
import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.amos_tech_code.domain.services.AccountService
import com.amos_tech_code.utils.getUserIdFromJWT
import com.amos_tech_code.utils.getUserRoleFromJWT
import com.amos_tech_code.utils.respondBadRequest
import com.amos_tech_code.utils.respondForbidden
import domain.models.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.accountRoutes(
    accountService: AccountService
) {

    route("api/v1/account") {

        route("/profile") {

            patch("/lecturer") {

                val lecturerId = call.getUserIdFromJWT() ?: return@patch call.respondBadRequest("Lecturer ID is required")
                if (call.getUserRoleFromJWT()?.uppercase() != UserRole.LECTURER.name) return@patch call.respondForbidden()

                val request = call.receive<UpdateLecturerProfileRequest>()

                accountService.updateLecturerProfile(lecturerId, request)

                call.respond(
                    HttpStatusCode.OK,
                    GenericResponseDto(HttpStatusCode.OK.value, "Profile updated successfully")
                )
            }

            patch("/student") {

                val studentId = call.getUserIdFromJWT() ?: return@patch call.respondBadRequest("Student ID is required")
                if (call.getUserRoleFromJWT()?.uppercase() != UserRole.STUDENT.name) return@patch call.respondForbidden()

                val request = call.receive<UpdateStudentProfileRequest>()

                accountService.updateStudentProfile(studentId, request)

                call.respond(
                    HttpStatusCode.OK,
                    GenericResponseDto(HttpStatusCode.OK.value, "Profile updated successfully")
                )
            }
        }

        // FCM Token Update
        route("/fcm-token") {

            patch("/student") {
                val studentId = call.getUserIdFromJWT() ?: return@patch call.respondForbidden("Student ID is required")
                val request = call.receive<FcmTokenRequest>()

                accountService.updateStudentFcmToken(studentId, request.fcmToken)

                call.respond(HttpStatusCode.OK, mapOf("message" to "FCM token updated"))

            }

            patch("/lecturer") {
                val lecturerId =
                    call.getUserIdFromJWT() ?: return@patch call.respondBadRequest("Lecturer ID is required")
                val request = call.receive<FcmTokenRequest>()

                accountService.updateLecturerFcmToken(lecturerId, request.fcmToken)

                call.respond(HttpStatusCode.OK, mapOf("message" to "FCM token updated"))

            }
        }
    }
}