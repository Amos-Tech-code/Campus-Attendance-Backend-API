package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.AdminResponse
import com.amos_tech_code.api.dtos.admin.CreateAdminRequest
import com.amos_tech_code.api.dtos.admin.UpdateAdminRequest
import com.amos_tech_code.data.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class AdminManagementService(
    private val adminRepository: AdminRepository
) {

    suspend fun getAllAdmins(): List<AdminResponse> = withContext(Dispatchers.IO) {
        adminRepository.getAllAdmins()
    }

    suspend fun getAdminById(id: UUID): AdminResponse? = withContext(Dispatchers.IO) {
        adminRepository.getAdminByIdResponse(id)
    }

    suspend fun createAdmin(request: CreateAdminRequest): AdminResponse? = withContext(Dispatchers.IO) {
        adminRepository.createAdmin(request)
    }

    suspend fun updateAdmin(id: UUID, request: UpdateAdminRequest): Boolean = withContext(Dispatchers.IO) {
        adminRepository.updateAdmin(id, request)
    }

    suspend fun deleteAdmin(id: UUID): Boolean = withContext(Dispatchers.IO) {
        adminRepository.deleteAdmin(id)
    }

    suspend fun resetAdminPassword(id: UUID, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        adminRepository.resetAdminPassword(id, newPassword)
    }
}