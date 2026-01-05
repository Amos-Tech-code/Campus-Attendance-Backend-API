package com.amos_tech_code.domain.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class GenericResponseDto(
    val statusCode: Int,
    val message: String?
)