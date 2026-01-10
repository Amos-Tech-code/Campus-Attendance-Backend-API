package data.repository

import api.dtos.response.LiveAttendanceSnapshot
import api.dtos.response.LiveAttendanceStudentDto
import api.dtos.response.ProgrammeAttendanceDto
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.domain.models.*
import com.amos_tech_code.utils.AuthorizationException
import data.database.entities.*
import domain.models.AttendanceMethod
import domain.models.AttendanceSessionStatus
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class AttendanceSessionRepository() {

    suspend fun existsByIdAndLecturerId(sessionId: UUID, lecturerId: UUID) : Boolean = exposedTransaction {
        AttendanceSessionsTable
            .select(
                AttendanceSessionsTable.lecturerId
            ).where {
                (AttendanceSessionsTable.id eq sessionId) and
                        (AttendanceSessionsTable.lecturerId eq lecturerId)
            }.any()
    }

    suspend fun getActiveSession(lecturerId: UUID): SessionResponse? = exposedTransaction {
        val session = AttendanceSessionsTable
            .join(UnitsTable, JoinType.INNER, AttendanceSessionsTable.unitId, UnitsTable.id)
            .selectAll().where {
                (AttendanceSessionsTable.lecturerId eq lecturerId) and
                        (AttendanceSessionsTable.status eq AttendanceSessionStatus.ACTIVE)
            }
            .limit(1)
            .singleOrNull()

        session?.let {
            val programmes = SessionProgrammesTable
                .innerJoin(ProgrammesTable)
                .innerJoin(DepartmentsTable)
                .selectAll().where { SessionProgrammesTable.sessionId eq session[AttendanceSessionsTable.id] }
                .map { row ->
                    ProgrammeInfo(
                        id = row[ProgrammesTable.id].toString(),
                        name = row[ProgrammesTable.name],
                        department = row[DepartmentsTable.name]
                    )
                }

            SessionResponse(
                sessionId = session[AttendanceSessionsTable.id].toString(),
                sessionCode = session[AttendanceSessionsTable.sessionCode],
                qrCodeUrl = session[AttendanceSessionsTable.qrCodeUrl],
                method = session[AttendanceSessionsTable.allowedMethod],
                universityId = session[AttendanceSessionsTable.universityId].toString(),
                programmes = programmes,
                unit = UnitInfo(
                    id = session[UnitsTable.id].toString(),
                    name = session[UnitsTable.name],
                    code = session[UnitsTable.code]
                ),

                isLocationRequired = session[AttendanceSessionsTable.isLocationRequired],
                location = LocationInfo(
                    latitude = session[AttendanceSessionsTable.lecturerLatitude],
                    longitude = session[AttendanceSessionsTable.lecturerLongitude],
                    radiusMeters = session[AttendanceSessionsTable.locationRadius]
                ),
                timeInfo = TimeInfo(
                    startTime = session[AttendanceSessionsTable.scheduledStartTime].toString(),
                    endTime = session[AttendanceSessionsTable.scheduledEndTime].toString(),
                    durationMinutes = session[AttendanceSessionsTable.durationMinutes]
                ),
                status = session[AttendanceSessionsTable.status],
                title = session[AttendanceSessionsTable.sessionTitle],
                sessionType = session[AttendanceSessionsTable.attendanceSessionType],
                weekNumber = session[AttendanceSessionsTable.weekNumber]
            )
        }
    }

    suspend fun createSession(sessionData: CreateSessionData): UUID = exposedTransaction {

        val sessionId = UUID.randomUUID()

        val sessionNumber = nextSessionNumber(
            lecturerId = sessionData.lecturerId,
            unitId = sessionData.unitId,
            academicTermId = sessionData.academicTermId,
            weekNumber = sessionData.weekNumber
        )

        AttendanceSessionsTable.insert {
            it[id] = sessionId
            it[AttendanceSessionsTable.lecturerId] = sessionData.lecturerId
            it[universityId] = sessionData.universityId
            it[academicTermId] = sessionData.academicTermId
            it[sessionTitle] = sessionData.title
            it[weekNumber] = sessionData.weekNumber
            it[attendanceSessionType] = sessionData.attendanceSessionType
            it[AttendanceSessionsTable.sessionNumber] = sessionNumber
            it[unitId] = sessionData.unitId
            it[sessionCode] = sessionData.sessionCode
            it[allowedMethod] = sessionData.allowedMethod
            it[qrCodeUrl] = sessionData.qrCodeUrl
            it[isLocationRequired] = sessionData.isLocationRequired
            it[lecturerLatitude] = sessionData.lecturerLatitude
            it[lecturerLongitude] = sessionData.lecturerLongitude
            it[locationRadius] = sessionData.locationRadius
            it[scheduledStartTime] = sessionData.scheduledStartTime
            it[scheduledEndTime] = sessionData.scheduledEndTime
            it[durationMinutes] = sessionData.durationMinutes
            it[status] = sessionData.sessionStatus
        }

        sessionId
    }

    suspend fun linkSessionToProgrammes(
        sessionId: UUID,
        programmeIds: List<UUID>,
        unitId: UUID,
        lecturerId: UUID,
        universityId: UUID
    ) = exposedTransaction {
        programmeIds.forEach { programmeId ->
            // Get yearOfStudy directly from LecturerTeachingAssignmentsTable for each programme
            val teachingAssignment = LecturerTeachingAssignmentsTable
                .select(LecturerTeachingAssignmentsTable.yearOfStudy)
                .where {
                    (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                            (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                            (LecturerTeachingAssignmentsTable.unitId eq unitId) and
                            (LecturerTeachingAssignmentsTable.programmeId eq programmeId)
                }
                .singleOrNull()

            val yearOfStudy = teachingAssignment?.get(LecturerTeachingAssignmentsTable.yearOfStudy)
                ?: throw AuthorizationException("No teaching assignment found for programme $programmeId and unit $unitId")

            SessionProgrammesTable.insert {
                it[id] = UUID.randomUUID()
                it[SessionProgrammesTable.sessionId] = sessionId
                it[SessionProgrammesTable.programmeId] = programmeId
                it[SessionProgrammesTable.yearOfStudy] = yearOfStudy
            }
        }
    }

    suspend fun validateLecturerAuthorization(
        lecturerId: UUID,
        universityId: UUID,
        academicTermId: UUID,
        unitId: UUID,
        programmeIds: List<UUID>
    ) =
        exposedTransaction {
            // Check if lecturer is authorized to teach this unit
            val unauthorizedProgrammes = programmeIds.filterNot { programmeId ->
                LecturerTeachingAssignmentsTable
                    .selectAll()
                    .where {
                        (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                                (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                                (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId) and
                                (LecturerTeachingAssignmentsTable.unitId eq unitId) and
                                (LecturerTeachingAssignmentsTable.programmeId eq programmeId)
                    }
                    .any()
            }
            if (unauthorizedProgrammes.isNotEmpty()) {
                throw AuthorizationException("Unauthorized for programmes: $unauthorizedProgrammes")
            }

        }

    suspend fun findUnitCodeById(unitId: UUID) : String = exposedTransaction {
        UnitsTable
            .select(UnitsTable.code)
            .where { UnitsTable.id eq unitId }
            .map { it[UnitsTable.code] }
            .singleOrNull()
            ?: throw NotFoundException("Unit not found")
    }

    suspend fun getSessionDetails(sessionId: UUID): SessionResponse? {
        return exposedTransaction {
            val session = AttendanceSessionsTable
                .join(UnitsTable, JoinType.INNER, AttendanceSessionsTable.unitId, UnitsTable.id)
                .selectAll().where { AttendanceSessionsTable.id eq sessionId }
                .singleOrNull()

            // Get linked programmes
            val programmes = SessionProgrammesTable
                .join(ProgrammesTable, JoinType.INNER, SessionProgrammesTable.programmeId, ProgrammesTable.id)
                .join(DepartmentsTable, JoinType.INNER, ProgrammesTable.departmentId, DepartmentsTable.id)
                .selectAll().where { SessionProgrammesTable.sessionId eq sessionId }
                .map { row ->
                    ProgrammeInfo(
                        id = row[ProgrammesTable.id].toString(),
                        name = row[ProgrammesTable.name],
                        department = row[DepartmentsTable.name]
                    )
                }

            session?.let {
                SessionResponse(
                    sessionId = sessionId.toString(),
                    sessionCode = session[AttendanceSessionsTable.sessionCode],
                    qrCodeUrl = session[AttendanceSessionsTable.qrCodeUrl],
                    method = when (session[AttendanceSessionsTable.allowedMethod]) {
                        AttendanceMethod.QR_CODE -> AttendanceMethod.QR_CODE
                        AttendanceMethod.MANUAL_CODE -> AttendanceMethod.MANUAL_CODE
                        else -> AttendanceMethod.QR_CODE
                    },
                    universityId = session[AttendanceSessionsTable.universityId].toString(),
                    programmes = programmes,
                    unit = UnitInfo(
                        id = session[UnitsTable.id].toString(),
                        name = session[UnitsTable.name],
                        code = session[UnitsTable.code]
                    ),
                    isLocationRequired = session[AttendanceSessionsTable.isLocationRequired],
                    location = LocationInfo(
                        latitude = session[AttendanceSessionsTable.lecturerLatitude],
                        longitude = session[AttendanceSessionsTable.lecturerLongitude],
                        radiusMeters = session[AttendanceSessionsTable.locationRadius]
                    ),
                    timeInfo = TimeInfo(
                        startTime = session[AttendanceSessionsTable.scheduledStartTime].toString(),
                        endTime = session[AttendanceSessionsTable.scheduledEndTime].toString(),
                        durationMinutes = session[AttendanceSessionsTable.durationMinutes]
                    ),
                    status = session[AttendanceSessionsTable.status],
                    title = session[AttendanceSessionsTable.sessionTitle],
                    sessionType = session[AttendanceSessionsTable.attendanceSessionType],
                    weekNumber = session[AttendanceSessionsTable.weekNumber]
                )
            }
        }
    }

    suspend fun updateSession(
        sessionId: UUID,
        lecturerId: UUID,
        updateData: UpdateSessionData
    ): SessionResponse? {

        exposedTransaction {
            val sessionRow = AttendanceSessionsTable
                .selectAll()
                .where {
                    (AttendanceSessionsTable.id eq sessionId) and
                            (AttendanceSessionsTable.lecturerId eq lecturerId) and
                            (AttendanceSessionsTable.status inList listOf(
                                AttendanceSessionStatus.ACTIVE,
                                AttendanceSessionStatus.SCHEDULED
                            ))
                }
                .singleOrNull() ?: return@exposedTransaction null

            val currentStartTime = sessionRow[AttendanceSessionsTable.scheduledStartTime]
            val newStartTime = updateData.scheduledStartTime ?: currentStartTime
            val newDuration = updateData.durationMinutes ?: sessionRow[AttendanceSessionsTable.durationMinutes]
            val newEndTime = newStartTime.plusMinutes(newDuration.toLong())

            AttendanceSessionsTable.update(
                where = { AttendanceSessionsTable.id eq sessionId }
            ) {
                updateData.title?.let { it1 -> it[sessionTitle] = it1 }
                updateData.attendanceSessionType?.let { it1 -> it[attendanceSessionType] = it1 }
                updateData.weekNumber?.let { it1 -> it[weekNumber] = it1 }
                updateData.unitId?.let { it1 -> it[unitId] = it1 }
                updateData.allowedMethod?.let { it1 -> it[allowedMethod] = it1 }
                updateData.isLocationRequired?.let { it1 -> it[isLocationRequired] = it1 }
                updateData.lecturerLatitude?.let { it1 -> it[lecturerLatitude] = it1 }
                updateData.lecturerLongitude?.let { it1 -> it[lecturerLongitude] = it1 }
                updateData.locationRadius?.let { it1 -> it[locationRadius] = it1 }

                if (updateData.scheduledStartTime != null || updateData.durationMinutes != null) {
                    it[scheduledStartTime] = newStartTime
                    it[scheduledEndTime] = newEndTime
                    it[durationMinutes] = newDuration
                }

                it[updatedAt] = LocalDateTime.now()
            }

            // Programme relinking (unchanged logic, correct)
            updateData.programmeIds?.let { programmeIds ->
                val currentUnitId = updateData.unitId ?: sessionRow[AttendanceSessionsTable.unitId]
                val universityId = sessionRow[AttendanceSessionsTable.universityId]

                SessionProgrammesTable.deleteWhere {
                    SessionProgrammesTable.sessionId eq sessionId
                }

                programmeIds.forEach { programmeId ->
                    val yearOfStudy = LecturerTeachingAssignmentsTable
                        .select(LecturerTeachingAssignmentsTable.yearOfStudy)
                        .where {
                            (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                                    (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                                    (LecturerTeachingAssignmentsTable.unitId eq currentUnitId) and
                                    (LecturerTeachingAssignmentsTable.programmeId eq programmeId)
                        }
                        .singleOrNull()
                        ?.get(LecturerTeachingAssignmentsTable.yearOfStudy)
                        ?: throw AuthorizationException("Unauthorized programme $programmeId")

                    SessionProgrammesTable.insert {
                        it[id] = UUID.randomUUID()
                        it[SessionProgrammesTable.sessionId] = sessionId
                        it[SessionProgrammesTable.programmeId] = programmeId
                        it[SessionProgrammesTable.yearOfStudy] = yearOfStudy
                    }
                }
            }
        }

        return getSessionDetails(sessionId)
    }

    suspend fun isSessionCodeUnique(sessionCode: String): Boolean = exposedTransaction {
        AttendanceSessionsTable
            .select(AttendanceSessionsTable.id)
            .where {
                (AttendanceSessionsTable.sessionCode eq sessionCode) and
                (AttendanceSessionsTable.status inList listOf(
                    AttendanceSessionStatus.ACTIVE, AttendanceSessionStatus.SCHEDULED)
                )
            }
            .limit(1)
            .empty()
    }

    suspend fun endSession(lecturerId: UUID, sessionId: UUID): Boolean = exposedTransaction {
        val session = AttendanceSessionsTable
            .selectAll().where {
                (AttendanceSessionsTable.id eq sessionId) and
                        (AttendanceSessionsTable.lecturerId eq lecturerId) and
                        (AttendanceSessionsTable.status eq AttendanceSessionStatus.ACTIVE)
            }
            .singleOrNull()

        session?.let {
            val updatedRows = AttendanceSessionsTable.update(
                where = {
                    (AttendanceSessionsTable.id eq sessionId) and
                            (AttendanceSessionsTable.lecturerId eq lecturerId)
                }
            ) {
                it[status] = AttendanceSessionStatus.ENDED
                it[updatedAt] = LocalDateTime.now()
            }

            updatedRows > 0
        } ?: false
    }

    suspend fun getSessionQrCodeUrl(sessionId: UUID): String? = exposedTransaction {
        AttendanceSessionsTable
            .selectAll().where { AttendanceSessionsTable.id eq sessionId }
            .singleOrNull()
            ?.get(AttendanceSessionsTable.qrCodeUrl)
    }

    suspend fun resolveActiveAcademicTermId(universityId: UUID): UUID = exposedTransaction {
        AcademicTermsTable
            .select(AcademicTermsTable.id)
            .where {
                (AcademicTermsTable.universityId eq universityId) and
                        (AcademicTermsTable.isActive eq true)
            }
            .singleOrNull()
            ?.get(AcademicTermsTable.id)
            ?: throw NotFoundException("No active academic term for university")
    }

    suspend fun autoExpireSessions() = exposedTransaction {
        AttendanceSessionsTable.update({
            (AttendanceSessionsTable.status eq AttendanceSessionStatus.ACTIVE) and
                    (AttendanceSessionsTable.scheduledEndTime lessEq LocalDateTime.now())
        }) {
            it[status] = AttendanceSessionStatus.EXPIRED
        }
    }

    suspend fun autoActivateSessions() = exposedTransaction {
        AttendanceSessionsTable.update(
            {
                (AttendanceSessionsTable.status eq AttendanceSessionStatus.SCHEDULED) and
                        (AttendanceSessionsTable.scheduledStartTime greaterEq LocalDateTime.now())
            }
        ) {
            it[status] = AttendanceSessionStatus.ACTIVE
        }
    }

    /**
     * Attendance Marking Implementation
     * Link students with universities and programmes on first attendance
     */

    suspend fun hasExistingAttendance(studentId: UUID, sessionId: UUID): Boolean = exposedTransaction {
            AttendanceRecordsTable
                .select(AttendanceRecordsTable.id)
                .where {
                    (AttendanceRecordsTable.studentId eq studentId) and
                            (AttendanceRecordsTable.sessionId eq sessionId)
                }
                .any()
    }

    suspend fun getActiveSessionBySessionCodeAndUnitCode(sessionCode: String, unitCode: String): AttendanceSession? =
        exposedTransaction {
            AttendanceSessionsTable
                .join(UnitsTable, JoinType.INNER, AttendanceSessionsTable.unitId, UnitsTable.id)
                .join(LecturersTable, JoinType.INNER, AttendanceSessionsTable.lecturerId, LecturersTable.id)
                .selectAll()
                .where {
                    (AttendanceSessionsTable.sessionCode eq sessionCode) and
                            (UnitsTable.code eq unitCode) and
                            (AttendanceSessionsTable.status eq AttendanceSessionStatus.ACTIVE)
                }
                .orderBy(AttendanceSessionsTable.createdAt to SortOrder.DESC)
                .limit(1)
                .map { row ->
                    AttendanceSession(
                        id = row[AttendanceSessionsTable.id],
                        sessionCode = row[AttendanceSessionsTable.sessionCode],
                        unitId = row[AttendanceSessionsTable.unitId],
                        universityId = row[AttendanceSessionsTable.universityId],
                        lecturerId = row[AttendanceSessionsTable.lecturerId],
                        isLocationRequired = row[AttendanceSessionsTable.isLocationRequired],
                        lecturerLatitude = row[AttendanceSessionsTable.lecturerLatitude],
                        lecturerLongitude = row[AttendanceSessionsTable.lecturerLongitude],
                        locationRadius = row[AttendanceSessionsTable.locationRadius],
                        unitName = row[UnitsTable.name],
                        unitCode = row[UnitsTable.code],
                        lecturerName = row[LecturersTable.fullName] ?: "Unknown",
                        academicTermId = row[AttendanceSessionsTable.academicTermId],
                        allowedMethod = row[AttendanceSessionsTable.allowedMethod],
                        scheduledStartTime = row[AttendanceSessionsTable.scheduledStartTime],
                        scheduledEndTime = row[AttendanceSessionsTable.scheduledEndTime]
                    )
                }
                .singleOrNull()
        }

    suspend fun isFirstAttendance(studentId: UUID): Boolean = exposedTransaction {
        AttendanceRecordsTable
            .select(AttendanceRecordsTable.id).where { AttendanceRecordsTable.studentId eq studentId }
            .any()
    }

    suspend fun getSessionProgrammes(sessionId: UUID): List<SessionProgramme> = exposedTransaction {
        SessionProgrammesTable
            .join(ProgrammesTable, JoinType.INNER, SessionProgrammesTable.programmeId, ProgrammesTable.id)
            .join(DepartmentsTable, JoinType.INNER, ProgrammesTable.departmentId, DepartmentsTable.id)
            .selectAll().where { SessionProgrammesTable.sessionId eq sessionId }
            .map { row ->
                SessionProgramme(
                    programmeId = row[SessionProgrammesTable.programmeId],
                    programmeName = row[ProgrammesTable.name],
                    departmentName = row[DepartmentsTable.name],
                    yearOfStudy = row[SessionProgrammesTable.yearOfStudy]
                )
            }

    }

    suspend fun createAttendanceRecord(
        studentId: UUID,
        sessionId: UUID,
        deviceId: String?,
        studentLat: Double?,
        studentLng: Double?,
        distance: Double?,
        isDeviceVerified: Boolean,
        isLocationVerified: Boolean,
        attendanceMethod: AttendanceMethod,
        isSuspicious: Boolean,
        suspiciousReason: String?
    ): AttendanceRecord = exposedTransaction {

        val attendanceId = UUID.randomUUID()
        val attendedAt = LocalDateTime.now()

        // Insert attendance record
        AttendanceRecordsTable.insert {
            it[id] = attendanceId
            it[AttendanceRecordsTable.sessionId] = sessionId
            it[AttendanceRecordsTable.studentId] = studentId
            it[AttendanceRecordsTable.attendanceMethodUsed] = attendanceMethod
            it[AttendanceRecordsTable.studentLatitude] = studentLat
            it[AttendanceRecordsTable.studentLongitude] = studentLng
            it[AttendanceRecordsTable.distanceFromLecturer] = distance
            it[AttendanceRecordsTable.isLocationVerified] = isLocationVerified
            it[AttendanceRecordsTable.deviceId] = deviceId
            it[AttendanceRecordsTable.isDeviceVerified] = isDeviceVerified
            it[AttendanceRecordsTable.isSuspicious] = isSuspicious
            it[AttendanceRecordsTable.suspiciousReason] = suspiciousReason
            it[AttendanceRecordsTable.attendedAt] = attendedAt
        }

        AttendanceRecord(
            id = attendanceId,
            attendedAt = attendedAt,
            isSuspicious = isSuspicious,
            suspiciousReason = suspiciousReason
        )
    }

    private fun nextSessionNumber(
        lecturerId: UUID,
        unitId: UUID,
        academicTermId: UUID,
        weekNumber: Int
    ): Int =
        AttendanceSessionsTable
            .selectAll()
            .where {
                (AttendanceSessionsTable.lecturerId eq lecturerId) and
                        (AttendanceSessionsTable.unitId eq unitId) and
                        (AttendanceSessionsTable.academicTermId eq academicTermId) and
                        (AttendanceSessionsTable.weekNumber eq weekNumber)
            }
            .maxOfOrNull { it[AttendanceSessionsTable.sessionNumber] }
            ?.plus(1)
            ?: 1


    /**
     * Live Attendance Implementation
     * Query Service (Grouped by Programme)
     *
     */
    suspend fun getLiveAttendanceSnapshot(
        sessionId: UUID
    ): LiveAttendanceSnapshot = exposedTransaction {

        // Get the session first to get academic term ID
        val session = AttendanceSessionsTable
            .select(
                AttendanceSessionsTable.academicTermId,
                AttendanceSessionsTable.unitId,
                AttendanceSessionsTable.lecturerId
            )
            .where { AttendanceSessionsTable.id eq sessionId }
            .singleOrNull()
            ?: throw IllegalArgumentException("Session not found")

        val academicTermId = session[AttendanceSessionsTable.academicTermId]

        // Step 1: Get all programmes linked to this session
        val programmesData = SessionProgrammesTable
            .innerJoin(ProgrammesTable) { SessionProgrammesTable.programmeId eq ProgrammesTable.id }
            .innerJoin(LecturerTeachingAssignmentsTable) {
                (SessionProgrammesTable.programmeId eq LecturerTeachingAssignmentsTable.programmeId) and
                        (SessionProgrammesTable.yearOfStudy eq LecturerTeachingAssignmentsTable.yearOfStudy) and
                        (LecturerTeachingAssignmentsTable.unitId eq session[AttendanceSessionsTable.unitId]) and
                        (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId) and
                        (LecturerTeachingAssignmentsTable.lecturerId eq session[AttendanceSessionsTable.lecturerId])
            }
            .select(
                SessionProgrammesTable.programmeId,
                SessionProgrammesTable.yearOfStudy,
                ProgrammesTable.name,
                LecturerTeachingAssignmentsTable.expectedStudents
            )
            .where { SessionProgrammesTable.sessionId eq sessionId }
            .groupBy(
                SessionProgrammesTable.programmeId,
                SessionProgrammesTable.yearOfStudy,
                ProgrammesTable.name,
                LecturerTeachingAssignmentsTable.expectedStudents
            )

        // Step 2: For each programme, get the students who attended
        val programmes = programmesData.map { programmeRow ->
            val programmeId = programmeRow[SessionProgrammesTable.programmeId]
            val programmeName = programmeRow[ProgrammesTable.name]
            val yearOfStudy = programmeRow[SessionProgrammesTable.yearOfStudy]
            val expectedStudents = programmeRow[LecturerTeachingAssignmentsTable.expectedStudents]

            // Get students who attended AND are enrolled in this specific programme
            val students = AttendanceRecordsTable
                .innerJoin(StudentsTable) { AttendanceRecordsTable.studentId eq StudentsTable.id }
                .innerJoin(StudentEnrollmentsTable) {
                    (AttendanceRecordsTable.studentId eq StudentEnrollmentsTable.studentId) and
                            (StudentEnrollmentsTable.programmeId eq programmeId) and
                            (StudentEnrollmentsTable.academicTermId eq academicTermId) and
                            (StudentEnrollmentsTable.yearOfStudy eq yearOfStudy)
                }
                .select(
                    StudentsTable.id,
                    StudentsTable.registrationNumber,
                    StudentsTable.fullName,
                    AttendanceRecordsTable.attendedAt,
                    AttendanceRecordsTable.isSuspicious,
                    AttendanceRecordsTable.suspiciousReason
                )
                .where { AttendanceRecordsTable.sessionId eq sessionId }
                .map { studentRow ->
                    LiveAttendanceStudentDto(
                        studentId = studentRow[StudentsTable.id].toString(),
                        regNo = studentRow[StudentsTable.registrationNumber],
                        name = studentRow[StudentsTable.fullName],
                        attendedAt = studentRow[AttendanceRecordsTable.attendedAt].toString(),
                        isSuspicious = studentRow[AttendanceRecordsTable.isSuspicious],
                        suspiciousReason = studentRow[AttendanceRecordsTable.suspiciousReason]
                    )
                }
                .distinctBy { it.studentId } // Ensure no duplicates

            ProgrammeAttendanceDto(
                programmeId = programmeId.toString(),
                programmeName = programmeName,
                yearOfStudy = yearOfStudy,
                noOfExpectedStudents = expectedStudents,
                students = students
            )
        }

        LiveAttendanceSnapshot(
            sessionId = sessionId.toString(),
            programmes = programmes
        )
    }

}