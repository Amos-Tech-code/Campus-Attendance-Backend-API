package data.repository

import com.amos_tech_code.api.dtos.admin.EnrollmentInfoAdmin
import com.amos_tech_code.api.dtos.admin.StudentResponse
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Device
import com.amos_tech_code.domain.models.Student
import data.database.entities.AcademicTermsTable
import data.database.entities.DevicesTable
import data.database.entities.ProgrammesTable
import data.database.entities.StudentEnrollmentsTable
import data.database.entities.StudentsTable
import data.database.entities.UniversitiesTable
import domain.models.DeviceStatus
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime
import java.util.*

class StudentRepository {

    suspend fun findByRegistrationNumber(regNo: String): Student? = exposedTransaction {
        StudentsTable
            .selectAll().where { StudentsTable.registrationNumber eq regNo }
            .map { it.toStudent() }
            .singleOrNull()
    }

    suspend fun findById(id: UUID): Student? = exposedTransaction {
        StudentsTable
            .selectAll().where { StudentsTable.id eq id }
            .map { it.toStudent() }
            .singleOrNull()
    }

    suspend fun findDeviceByStudentId(studentId: UUID): Device? = exposedTransaction {
        DevicesTable
            .selectAll()
            .where { DevicesTable.studentId eq studentId }
            .map {
                Device(
                    id = it[DevicesTable.id],
                    status = it[DevicesTable.status],
                    studentId = it[DevicesTable.studentId],
                    deviceId = it[DevicesTable.deviceId],
                    model = it[DevicesTable.deviceModel],
                    os = it[DevicesTable.os],
                    fcmToken = it[DevicesTable.fcmToken],
                    lastSeen = it[DevicesTable.lastSeen],
                    createdAt = it[DevicesTable.createdAt],
                    updatedAt = it[DevicesTable.updatedAt]
                )
            }
            .singleOrNull()
    }

    /**
     * Find active device by student ID
     */
    suspend fun findActiveDeviceByStudentId(studentId: UUID): Device? = exposedTransaction {
        DevicesTable
            .selectAll()
            .where {
                (DevicesTable.studentId eq studentId) and
                        (DevicesTable.status eq DeviceStatus.ACTIVE)
            }
            .map { it.toDevice() }
            .singleOrNull()
    }

    suspend fun existsByRegistrationNumber(regNo: String, excludeStudentId: UUID): Boolean =
        exposedTransaction {
            StudentsTable
                .select(StudentsTable.id)
                .where {
                    (StudentsTable.registrationNumber eq regNo) and
                            (StudentsTable.id neq excludeStudentId)
                }
                .count() > 0
        }

