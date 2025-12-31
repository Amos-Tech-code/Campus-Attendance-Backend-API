package com.amos_tech_code.services

import com.amos_tech_code.domain.dtos.requests.DeviceInfo
import com.amos_tech_code.domain.dtos.requests.StudentRegistrationRequest
import com.amos_tech_code.domain.dtos.response.LecturerAuthResponse
import com.amos_tech_code.domain.dtos.response.StudentAuthResponse

interface AuthService {

    // Lecturer auth test
    suspend fun mockAuthenticateLecturerWithGoogle(idToken: String): LecturerAuthResponse

    // Lecturer authentication
    suspend fun authenticateLecturerWithGoogle(idToken: String): LecturerAuthResponse

    // Student authentication
    suspend fun registerStudent(request: StudentRegistrationRequest): StudentAuthResponse

    suspend fun loginStudent(registrationNumber: String, deviceInfo: DeviceInfo): StudentAuthResponse

}