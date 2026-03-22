package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.requests.StudentLookupRequest
import com.amos_tech_code.api.dtos.response.DeviceLookupInfo
import com.amos_tech_code.api.dtos.response.PendingDeviceChangeInfo
import com.amos_tech_code.api.dtos.response.StudentBasicInfo
import com.amos_tech_code.api.dtos.response.StudentLookupResponse
import com.amos_tech_code.data.repository.AttendanceRecordRepository
import com.amos_tech_code.data.repository.LecturerRepository
import com.amos_tech_code.utils.AuthorizationException
import com.amos_tech_code.utils.ResourceNotFoundException
import api.dtos.*
import com.amos_tech_code.api.dtos.response.AttendanceSummary
import com.amos_tech_code.api.dtos.response.EnrollmentInfo
import com.amos_tech_code.api.dtos.response.RecentActivityInfo
import com.amos_tech_code.api.dtos.response.UnitAttendanceInfo
import com.amos_tech_code.api.dtos.response.UnitInfo
import com.amos_tech_code.domain.models.ResolvedUniversity
import com.amos_tech_code.domain.services.StudentLookUpService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ValidationException
import data.repository.*
import domain.models.ActivityType
import domain.models.DeviceChangeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.*

class StudentLookUpServiceImpl(
    private val studentRepository: StudentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val deviceChangeRequestRepository: DeviceChangeRequestRepository,
    private val lecturerRepository: LecturerRepository,
    private val attendanceSessionRepository: AttendanceSessionRepository
) : StudentLookUpService
{

    private val logger = LoggerFactory.getLogger(StudentLookUpServiceImpl::class.java)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Look up student by registration number for a specific lecturer
     * Only returns data if the student is enrolled in the lecturer's university
     */
    override suspend fun lookupStudent(
        lecturerId: UUID,
        request: StudentLookupRequest
    ): StudentLookupResponse = withContext(Dispatchers.IO) {
        try {

            require(request.registrationNumber.isNotBlank()) {
                throw ValidationException("Student Registration is required")
            }
            // Validate lecturer exists and get their university
            val lecturer = lecturerRepository.findById(lecturerId)
                ?: throw ResourceNotFoundException("Lecturer not found")

            val lecturerUniversities = lecturerRepository.getLecturerUniversities(lecturerId)
            if (lecturerUniversities.isEmpty()) {
                throw AuthorizationException("You are not assigned to any university")
            }

            // Find student by registration number
            val student = studentRepository.findByRegistrationNumber(request.registrationNumber)
                ?: throw ResourceNotFoundException("Student not found")

            // Verify student is enrolled in one of the lecturer's universities
            val studentEnrollment = studentEnrollmentRepository.findActiveEnrollment(student.id)
                ?: throw AuthorizationException("Student is not enrolled in your university")

            val validEnrollment = lecturerUniversities.any { it.id == studentEnrollment.universityId }
            if (!validEnrollment) {
                throw AuthorizationException("Student is not enrolled in your university")
            }
            // Get student's device info
            val device = studentRepository.findActiveDeviceByStudentId(student.id)

            // Get enrollment information
            val enrollmentInfo = getEnrollmentInfo(student.id, lecturerId, lecturerUniversities)

            // Get attendance summary
            val attendanceSummary = getAttendanceSummary(student.id, lecturerId)

            // Check for pending device change request
            val pendingRequest = deviceChangeRequestRepository.findRequestsByStudent(student.id)
                .firstOrNull { it.status == DeviceChangeStatus.PENDING }
                ?.let { request ->
                    PendingDeviceChangeInfo(
                        requestId = request.id.toString(),
                        requestedAt = request.requestedAt.toString(),
                        newDeviceModel = request.newDeviceModel,
                        newDeviceOS = request.newDeviceOS,
                        reason = request.reason
                    )
                }

            // Get recent activity
            val recentActivity = getRecentActivity(student.id, lecturerId)

            StudentLookupResponse(
                studentInfo = StudentBasicInfo(
                    studentId = student.id.toString(),
                    fullName = student.fullName,
                    registrationNumber = student.registrationNumber,
                    isActive = student.isActive,
                    lastLoginAt = student.lastLogin?.toString()
                ),
                deviceInfo = DeviceLookupInfo(
                    deviceId = device?.deviceId,
                    deviceModel = device?.model,
                    deviceStatus = device?.status?.name,
                    lastSeen = device?.lastSeen?.toString()
                ),
                enrollmentInfo = enrollmentInfo,
                attendanceSummary = attendanceSummary,
                pendingDeviceChange = pendingRequest,
                recentActivity = recentActivity
            )

        } catch (e: Exception) {
            logger.error("Student lookup failed for lecturer $lecturerId", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to lookup student")
            }
        }
    }

    private suspend fun getEnrollmentInfo(
        studentId: UUID,
        lecturerId: UUID,
        lecturerUniversities: List<ResolvedUniversity>
    ): List<EnrollmentInfo> {
        val enrollments = studentEnrollmentRepository.findActiveEnrollments(studentId)

        return enrollments.map { enrollment ->
            // Get units for this enrollment
            val units = getUnitsForEnrollment(
                studentId = studentId,
                programmeId = enrollment.programmeId,
                academicTermId = enrollment.academicTermId,
                lecturerId = lecturerId
            )

            EnrollmentInfo(
                programmeName = enrollment.programmeName,
                yearOfStudy = enrollment.yearOfStudy,
                academicTerm = "${enrollment.academicYear} - Semester ${enrollment.semester}",
                enrollmentDate = enrollment.enrollmentDate.toString(),
                units = units
            )
        }
    }

    private suspend fun getUnitsForEnrollment(
        studentId: UUID,
        programmeId: UUID,
        academicTermId: UUID,
        lecturerId: UUID
    ): List<UnitInfo> {
        val units = attendanceSessionRepository.getUnitsForStudent(
            studentId = studentId,
            programmeId = programmeId,
            academicTermId = academicTermId
        )

        // Get teaching assignments for this lecturer
        val teachingUnits = lecturerRepository.getTeachingUnits(
            lecturerId = lecturerId,
            academicTermId = academicTermId
        ).map { it.unitId }

        return units.map { unit ->
            val attendance = attendanceRecordRepository.getAttendanceStatsForUnit(
                studentId = studentId,
                unitId = unit.unitId,
                academicTermId = academicTermId
            )

            UnitInfo(
                unitId = unit.unitId.toString(),
                unitCode = unit.unitCode,
                unitName = unit.unitName,
                sessionsAttended = attendance.attended,
                totalSessions = attendance.total,
                attendancePercentage = if (attendance.total > 0)
                    (attendance.attended.toDouble() / attendance.total) * 100 else 0.0,
                isTeaching = teachingUnits.contains(unit.unitId)
            )
        }
    }

    private suspend fun getAttendanceSummary(
        studentId: UUID,
        lecturerId: UUID
    ): AttendanceSummary {
        // Get overall attendance stats
        val overallStats = attendanceRecordRepository.getOverallAttendanceStats(studentId)

        // Get attendance by unit
        val unitsAttendance = attendanceRecordRepository.getAttendanceByUnit(studentId)

        // Get suspicious activity count
        val suspiciousCount = attendanceRecordRepository.getSuspiciousActivityCount(studentId)

        // Get last attendance date
        val lastAttendance = attendanceRecordRepository.getLastAttendanceDate(studentId)

        return AttendanceSummary(
            overallAttendance = if (overallStats.total > 0)
                (overallStats.attended.toDouble() / overallStats.total) * 100 else 0.0,
            totalSessions = overallStats.total,
            sessionsAttended = overallStats.attended,
            suspiciousActivities = suspiciousCount,
            lastAttendanceDate = lastAttendance?.toString(),
            attendanceByUnit = unitsAttendance.map { unit ->
                UnitAttendanceInfo(
                    unitCode = unit.unitCode,
                    unitName = unit.unitName,
                    attended = unit.attended,
                    total = unit.total,
                    percentage = if (unit.total > 0)
                        (unit.attended.toDouble() / unit.total) * 100 else 0.0
                )
            }
        )
    }

    private suspend fun getRecentActivity(
        studentId: UUID,
        lecturerId: UUID
    ): List<RecentActivityInfo> {
        val activities = mutableListOf<RecentActivityInfo>()

        // Get recent attendance records
        val recentAttendance = attendanceRecordRepository.getRecentAttendance(studentId, limit = 5)
        recentAttendance.forEach { attendance ->
            activities.add(
                RecentActivityInfo(
                    activityType = ActivityType.ATTENDANCE_MARKED,
                    description = "Attended ${attendance.unitCode} - ${attendance.sessionTitle}",
                    timestamp = attendance.attendedAt.toString(),
                    details = mapOf(
                        "unitCode" to attendance.unitCode,
                        "sessionTitle" to attendance.sessionTitle,
                        "method" to attendance.attendanceMethod,
                        "isSuspicious" to attendance.isSuspicious.toString()
                    )
                )
            )
        }

        // Get device change requests
        val deviceRequests = deviceChangeRequestRepository.findRequestsByStudent(studentId)
            .take(3)
        deviceRequests.forEach { request ->
            activities.add(
                RecentActivityInfo(
                    activityType = request.status.toActivityType(),
                    description = request.status.getDescription(request.newDeviceModel),
                    timestamp = request.requestedAt.toString(),
                    details = mapOf(
                        "status" to request.status.name,
                        "newDeviceModel" to request.newDeviceModel,
                        "newDeviceOS" to request.newDeviceOS,
                        "requestId" to request.id.toString(),
                        "reason" to (request.reason ?: "")
                    )
                )
            )
        }

        return activities.sortedByDescending { it.timestamp }.take(10)
    }

    fun DeviceChangeStatus.toActivityType(): ActivityType {
        return when (this) {
            DeviceChangeStatus.PENDING -> ActivityType.DEVICE_CHANGE_REQUESTED
            DeviceChangeStatus.APPROVED -> ActivityType.DEVICE_CHANGE_APPROVED
            DeviceChangeStatus.REJECTED -> ActivityType.DEVICE_CHANGE_REJECTED
            DeviceChangeStatus.CANCELLED -> ActivityType.DEVICE_CHANGE_CANCELLED
        }
    }

    fun DeviceChangeStatus.getDescription(deviceModel: String): String {
        return when (this) {
            DeviceChangeStatus.PENDING -> "Device change requested for $deviceModel"
            DeviceChangeStatus.APPROVED -> "Device change approved for $deviceModel"
            DeviceChangeStatus.REJECTED -> "Device change rejected for $deviceModel"
            DeviceChangeStatus.CANCELLED -> "Device change cancelled for $deviceModel"
        }
    }
}