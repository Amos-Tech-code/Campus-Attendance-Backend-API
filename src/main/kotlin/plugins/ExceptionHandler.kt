package com.amos_tech_code.plugins

import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.amos_tech_code.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureExceptionHandler() {
    install(StatusPages) {

        status(HttpStatusCode.NotFound) { call, status ->
            call.respondNotFound()
        }

        exception<Throwable> { call, cause ->
            when (cause) {

                is BadRequestException -> {
                    call.respondBadRequest("Invalid Request")
                }

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
