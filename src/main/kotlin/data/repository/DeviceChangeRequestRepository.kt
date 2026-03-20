package data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import data.database.entities.DeviceChangeRequestsTable
import data.database.entities.StudentEnrollmentsTable
import data.database.entities.LecturerTeachingAssignmentsTable
import domain.models.DeviceChangeDomainRequest
import domain.models.DeviceChangeStatus
import org.jetbrains.exposed.sql.*
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
}