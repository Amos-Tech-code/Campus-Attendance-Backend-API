package com.amos_tech_code.services

interface SessionCodeGenerator {

    fun generateSixDigitCode(): String

    fun generateSecretKey(): String
}