package data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Student
import data.database.entities.*
import domain.models.DeviceChangeDomainRequest
import domain.models.DeviceChangeStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class DeviceChangeRequestRepository {

    suspend fun create(request: DeviceChangeDomainRequest): DeviceChangeDomainRequest = exposedTransaction {
        DeviceChangeRequestsTable.insert {
            it[id] = request.id
            it[studentId] = request.studentId
            it[oldDeviceId] = request.oldDeviceId
            it[newDeviceId] = request.newDeviceId
            it[newDeviceModel] = request.newDeviceModel
            it[newDeviceOS] = request.newDeviceOS
            it[newFcmToken] = request.newFcmToken
            it[reason] = request.reason
            it[status] = request.status
            it[requestedAt] = request.requestedAt
        }
        request
    }

    suspend fun findById(id: UUID): DeviceChangeDomainRequest? = exposedTransaction {
        DeviceChangeRequestsTable
            .selectAll()
            .where { DeviceChangeRequestsTable.id eq id }
            .map { it.toDeviceChangeRequest() }
            .singleOrNull()
    }

    suspend fun findPendingRequestsForLecturer(lecturerId: UUID): List<DeviceChangeDomainRequest> = exposedTransaction {
        DeviceChangeRequestsTable
            .innerJoin(StudentEnrollmentsTable) {
                DeviceChangeRequestsTable.studentId eq StudentEnrollmentsTable.studentId
            }
            .innerJoin(LecturerTeachingAssignmentsTable) {
                (StudentEnrollmentsTable.programmeId eq LecturerTeachingAssignmentsTable.programmeId) and
                        (StudentEnrollmentsTable.yearOfStudy eq LecturerTeachingAssignmentsTable.yearOfStudy) and
                        (StudentEnrollmentsTable.academicTermId eq LecturerTeachingAssignmentsTable.academicTermId)
            }
            .select(DeviceChangeRequestsTable.columns)
            .where {
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (DeviceChangeRequestsTable.status eq DeviceChangeStatus.PENDING) and
                        (StudentEnrollmentsTable.isActive eq true) and
                        (LecturerTeachingAssignmentsTable.isActive eq true)
            }
            .orderBy(DeviceChangeRequestsTable.requestedAt to SortOrder.DESC)
            .map { it.toDeviceChangeRequest() }
            .distinctBy { it.id } // Prevent duplicates if multiple teaching assignments
    }

    suspend fun findRequestsByStudent(studentId: UUID): List<DeviceChangeDomainRequest> = exposedTransaction {
        DeviceChangeRequestsTable
            .selectAll()
            .where { DeviceChangeRequestsTable.studentId eq studentId }
            .orderBy(DeviceChangeRequestsTable.requestedAt to SortOrder.DESC)
            .map { it.toDeviceChangeRequest() }
    }

    suspend fun cancelRequest(
        requestId: UUID,
        studentId: UUID
    ): Boolean = exposedTransaction {
        // Verify the request belongs to the student and is still pending
        val request = DeviceChangeRequestsTable
            .selectAll()
            .where {
                (DeviceChangeRequestsTable.id eq requestId) and
                        (DeviceChangeRequestsTable.studentId eq studentId) and
                        (DeviceChangeRequestsTable.status eq DeviceChangeStatus.PENDING)
            }
            .singleOrNull()

        if (request == null) {
            return@exposedTransaction false
        }

        // Update request status to CANCELLED
        val updated = DeviceChangeRequestsTable.update(
            where = { DeviceChangeRequestsTable.id eq requestId }
        ) {
            it[status] = DeviceChangeStatus.CANCELLED
            it[reviewedAt] = LocalDateTime.now()
            it[rejectionReason] = "Cancelled by student"
        } > 0

        // Also delete in DevicesTable if only it was a new device
        if (updated) {
            val newDeviceId = request[DeviceChangeRequestsTable.newDeviceId]
            DevicesTable.deleteWhere { DevicesTable.deviceId eq newDeviceId }
        }

        updated
    }

    suspend fun approveRequest(
        requestId: UUID,
        reviewerId: UUID
    ): Boolean = exposedTransaction {
        DeviceChangeRequestsTable.update(
            where = { DeviceChangeRequestsTable.id eq requestId }
        ) {
            it[status] = DeviceChangeStatus.APPROVED
            it[reviewedBy] = reviewerId
            it[reviewedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun rejectRequest(
        requestId: UUID,
        reviewerId: UUID,
        rejectionReason: String
    ): Boolean = exposedTransaction {
        DeviceChangeRequestsTable.update(
            where = { DeviceChangeRequestsTable.id eq requestId }
        ) {
            it[status] = DeviceChangeStatus.REJECTED
            it[reviewedBy] = reviewerId
            it[reviewedAt] = LocalDateTime.now()
            it[this.rejectionReason] = rejectionReason
        } > 0
    }

    suspend fun systemRejectRequest(
        requestId: UUID,
        rejectionReason: String
    ): Boolean = exposedTransaction {
        DeviceChangeRequestsTable.update(
            where = { DeviceChangeRequestsTable.id eq requestId }
        ) {
            it[status] = DeviceChangeStatus.REJECTED
            it[reviewedAt] = LocalDateTime.now()
            it[this.rejectionReason] = rejectionReason
        } > 0
    }

    suspend fun canLecturerApproveRequest(lecturerId: UUID, studentId: UUID): Boolean = exposedTransaction {
        StudentEnrollmentsTable
            .innerJoin(LecturerTeachingAssignmentsTable) {
                (StudentEnrollmentsTable.programmeId eq LecturerTeachingAssignmentsTable.programmeId) and
                        (StudentEnrollmentsTable.yearOfStudy eq LecturerTeachingAssignmentsTable.yearOfStudy) and
                        (StudentEnrollmentsTable.academicTermId eq LecturerTeachingAssignmentsTable.academicTermId)
            }
            .select(StudentEnrollmentsTable.id)
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.isActive eq true) and
                        (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.isActive eq true)
            }
            .limit(1)
            .count() > 0
    }

    private fun ResultRow.toDeviceChangeRequest(): DeviceChangeDomainRequest = DeviceChangeDomainRequest(
        id = this[DeviceChangeRequestsTable.id],
        studentId = this[DeviceChangeRequestsTable.studentId],
        oldDeviceId = this[DeviceChangeRequestsTable.oldDeviceId],
        newDeviceId = this[DeviceChangeRequestsTable.newDeviceId],
        newDeviceModel = this[DeviceChangeRequestsTable.newDeviceModel],
        newDeviceOS = this[DeviceChangeRequestsTable.newDeviceOS],
        newFcmToken = this[DeviceChangeRequestsTable.newFcmToken],
        reason = this[DeviceChangeRequestsTable.reason],
        status = this[DeviceChangeRequestsTable.status],
        requestedAt = this[DeviceChangeRequestsTable.requestedAt],
        reviewedBy = this[DeviceChangeRequestsTable.reviewedBy],
        reviewedAt = this[DeviceChangeRequestsTable.reviewedAt],
        rejectionReason = this[DeviceChangeRequestsTable.rejectionReason]
    )


    /*------------------------------------
           ADMIN METHODS
    ---------------------------------------*/

    suspend fun getAllDeviceChangeRequests(
        page: Int = 1,
        pageSize: Int = 20,
        status: DeviceChangeStatus? = null,
        studentId: UUID? = null,
        search: String? = null
    ): Triple<List<DeviceChangeDomainRequest>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = DeviceChangeRequestsTable
            .innerJoin(StudentsTable, { DeviceChangeRequestsTable.studentId }, { StudentsTable.id })
            .select(DeviceChangeRequestsTable.columns + StudentsTable.fullName + StudentsTable.registrationNumber)

        status?.let {
            query = query.andWhere { DeviceChangeRequestsTable.status eq it }
        }

        studentId?.let {
            query = query.andWhere { DeviceChangeRequestsTable.studentId eq it }
        }

        if (!search.isNullOrBlank()) {
            query = query.andWhere {
                (StudentsTable.fullName like "%$search%") or
                        (StudentsTable.registrationNumber like "%$search%")
            }
        }

        val total = query.count()

        val requests = query
            .orderBy(DeviceChangeRequestsTable.requestedAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                row.toDeviceChangeRequest()
            }

        Triple(requests, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun getDeviceChangeRequestWithStudentDetails(id: UUID): Pair<DeviceChangeDomainRequest, Student?>? = exposedTransaction {
        val row = DeviceChangeRequestsTable
            .innerJoin(StudentsTable, { DeviceChangeRequestsTable.studentId }, { StudentsTable.id })
            .select(DeviceChangeRequestsTable.columns + StudentsTable.fullName + StudentsTable.registrationNumber)
            .where { DeviceChangeRequestsTable.id eq id }
            .singleOrNull()

        row?.let {
            val request = it.toDeviceChangeRequest()
            val student = Student(
                id = it[StudentsTable.id],
                registrationNumber = it[StudentsTable.registrationNumber],
                fullName = it[StudentsTable.fullName],
                isActive = it[StudentsTable.isActive],
                lastLogin = it[StudentsTable.lastLoginAt],
                createdAt = it[StudentsTable.createdAt],
                updatedAt = it[StudentsTable.updatedAt],
                device = null
            )
            request to student
        }
    }
}