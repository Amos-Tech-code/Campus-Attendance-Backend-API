package domain.services.impl

import api.dtos.response.LiveAttendanceEvent
import api.dtos.response.LiveAttendanceSnapshot
import com.amos_tech_code.domain.services.AttendanceEventBus
import com.amos_tech_code.domain.services.LiveAttendanceService
import com.amos_tech_code.utils.AuthorizationException
import com.amos_tech_code.utils.ConflictException
import data.repository.AttendanceSessionRepository
import domain.models.AttendanceSessionStatus
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import java.util.*

class LiveAttendanceServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
    private val attendanceEventBus: AttendanceEventBus
) : LiveAttendanceService {

    private val logger = LoggerFactory.getLogger(LiveAttendanceServiceImpl::class.java)

    override suspend fun authorize(
        lecturerId: UUID,
        sessionId: UUID
    ) {
        val ownsSession =
            attendanceSessionRepository.existsByIdAndLecturerId(sessionId, lecturerId)

        if (!ownsSession) {
            throw AuthorizationException("You do not own this attendance session")
        }
        if(attendanceSessionRepository.getSessionDetails(sessionId)?.status != AttendanceSessionStatus.ACTIVE) {
            throw ConflictException("This session has already ended.")
        }
    }

    override suspend fun getInitialSnapshot(
        sessionId: UUID
    ): LiveAttendanceSnapshot {
        return attendanceSessionRepository.getLiveAttendanceSnapshot(sessionId)
    }

    override fun liveEvents(
        sessionId: UUID
    ): Flow<LiveAttendanceEvent> {
        return attendanceEventBus.subscribe(sessionId)
    }
}