package com.amos_tech_code.services

import com.amos_tech_code.domain.models.GoogleUser

interface GoogleAuthService {

    suspend fun validateGoogleToken(idToken: String): GoogleUser?

}