package com.amos_tech_code.services

import com.amos_tech_code.domain.dtos.requests.AcademicSetUpRequest
import com.amos_tech_code.domain.dtos.requests.DepartmentSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.ProgrammeSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UnitSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UniversitySuggestionRequest
import api.dtos.response.AcademicSetupResponse
import api.dtos.response.GenericResponseDto
import com.amos_tech_code.api.dtos.admin.UnitResponse
import com.amos_tech_code.domain.dtos.requests.AddAcademicTermRequest
import com.amos_tech_code.domain.dtos.requests.AddProgrammeRequest
import com.amos_tech_code.domain.dtos.requests.AddTeachingAssignmentRequest
import com.amos_tech_code.domain.dtos.requests.AddUnitToProgrammeRequest
import com.amos_tech_code.domain.dtos.requests.UpdateAcademicTermRequest
import com.amos_tech_code.domain.dtos.requests.UpdateProgrammeRequest
import com.amos_tech_code.domain.dtos.requests.UpdateTeachingAssignmentRequest
import com.amos_tech_code.domain.dtos.requests.UpdateUnitRequest
import com.amos_tech_code.domain.dtos.response.DepartmentSuggestion
import com.amos_tech_code.domain.dtos.response.LecturerAcademicSetupResponse
import com.amos_tech_code.domain.dtos.response.ProgrammeSuggestion
import com.amos_tech_code.domain.dtos.response.UnitSuggestion
import com.amos_tech_code.domain.dtos.response.UniversitySuggestion
import java.util.UUID

interface LecturerAcademicService {

    suspend fun saveAcademicSetup(
        lecturerId: UUID,
        request: AcademicSetUpRequest
    ): AcademicSetupResponse

    suspend fun getLecturerAcademicSetup(
        lecturerId: UUID,
        universityId: String? = null
    ): LecturerAcademicSetupResponse

    // ============ UPDATE METHODS ============
    // University Operations
    suspend fun deactivateUniversityForLecturer(
        lecturerId: UUID,
        universityId: UUID
    ): GenericResponseDto

    suspend fun reactivateUniversityForLecturer(
        lecturerId: UUID,
        universityId: UUID
    ): GenericResponseDto

    // Academic Term Operations
    suspend fun addAcademicTerm(
        lecturerId: UUID,
        universityId: UUID,
        request: AddAcademicTermRequest
    ): GenericResponseDto

    suspend fun updateAcademicTerm(
        lecturerId: UUID,
        termId: UUID,
        request: UpdateAcademicTermRequest
    ): GenericResponseDto

    suspend fun activateTerm(
        lecturerId: UUID,
        termId: UUID
    ): GenericResponseDto

    // Programme Operations
    suspend fun addProgramme(
        lecturerId: UUID,
        universityId: UUID,
        request: AddProgrammeRequest
    ): GenericResponseDto

    suspend fun updateProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        request: UpdateProgrammeRequest
    ): GenericResponseDto

    suspend fun deactivateProgramme(
        lecturerId: UUID,
        programmeId: UUID
    ): GenericResponseDto

    // Unit Operations
    suspend fun addUnitToProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        request: AddUnitToProgrammeRequest
    ): GenericResponseDto

    suspend fun updateUnit(
        lecturerId: UUID,
        unitId: UUID,
        request: UpdateUnitRequest
    ): GenericResponseDto

    suspend fun deactivateUnit(
        lecturerId: UUID,
        unitId: UUID
    ): GenericResponseDto

    // Teaching Assignment Operations
    suspend fun addTeachingAssignment(
        lecturerId: UUID,
        request: AddTeachingAssignmentRequest
    ): GenericResponseDto

    suspend fun updateTeachingAssignment(
        lecturerId: UUID,
        assignmentId: UUID,
        request: UpdateTeachingAssignmentRequest
    ): GenericResponseDto

    suspend fun deleteTeachingAssignment(
        lecturerId: UUID,
        assignmentId: UUID
    ): GenericResponseDto

    // ============ SUGGESTION METHODS ============

    suspend fun suggestUniversities(request: UniversitySuggestionRequest): List<UniversitySuggestion>

    suspend fun suggestDepartments(request: DepartmentSuggestionRequest): List<DepartmentSuggestion>

    suspend fun suggestProgrammes(request: ProgrammeSuggestionRequest): List<ProgrammeSuggestion>

    suspend fun suggestUnits(request: UnitSuggestionRequest): List<UnitSuggestion>

}