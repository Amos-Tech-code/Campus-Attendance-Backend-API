package com.amos_tech_code.domain.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class UniversitySuggestionRequest(
    val query: String,
    val limit: Int = 10
)

@Serializable
data class DepartmentSuggestionRequest(
    val universityId: String,
    val query: String,
    val limit: Int = 10
)

@Serializable
data class ProgrammeSuggestionRequest(
    val universityId: String,
    val departmentId: String? = null,
    val query: String,
    val limit: Int = 10
)

@Serializable
data class UnitSuggestionRequest(
    val universityId: String,
    val departmentId: String? = null,
    val programmeId: String? = null,
    val query: String,
    val limit: Int = 10
)
