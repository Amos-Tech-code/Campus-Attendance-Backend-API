package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.models.Device
import com.amos_tech_code.domain.models.Student
import data.database.entities.DevicesTable
import data.database.entities.StudentsTable
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
                    lastSeen = it[DevicesTable.lastSeen],
                    createdAt = it[DevicesTable.createdAt],
                    updatedAt = it[DevicesTable.updatedAt]
                )
            }
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


    suspend fun updateDevice(studentId: UUID, device: Device): Boolean = exposedTransaction {
        DevicesTable.insert {
            it[id] = device.id
            it[DevicesTable.studentId] = studentId
            it[deviceId] = device.deviceId
            it[deviceModel] = device.model
            it[os] = device.os
            it[fcmToken] = device.fcmToken
            it[lastSeen] = device.lastSeen
            it[createdAt] = device.createdAt
        }
        true
    }

    suspend fun updateLastLogin(studentId: UUID, timestamp: LocalDateTime): Boolean = exposedTransaction {
        StudentsTable.update({ StudentsTable.id eq studentId }) {
            it[lastLoginAt] = timestamp
            it[updatedAt] = timestamp
        } > 0
    }

    suspend fun updateDeviceLastSeen(
        deviceId: String,
        timestamp: LocalDateTime
    ): Boolean = exposedTransaction {
        DevicesTable.update({ DevicesTable.deviceId eq deviceId }) {
            it[lastSeen] = timestamp
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

    suspend fun updateDeviceStatus(deviceId: UUID, status: DeviceStatus): Boolean = exposedTransaction {
        DevicesTable.update({ DevicesTable.id eq deviceId }) {
            it[DevicesTable.status] = status
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun updateDeviceLastSeen(deviceId: UUID, lastSeen: LocalDateTime, fcmToken: String?): Boolean = exposedTransaction {
        DevicesTable.update({ DevicesTable.id eq deviceId }) {
            it[DevicesTable.lastSeen] = lastSeen
            it[updatedAt] = lastSeen
            fcmToken?.let { token -> it[DevicesTable.fcmToken] = token }
        } > 0
    }

    suspend fun getPendingRequests(): List<Device> = exposedTransaction {
        DevicesTable
            .selectAll()
            .where { DevicesTable.status eq DeviceStatus.PENDING }
            .orderBy(DevicesTable.createdAt to SortOrder.DESC)
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
        device = null,
    )
}
