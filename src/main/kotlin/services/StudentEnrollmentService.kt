package com.amos_tech_code.services

import com.amos_tech_code.domain.dtos.requests.StudentEnrollmentRequest
import com.amos_tech_code.domain.dtos.response.StudentEnrollmentResponse
import java.util.UUID

interface StudentEnrollmentService {

    suspend fun enrollStudent(
        studentId: UUID,
        request: StudentEnrollmentRequest
    ): StudentEnrollmentResponse

    suspend fun getStudentEnrollments(studentId: UUID): List<StudentEnrollmentResponse>

    suspend fun deactivateEnrollment(
        studentId: UUID,
        enrollmentId: UUID
    ): StudentEnrollmentResponse

    suspend fun updateEnrollmentYear(
        studentId: UUID,
        enrollmentId: UUID,
        newYearOfStudy: Int
    ): StudentEnrollmentResponse

}