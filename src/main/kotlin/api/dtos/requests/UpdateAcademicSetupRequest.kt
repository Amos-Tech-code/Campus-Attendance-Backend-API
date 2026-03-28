package com.amos_tech_code.domain.dtos.requests

import kotlinx.serialization.Serializable

// Academic Term DTOs
@Serializable
data class AddAcademicTermRequest(
    val academicYear: String,
    val semester: Int,
    val weekCount: Int = 14
)

@Serializable
data class UpdateAcademicTermRequest(
    val academicYear: String? = null,
    val semester: Int? = null,
    val weekCount: Int? = null
)

// Programme DTOs
@Serializable
data class AddProgrammeRequest(
    val name: String,
    val departmentId: String,
    val yearOfStudy: Int,
    val expectedStudentCount: Int
)

@Serializable
data class UpdateProgrammeRequest(
    val name: String? = null,
    val departmentId: String? = null,
    val yearOfStudy: Int? = null,
    val expectedStudentCount: Int? = null,
    val isActive: Boolean? = null
)

// Unit DTOs
@Serializable
data class AddUnitToProgrammeRequest(
    val code: String,
    val name: String,
    val semester: Int,
    val yearOfStudy: Int,
    val departmentId: String,
    val lectureDay: String? = null,
    val lectureTime: String? = null,
    val lectureVenue: String? = null
)

@Serializable
data class UpdateUnitRequest(
    val code: String? = null,
    val name: String? = null,
    val semester: Int? = null,
    val departmentId: String? = null,
    val lectureDay: String? = null,
    val lectureTime: String? = null,
    val lectureVenue: String? = null,
    val isActive: Boolean? = null
)

// Teaching Assignment DTOs
@Serializable
data class AddTeachingAssignmentRequest(
    val universityId: String,
    val programmeId: String,
    val unitId: String,
    val academicTermId: String,
    val yearOfStudy: Int,
    val expectedStudents: Int,
    val lectureDay: String? = null,
    val lectureTime: String? = null,
    val lectureVenue: String? = null
)

@Serializable
data class UpdateTeachingAssignmentRequest(
    val expectedStudents: Int? = null,
    val lectureDay: String? = null,
    val lectureTime: String? = null,
    val lectureVenue: String? = null,
    val isActive: Boolean? = null
)
/*
@Serializable
data class UpdateAcademicSetupRequest(
    val universityId: String,

    /**
     * If false → lecturer intends to REMOVE this university from their setup
     * Must be validated against attendance data
     */
    val isActive: Boolean = true,

    val academicTerms: List<UpdateAcademicTermDto> = emptyList(),

    val programmes: List<UpdateProgrammeSetupDto> = emptyList()
)

@Serializable
data class UpdateAcademicTermDto(
    val academicTermId: String? = null,

    val draft: NewAcademicTermDraft? = null,

    /**
     * Allows switching active teaching term
     * Cannot deactivate if attendance exists
     */
    val isActive: Boolean = true
)


@Serializable
data class NewAcademicTermDraft(
    val academicYear: String, // "2025-2026"
    val semester: Int,        // 1 or 2
    val weekCount: Int = 14
)


@Serializable
data class UpdateProgrammeSetupDto(
    val programmeId: String? = null,

    val draft: NewProgrammeDraft? = null,

    val isActive: Boolean = true,

    val yearOfStudy: Int,

    val expectedStudentCount: Int = 0,

    val units: List<UpdateUnitAssignmentDto>
)

@Serializable
data class NewProgrammeDraft(
    val name: String,
    val department: DepartmentRef
)

@Serializable
data class DepartmentRef(
    val departmentId: String? = null,
    val draftName: String? = null
)


@Serializable
data class UpdateUnitAssignmentDto(
    val unitId: String? = null,

    val draft: NewUnitDraft? = null,

    val academicTermRef: AcademicTermRef,

    val yearOfStudy: Int,

    val isActive: Boolean = true,

    val lectureDay: String? = null,
    val lectureTime: String? = null,
    val lectureVenue: String? = null
)

@Serializable
data class NewUnitDraft(
    val code: String,
    val name: String,
    val department: DepartmentRef
)

@Serializable
data class AcademicTermRef(
    val academicTermId: String? = null,
    val draft: NewAcademicTermDraft? = null
)

 */
