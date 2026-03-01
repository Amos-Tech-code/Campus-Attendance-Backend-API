package com.amos_tech_code.plugins

import com.amos_tech_code.config.AppConfig
import com.amos_tech_code.config.GoogleAuthConfig
import com.amos_tech_code.config.JwtConfig
import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import domain.models.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond

fun Application.configureAuthentication() {

    val jwtAudience = AppConfig.JWT_AUDIENCE
    val jwtIssuer = AppConfig.JWT_ISSUER
    val jwtRealm = AppConfig.JWT_REALM
    val jwtSecret = AppConfig.JWT_SECRET

    install(Authentication) {

        jwt("jwt-auth") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience))
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { defaultScheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    GenericResponseDto(HttpStatusCode.Unauthorized.value, "Token is not valid or has expired")
                )
            }
        }

        jwt("admin-jwt") {
            verifier(JwtConfig.verifier)
            realm = "smart-attend"
            validate { credential ->
                if (credential.payload.getClaim("role").asString() == UserRole.ADMIN.name) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
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
