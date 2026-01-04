package com.amos_tech_code.services.impl

import com.amos_tech_code.data.repository.StudentEnrollmentRepository
import com.amos_tech_code.domain.dtos.requests.StudentEnrollmentRequest
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.services.StudentEnrollmentService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ValidationException
import java.util.*

class StudentEnrollmentServiceImpl(
    private val repository: StudentEnrollmentRepository,
) : StudentEnrollmentService {

    override suspend fun enrollStudent(
        studentId: UUID,
        request: StudentEnrollmentRequest
    ): StudentEnrollmentResponse {

        try {
            // Validate request
            request.validate()

            // Convert IDs
            val universityId = UUID.fromString(request.universityId)
            val programmeId = UUID.fromString(request.programmeId)

            // Check if student is already enrolled in ANY active programme
            val existingActiveEnrollment = repository.findActiveEnrollment(studentId)
            if (existingActiveEnrollment != null) {
                throw ValidationException(
                    "Student is already enrolled in ${existingActiveEnrollment.programmeName} " +
                            "at ${existingActiveEnrollment.universityName}. " +
                            "Please deactivate your current enrollment before enrolling in a new programme."
                )
            }

            // Get active academic term and year of study from lecturer's teaching assignments
            val teachingAssignment = repository.findActiveTeachingAssignment(
                universityId = universityId,
                programmeId = programmeId
            ) ?: throw ValidationException(
                "Cannot enroll. No active lecturer teaching assignment found for this programme. " +
                        "Please ensure a lecturer has set up this programme in the current academic term."
            )

            // Get student info
            repository.getStudentInfo(studentId)
                ?: throw ValidationException("Student not found")

            // Check if student is already enrolled in this programme for this term
            val existingEnrollment = repository.findExistingEnrollment(
                studentId = studentId,
                programmeId = programmeId,
                academicTermId = teachingAssignment.academicTermId
            )

            if (existingEnrollment != null) {
                if (existingEnrollment.isActive) {
                    throw ValidationException("Student is already enrolled in this programme for the current term")
                } else {
                    // Reactivate existing enrollment
                    return repository.reactivateEnrollment(
                        enrollmentId = existingEnrollment.id,
                        yearOfStudy = teachingAssignment.yearOfStudy
                    )
                }
            }

            // Create new enrollment
            return repository.createEnrollment(
                studentId = studentId,
                universityId = universityId,
                programmeId = programmeId,
                academicTermId = teachingAssignment.academicTermId,
                yearOfStudy = teachingAssignment.yearOfStudy,
                enrollmentSource = request.enrollmentSource
            )

        } catch (ex: Exception) {
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Student enrollment failed.")
            }
        }
    }

    override suspend fun getStudentEnrollment(studentId: UUID): StudentEnrollmentResponse {
        try {
            return repository.getStudentEnrollment(studentId)
        } catch (ex: Exception) {
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to fetch student enrollments.")
            }
        }
    }

    override suspend fun deactivateEnrollment(
        studentId: UUID,
        enrollmentId: UUID
    ): StudentEnrollmentResponse {
        try {
            return repository.deactivateEnrollment(studentId, enrollmentId)
        } catch (ex: Exception) {
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to deactivate student enrollment.")
            }
        }
    }

    override suspend fun updateEnrollmentYear(
        studentId: UUID,
        enrollmentId: UUID,
        newYearOfStudy: Int
    ): StudentEnrollmentResponse {
        try {
            if (newYearOfStudy !in 1..6) {
                throw ValidationException("Year of study must be between 1 and 6")
            }

            return repository.updateEnrollmentYear(studentId, enrollmentId, newYearOfStudy)
        } catch (ex: Exception) {
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to update student enrollment.")
            }
        }
    }

    private fun StudentEnrollmentRequest.validate() {
        require(this.universityId.isNotBlank()) {
            throw ValidationException("University ID is required")
        }
        require(this.programmeId.isNotBlank()) {
            throw ValidationException("Programme ID is required")
        }
    }

}