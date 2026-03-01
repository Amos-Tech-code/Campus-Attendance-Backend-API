package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.requests.UpdateLecturerProfileRequest
import com.amos_tech_code.api.dtos.requests.UpdateStudentProfileRequest
import com.amos_tech_code.data.repository.LecturerRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.services.AccountService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.ConflictException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ResourceNotFoundException
import com.amos_tech_code.utils.ValidationException
import org.slf4j.LoggerFactory
import java.util.UUID

class AccountServiceImpl(
    private val studentRepository: StudentRepository,
    private val lecturerRepository: LecturerRepository
) : AccountService {

    private val logger = LoggerFactory.getLogger(AccountServiceImpl::class.java)

    override suspend fun updateStudentProfile(
        studentId: UUID,
        request: UpdateStudentProfileRequest
    ) {
        try {
            if (request.fullName.isBlank())
                throw ValidationException("Full name is required")

            if (request.registrationNumber.isBlank())
                throw ValidationException("Registration number is required")

            val exists = studentRepository.existsByRegistrationNumber(
                regNo = request.registrationNumber,
                excludeStudentId = studentId
            )

            if (exists)
                throw ConflictException("Registration number already exists")

            val updated = studentRepository.updateProfile(
                studentId = studentId,
                fullName = request.fullName,
                registrationNumber = request.registrationNumber
            )

            if (!updated) throw ResourceNotFoundException("Student not found")

        } catch (ex: Exception) {
            logger.error("Failed to update student profile: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to update profile")
            }
        }
    }

    override suspend fun updateLecturerProfile(
        lecturerId: UUID,
        request: UpdateLecturerProfileRequest
    ) {
        try {
            if (request.fullName.isBlank())
                throw ValidationException("Full name is required")

            if (lecturerRepository.findById(lecturerId) == null) throw ResourceNotFoundException("Lecturer not found")

            val updated = lecturerRepository.updateName(
                lecturerId = lecturerId,
                fullName = request.fullName
            )

            if (!updated)
                throw ResourceNotFoundException("Lecturer not found")
        } catch (ex: Exception) {
            logger.error("Failed to update lecturer profile: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to update profile")
            }
        }
    }

    override suspend fun updateStudentFcmToken(studentId: UUID, fcmToken: String) {
        try {
            val result = studentRepository.updateFcmToken(studentId, fcmToken)
            if (!result) throw ValidationException("Failed to update fcm token")

        } catch (ex: Exception) {
            logger.error("Failed to update fcm token: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to update fcm token")
            }
        }
    }

    override suspend fun updateLecturerFcmToken(lecturerId: UUID, fcmToken: String) {
        try {
            val result = lecturerRepository.updateFcmToken(lecturerId, fcmToken)
            if (!result) throw ValidationException("Failed to update fcm token")

        } catch (ex: Exception) {
            logger.error("Failed to update fcm token: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to update fcm token")
            }
        }
    }


}
