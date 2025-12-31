package com.amos_tech_code.plugins

import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.amos_tech_code.utils.AuthenticationException
import com.amos_tech_code.utils.AuthorizationException
import com.amos_tech_code.utils.ConflictException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ResourceNotFoundException
import com.amos_tech_code.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureExceptionHandler() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {

                is ValidationException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.BadRequest.value,
                            message = cause.message,
                        )
                    )
                }

                is AuthenticationException -> {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.Unauthorized.value,
                            message = cause.message
                        )
                    )
                }
                is AuthorizationException -> {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.Forbidden.value,
                            message = cause.message
                        )
                    )
                }
                is ResourceNotFoundException -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.NotFound.value,
                            message = cause.message
                        )
                    )
                }
                is ConflictException -> {
                    call.respond(
                        HttpStatusCode.Conflict,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.Conflict.value,
                            message = cause.message
                        )
                    )
                }

                is InternalServerException -> {
                    // Log the actual error for debugging
                    println("Internal server error: ${cause.message}")

                    call.respond(
                        HttpStatusCode.InternalServerError,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.InternalServerError.value,
                            message = cause.message
                        )
                    )
                }
                else -> {
                    // Log unexpected errors
                    println("Unexpected error: ${cause.message}")
                    cause.printStackTrace()

                    call.respond(
                        HttpStatusCode.InternalServerError,
                        GenericResponseDto(
                            statusCode = HttpStatusCode.InternalServerError.value,
                            message = "An unexpected error occurred. Please try again later."
                        )
                    )
                }
            }
        }
    }
}
