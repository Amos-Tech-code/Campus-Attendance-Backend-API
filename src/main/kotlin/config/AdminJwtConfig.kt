package com.amos_tech_code.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import domain.models.UserRole
import java.util.Date
import java.util.UUID

object AdminJwtConfig {

    private val SECRET = AppConfig.JWT_SECRET
    private val ISSUER = AppConfig.JWT_ISSUER
    private val AUDIENCE = AppConfig.JWT_AUDIENCE
    private val VALIDITY_IN_MS = 3600000 // 1 hour
    private val REFRESH_VALIDITY_IN_MS = 604800000 // 7 days

    private val algorithm = Algorithm.HMAC256(SECRET)
    private val verifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()

    fun generateToken(userId: String, role: UserRole): String = JWT.create()
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withSubject(userId)
        .withClaim("role", role.name)
        .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_IN_MS))
        .sign(algorithm)

    fun generateRefreshToken(): String = UUID.randomUUID().toString()

    fun verifyToken(token: String): com.auth0.jwt.interfaces.DecodedJWT = verifier.verify(token)

    fun getExpirationTime(): Long = VALIDITY_IN_MS.toLong()

    fun getRefreshExpirationTime(): Long = REFRESH_VALIDITY_IN_MS.toLong()
}