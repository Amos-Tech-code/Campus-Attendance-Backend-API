package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.response.AdminAuthResponse
import com.amos_tech_code.api.dtos.response.AdminLoginRequest
import com.amos_tech_code.api.dtos.response.AdminResponse
import com.amos_tech_code.config.AdminJwtConfig
import com.amos_tech_code.data.repository.AdminRepository
import com.amos_tech_code.domain.models.Admin
import domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AdminAuthService(
    private val adminRepository: AdminRepository
) {

    suspend fun login(request: AdminLoginRequest): AdminAuthResponse? = withContext(Dispatchers.IO) {
        val admin = adminRepository.findByEmail(request.email) ?: return@withContext null

        if (!adminRepository.validatePassword(admin, request.password)) {
            return@withContext null
        }

        val accessToken = AdminJwtConfig.generateToken(admin.id.toString(), UserRole.ADMIN)
        val refreshToken = AdminJwtConfig.generateRefreshToken()
        val expiresAt = LocalDateTime.now().plusSeconds(AdminJwtConfig.getRefreshExpirationTime() / 1000)

        adminRepository.saveRefreshToken(admin.id, refreshToken, expiresAt)

        AdminAuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            admin = admin.toAdminResponseDto(),
            expiresIn = AdminJwtConfig.getExpirationTime() / 1000
        )
    }

    suspend fun refreshToken(refreshToken: String): AdminAuthResponse? = withContext(Dispatchers.IO) {
        val adminId = adminRepository.findRefreshToken(refreshToken) ?: return@withContext null

        val admin = adminRepository.findById(adminId) ?: return@withContext null

        // Revoke old refresh token
        adminRepository.revokeRefreshToken(refreshToken)

        // Generate new tokens
        val newAccessToken = AdminJwtConfig.generateToken(admin.id.toString(), UserRole.ADMIN)
        val newRefreshToken = AdminJwtConfig.generateRefreshToken()
        val expiresAt = LocalDateTime.now().plusSeconds(AdminJwtConfig.getRefreshExpirationTime() / 1000)

        adminRepository.saveRefreshToken(admin.id, newRefreshToken, expiresAt)

        AdminAuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            admin = admin.toAdminResponseDto(),
            expiresIn = AdminJwtConfig.getExpirationTime() / 1000
        )
    }

    suspend fun logout(refreshToken: String): Boolean = withContext(Dispatchers.IO) {
        adminRepository.revokeRefreshToken(refreshToken)
    }

    fun Admin.toAdminResponseDto() = AdminResponse(
        id = id.toString(),
        email = email,
        fullName = fullName,
        lastLoginAt = lastLoginAt
    )
}