package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.*
import com.amos_tech_code.data.repository.AttendanceRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SuspiciousActivityService(
    private val repository: AttendanceRecordRepository
) {

    suspend fun getAllSuspiciousActivities(
        page: Int = 1,
        pageSize: Int = 20,
        studentId: UUID? = null,
        sessionId: UUID? = null,
        unitId: UUID? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        search: String? = null
    ): SuspiciousActivityListResponse = withContext(Dispatchers.IO) {
        val (activities, total, totalPages) = repository.getAllSuspiciousActivities(
            page = page,
            pageSize = pageSize,
            studentId = studentId,
            sessionId = sessionId,
            unitId = unitId,
            dateFrom = dateFrom,
            dateTo = dateTo,
            search = search
        )

        SuspiciousActivityListResponse(
            activities = activities,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    suspend fun getSuspiciousActivityById(id: UUID): SuspiciousActivityResponse? = withContext(Dispatchers.IO) {
        repository.getSuspiciousActivityById(id)
    }

    suspend fun getSuspiciousActivityStats(): SuspiciousActivityStatsResponse = withContext(Dispatchers.IO) {
        repository.getSuspiciousActivityStats()
    }

    suspend fun reviewSuspiciousActivity(
        id: UUID,
        request: ReviewSuspiciousActivityRequest
    ): Boolean = withContext(Dispatchers.IO) {
        repository.updateSuspiciousFlag(
            id = id,
            isSuspicious = request.isSuspicious,
            notes = request.notes
        )
    }
}