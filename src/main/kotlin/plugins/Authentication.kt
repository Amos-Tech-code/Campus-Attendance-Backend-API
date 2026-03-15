package com.amos_tech_code.plugins

import com.amos_tech_code.config.GoogleAuthConfig
import com.amos_tech_code.config.JwtConfig
import api.dtos.response.GenericResponseDto
import com.amos_tech_code.config.AdminJwtConfig
import domain.models.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond

fun Application.configureAuthentication() {

    install(Authentication) {

        jwt("jwt-auth") {
            verifier(JwtConfig.verifier)
            realm = JwtConfig.realm
            validate { credential ->
                if (credential.payload.audience.contains(JwtConfig.audience))
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    GenericResponseDto(HttpStatusCode.Unauthorized.value, "Token is not valid or has expired")
                )
            }
        }

        jwt("admin-jwt") {
            verifier(AdminJwtConfig.verifier)
            realm = JwtConfig.realm
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val role = credential.payload.getClaim("role").asString()

                if (!userId.isNullOrBlank() && role == UserRole.ADMIN.name) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    GenericResponseDto(
                        HttpStatusCode.Unauthorized.value,
                        "Token is not valid or has expired"
                    )
                )
            }
        }

        oauth("google-oauth") {
            client = HttpClient(CIO) // Apache or CIO
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = GoogleAuthConfig.authorizeUrl,
                    accessTokenUrl = GoogleAuthConfig.tokenUrl,
                    clientId = GoogleAuthConfig.clientId,
                    clientSecret = GoogleAuthConfig.clientSecret,
                    defaultScopes = listOf("profile", "email")
                )
            }
            urlProvider = { GoogleAuthConfig.redirectUri }
        }
    }

}
