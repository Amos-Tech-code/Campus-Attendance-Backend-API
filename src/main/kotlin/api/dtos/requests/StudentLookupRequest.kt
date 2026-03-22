package com.amos_tech_code.api.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class StudentLookupRequest(
    val registrationNumber: String
)