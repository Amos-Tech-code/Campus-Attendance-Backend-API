package com.amos_tech_code.domain.dtos.response

import kotlinx.serialization.Serializable
import kotlin.Boolean

@Serializable
data class LecturerAcademicSetupResponse(
    val universities: List<UniversityAcademicSetup>
)

@Serializable
data class UniversityAcademicSetup(
    val university: UniversityResponse,
    val academicTerms: List<AcademicTermSetup>,
    val programmes: List<ProgrammeAcademicSetup>
)

@Serializable
data class AcademicTermSetup(
    val id: String,
    val academicYear: String,
    val semester: Int,
    val isActive: Boolean
)

@Serializable
data class ProgrammeAcademicSetup(
    val programme: ProgrammeResponse,
    val department: DepartmentResponse,
    val yearOfStudy: Int,
    val expectedStudentCount: Int,
    val units: List<UnitAcademicSetup>
)

@Serializable
data class ProgrammeResponse(
    val id: String,
    val name: String
)

@Serializable
data class DepartmentResponse(
    val id: String,
    val name: String
)

@Serializable
data class UnitAcademicSetup(
    val unitId: String,
    val code: String,
    val name: String,
    val semester: Int,
    val lectureDay: String?,
    val lectureTime: String?,
    val lectureVenue: String?
)