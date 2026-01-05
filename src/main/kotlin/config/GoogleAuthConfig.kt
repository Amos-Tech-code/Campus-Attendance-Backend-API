package com.amos_tech_code.config

object GoogleAuthConfig {

    val clientId = AppConfig.GOOGLE_CLIENT_ID
    val clientSecret = AppConfig.GOOGLE_CLIENT_SECRET

    const val redirectUri = "http://localhost:8080/auth/google/callback"
    const val authorizeUrl = "https://accounts.google.com/o/oauth2/auth"
    const val tokenUrl = "https://oauth2.googleapis.com/token"
}