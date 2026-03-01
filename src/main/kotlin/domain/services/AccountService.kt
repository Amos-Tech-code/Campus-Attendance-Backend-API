package com.amos_tech_code.domain.services

import com.amos_tech_code.api.dtos.requests.UpdateLecturerProfileRequest
import com.amos_tech_code.api.dtos.requests.UpdateStudentProfileRequest
import java.util.UUID

interface AccountService {

    suspend fun updateStudentProfile(
        studentId: UUID,
        request: UpdateStudentProfileRequest
    )

    suspend fun updateLecturerProfile(
        lecturerId: UUID,
        request: UpdateLecturerProfileRequest
    )

    suspend fun updateStudentFcmToken(studentId: UUID, fcmToken: String)

    suspend fun updateLecturerFcmToken(lecturerId: UUID, fcmToken: String)
}
