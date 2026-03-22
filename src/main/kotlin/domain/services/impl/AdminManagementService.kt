package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.AdminResponse
import com.amos_tech_code.api.dtos.admin.CreateAdminRequest
import com.amos_tech_code.api.dtos.admin.UpdateAdminRequest
import com.amos_tech_code.data.repository.AdminManagementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AdminManagementService(
    private val adminManagementRepository: AdminManagementRepository
) {

    suspend fun getAllAdmins(): List<AdminResponse> = withContext(Dispatchers.IO) {
        adminManagementRepository.getAllAdmins()
    }

    suspend fun getAdminById(id: UUID): AdminResponse? = withContext(Dispatchers.IO) {
        adminManagementRepository.getAdminById(id)
    }

    suspend fun createAdmin(request: CreateAdminRequest): AdminResponse? = withContext(Dispatchers.IO) {
        val admin = adminManagementRepository.createAdmin(
            email = request.email,
            password = request.password,
            fullName = request.fullName,
            role = request.role
        )

        admin?.let { adminManagementRepository.getAdminById(it.id) }
    }

    suspend fun updateAdmin(id: UUID, request: UpdateAdminRequest): Boolean = withContext(Dispatchers.IO) {
        adminManagementRepository.updateAdmin(
            id = id,
            fullName = request.fullName,
            role = request.role,
            isActive = request.isActive
        )
    }

    suspend fun deleteAdmin(id: UUID): Boolean = withContext(Dispatchers.IO) {
        // Prevent self-deletion - this should be checked at the controller level
        adminManagementRepository.deleteAdmin(id)
    }

    suspend fun resetAdminPassword(id: UUID, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        adminManagementRepository.resetAdminPassword(id, newPassword)
    }
}