    suspend fun updateProfile(
        studentId: UUID,
        fullName: String,
        registrationNumber: String
    ): Boolean = exposedTransaction {
        StudentsTable.update({ StudentsTable.id eq studentId }) {
            it[StudentsTable.fullName] = fullName
            it[StudentsTable.registrationNumber] = registrationNumber
            it[StudentsTable.updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun createStudentWithDevice(
        student: Student
    ): Student = exposedTransaction {

        StudentsTable.insert {
            it[id] = student.id
            it[registrationNumber] = student.registrationNumber
            it[fullName] = student.fullName
            it[createdAt] = student.createdAt
            it[updatedAt] = student.updatedAt ?: LocalDateTime.now()
            it[lastLoginAt] = student.lastLogin
        }

        student.device?.let { device ->
            DevicesTable.insert {
                it[id] = device.id
                it[studentId] = student.id
                it[deviceId] = device.deviceId
                it[deviceModel] = device.model
                it[os] = device.os
                it[fcmToken] = device.fcmToken
                it[lastSeen] = device.lastSeen
                it[createdAt] = device.createdAt
            }
        }

        student
    }


    suspend fun updateDevice(device: Device): Boolean = exposedTransaction {
        DevicesTable.update(
            where = { DevicesTable.studentId eq device.studentId },
        ) {
            it[id] = device.id
            it[DevicesTable.studentId] = studentId
            it[deviceId] = device.deviceId
            it[deviceModel] = device.model
            it[os] = device.os
            it[fcmToken] = device.fcmToken
            it[status] = device.status
            it[lastSeen] = device.lastSeen
            it[createdAt] = device.createdAt
        } > 0
    }

    suspend fun updateLastLogin(studentId: UUID, timestamp: LocalDateTime): Boolean = exposedTransaction {
        StudentsTable.update({ StudentsTable.id eq studentId }) {
            it[lastLoginAt] = timestamp
            it[updatedAt] = timestamp
        } > 0
    }

    suspend fun findDeviceByDeviceId(deviceId: String): Device? = exposedTransaction {
        DevicesTable
            .selectAll()
            .where { DevicesTable.deviceId eq deviceId }
            .map { it.toDevice() }
            .singleOrNull()
    }

    suspend fun findActiveDeviceByDeviceId(deviceId: String): Device? = exposedTransaction {
        DevicesTable
            .selectAll()
            .where {
                (DevicesTable.deviceId eq deviceId) and
                        (DevicesTable.status eq DeviceStatus.ACTIVE)
            }
            .map { it.toDevice() }
            .singleOrNull()
    }

    suspend fun findDeviceByStudentIdAndDeviceId(studentId: UUID, deviceId: String): Device? = exposedTransaction {
        DevicesTable
            .selectAll()
            .where {
                (DevicesTable.studentId eq studentId) and
                        (DevicesTable.deviceId eq deviceId)
            }
            .map { it.toDevice() }
            .singleOrNull()
    }

    suspend fun updateDeviceLastSeen(deviceId: UUID, lastSeen: LocalDateTime, fcmToken: String?): Boolean = exposedTransaction {
        DevicesTable.update({ DevicesTable.id eq deviceId }) {
            it[DevicesTable.lastSeen] = lastSeen
            it[updatedAt] = lastSeen
            fcmToken?.let { token -> it[DevicesTable.fcmToken] = token }
        } > 0
    }

     suspend fun updateFcmToken(studentId: UUID, fcmToken: String): Boolean = exposedTransaction {
         DevicesTable.update(
             { (DevicesTable.studentId eq studentId ) and
                     ( DevicesTable.status eq DeviceStatus.ACTIVE)
             },
         ) {
             it[DevicesTable.fcmToken] = fcmToken
             it[updatedAt] = LocalDateTime.now()
         } > 0
     }

    suspend fun getAllActiveDevices(): List<Device> = exposedTransaction {
        DevicesTable
            .selectAll()
            .where {
                (DevicesTable.fcmToken.isNotNull()) and
                        (DevicesTable.status eq DeviceStatus.ACTIVE)
            }
            .map { it.toDevice() }
    }

    private fun ResultRow.toDevice(): Device = Device(
        id = this[DevicesTable.id],
        studentId = this[DevicesTable.studentId],
        deviceId = this[DevicesTable.deviceId],
        model = this[DevicesTable.deviceModel],
        os = this[DevicesTable.os],
        fcmToken = this[DevicesTable.fcmToken],
        status = this[DevicesTable.status],
        lastSeen = this[DevicesTable.lastSeen],
        createdAt = this[DevicesTable.createdAt],
        updatedAt = this[DevicesTable.updatedAt]
    )

    // helpers
    private fun ResultRow.toStudent(): Student = Student(
        id = this[StudentsTable.id],
        registrationNumber = this[StudentsTable.registrationNumber],
        fullName = this[StudentsTable.fullName],
        createdAt = this[StudentsTable.createdAt],
        lastLogin = this[StudentsTable.lastLoginAt],
        isActive = this[StudentsTable.isActive],
        device = null,
    )


    /*---------------------
    ADMIN METHODS
     -------------------*/
    suspend fun getAllStudents(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        status: Boolean? = null
    ): Triple<List<StudentResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = StudentsTable
            .selectAll()

        // Apply filters
        if (!search.isNullOrBlank()) {
            query = query.andWhere {
                (StudentsTable.fullName like "%$search%") or
                        (StudentsTable.registrationNumber like "%$search%")
            }
        }

        if (status != null) {
            query = query.andWhere { StudentsTable.isActive eq status }
        }

        // Get total count
        val total = query.count()

        // Get paginated results
        val students = query
            .orderBy(StudentsTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                val studentId = row[StudentsTable.id]

                // Get enrollments for this student
                val enrollments = StudentEnrollmentsTable
                    .innerJoin(ProgrammesTable, { StudentEnrollmentsTable.programmeId }, { ProgrammesTable.id })
                    .innerJoin(UniversitiesTable, { ProgrammesTable.universityId }, { UniversitiesTable.id })
                    .innerJoin(AcademicTermsTable, { StudentEnrollmentsTable.academicTermId }, { AcademicTermsTable.id })
                    .select(
                        ProgrammesTable.name,
                        UniversitiesTable.name,
                        AcademicTermsTable.academicYear,
                        AcademicTermsTable.semester,
                        StudentEnrollmentsTable.yearOfStudy,
                        StudentEnrollmentsTable.enrollmentDate
                    )
                    .where { StudentEnrollmentsTable.studentId eq studentId }
                    .andWhere { StudentEnrollmentsTable.isActive eq true }
                    .map { enrollRow ->
                        EnrollmentInfoAdmin(
                            programmeName = enrollRow[ProgrammesTable.name],
                            universityName = enrollRow[UniversitiesTable.name],
                            academicTerm = "${enrollRow[AcademicTermsTable.academicYear]} - Semester ${enrollRow[AcademicTermsTable.semester]}",
                            yearOfStudy = enrollRow[StudentEnrollmentsTable.yearOfStudy],
                            enrollmentDate = enrollRow[StudentEnrollmentsTable.enrollmentDate].toString()
                        )
                    }

                // Count devices
                val devicesCount = DevicesTable
                    .selectAll()
                    .where { DevicesTable.studentId eq studentId }
                    .andWhere { DevicesTable.status eq DeviceStatus.ACTIVE }
                    .count()
                    .toInt()

                StudentResponse(
                    id = studentId.toString(),
                    registrationNumber = row[StudentsTable.registrationNumber],
                    fullName = row[StudentsTable.fullName],
                    isActive = row[StudentsTable.isActive],
                    lastLoginAt = row[StudentsTable.lastLoginAt]?.toString(),
                    enrollments = enrollments,
                    devices = devicesCount
                )
            }

        Triple(students, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun getStudentById(id: UUID): StudentResponse? = exposedTransaction {
        val row = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq id }
            .singleOrNull()

        row?.let {
            val enrollments = StudentEnrollmentsTable
                .innerJoin(ProgrammesTable, { StudentEnrollmentsTable.programmeId }, { ProgrammesTable.id })
                .innerJoin(UniversitiesTable, { ProgrammesTable.universityId }, { UniversitiesTable.id })
                .innerJoin(AcademicTermsTable, { StudentEnrollmentsTable.academicTermId }, { AcademicTermsTable.id })
                .select(
                    ProgrammesTable.name,
                    UniversitiesTable.name,
                    AcademicTermsTable.academicYear,
                    AcademicTermsTable.semester,
                    StudentEnrollmentsTable.yearOfStudy,
                    StudentEnrollmentsTable.enrollmentDate
                )
                .where { StudentEnrollmentsTable.studentId eq id }
                .andWhere { StudentEnrollmentsTable.isActive eq true }
                .map { enrollRow ->
                    EnrollmentInfoAdmin(
                        programmeName = enrollRow[ProgrammesTable.name],
                        universityName = enrollRow[UniversitiesTable.name],
                        academicTerm = "${enrollRow[AcademicTermsTable.academicYear]} - Semester ${enrollRow[AcademicTermsTable.semester]}",
                        yearOfStudy = enrollRow[StudentEnrollmentsTable.yearOfStudy],
                        enrollmentDate = enrollRow[StudentEnrollmentsTable.enrollmentDate].toString()
                    )
                }

            val devicesCount = DevicesTable
                .selectAll()
                .where { DevicesTable.studentId eq id }
                .andWhere { DevicesTable.status eq DeviceStatus.ACTIVE }
                .count()
                .toInt()

            StudentResponse(
                id = it[StudentsTable.id].toString(),
                registrationNumber = it[StudentsTable.registrationNumber],
                fullName = it[StudentsTable.fullName],
                isActive = it[StudentsTable.isActive],
                lastLoginAt = it[StudentsTable.lastLoginAt]?.toString(),
                enrollments = enrollments,
                devices = devicesCount
            )
        }
    }

    suspend fun updateStudent(
        id: UUID,
        fullName: String? = null,
        isActive: Boolean? = null
    ): Boolean = exposedTransaction {
        val updateCount = StudentsTable.update({ StudentsTable.id eq id }) {
            fullName?.let { fullName -> it[StudentsTable.fullName] = fullName }
            isActive?.let { isActive -> it[StudentsTable.isActive] = isActive }
            it[StudentsTable.updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    suspend fun deleteStudent(id: UUID): Boolean = exposedTransaction {
        // Soft delete - just deactivate
        val updateCount = StudentsTable.update({ StudentsTable.id eq id }) {
            it[StudentsTable.isActive] = false
            it[StudentsTable.updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    suspend fun activateStudent(id: UUID): Boolean = exposedTransaction {
        val updateCount = StudentsTable.update({ StudentsTable.id eq id }) {
            it[StudentsTable.isActive] = true
            it[StudentsTable.updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

}
