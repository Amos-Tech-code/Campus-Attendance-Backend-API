package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.response.ActivityLogDto
import com.amos_tech_code.api.dtos.response.DashboardStatsDto
import com.amos_tech_code.data.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminDashboardService(
    private val adminRepository: AdminRepository
) {

    suspend fun getDashboardStats(): DashboardStatsDto = withContext(Dispatchers.IO) {
        val stats = adminRepository.getDashboardStats()

        DashboardStatsDto(
            totalStudents = stats.totalStudents,
            totalLecturers = stats.totalLecturers,
            totalUniversities = stats.totalUniversities,
            totalProgrammes = stats.totalProgrammes,
            totalSessions = stats.totalSessions,
            todaySessions = stats.todaySessions,
            totalAttendance = stats.totalAttendance,
            recentActivities = stats.recentActivities.map { activity ->
                ActivityLogDto(
                    id = activity.id,
                    type = activity.type,
                    description = activity.description,
                    timestamp = activity.timestamp,
                    performedBy = activity.performedBy
                )
            }
        )
    }
}