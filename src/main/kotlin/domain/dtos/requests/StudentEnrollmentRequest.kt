package com.amos_tech_code.domain.dtos.requests

import com.amos_tech_code.domain.models.StudentEnrollmentSource
import kotlinx.serialization.Serializable

@Serializable
data class StudentEnrollmentRequest(
    val universityId: String,
    val programmeId: String,
    val enrollmentSource: StudentEnrollmentSource = StudentEnrollmentSource.SELF
)

// Helper DTO for PATCH request
@Serializable
data class UpdateYearRequest(
    val newYearOfStudy: Int
)