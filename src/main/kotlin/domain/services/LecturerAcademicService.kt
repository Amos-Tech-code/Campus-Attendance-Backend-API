package com.amos_tech_code.services

import com.amos_tech_code.domain.dtos.requests.AcademicSetUpRequest
import com.amos_tech_code.domain.dtos.requests.DepartmentSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.ProgrammeSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UnitSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UniversitySuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UpdateAcademicSetupRequest
import com.amos_tech_code.domain.dtos.response.AcademicSetupResponse
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

    suspend fun updateLecturerAcademicSetup(
        lecturerId: UUID,
        request: UpdateAcademicSetupRequest
    ): AcademicSetupResponse

    suspend fun suggestUniversities(request: UniversitySuggestionRequest): List<UniversitySuggestion>

    suspend fun suggestDepartments(request: DepartmentSuggestionRequest): List<DepartmentSuggestion>

    suspend fun suggestProgrammes(request: ProgrammeSuggestionRequest): List<ProgrammeSuggestion>

    suspend fun suggestUnits(request: UnitSuggestionRequest): List<UnitSuggestion>

}