package com.amos_tech_code.config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {

    // Determine environment
    private val appEnv: String =
        System.getProperty("env")        // JVM flag (-Denv=prod)
            ?: System.getenv("APP_ENV")  // OS environment variable
            ?: "dev"                      // default to dev

    // Load .env file (only if it exists)
    private val env = dotenv {
        ignoreIfMissing = true
        filename = ".env.$appEnv"
    }

    // Helper to fetch config: prefer OS env, fallback to .env file
    private fun getEnv(key: String, required: Boolean = true): String? {
        return System.getenv(key) ?: env[key] ?: if (required) throw IllegalArgumentException("$key is required") else null
    }

    // ---------------- Server ----------------
    val SERVER_HOST: String = getEnv("SERVER_HOST") ?: "0.0.0.0"
    val SERVER_PORT: Int = getEnv("SERVER_PORT")?.toInt() ?: 8080

    // ---------------- JWT ----------------
    val JWT_SECRET: String = getEnv("JWT_SECRET")!!
    val JWT_ISSUER: String = getEnv("JWT_ISSUER")!!
    val JWT_AUDIENCE: String = getEnv("JWT_AUDIENCE")!!
    val JWT_REALM: String = getEnv("JWT_REALM")!!
    val JWT_EXPIRATION: Long = getEnv("JWT_EXPIRATION")!!.toLong()

    // ---------------- Database ----------------
    val DB_HOST: String = getEnv("DB_HOST")!!
    val DB_PORT: Int = getEnv("DB_PORT")!!.toInt()
    val DB_NAME: String = getEnv("DB_NAME")!!
    val DB_USER: String = getEnv("DB_USER")!!
    val DB_PASSWORD: String = getEnv("DB_PASSWORD")!!

    // ---------------- Google ----------------
    val GOOGLE_CLIENT_ID: String = getEnv("GOOGLE_CLIENT_ID")!!
    val GOOGLE_CLIENT_SECRET: String = getEnv("GOOGLE_CLIENT_SECRET")!!

    // ---------------- Cloudinary ----------------
    val CLOUD_NAME: String = getEnv("CLOUDINARY_NAME")!!
    val API_KEY: String = getEnv("CLOUDINARY_API_KEY")!!
    val API_SECRET: String = getEnv("CLOUDINARY_API_SECRET")!!

    // ------------- Firebase Configuration ---------------
    val FIREBASE_CONFIG = getEnv("FIREBASE_CONFIG")
}
