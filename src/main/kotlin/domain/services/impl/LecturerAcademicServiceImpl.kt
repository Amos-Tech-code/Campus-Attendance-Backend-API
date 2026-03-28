package domain.services.impl

import api.dtos.response.*
import data.repository.LecturerAcademicRepository
import com.amos_tech_code.domain.dtos.requests.*
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.domain.models.ResolvedProgramme
import com.amos_tech_code.domain.models.ResolvedUniversity
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.utils.*
import domain.dtos.requests.AddAcademicTermRequest
import domain.dtos.requests.AddProgrammeWithUnitsRequest
import domain.dtos.requests.AddUnitToProgrammeRequest
import domain.dtos.requests.UpdateProgrammeDetailsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

class LecturerAcademicServiceImpl(
    private val lecturerAcademicRepository: LecturerAcademicRepository,
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

    // ============ 1. DEACTIVATE UNIVERSITY FOR LECTURER ============

    override suspend fun deactivateUniversityForLecturer(
        lecturerId: UUID,
        universityId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            // Verify lecturer belongs to university
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, universityId)

            // Check for attendance records in current term
            val hasAttendance = lecturerAcademicRepository.hasAttendanceRecordsInCurrentTerm(universityId)
            if (hasAttendance) {
                throw ValidationException(
                    "Cannot deactivate from university - there are attendance records for the current term"
                )
            }

            // Get active term to deactivate teaching assignments
            val activeTerm = lecturerAcademicRepository.getActiveAcademicTerm(universityId)

            // Deactivate all teaching assignments for this university
            lecturerAcademicRepository.deactivateTeachingAssignmentsForUniversity(
                lecturerId = lecturerId,
                universityId = universityId,
                academicTermId = activeTerm.id
            )

            // Deactivate the lecturer-university link
            lecturerAcademicRepository.deactivateUniversityForLecturer(lecturerId, universityId)

            GenericResponseDto(
                statusCode = 200,
                message = "Successfully deactivated from university"
            )
        } catch (e: Exception) {
            logger.error("Failed to deactivate university for lecturer", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to deactivate from university")
            }
        }
    }

    // ============ 2. ADD ACADEMIC TERM ============

    override suspend fun addAcademicTerm(
        lecturerId: UUID,
        universityId: UUID,
        request: AddAcademicTermRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            // Verify lecturer belongs to university
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, universityId)

            // Check if term already exists
            val existing = lecturerAcademicRepository.findAcademicTerm(
                universityId, request.academicYear, request.semester
            )
            if (existing != null) {
                throw ConflictException("Academic term already exists")
            }

            // Create new term
            lecturerAcademicRepository.createAcademicTerm(
                universityId = universityId,
                academicYear = request.academicYear,
                semester = request.semester,
                weekCount = request.weekCount
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Academic term added successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to add academic term", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to add academic term")
            }
        }
    }

    // ============ 3. ADD PROGRAMME WITH UNITS ============

    override suspend fun addProgrammeWithUnits(
        lecturerId: UUID,
        universityId: UUID,
        request: AddProgrammeWithUnitsRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            // Validate at least one unit
            if (request.units.isEmpty()) {
                throw ValidationException("At least one unit is required")
            }

            // Verify lecturer belongs to university
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, universityId)

            // ============ RESOLVE OR CREATE DEPARTMENT ============
            val departmentId = when {
                !request.departmentId.isNullOrBlank() -> {
                    val department = lecturerAcademicRepository.findDepartmentById(UUID.fromString(request.departmentId))
                        ?: throw ResourceNotFoundException("Department not found")

                    if (department.universityId != universityId) {
                        throw ValidationException("Department does not belong to this university")
                    }
                    department.id
                }
                !request.departmentName.isNullOrBlank() -> {
                    val (id, name) = lecturerAcademicRepository.findOrCreateDepartment(
                        universityId = universityId,
                        departmentName = request.departmentName
                    )
                    id
                }
                else -> {
                    throw ValidationException("Either departmentId or departmentName is required")
                }
            }

            // Get active academic term
            val activeTerm = lecturerAcademicRepository.getActiveAcademicTerm(universityId)

            // Create programme (no yearOfStudy or expectedStudentCount in Programme)
            val programme = lecturerAcademicRepository.createProgramme(
                universityId = universityId,
                departmentId = departmentId,
                name = request.name,
                yearOfStudy = 0,  // Not used in Programme table, will be in teaching assignment
                expectedStudentCount = 0  // Not used in Programme table, will be in teaching assignment
            )

            // Process each unit
            request.units.forEach { unitRequest ->
                // Create or find unit
                val unit = lecturerAcademicRepository.findOrCreateUnit(
                    universityId = universityId,
                    departmentId = departmentId,  // Units use same department as programme
                    code = unitRequest.code,
                    name = unitRequest.name
                )

                // Link unit to programme (yearOfStudy stored here)
                lecturerAcademicRepository.linkUnitToProgramme(
                    programmeId = programme.id,
                    unitId = unit.id,
                    yearOfStudy = request.yearOfStudy,
                    semester = unitRequest.semester
                )

                // Create teaching assignment (yearOfStudy and expectedStudents stored here)
                lecturerAcademicRepository.createTeachingAssignment(
                    lecturerId = lecturerId,
                    universityId = universityId,
                    programmeId = programme.id,
                    unitId = unit.id,
                    academicTermId = activeTerm.id,
                    yearOfStudy = request.yearOfStudy,
                    expectedStudents = request.expectedStudentCount,
                    lectureDay = unitRequest.lectureDay,
                    lectureTime = unitRequest.lectureTime,
                    lectureVenue = unitRequest.lectureVenue
                )
            }

            GenericResponseDto(
                statusCode = 200,
                message = "Programme added successfully with ${request.units.size} unit(s)"
            )
        } catch (e: Exception) {
            logger.error("Failed to add programme with units", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to add programme")
            }
        }
    }

    // ============ 4. UPDATE PROGRAMME DETAILS ============

    override suspend fun updateProgrammeDetails(
        lecturerId: UUID,
        programmeId: UUID,
        request: UpdateProgrammeDetailsRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            // Verify lecturer belongs to university
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, programme.universityId)

            // Get active term to update teaching assignments
            val activeTerm = lecturerAcademicRepository.getActiveAcademicTerm(programme.universityId)

            // Update programme name only (yearOfStudy and expectedStudentCount are in teaching assignments)
            if (request.name != null) {
                lecturerAcademicRepository.updateProgramme(
                    programmeId = programmeId,
                    name = request.name,
                    departmentId = null,
                    yearOfStudy = null,
                    expectedStudentCount = null,
                    isActive = request.isActive
                )
            }

            // If yearOfStudy or expectedStudentCount changed, update all teaching assignments for this programme
            if (request.yearOfStudy != null || request.expectedStudentCount != null) {
                lecturerAcademicRepository.updateTeachingAssignmentsForProgramme(
                    lecturerId = lecturerId,
                    programmeId = programmeId,
                    academicTermId = activeTerm.id,
                    yearOfStudy = request.yearOfStudy,
                    expectedStudents = request.expectedStudentCount
                )
            }

            // If just deactivating programme
            if (request.isActive == false) {
                lecturerAcademicRepository.deactivateProgramme(programmeId)
            }

            GenericResponseDto(
                statusCode = 200,
                message = "Programme updated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to update programme", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to update programme")
            }
        }
    }

    // ============ 5. DEACTIVATE PROGRAMME ============
    override suspend fun deactivateProgramme(
        lecturerId: UUID,
        programmeId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            // Verify lecturer belongs to university
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, programme.universityId)

            // Get active term
            val activeTerm = lecturerAcademicRepository.getActiveAcademicTerm(programme.universityId)

            // Check for attendance records in current term
            val hasAttendance = lecturerAcademicRepository.hasAttendanceForProgrammeInCurrentTerm(
                programmeId = programmeId,
                universityId = programme.universityId
            )

            if (hasAttendance) {
                throw ValidationException("Cannot deactivate programme with attendance records")
            }

            // Deactivate teaching assignments for this programme (NOT the programme itself)
            lecturerAcademicRepository.deactivateTeachingAssignmentsForProgramme(
                lecturerId = lecturerId,
                programmeId = programmeId,
                academicTermId = activeTerm.id
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Programme deactivated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to deactivate programme", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to deactivate programme")
            }
        }
    }

    // ============ 6. ADD UNIT TO PROGRAMME ============

    override suspend fun addUnitToProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        request: AddUnitToProgrammeRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            // Verify lecturer belongs to university
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, programme.universityId)

            // Get the teaching assignment to get yearOfStudy and expectedStudents
            val activeTerm = lecturerAcademicRepository.getActiveAcademicTerm(programme.universityId)

            val teachingAssignment = lecturerAcademicRepository.findTeachingAssignment(
                lecturerId = lecturerId,
                programmeId = programmeId,
                academicTermId = activeTerm.id
            ) ?: throw ValidationException("Programme not found in your teaching assignments")

            // Verify department
            val department = lecturerAcademicRepository.findDepartmentById(UUID.fromString(request.departmentId))
                ?: throw ResourceNotFoundException("Department not found")

            if (department.universityId != programme.universityId) {
                throw ValidationException("Department does not belong to this university")
            }

            // Create or find unit
            val unit = lecturerAcademicRepository.findOrCreateUnit(
                universityId = programme.universityId,
                departmentId = UUID.fromString(request.departmentId),
                code = request.code,
                name = request.name
            )

            // Link unit to programme
            lecturerAcademicRepository.linkUnitToProgramme(
                programmeId = programmeId,
                unitId = unit.id,
                yearOfStudy = teachingAssignment.yearOfStudy,
                semester = request.semester
            )

            // Create teaching assignment for the unit
            lecturerAcademicRepository.createTeachingAssignment(
                lecturerId = lecturerId,
                universityId = programme.universityId,
                programmeId = programmeId,
                unitId = unit.id,
                academicTermId = activeTerm.id,
                yearOfStudy = teachingAssignment.yearOfStudy,
                expectedStudents = teachingAssignment.expectedStudents,
                lectureDay = request.lectureDay,
                lectureTime = request.lectureTime,
                lectureVenue = request.lectureVenue
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Unit added successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to add unit to programme", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to add unit to programme")
            }
        }
    }

    // ============ 7. REMOVE UNIT FROM PROGRAMME ============
    override suspend fun removeUnitFromProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        unitId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            // Verify lecturer belongs to university
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, programme.universityId)

            // Check if unit is linked to this programme
            val isLinked = lecturerAcademicRepository.isUnitLinkedToProgramme(unitId, programmeId)
            if (!isLinked) {
                throw ResourceNotFoundException("Unit not linked to this programme")
            }

            // Check for attendance records for this unit in current term
            val hasAttendance = lecturerAcademicRepository.hasAttendanceForUnitInCurrentTerm(
                unitId = unitId,
                universityId = programme.universityId
            )

            if (hasAttendance) {
                throw ValidationException("Cannot remove unit with attendance records")
            }

            // Get active term
            val activeTerm = lecturerAcademicRepository.getActiveAcademicTerm(programme.universityId)

            // Remove the programme-unit link
            lecturerAcademicRepository.removeProgrammeUnitLink(programmeId, unitId)

            // Deactivate teaching assignment for this unit
            lecturerAcademicRepository.deactivateTeachingAssignmentForUnit(
                lecturerId = lecturerId,
                programmeId = programmeId,
                unitId = unitId,
                academicTermId = activeTerm.id
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Unit removed from programme successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to remove unit from programme", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to remove unit from programme")
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


}