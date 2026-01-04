package com.amos_tech_code.domain.dtos.response

import kotlinx.serialization.Serializable
import kotlin.Boolean

@Serializable
data class AcademicSetupResponse(
    val university: UniversityResponse,
    val academicTerm: AcademicTermResponse?,
    val programmes: List<LecturerProgrammeResponse>,
    val isActive: kotlin.Boolean,
    val createdAt: Long
)

@Serializable
data class UniversityResponse(
    val id: String,
    val name: String
)

@Serializable
data class AcademicTermResponse(
    val id: String,
    val academicYear: String,
    val semester: Int,
    val isActive: Boolean
)

@Serializable
data class LecturerProgrammeResponse(
    val programmeId: String,
    val programmeName: String,
    val departmentId: String,
    val departmentName: String,

    val yearOfStudy: Int,
    val expectedStudentCount: Int,

    val units: List<LecturerUnitResponse>
)

@Serializable
data class LecturerUnitResponse(
    val unitId: String,
    val code: String,
    val name: String,
    val semester: Int,
    val lectureDay: String? = null,
    val lectureTime: String? = null,
    val lectureVenue: String? = null
)