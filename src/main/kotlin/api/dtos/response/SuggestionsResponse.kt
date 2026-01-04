package com.amos_tech_code.domain.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class UniversitySuggestion(
    val id: String,
    val name: String,
    val matchType: String // "exact", "prefix", "partial", "similar"
)

@Serializable
data class DepartmentSuggestion(
    val id: String,
    val name: String,
    val universityId: String,
    val universityName: String
)

@Serializable
data class ProgrammeSuggestion(
    val id: String,
    val name: String,
    val universityId: String,
    val universityName: String,
    val departmentId: String?,
    val departmentName: String?
)

@Serializable
data class UnitSuggestion(
    val id: String,
    val code: String,
    val name: String,
    val semester: Int,
    val universityId: String,
    val universityName: String,
    val departmentId: String?,
    val departmentName: String?
)