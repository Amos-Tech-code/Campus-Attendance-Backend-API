package domain.services.impl

import api.dtos.response.*
import com.amos_tech_code.data.repository.LecturerAcademicRepository
import com.amos_tech_code.domain.dtos.requests.*
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.domain.models.ResolvedProgramme
import com.amos_tech_code.domain.models.ResolvedUniversity
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.utils.*
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


    /**
     * Deactivate university for lecturer (soft delete)
     * Cannot deactivate if there are attendance records in the current term
     */
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
                    "Cannot deactivate university - there are attendance records for the current academic term. " +
                            "You can only deactivate after the term ends."
                )
            }

            // Deactivate the university for this lecturer
            val deactivated = lecturerAcademicRepository.deactivateUniversityForLecturer(lecturerId, universityId)

            if (!deactivated) {
                throw InternalServerException("Failed to deactivate university")
            }

            GenericResponseDto(
                statusCode = 200,
                message = "University deactivated successfully. You can reactivate it in the next term."
            )
        } catch (e: Exception) {
            logger.error("Failed to deactivate university", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to deactivate university")
            }
        }
    }

    /**
     * Reactivate university for lecturer
     */
    override suspend fun reactivateUniversityForLecturer(
        lecturerId: UUID,
        universityId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            // Verify university exists and lecturer is associated
            val university = lecturerAcademicRepository.findUniversityById(universityId)
                ?: throw ResourceNotFoundException("University not found")

            // Check if there's an active term for this university
            val activeTerm = lecturerAcademicRepository.findActiveAcademicTerm(universityId)
            if (activeTerm == null) {
                throw ValidationException(
                    "Cannot reactivate university - no active academic term found. " +
                            "Please set an active term first."
                )
            }

            // Reactivate the university for this lecturer
            val reactivated = lecturerAcademicRepository.reactivateUniversityForLecturer(lecturerId, universityId)

            if (!reactivated) {
                throw InternalServerException("Failed to reactivate university")
            }

            GenericResponseDto(
                statusCode = 200,
                message = "University reactivated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to reactivate university", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to reactivate university")
            }
        }
    }

    override suspend fun addAcademicTerm(
        lecturerId: UUID,
        universityId: UUID,
        request: AddAcademicTermRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, universityId)

            val existingTerm = lecturerAcademicRepository.findAcademicTerm(
                universityId = universityId,
                academicYear = request.academicYear,
                semester = request.semester
            )

            if (existingTerm != null) {
                throw ConflictException("Academic term already exists")
            }

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

    override suspend fun updateAcademicTerm(
        lecturerId: UUID,
        termId: UUID,
        request: UpdateAcademicTermRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val term = lecturerAcademicRepository.findAcademicTermById(termId)
                ?: throw ResourceNotFoundException("Academic term not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, term.universityId)

            lecturerAcademicRepository.updateAcademicTerm(
                termId = termId,
                academicYear = request.academicYear,
                semester = request.semester,
                weekCount = request.weekCount
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Academic term updated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to update academic term", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to update academic term")
            }
        }
    }

    override suspend fun activateTerm(
        lecturerId: UUID,
        termId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val term = lecturerAcademicRepository.findAcademicTermById(termId)
                ?: throw ResourceNotFoundException("Academic term not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, term.universityId)

            // Deactivate all other terms for this university
            lecturerAcademicRepository.deactivateAllTerms(term.universityId)

            // Activate this term
            lecturerAcademicRepository.activateTerm(termId)

            GenericResponseDto(
                statusCode = 200,
                message = "Term activated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to activate term", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to activate term")
            }
        }
    }

    override suspend fun addProgramme(
        lecturerId: UUID,
        universityId: UUID,
        request: AddProgrammeRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, universityId)

            val department = lecturerAcademicRepository.findDepartmentById(UUID.fromString(request.departmentId))
                ?: throw ResourceNotFoundException("Department not found")

            if (department.universityId != universityId) {
                throw ValidationException("Department does not belong to this university")
            }

            lecturerAcademicRepository.createProgramme(
                universityId = universityId,
                departmentId = UUID.fromString(request.departmentId),
                name = request.name,
                yearOfStudy = request.yearOfStudy,
                expectedStudentCount = request.expectedStudentCount
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Programme added successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to add programme", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to add programme")
            }
        }
    }

    override suspend fun updateProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        request: UpdateProgrammeRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, programme.universityId)

            lecturerAcademicRepository.updateProgramme(
                programmeId = programmeId,
                name = request.name,
                departmentId = request.departmentId?.let(UUID::fromString),
                yearOfStudy = request.yearOfStudy,
                expectedStudentCount = request.expectedStudentCount,
                isActive = request.isActive
            )

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

    override suspend fun deactivateProgramme(
        lecturerId: UUID,
        programmeId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, programme.universityId)

            // Check for attendance records
            val hasAttendance = lecturerAcademicRepository.hasAttendanceForProgrammeInCurrentTerm(programmeId, programme.universityId)
            if (hasAttendance) {
                throw ValidationException("Cannot deactivate programme with existing attendance records")
            }

            lecturerAcademicRepository.deactivateProgramme(programmeId)

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

    override suspend fun addUnitToProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        request: AddUnitToProgrammeRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, programme.universityId)

            val department = lecturerAcademicRepository.findDepartmentById(UUID.fromString(request.departmentId))
                ?: throw ResourceNotFoundException("Department not found")

            if (department.universityId != programme.universityId) {
                throw ValidationException("Department does not belong to this university")
            }

            val unit = lecturerAcademicRepository.findOrCreateUnit(
                universityId = programme.universityId,
                departmentId = UUID.fromString(request.departmentId),
                code = request.code,
                name = request.name
            )

            lecturerAcademicRepository.linkUnitToProgramme(
                programmeId = programmeId,
                unitId = unit.id,
                yearOfStudy = request.yearOfStudy,
                semester = request.semester
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

    override suspend fun updateUnit(
        lecturerId: UUID,
        unitId: UUID,
        request: UpdateUnitRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val unit = lecturerAcademicRepository.findUnitById(unitId)
                ?: throw ResourceNotFoundException("Unit not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, unit.universityId)

            lecturerAcademicRepository.updateUnit(
                unitId = unitId,
                code = request.code,
                name = request.name,
                departmentId = request.departmentId?.let(UUID::fromString),
                isActive = request.isActive
            )

            if (request.semester != null) {
                lecturerAcademicRepository.updateUnitSemester(unitId, request.semester)
            }

            GenericResponseDto(
                statusCode = 200,
                message = "Unit updated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to update unit", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to update unit")
            }
        }
    }

    override suspend fun deactivateUnit(
        lecturerId: UUID,
        unitId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val unit = lecturerAcademicRepository.findUnitById(unitId)
                ?: throw ResourceNotFoundException("Unit not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, unit.universityId)

            // Check for attendance records
            val hasAttendance = lecturerAcademicRepository.hasAttendanceForUnitInCurrentTerm(unitId, unit.universityId)
            if (hasAttendance) {
                throw ValidationException("Cannot deactivate unit with existing attendance records")
            }

            lecturerAcademicRepository.deactivateUnit(unitId)

            GenericResponseDto(
                statusCode = 200,
                message = "Unit deactivated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to deactivate unit", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to deactivate unit")
            }
        }
    }

    override suspend fun addTeachingAssignment(
        lecturerId: UUID,
        request: AddTeachingAssignmentRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val universityId = UUID.fromString(request.universityId)
            val programmeId = UUID.fromString(request.programmeId)
            val unitId = UUID.fromString(request.unitId)
            val academicTermId = UUID.fromString(request.academicTermId)

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, universityId)

            val programme = lecturerAcademicRepository.findProgrammeById(programmeId)
                ?: throw ResourceNotFoundException("Programme not found")

            if (programme.universityId != universityId) {
                throw ValidationException("Programme does not belong to this university")
            }

            val unit = lecturerAcademicRepository.findUnitById(unitId)
                ?: throw ResourceNotFoundException("Unit not found")

            if (unit.universityId != universityId) {
                throw ValidationException("Unit does not belong to this university")
            }

            val isLinked = lecturerAcademicRepository.isUnitLinkedToProgramme(unitId, programmeId)
            if (!isLinked) {
                throw ValidationException("Unit is not linked to this programme")
            }

            val term = lecturerAcademicRepository.findAcademicTermById(academicTermId)
                ?: throw ResourceNotFoundException("Academic term not found")

            if (term.universityId != universityId) {
                throw ValidationException("Academic term does not belong to this university")
            }

            lecturerAcademicRepository.createTeachingAssignment(
                lecturerId = lecturerId,
                universityId = universityId,
                programmeId = programmeId,
                unitId = unitId,
                academicTermId = academicTermId,
                yearOfStudy = request.yearOfStudy,
                expectedStudents = request.expectedStudents,
                lectureDay = request.lectureDay,
                lectureTime = request.lectureTime,
                lectureVenue = request.lectureVenue
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Teaching assignment added successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to add teaching assignment", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to add teaching assignment")
            }
        }
    }

    override suspend fun updateTeachingAssignment(
        lecturerId: UUID,
        assignmentId: UUID,
        request: UpdateTeachingAssignmentRequest
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val assignment = lecturerAcademicRepository.findTeachingAssignmentById(assignmentId)
                ?: throw ResourceNotFoundException("Teaching assignment not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, assignment.universityId)

            if (request.isActive == false) {
                val hasAttendance = lecturerAcademicRepository.hasAttendanceRecordsForTeachingAssignment(assignmentId)
                if (hasAttendance) {
                    throw ValidationException(
                        "Cannot deactivate teaching assignment - there are attendance records associated with this assignment."
                    )
                }
            }

            lecturerAcademicRepository.updateTeachingAssignment(
                assignmentId = assignmentId,
                expectedStudents = request.expectedStudents,
                lectureDay = request.lectureDay,
                lectureTime = request.lectureTime,
                lectureVenue = request.lectureVenue,
                isActive = request.isActive
            )

            GenericResponseDto(
                statusCode = 200,
                message = "Teaching assignment updated successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to update teaching assignment", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to update teaching assignment")
            }
        }
    }

    override suspend fun deleteTeachingAssignment(
        lecturerId: UUID,
        assignmentId: UUID
    ): GenericResponseDto = withContext(Dispatchers.IO) {
        try {
            val assignment = lecturerAcademicRepository.findTeachingAssignmentById(assignmentId)
                ?: throw ResourceNotFoundException("Teaching assignment not found")

            lecturerAcademicRepository.assertLecturerBelongsToUniversity(lecturerId, assignment.universityId)

            val hasAttendance = lecturerAcademicRepository.hasAttendanceRecordsForTeachingAssignment(assignmentId)

            if (hasAttendance) {
                throw ValidationException(
                    "Cannot delete teaching assignment - there are attendance records associated with this assignment."
                )
            }

            lecturerAcademicRepository.deleteTeachingAssignment(assignmentId)

            GenericResponseDto(
                statusCode = 200,
                message = "Teaching assignment deleted successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to delete teaching assignment", e)
            when (e) {
                is AppException -> throw e
                else -> throw InternalServerException("Failed to delete teaching assignment")
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