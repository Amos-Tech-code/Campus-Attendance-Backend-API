package com.amos_tech_code.domain.models

import java.util.UUID

data class TeachingAssignmentInfo(
    val academicTermId: UUID,
    val yearOfStudy: Int,
    val lecturerId: UUID
)

// Helper data classes
data class ExistingEnrollment(
    val id: UUID,
    val isActive: Boolean
)

data class StudentEnrollmentInfo(
    val id: UUID,
    val registrationNumber: String,
    val fullName: String
)


data class ActiveEnrollmentInfo(
    val enrollmentId: UUID,
    val programmeName: String,
    val universityName: String
)