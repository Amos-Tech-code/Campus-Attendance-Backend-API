package com.amos_tech_code.domain.models

import com.amos_tech_code.domain.dtos.response.DepartmentResponse
import com.amos_tech_code.domain.dtos.response.ProgrammeResponse
import java.util.UUID

data class ResolvedTeachingAssignment(
    val programmeId: UUID,
    val departmentId: UUID,
    val yearOfStudy: Int,
    val expectedStudents: Int,

    val programme: ProgrammeResponse,
    val department: DepartmentResponse,
    val unit: ResolvedUnit,

    val lectureDay: String?,
    val lectureTime: String?,
    val lectureVenue: String?
)
