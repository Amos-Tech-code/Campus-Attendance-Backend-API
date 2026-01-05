package com.amos_tech_code.domain.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSignInRequest(
    val idToken: String
)
