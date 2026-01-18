package api.routes

import com.amos_tech_code.domain.dtos.requests.GoogleSignInRequest
import com.amos_tech_code.domain.dtos.requests.StudentLoginRequest
import com.amos_tech_code.domain.dtos.requests.StudentRegistrationRequest
import com.amos_tech_code.domain.dtos.response.LecturerAuthResponse
import api.dtos.response.StudentAuthResponse
import com.amos_tech_code.services.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {

    route("api/v1/auth") {
        // Lecturer Google Sign-in
        post("/lecturers/google") {
            val request = call.receive<GoogleSignInRequest>()
            //val result = authService.mockAuthenticateLecturerWithGoogle(request.idToken) // TODO(): Remove
            val result = authService.authenticateLecturerWithGoogle(request.idToken)

            call.respond(
                HttpStatusCode.OK,
                LecturerAuthResponse(
                    token = result.token,
                    email = result.email,
                    name = result.name,
                    profileComplete = result.profileComplete,
                    userType = result.userType,
                )
            )
        }

        /**
         * Mock implementation of authenticateLecturerWithGoogle for testing
         * Simulates Google authentication without actually calling Google API
         */
        post("mock/lecturers/google") {
            val request = call.receive<GoogleSignInRequest>()
            val result = authService.mockAuthenticateLecturerWithGoogle(request.idToken)
            call.respond(
                HttpStatusCode.OK,
                LecturerAuthResponse(
                    token = result.token,
                    email = result.email,
                    name = result.name,
                    profileComplete = result.profileComplete,
                    userType = result.userType,
                )
            )
        }

        // Student Registration
        post("/students/register") {
            val request = call.receive<StudentRegistrationRequest>()

            val result = authService.registerStudent(request)

            call.respond(
                HttpStatusCode.Created,
                StudentAuthResponse(
                token = result.token,
                fullName = result.fullName,
                regNumber = result.regNumber,
                userType = result.userType,
            ))
        }

        // Student Login
        post("/students/login") {
            val request = call.receive<StudentLoginRequest>()
            val result = authService.loginStudent(
                request.registrationNumber,
                request.deviceInfo
            )

            call.respond(
                HttpStatusCode.OK,
                StudentAuthResponse(
                token = result.token,
                fullName = result.fullName,
                regNumber = result.regNumber,
                userType = result.userType
                ))
        }
    }
}