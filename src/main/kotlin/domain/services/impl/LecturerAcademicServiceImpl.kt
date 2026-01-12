package domain.services.impl

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.data.repository.LecturerAcademicRepository
import com.amos_tech_code.domain.dtos.requests.AcademicSetUpRequest
import com.amos_tech_code.domain.dtos.requests.DepartmentSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.ProgrammeSetupRequest
import com.amos_tech_code.domain.dtos.requests.ProgrammeSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UnitSetupRequest
import com.amos_tech_code.domain.dtos.requests.UnitSuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UniversitySuggestionRequest
import com.amos_tech_code.domain.dtos.requests.UpdateAcademicSetupRequest
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.domain.models.ResolvedProgramme
import com.amos_tech_code.domain.models.ResolvedUniversity
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ValidationException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

class LecturerAcademicServiceImpl(
    private val lecturerAcademicRepository: LecturerAcademicRepository
) : LecturerAcademicService {

    private val logger = LoggerFactory.getLogger(LecturerAcademicServiceImpl::class.java)

    override suspend fun saveAcademicSetup(
        lecturerId: UUID,
        request: AcademicSetUpRequest
    ): AcademicSetupResponse = newSuspendedTransaction {

        validateAcademicSetupRequest(request)

        try {
            /* -----------------------------
             * 1. Resolve university
             * ----------------------------- */
            val university = if (request.universityId != null) {
                ResolvedUniversity(
                    id = UUID.fromString(request.universityId),
                    name = request.universityName ?: ""
                )
            } else {
                lecturerAcademicRepository.findOrCreateUniversity(
                    request.universityName
                        ?: throw ValidationException("University name is required")
                )
            }

            lecturerAcademicRepository.linkLecturerToUniversity(
                lecturerId = lecturerId,
                universityId = university.id
            )

            /* -----------------------------
             * 2. Resolve academic term
             * ----------------------------- */
            val academicTerm = lecturerAcademicRepository.findOrCreateAcademicTerm(
                universityId = university.id,
                academicYear = request.academicYear,
                semester = request.semester
            )

            val programmeResponses = mutableListOf<LecturerProgrammeResponse>()

            /* -----------------------------
             * 3. Process programmes
             * ----------------------------- */
            request.programmes.forEach { programmeRequest ->

                // 3a. Resolve department
                val (departmentId, departmentName) =
                    if (programmeRequest.departmentId != null) {
                        UUID.fromString(programmeRequest.departmentId) to
                                (programmeRequest.departmentName ?: "")
                    } else {
                        lecturerAcademicRepository.findOrCreateDepartment(
                            university.id,
                            programmeRequest.departmentName
                                ?: throw ValidationException("Department name is required")
                        )
                    }

                // 3b. Resolve programme
                val programme =
                    if (programmeRequest.programmeId != null) {
                        ResolvedProgramme(
                            id = UUID.fromString(programmeRequest.programmeId),
                            name = programmeRequest.programmeName ?: "",
                            departmentId = departmentId,
                            departmentName = departmentName
                        )
                    } else {
                        lecturerAcademicRepository.findOrCreateProgramme(
                            universityId = university.id,
                            departmentId = departmentId,
                            programmeName = programmeRequest.programmeName
                                ?: throw ValidationException("Programme name is required")
                        ).copy(departmentName = departmentName)
                    }

                // 3c. Resolve units
                val resolvedUnits = lecturerAcademicRepository.findOrCreateUnitsBatch(
                    universityId = university.id,
                    departmentId = departmentId,
                    unitRequests = programmeRequest.units
                )

                // 3d. Link programme–units
                lecturerAcademicRepository.linkProgrammeUnitsBatch(
                    programmeId = programme.id,
                    units = resolvedUnits,
                    yearOfStudy = programmeRequest.yearOfStudy
                )

                // 3e. Teaching assignments
                lecturerAcademicRepository.createTeachingAssignmentsBatch(
                    lecturerId = lecturerId,
                    universityId = university.id,
                    programmeId = programme.id,
                    units = resolvedUnits,
                    academicTermId = academicTerm.id,
                    yearOfStudy = programmeRequest.yearOfStudy,
                    expectedStudentsCount = programmeRequest.expectedStudentCount
                )

                /* -----------------------------
                 * 3f. Build programme response
                 * ----------------------------- */
                programmeResponses += LecturerProgrammeResponse(
                    programmeId = programme.id.toString(),
                    programmeName = programme.name,
                    departmentId = departmentId.toString(),
                    departmentName = departmentName,
                    yearOfStudy = programmeRequest.yearOfStudy,
                    expectedStudentCount = programmeRequest.expectedStudentCount,
                    units = resolvedUnits.map { unit ->
                        LecturerUnitResponse(
                            unitId = unit.unitId.toString(),
                            code = unit.code,
                            name = unit.name,
                            semester = unit.semester,
                            lectureDay = unit.lectureDay,
                            lectureTime = unit.lectureTime,
                            lectureVenue = unit.lectureVenue
                        )
                    }
                )
            }

            /* -----------------------------
             * 4. Finalize
             * ----------------------------- */
            lecturerAcademicRepository.markLecturerProfileComplete(lecturerId)

            AcademicSetupResponse(
                university = UniversityResponse(
                    id = university.id.toString(),
                    name = university.name
                ),
                academicTerm = AcademicTermResponse(
                    id = academicTerm.id.toString(),
                    academicYear = academicTerm.academicYear,
                    semester = academicTerm.semester,
                    isActive = academicTerm.isActive
                ),
                programmes = programmeResponses,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

        } catch (ex: Exception) {
            logger.error("Failed to save academic setup: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to save academic setup")
            }
        }
    }

    override suspend fun getLecturerAcademicSetup(
        lecturerId: UUID,
        universityId: String?
    ): LecturerAcademicSetupResponse = newSuspendedTransaction {

        try {
            val universityFilter = universityId?.let(UUID::fromString)

            val universities = lecturerAcademicRepository.getLecturerUniversities(
                lecturerId = lecturerId,
                universityId = universityFilter
            )

            if (universities.isEmpty()) {
                return@newSuspendedTransaction LecturerAcademicSetupResponse(emptyList())
            }

            val universitySetups = universities.map { university ->

                val activeTerm = lecturerAcademicRepository.getActiveAcademicTerm(university.id)

                val teachingAssignments =
                    lecturerAcademicRepository.getLecturerTeachingAssignments(
                        lecturerId = lecturerId,
                        universityId = university.id,
                        academicTermId = activeTerm.id
                    )

                val programmes = teachingAssignments
                    .groupBy {
                        Triple(
                            it.programmeId,
                            it.departmentId,
                            it.yearOfStudy
                        )
                    }
                    .map { (_, assignments) ->

                        val first = assignments.first()

                        ProgrammeAcademicSetup(
                            programme = first.programme,
                            department = first.department,
                            yearOfStudy = first.yearOfStudy,
                            expectedStudentCount = first.expectedStudents,
                            units = assignments.map { assignment ->
                                UnitAcademicSetup(
                                    unitId = assignment.unit.unitId.toString(),
                                    code = assignment.unit.code,
                                    name = assignment.unit.name,
                                    semester = assignment.unit.semester,
                                    lectureDay = assignment.lectureDay,
                                    lectureTime = assignment.lectureTime,
                                    lectureVenue = assignment.lectureVenue
                                )
                            }
                        )
                    }

                UniversityAcademicSetup(
                    university = UniversityResponse(
                        id = university.id.toString(),
                        name = university.name
                    ),
                    academicTerms = listOf(
                        AcademicTermSetup(
                            id = activeTerm.id.toString(),
                            academicYear = activeTerm.academicYear,
                            semester = activeTerm.semester,
                            isActive = activeTerm.isActive
                        )
                    ),
                    programmes = programmes
                )
            }

            LecturerAcademicSetupResponse(universitySetups)

        } catch (ex: Exception) {
            logger.error("Failed to retrieve academic setup: $ex")
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to retrieve academic setup.")
            }
        }
    }

    override suspend fun updateLecturerAcademicSetup(
        lecturerId: UUID,
        request: UpdateAcademicSetupRequest
    ): AcademicSetupResponse {

        try {

            request.validateRequest()

            val universityId = UUID.fromString(request.universityId)

            return exposedTransaction {

                // 1. Validate lecturer ↔ university relationship
                lecturerAcademicRepository.assertLecturerBelongsToUniversity(
                    lecturerId = lecturerId,
                    universityId = universityId
                )

                // 2. Resolve / create academic terms
                val academicTermMap = lecturerAcademicRepository.resolveAcademicTerms(
                    universityId = universityId,
                    terms = request.academicTerms
                )

                // 3. Resolve / create departments
                val departmentMap = lecturerAcademicRepository.resolveDepartments(
                    universityId = universityId,
                    programmes = request.programmes
                )

                // 4. Resolve / create programmes
                val programmeMap = lecturerAcademicRepository.resolveProgrammes(
                    universityId = universityId,
                    programmes = request.programmes,
                    departmentMap = departmentMap
                )

                // 5. Resolve / create units
                val unitMap = lecturerAcademicRepository.resolveUnits(
                    universityId = universityId,
                    programmes = request.programmes,
                    departmentMap = departmentMap
                )

                // 6. Resolve programme ↔ unit links
                lecturerAcademicRepository.resolveProgrammeUnits(
                    programmes = request.programmes,
                    programmeMap = programmeMap,
                    unitMap = unitMap
                )

                // 7. Resolve lecturer teaching assignments (TERM-SCOPED)
                lecturerAcademicRepository.resolveLecturerAssignments(
                    lecturerId = lecturerId,
                    universityId = universityId,
                    programmes = request.programmes,
                    programmeMap = programmeMap,
                    unitMap = unitMap,
                    academicTermMap = academicTermMap
                )

                // 8. Deactivate removed assignments safely
                lecturerAcademicRepository.deactivateMissingAssignments(
                    lecturerId = lecturerId,
                    universityId = universityId,
                    request = request
                )

                // 9. Return UPDATED snapshot (single university)
                lecturerAcademicRepository.getAcademicSetupForUniversity(
                    lecturerId = lecturerId,
                    universityId = universityId
                )
            }

        } catch (ex: Exception) {
            logger.error("Failed to update academic setup: $ex")
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Failed to update academic setup.")
            }
        }
    }

    override suspend fun suggestUniversities(request: UniversitySuggestionRequest): List<UniversitySuggestion> {
        try {
            return lecturerAcademicRepository.searchUniversities(request.query, request.limit)
        } catch (e: Exception) {
            throw InternalServerException()
        }
    }

    override suspend fun suggestDepartments(request: DepartmentSuggestionRequest): List<DepartmentSuggestion> {
        try {
            val universityId = UUID.fromString(request.universityId)
            return lecturerAcademicRepository.searchDepartments(universityId, request.query, request.limit)
        } catch (e: Exception) {
            throw InternalServerException()
        }
    }

    override suspend fun suggestProgrammes(request: ProgrammeSuggestionRequest): List<ProgrammeSuggestion> {
        try {
            val universityId = UUID.fromString(request.universityId)
            val departmentId = request.departmentId?.let { UUID.fromString(it) }
            return lecturerAcademicRepository.searchProgrammes(universityId, departmentId, request.query, request.limit)
        } catch (e: Exception) {
            throw InternalServerException()
        }
    }

    override suspend fun suggestUnits(request: UnitSuggestionRequest): List<UnitSuggestion> {
        try {
            val universityId = UUID.fromString(request.universityId)
            val departmentId = request.departmentId?.let { UUID.fromString(it) }
            val programmeId = request.programmeId?.let { UUID.fromString(it) }
            return lecturerAcademicRepository.searchUnits(universityId, departmentId, programmeId, request.query, request.limit)
        } catch (e: Exception) {
            throw InternalServerException()
        }
    }

    private fun validateAcademicSetupRequest(request: AcademicSetUpRequest) {

        if (request.academicYear.isBlank()) {
            throw ValidationException("Academic year is required")
        }
        if (request.semester !in listOf(1, 2, 3)) {
            throw ValidationException("Semester must be 1, 2 or 3")
        }
        if (request.programmes.isEmpty()) {
            throw ValidationException("At least one programme is required")
        }
        request.programmes.forEachIndexed { index, programmeRequest -> validateProgrammeRequest(programmeRequest, index) }
    }

    private fun validateProgrammeRequest(programmeRequest: ProgrammeSetupRequest, index: Int) {
        if (programmeRequest.programmeId == null && programmeRequest.programmeName.isNullOrBlank()) {
            throw ValidationException("Programme name is required for programme at index $index")
        }
        if (programmeRequest.departmentId == null && programmeRequest.departmentName.isNullOrBlank()) {
            throw ValidationException("Department name is required for programme '${programmeRequest.programmeName}'")
        }
        if (programmeRequest.yearOfStudy <= 0) {
            throw ValidationException("Year of study must be positive for programme '${programmeRequest.programmeName}'")
        }
        if (programmeRequest.units.isEmpty()) {
            throw ValidationException("At least one unit is required for programme '${programmeRequest.programmeName}'")
        }

        // Validate all units in this programme
        programmeRequest.units.forEachIndexed { _, unitRequest ->
            validateUnitRequest(unitRequest)
        }
    }

    private fun validateUnitRequest(unitRequest: UnitSetupRequest) {
        if (unitRequest.code.isBlank()) {
            throw ValidationException("Missing unit code is required")
        }
        if (unitRequest.name.isBlank()) {
            throw ValidationException("Unit name is required for ${unitRequest.code}")
        }
        if (unitRequest.semester !in listOf(1, 2)) {
            throw ValidationException("Invalid semester for unit ${unitRequest.code}")
        }
    }

    private fun UpdateAcademicSetupRequest.validateRequest() {

            academicTerms.forEach {
                require((it.academicTermId != null) xor (it.draft != null)) {
                   throw ValidationException("Academic term must have either id or have a new draft")
                }
            }

            programmes.forEach { programme ->
                require((programme.programmeId != null) xor (programme.draft != null)) {
                    throw ValidationException("Programme must have either id or have a new draft")
                }

                require(programme.yearOfStudy in 1..8) {
                    throw ValidationException("Year of study must be between 1 and 8")
                }

                programme.units.forEach { unit ->
                    require((unit.unitId != null) xor (unit.draft != null)) {
                        throw ValidationException("Unit must have either id or have a new draft")
                    }

                    require(unit.yearOfStudy == programme.yearOfStudy) {
                        throw ValidationException("Year of study for unit and programme must match")
                    }
                }
            }
    }

}