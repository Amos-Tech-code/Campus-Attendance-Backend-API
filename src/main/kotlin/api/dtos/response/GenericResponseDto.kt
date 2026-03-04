package api.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class GenericResponseDto(
    val statusCode: Int,
    val message: String?
)