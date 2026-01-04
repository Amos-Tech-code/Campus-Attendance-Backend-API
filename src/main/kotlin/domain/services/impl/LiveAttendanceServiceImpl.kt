package domain.services.impl

import api.dtos.response.LiveAttendanceEvent
import api.dtos.response.LiveAttendanceMessage
import data.repository.AttendanceSessionRepository
import api.dtos.response.LiveAttendanceSnapshot
import com.amos_tech_code.domain.services.AttendanceEventBus
import com.amos_tech_code.domain.services.AttendanceWebSocketManager
import com.amos_tech_code.domain.services.LiveAttendanceService
import com.amos_tech_code.utils.AuthorizationException
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LiveAttendanceServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
    private val attendanceEventBus: AttendanceEventBus
) : LiveAttendanceService {

    override suspend fun authorize(
        lecturerId: UUID,
        sessionId: UUID
    ) {
        val ownsSession =
            attendanceSessionRepository.existsByIdAndLecturerId(sessionId, lecturerId)

        if (!ownsSession) {
            throw AuthorizationException("You do not own this attendance session")
        }
    }

    override suspend fun getInitialSnapshot(
        sessionId: UUID
    ): LiveAttendanceEvent {
        val data = attendanceSessionRepository.getLiveAttendanceSnapshot(sessionId)
        return LiveAttendanceEvent.InitialState(
            data = data
        )
    }

    override fun liveEvents(
        sessionId: UUID
    ): Flow<LiveAttendanceEvent> {
        return attendanceEventBus.subscribe(sessionId)
    }
}

/*
class LiveAttendanceServiceImpl(
    private val attendanceSessionRepository: AttendanceSessionRepository,
) : LiveAttendanceService {

    override suspend fun authorizeAndConnect(
        lecturerId: UUID,
        sessionId: UUID,
        socket: DefaultWebSocketServerSession
    ) {

        val ownsSession =
            attendanceSessionRepository.existsByIdAndLecturerId(sessionId, lecturerId)

        if (!ownsSession) {
            throw AuthorizationException("You do not own this attendance session")
        }

        AttendanceWebSocketManager.register(sessionId, socket)
    }

    override suspend fun disconnect(
        lecturerId: UUID,
        sessionId: UUID,
        socket: DefaultWebSocketServerSession
    ) {
        AttendanceWebSocketManager.unregister(sessionId, socket)
    }

    override suspend fun getInitialSnapshot(
        lecturerId: UUID,
        sessionId: UUID
    ): LiveAttendanceSnapshot {

        val ownsSession =
            attendanceSessionRepository.existsByIdAndLecturerId(sessionId, lecturerId)

        if (!ownsSession) {
            throw AuthorizationException("You do not own this attendance session")
        }

        return attendanceSessionRepository.getLiveAttendanceSnapshot(sessionId)
    }
}

 */
