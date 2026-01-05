package com.amos_tech_code.services.impl

import com.amos_tech_code.services.SessionCodeGenerator
import java.security.SecureRandom

class SessionCodeGeneratorImpl : SessionCodeGenerator {

    private val secureRandom = SecureRandom()


    override fun generateSixDigitCode(): String {
        return "%06d".format(secureRandom.nextInt(999999))
    }

    override fun generateSecretKey(): String {
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz"
        return StringBuilder().apply {
            repeat(8) {
                append(allowedChars[secureRandom.nextInt(allowedChars.length)])
            }
        }.toString()
    }

}