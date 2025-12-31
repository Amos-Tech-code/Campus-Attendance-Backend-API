package com.amos_tech_code.domain.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class StudentEnrollmentResponse(
    val enrollmentId: String,
    val studentId: String,
    val registrationNumber: String,
    val fullName: String,
    val university: UniversityResponse,
    val programme: ProgrammeResponse,
    val academicTerm: AcademicTermResponse,
    val yearOfStudy: Int,
    val enrollmentDate: Long,
    val enrollmentSource: String,
    val isActive: Boolean
)

// Reuse existing DTOs from your academic setup
//@Serializable
//data class UniversityResponse(
//    val id: String,
//    val name: String
//)
//
//@Serializable
//data class ProgrammeResponse(
//    val id: String,
//    val name: String,
//    val departmentId: String? = null,
//    val departmentName: String? = null
//)

//@Serializable
//data class AcademicTermResponse(
//    val id: String,
//    val academicYear: String,
//    val semester: Int,
//    val isActive: Boolean
//)