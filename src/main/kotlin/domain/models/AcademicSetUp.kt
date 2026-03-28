package com.amos_tech_code.domain.models

import java.time.LocalDateTime
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

// UPDATE MODELS
data class Department(
    val id: UUID,
    val universityId: UUID,
    val name: String
)

data class Programme(
    val id: UUID,
    val universityId: UUID,
    val departmentId: UUID,
    val name: String,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)


data class DomainUnit(
    val id: UUID,
    val universityId: UUID,
    val departmentId: UUID,
    val code: String,
    val name: String,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class TeachingAssignment(
    val id: UUID,
    val lecturerId: UUID,
    val universityId: UUID,
    val programmeId: UUID,
    val unitId: UUID,
    val academicTermId: UUID,
    val yearOfStudy: Int,
    val expectedStudents: Int,
    val lectureDay: String?,
    val lectureTime: String?,
    val lectureVenue: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)