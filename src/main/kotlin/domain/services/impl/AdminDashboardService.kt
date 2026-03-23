package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.ActivityLogDto
import com.amos_tech_code.api.dtos.admin.DashboardStatsDto
import com.amos_tech_code.data.repository.AdminDashboardRepository
import com.amos_tech_code.data.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class AdminDashboardService(
    private val adminRepository: AdminRepository,
    private val adminDashboardRepository: AdminDashboardRepository
) {

    suspend fun getDashboardStats(adminId: UUID): DashboardStatsDto = withContext(Dispatchers.IO) {
        val admin = adminRepository.findById(adminId)
        val stats = adminDashboardRepository.getDashboardStats()

        DashboardStatsDto(
            adminName = admin?.fullName ?: "Admin",
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