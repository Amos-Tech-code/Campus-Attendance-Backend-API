package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.*
import com.amos_tech_code.data.repository.LecturerAcademicRepository
import com.amos_tech_code.data.repository.UniversityStructureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.toIsoString
import utils.toUUID
import java.time.LocalDateTime
import java.util.*

class UniversityStructureService(
    private val repository: UniversityStructureRepository,
    private val lecturerAcademicRepository: LecturerAcademicRepository
) {

    // Universities
    suspend fun getAllUniversities(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): UniversityListResponse = withContext(Dispatchers.IO) {
        val (universities, total, totalPages) = repository.getAllUniversities(page, pageSize, search)
        UniversityListResponse(universities, total, page, pageSize)
    }

    suspend fun getUniversityById(id: UUID): UniversityResponse? = withContext(Dispatchers.IO) {
        repository.getUniversityById(id)
    }

    suspend fun createUniversity(request: CreateUniversityRequest): UniversityResponse? = withContext(Dispatchers.IO) {
        //repository.createUniversity(request.name)
        val resolvedUniversity = lecturerAcademicRepository.findOrCreateUniversity(request.name)
        UniversityResponse(
            id = resolvedUniversity.id.toString(),
            name = resolvedUniversity.name,
            departmentCount = 0,
            programmeCount = 0,
            unitCount = 0,
            createdAt = LocalDateTime.now().toIsoString(),
        )
    }

    suspend fun updateUniversity(id: UUID, request: UpdateUniversityRequest): Boolean = withContext(Dispatchers.IO) {
        repository.updateUniversity(id, request.name)
    }

    suspend fun deleteUniversity(id: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.deleteUniversity(id)
    }

    // Departments
    suspend fun getAllDepartments(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        search: String? = null
    ): DepartmentListResponse = withContext(Dispatchers.IO) {
        val (departments, total, totalPages) = repository.getAllDepartments(page, pageSize, universityId, search)
        DepartmentListResponse(departments, total, page, pageSize)
    }

    suspend fun createDepartment(request: CreateDepartmentRequest): DepartmentResponse? = withContext(Dispatchers.IO) {
        repository.createDepartment(request.universityId.toUUID(), request.name)
    }

    suspend fun updateDepartment(id: UUID, request: UpdateDepartmentRequest): Boolean = withContext(Dispatchers.IO) {
        repository.updateDepartment(id, request.name)
    }

    suspend fun deleteDepartment(id: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.deleteDepartment(id)
    }

    // Programmes
    suspend fun getAllProgrammes(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        departmentId: UUID? = null,
        search: String? = null,
        activeOnly: Boolean = false
    ): ProgrammeListResponse = withContext(Dispatchers.IO) {
        val (programmes, total, totalPages) = repository.getAllProgrammes(page, pageSize, universityId, departmentId, search, activeOnly)
        ProgrammeListResponse(programmes, total, page, pageSize)
    }

    suspend fun createProgramme(request: CreateProgrammeRequest): ProgrammeResponse? = withContext(Dispatchers.IO) {
        repository.createProgramme(request.universityId.toUUID(), request.departmentId.toUUID(), request.name, request.isActive)
    }

    suspend fun updateProgramme(id: UUID, request: UpdateProgrammeRequest): Boolean = withContext(Dispatchers.IO) {
        repository.updateProgramme(id, request.name, request.departmentId?.toUUID(), request.isActive)
    }

    suspend fun deleteProgramme(id: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.deleteProgramme(id)
    }

    // Units
    suspend fun getAllUnits(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        departmentId: UUID? = null,
        search: String? = null,
        activeOnly: Boolean = false
    ): UnitListResponse = withContext(Dispatchers.IO) {
        val (units, total, totalPages) = repository.getAllUnits(page, pageSize, universityId, departmentId, search, activeOnly)
        UnitListResponse(units, total, page, pageSize)
    }

    suspend fun createUnit(request: CreateUnitRequest): UnitResponse? = withContext(Dispatchers.IO) {
        repository.createUnit(request.universityId.toUUID(), request.departmentId.toUUID(), request.code, request.name, request.isActive)
    }

    suspend fun updateUnit(id: UUID, request: UpdateUnitRequest): Boolean = withContext(Dispatchers.IO) {
        repository.updateUnit(id, request.code, request.name, request.departmentId?.toUUID(), request.isActive)
    }

    suspend fun deleteUnit(id: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.deleteUnit(id)
    }

    suspend fun linkUnitToProgramme(unitId: UUID, request: LinkUnitToProgrammeRequest): Boolean = withContext(Dispatchers.IO) {
        repository.linkUnitToProgramme(unitId, request.programmeId.toUUID(), request.yearOfStudy, request.semester)
    }

    suspend fun unlinkUnitFromProgramme(unitId: UUID, programmeId: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.unlinkUnitFromProgramme(unitId, programmeId)
    }



    // ========== ACADEMIC TERMS ==========

    suspend fun getAllAcademicTerms(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        search: String? = null,
        activeOnly: Boolean = false
    ): AcademicTermListResponse = withContext(Dispatchers.IO) {
        val (terms, total, totalPages) = repository.getAllAcademicTerms(
            page = page,
            pageSize = pageSize,
            universityId = universityId,
            search = search,
            activeOnly = activeOnly
        )
        AcademicTermListResponse(
            terms = terms,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    suspend fun getAcademicTermById(id: UUID): AcademicTermResponse? = withContext(Dispatchers.IO) {
        repository.getAcademicTermById(id)
    }

    suspend fun createAcademicTerm(request: CreateAcademicTermRequest): AcademicTermResponse? = withContext(Dispatchers.IO) {
        val result = repository.createAcademicTerm(
            universityId = request.universityId.toUUID(),
            academicYear = request.academicYear,
            semester = request.semester,
            weekCount = request.weekCount,
            isActive = request.isActive
        )

        if (result == null) throw Exception("Academic term creation failed.")

        repository.getAcademicTermById(result)
    }

    suspend fun updateAcademicTerm(id: UUID, request: UpdateAcademicTermRequest): Boolean = withContext(Dispatchers.IO) {
        repository.updateAcademicTerm(
            id = id,
            academicYear = request.academicYear,
            semester = request.semester,
            weekCount = request.weekCount,
            isActive = request.isActive
        )
    }

    suspend fun deleteAcademicTerm(id: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.deleteAcademicTerm(id)
    }

    suspend fun setActiveAcademicTerm(universityId: UUID, termId: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.setActiveAcademicTerm(universityId, termId)
    }

    suspend fun getActiveAcademicTerm(universityId: UUID): AcademicTermResponse? = withContext(Dispatchers.IO) {
        repository.getActiveAcademicTerm(universityId)
    }
}