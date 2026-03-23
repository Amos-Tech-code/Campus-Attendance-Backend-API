package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.data.repository.LecturerRepository
import com.amos_tech_code.api.dtos.admin.LecturerListResponse
import com.amos_tech_code.api.dtos.admin.LecturerResponse
import com.amos_tech_code.api.dtos.admin.StudentListResponse
import com.amos_tech_code.api.dtos.admin.StudentResponse
import com.amos_tech_code.api.dtos.admin.UpdateLecturerRequest
import com.amos_tech_code.api.dtos.admin.UpdateStudentRequest
import data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class LecturerStudentManagementService(
    private val lecturerRepository: LecturerRepository,
    private val studentRepository: StudentRepository,
) {

    suspend fun getAllLecturers(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        status: Boolean? = null
    ): LecturerListResponse = withContext(Dispatchers.IO) {
        val (lecturers, total, totalPages) = lecturerRepository.getAllLecturers(
            page = page,
            pageSize = pageSize,
            search = search,
            status = status
        )

        LecturerListResponse(
            lecturers = lecturers,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    suspend fun getLecturerById(id: UUID): LecturerResponse? = withContext(Dispatchers.IO) {
        lecturerRepository.getLecturerById(id)
    }

    suspend fun updateLecturer(id: UUID, request: UpdateLecturerRequest): Boolean = withContext(Dispatchers.IO) {
        lecturerRepository.updateLecturer(
            id = id,
            fullName = request.fullName,
            isActive = request.isActive
        )
    }

    suspend fun deleteLecturer(id: UUID): Boolean = withContext(Dispatchers.IO) {
        lecturerRepository.deleteLecturer(id)
    }

    suspend fun activateLecturer(id: UUID): Boolean = withContext(Dispatchers.IO) {
        lecturerRepository.activateLecturer(id)
    }


    // STUDENTS

    suspend fun getAllStudents(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        status: Boolean? = null
    ): StudentListResponse = withContext(Dispatchers.IO) {
        val (students, total, totalPages) = studentRepository.getAllStudents(
            page = page,
            pageSize = pageSize,
            search = search,
            status = status
        )

        StudentListResponse(
            students = students,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    suspend fun getStudentById(id: UUID): StudentResponse? = withContext(Dispatchers.IO) {
        studentRepository.getStudentById(id)
    }

    suspend fun updateStudent(id: UUID, request: UpdateStudentRequest): Boolean = withContext(Dispatchers.IO) {
        studentRepository.updateStudent(
            id = id,
            fullName = request.fullName,
            isActive = request.isActive
        )
    }

    suspend fun deleteStudent(id: UUID): Boolean = withContext(Dispatchers.IO) {
        studentRepository.deleteStudent(id)
    }

    suspend fun activateStudent(id: UUID): Boolean = withContext(Dispatchers.IO) {
        studentRepository.activateStudent(id)
    }

}