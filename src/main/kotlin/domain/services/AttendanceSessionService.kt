package com.amos_tech_code.services

import com.amos_tech_code.domain.dtos.requests.StartSessionRequest
import com.amos_tech_code.domain.dtos.requests.UpdateSessionRequest
import com.amos_tech_code.domain.dtos.requests.VerifySessionRequest
import com.amos_tech_code.domain.dtos.response.SessionResponse
import api.dtos.response.VerifyAttendanceResponse
import java.util.UUID

interface AttendanceSessionService {

    suspend fun startSession(lecturerId: UUID, request: StartSessionRequest): SessionResponse

    suspend fun updateSession(
        lecturerId: UUID,
        sessionId: String,
        request: UpdateSessionRequest
    ): SessionResponse

    suspend fun endSession(lecturerId: UUID, sessionId: String): Boolean

    suspend fun getActiveSession(lecturerId: UUID): SessionResponse

    suspend fun verifySessionForAttendance(studentId: UUID, request: VerifySessionRequest): VerifyAttendanceResponse

}