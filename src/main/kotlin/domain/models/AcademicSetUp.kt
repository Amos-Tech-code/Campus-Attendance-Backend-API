package com.amos_tech_code.domain.models

import java.util.UUID

data class ResolvedUniversity(
    val id: UUID,
    val name: String
)

data class ResolvedAcademicTerm(
    val id: UUID,
    val academicYear: String,
    val semester: Int,
    val isActive: Boolean
)

data class ResolvedProgramme(
    val id: UUID,
    val name: String,
    val departmentId: UUID,
    val departmentName: String
)

data class ResolvedUnit(
    val unitId: UUID,
    val code: String,
    val name: String,
    val semester: Int,
    val lectureDay: String?,
    val lectureTime: String?,
    val lectureVenue: String?
)