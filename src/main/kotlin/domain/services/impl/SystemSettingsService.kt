package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.data.repository.SystemSettingsRepository

import com.amos_tech_code.api.dtos.admin.SystemSettingsResponse
import com.amos_tech_code.api.dtos.admin.UpdateSystemSettingsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemSettingsService(
    private val repository: SystemSettingsRepository
) {

    suspend fun getSettings(): SystemSettingsResponse = withContext(Dispatchers.IO) {
        repository.getSettings()
    }

    suspend fun updateSettings(request: UpdateSystemSettingsRequest): Boolean = withContext(Dispatchers.IO) {
        repository.updateSettings(request)
    }

    suspend fun getSetting(key: String): Any? = withContext(Dispatchers.IO) {
        repository.getSetting(key)
    }

    suspend fun isMaintenanceMode(): Boolean = withContext(Dispatchers.IO) {
        repository.isMaintenanceMode()
    }
}