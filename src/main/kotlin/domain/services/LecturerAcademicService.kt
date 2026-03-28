package com.amos_tech_code.services

import api.dtos.response.AcademicSetupResponse
import api.dtos.response.GenericResponseDto
import com.amos_tech_code.domain.dtos.requests.*
import com.amos_tech_code.domain.dtos.response.*
import domain.dtos.requests.AddAcademicTermRequest
import domain.dtos.requests.AddProgrammeWithUnitsRequest
import domain.dtos.requests.AddUnitToProgrammeRequest
import domain.dtos.requests.UpdateProgrammeDetailsRequest
import java.util.*

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
    suspend fun deactivateUniversityForLecturer(
        lecturerId: UUID,
        universityId: UUID
    ): GenericResponseDto

    suspend fun addAcademicTerm(
        lecturerId: UUID,
        universityId: UUID,
        request: AddAcademicTermRequest
    ): GenericResponseDto

    suspend fun addProgrammeWithUnits(
        lecturerId: UUID,
        universityId: UUID,
        request: AddProgrammeWithUnitsRequest
    ): GenericResponseDto

    suspend fun updateProgrammeDetails(
        lecturerId: UUID,
        programmeId: UUID,
        request: UpdateProgrammeDetailsRequest
    ): GenericResponseDto

    suspend fun deactivateProgramme(
        lecturerId: UUID,
        programmeId: UUID
    ): GenericResponseDto

    suspend fun addUnitToProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        request: AddUnitToProgrammeRequest
    ): GenericResponseDto

    suspend fun removeUnitFromProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        unitId: UUID
    ): GenericResponseDto

    // ============ SUGGESTION METHODS ============

    suspend fun suggestUniversities(request: UniversitySuggestionRequest): List<UniversitySuggestion>

    suspend fun suggestDepartments(request: DepartmentSuggestionRequest): List<DepartmentSuggestion>

    suspend fun suggestProgrammes(request: ProgrammeSuggestionRequest): List<ProgrammeSuggestion>

    suspend fun suggestUnits(request: UnitSuggestionRequest): List<UnitSuggestion>

}