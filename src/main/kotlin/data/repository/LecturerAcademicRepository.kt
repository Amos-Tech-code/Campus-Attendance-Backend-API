package data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.dtos.requests.UnitSetupRequest
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.domain.models.*
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ValidationException
import data.database.entities.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class LecturerAcademicRepository {

    /*------------------------
    CREATE ACADEMIC SETUP
    -------------------------*/
    suspend fun findOrCreateUniversity(universityName: String): ResolvedUniversity = exposedTransaction {

        val normalized = normalizeName(universityName)

        UniversitiesTable
            .selectAll()
            .where { UniversitiesTable.name eq normalized }
            .singleOrNull()
            ?.let {
                return@exposedTransaction ResolvedUniversity(
                    id = it[UniversitiesTable.id],
                    name = it[UniversitiesTable.name]
                )
            }

        val id = UUID.randomUUID()
        UniversitiesTable.insert {
            it[this.id] = id
            it[name] = normalized
        }

        ResolvedUniversity(id, normalized)

    }

    suspend fun findOrCreateDepartment(
        universityId: UUID,
        departmentName: String
    ): Pair<UUID, String> = exposedTransaction {

        val normalized = normalizeName(departmentName)

        DepartmentsTable
            .selectAll()
            .where {
                (DepartmentsTable.universityId eq universityId) and
                        (DepartmentsTable.name eq normalized)
            }
            .singleOrNull()
            ?.let {
                return@exposedTransaction it[DepartmentsTable.id] to it[DepartmentsTable.name]
            }

        val id = UUID.randomUUID()
        DepartmentsTable.insert {
            it[this.id] = id
            it[this.universityId] = universityId
            it[name] = normalized
        }

        id to normalized
    }

    suspend fun findOrCreateProgramme(
        universityId: UUID,
        departmentId: UUID,
        programmeName: String
    ): ResolvedProgramme= exposedTransaction {

        val normalized = normalizeName(programmeName)

        ProgrammesTable
            .selectAll()
            .where {
                (ProgrammesTable.universityId eq universityId) and
                        (ProgrammesTable.departmentId eq departmentId) and
                        (ProgrammesTable.name eq normalized)
            }
            .singleOrNull()
            ?.let {
                return@exposedTransaction ResolvedProgramme(
                    id = it[ProgrammesTable.id],
                    name = it[ProgrammesTable.name],
                    departmentId = departmentId,
                    departmentName = "" // filled by service from previous step
                )
            }

        val id = UUID.randomUUID()
        ProgrammesTable.insert {
            it[this.id] = id
            it[this.universityId] = universityId
            it[this.departmentId] = departmentId
            it[name] = normalized
        }

        ResolvedProgramme(id, normalized, departmentId, "")
    }

    // Batch operation for units
    suspend fun findOrCreateUnitsBatch(
        universityId: UUID,
        departmentId: UUID,
        unitRequests: List<UnitSetupRequest>
    ): List<ResolvedUnit> = exposedTransaction {
        if (unitRequests.isEmpty()) return@exposedTransaction emptyList()

        val normalizedUnits = unitRequests.associateBy { request ->
            normalizeCode(request.code)
        }

        // Single query to find all existing units
        val existingUnits = UnitsTable
            .selectAll().where {
                (UnitsTable.universityId eq universityId) and
                (UnitsTable.departmentId eq departmentId) and
                (UnitsTable.code inList normalizedUnits.keys)
            }
            .associateBy { it[UnitsTable.code] }

        val resolvedUnits = mutableListOf<ResolvedUnit>()

        // Separate existing units that need updates and new units to create
        normalizedUnits.forEach { (code, request) ->
            val row = existingUnits[code]

            val unitId = if (row != null) {
                // Update name if needed
                if (normalizeName(row[UnitsTable.name]) != normalizeName(request.name)) {
                    UnitsTable.update({ UnitsTable.id eq row[UnitsTable.id] }) {
                        it[name] = normalizeName(request.name)
                    }
                }
                row[UnitsTable.id]
            } else {
                val newId = UUID.randomUUID()
                UnitsTable.insert {
                    it[id] = newId
                    it[UnitsTable.universityId] = universityId
                    it[UnitsTable.departmentId] = departmentId
                    it[UnitsTable.code] = code
                    it[name] = normalizeName(request.name)
                }
                newId
            }

            resolvedUnits += ResolvedUnit(
                unitId = unitId,
                code = code,
                name = normalizeName(request.name),
                semester = request.semester,
                lectureDay = request.lectureDay,
                lectureTime = request.lectureTime,
                lectureVenue = request.lectureVenue
            )
        }

        resolvedUnits
    }

    suspend fun findOrCreateAcademicTerm(
        universityId: UUID,
        academicYear: String,
        semester: Int
    ): ResolvedAcademicTerm = exposedTransaction {

        AcademicTermsTable
            .selectAll()
            .where {
                (AcademicTermsTable.universityId eq universityId) and
                        (AcademicTermsTable.academicYear eq academicYear) and
                        (AcademicTermsTable.semester eq semester)
            }
            .singleOrNull()
            ?.let {
                return@exposedTransaction ResolvedAcademicTerm(
                    id = it[AcademicTermsTable.id],
                    academicYear = it[AcademicTermsTable.academicYear],
                    semester = it[AcademicTermsTable.semester],
                    isActive = it[AcademicTermsTable.isActive]
                )
            }

        val id = UUID.randomUUID()
        AcademicTermsTable.insert {
            it[this.id] = id
            it[this.universityId] = universityId
            it[this.academicYear] = academicYear
            it[this.semester] = semester
            it[this.isActive] = true
        }

        ResolvedAcademicTerm(id, academicYear, semester, true)
    }

    // Batch operation for programme-unit links
    suspend fun linkProgrammeUnitsBatch(
        programmeId: UUID,
        units: List<ResolvedUnit>,
        yearOfStudy: Int
    ) = exposedTransaction {
        if (units.isEmpty()) return@exposedTransaction

        ProgrammeUnitsTable.batchInsert(units, ignore = true) { unit ->
            this[ProgrammeUnitsTable.programmeId] = programmeId
            this[ProgrammeUnitsTable.unitId] = unit.unitId
            this[ProgrammeUnitsTable.yearOfStudy] = yearOfStudy
            this[ProgrammeUnitsTable.semester] = unit.semester
        }

    }

    // Batch operation for teaching assignments
    suspend fun createTeachingAssignmentsBatch(
        lecturerId: UUID,
        universityId: UUID,
        programmeId: UUID,
        units: List<ResolvedUnit>,
        academicTermId: UUID,
        yearOfStudy: Int,
        expectedStudentsCount: Int
    ) = exposedTransaction {
        if (units.isEmpty()) return@exposedTransaction

        // First, ensure any existing assignments are reactivated
        val unitIds = units.map { it.unitId }

        // Reactivate any existing assignments
        LecturerTeachingAssignmentsTable.update({
            (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                    (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                    (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                    (LecturerTeachingAssignmentsTable.unitId inList unitIds) and
                    (LecturerTeachingAssignmentsTable.yearOfStudy eq yearOfStudy)
        }) {
            it[LecturerTeachingAssignmentsTable.academicTermId] = academicTermId
            it[LecturerTeachingAssignmentsTable.expectedStudents] = expectedStudentsCount
            it[LecturerTeachingAssignmentsTable.isActive] = true
            // Note: We're not updating lecture details here to avoid overwriting
        }

        // Then create new assignments for any missing units
        val existingAssignments = LecturerTeachingAssignmentsTable
            .selectAll().where {
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                        (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                        (LecturerTeachingAssignmentsTable.unitId inList unitIds) and
                        (LecturerTeachingAssignmentsTable.yearOfStudy eq yearOfStudy)
            }
            .map { it[LecturerTeachingAssignmentsTable.unitId] }
            .toSet()

        val assignmentsToCreate = unitIds.filterNot { it in existingAssignments }

        LecturerTeachingAssignmentsTable.batchInsert(assignmentsToCreate) { unitId ->
            val resolved = units.first { it.unitId == unitId }

            this[LecturerTeachingAssignmentsTable.lecturerId] = lecturerId
            this[LecturerTeachingAssignmentsTable.universityId] = universityId
            this[LecturerTeachingAssignmentsTable.programmeId] = programmeId
            this[LecturerTeachingAssignmentsTable.unitId] = unitId
            this[LecturerTeachingAssignmentsTable.academicTermId] = academicTermId
            this[LecturerTeachingAssignmentsTable.yearOfStudy] = yearOfStudy
            this[LecturerTeachingAssignmentsTable.expectedStudents] = expectedStudentsCount
            this[LecturerTeachingAssignmentsTable.lectureDay] = resolved.lectureDay
            this[LecturerTeachingAssignmentsTable.lectureTime] = resolved.lectureTime
            this[LecturerTeachingAssignmentsTable.lectureVenue] = resolved.lectureVenue
            this[LecturerTeachingAssignmentsTable.isActive] = true
        }
    }

    suspend fun linkLecturerToUniversity(lecturerId: UUID, universityId: UUID) = exposedTransaction {
        val existingLink = LecturerUniversitiesTable
            .selectAll().where  {
                (LecturerUniversitiesTable.lecturerId eq lecturerId) and
                        (LecturerUniversitiesTable.universityId eq universityId)
            }
            .singleOrNull()

        if (existingLink == null) {
            LecturerUniversitiesTable.insert {
                it[id] = UUID.randomUUID()
                it[LecturerUniversitiesTable.lecturerId] = lecturerId
                it[LecturerUniversitiesTable.universityId] = universityId
            }
        } else {
            // Reactivate if currently inactive
            if (!existingLink[LecturerUniversitiesTable.isActive]) {
                LecturerUniversitiesTable.update({
                    (LecturerUniversitiesTable.lecturerId eq lecturerId) and
                            (LecturerUniversitiesTable.universityId eq universityId)
                }) {
                    it[LecturerUniversitiesTable.isActive] = true
                }
            }
            // If already active, do nothing (idempotent)
        }
    }

    suspend fun markLecturerProfileComplete(lecturerId: UUID) = exposedTransaction {
        LecturersTable.update({ LecturersTable.id eq lecturerId }) {
            it[isProfileComplete] = true
        }
    }


    /*------------------------
    FETCH ACADEMIC SETUP
    -------------------------*/

    suspend fun getLecturerUniversities(
        lecturerId: UUID,
        universityId: UUID?
    ): List<ResolvedUniversity> = exposedTransaction {

        val query = UniversitiesTable
            .innerJoin(LecturerUniversitiesTable)
            .selectAll()
            .where {
                (LecturerUniversitiesTable.lecturerId eq lecturerId) and
                (LecturerUniversitiesTable.isActive eq true) and
                (universityId?.let { UniversitiesTable.id eq it } ?: Op.TRUE)
            }

        query.map {
            ResolvedUniversity(
                id = it[UniversitiesTable.id],
                name = it[UniversitiesTable.name]
            )
        }
    }

    suspend fun getLecturerTeachingAssignments(
        lecturerId: UUID,
        universityId: UUID,
        academicTermId: UUID
    ): List<ResolvedTeachingAssignment> = exposedTransaction {

        LecturerTeachingAssignmentsTable
            .innerJoin(ProgrammesTable)
            .innerJoin(DepartmentsTable)
            .innerJoin(
                ProgrammeUnitsTable,
                onColumn = { LecturerTeachingAssignmentsTable.unitId },
                otherColumn = { ProgrammeUnitsTable.unitId }
            )
            .innerJoin(
                UnitsTable,
                onColumn = { ProgrammeUnitsTable.unitId },
                otherColumn = { UnitsTable.id }
            )
            .selectAll()
            .where {
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                        (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId) and
                        (LecturerTeachingAssignmentsTable.isActive eq true) and
                        (ProgrammeUnitsTable.programmeId eq LecturerTeachingAssignmentsTable.programmeId) and
                        (ProgrammeUnitsTable.yearOfStudy eq LecturerTeachingAssignmentsTable.yearOfStudy)
            }
            .map { row ->
                ResolvedTeachingAssignment(
                    programmeId = row[ProgrammesTable.id],
                    departmentId = row[DepartmentsTable.id],
                    yearOfStudy = row[LecturerTeachingAssignmentsTable.yearOfStudy],
                    expectedStudents = row[LecturerTeachingAssignmentsTable.expectedStudents],

                    programme = ProgrammeResponse(
                        id = row[ProgrammesTable.id].toString(),
                        name = row[ProgrammesTable.name]
                    ),

                    department = DepartmentResponse(
                        id = row[DepartmentsTable.id].toString(),
                        name = row[DepartmentsTable.name]
                    ),

                    unit = ResolvedUnit(
                        unitId = row[UnitsTable.id],
                        code = row[UnitsTable.code],
                        name = row[UnitsTable.name],
                        semester = row[ProgrammeUnitsTable.semester],
                        lectureDay = row[LecturerTeachingAssignmentsTable.lectureDay],
                        lectureTime = row[LecturerTeachingAssignmentsTable.lectureTime],
                        lectureVenue = row[LecturerTeachingAssignmentsTable.lectureVenue]
                    ),

                    lectureDay = row[LecturerTeachingAssignmentsTable.lectureDay],
                    lectureTime = row[LecturerTeachingAssignmentsTable.lectureTime],
                    lectureVenue = row[LecturerTeachingAssignmentsTable.lectureVenue]
                )
            }

    }

    suspend fun getActiveAcademicTerm(universityId: UUID): ResolvedAcademicTerm = exposedTransaction {
        AcademicTermsTable
            .selectAll()
            .where {
                (AcademicTermsTable.universityId eq universityId) and
                        (AcademicTermsTable.isActive eq true)
            }
            .orderBy(
                AcademicTermsTable.academicYear to SortOrder.DESC,
                AcademicTermsTable.semester to SortOrder.DESC
            )
            .limit(1)
            .map {
                ResolvedAcademicTerm(
                    id = it[AcademicTermsTable.id],
                    academicYear = it[AcademicTermsTable.academicYear],
                    semester = it[AcademicTermsTable.semester],
                    isActive = it[AcademicTermsTable.isActive]
                )
            }
            .single()
    }

    /*------------------------
    UPDATE ACADEMIC SETUP
    -------------------------*/
    suspend fun assertLecturerBelongsToUniversity(
        lecturerId: UUID,
        universityId: UUID
    ) = exposedTransaction {
        val exists = LecturerUniversitiesTable
            .select(LecturerUniversitiesTable.id)
            .where {
                (LecturerUniversitiesTable.lecturerId eq lecturerId) and
                        (LecturerUniversitiesTable.universityId eq universityId)
            }
            .any()

        if (!exists) {
            throw ValidationException("Lecturer not associated with this university")
        }
    }

    data class ProgrammeUnit(
        val id: UUID,
        val programmeId: UUID,
        val unitId: UUID,
        val yearOfStudy: Int,
        val semester: Int
    )
    
    /**
     * Check if unit is linked to programme
     */
    suspend fun isUnitLinkedToProgramme(unitId: UUID, programmeId: UUID): Boolean = exposedTransaction {
        ProgrammeUnitsTable
            .select(ProgrammeUnitsTable.id)
            .where {
                (ProgrammeUnitsTable.unitId eq unitId) and
                        (ProgrammeUnitsTable.programmeId eq programmeId)
            }
            .any()
    }

    /**
     * Remove programme-unit link by programmeId and unitId
     */
    suspend fun removeProgrammeUnitLink(programmeId: UUID, unitId: UUID) = exposedTransaction {
        ProgrammeUnitsTable.deleteWhere {
            (ProgrammeUnitsTable.programmeId eq programmeId) and
                    (ProgrammeUnitsTable.unitId eq unitId)
        }
    }

    // ============ TEACHING ASSIGNMENT METHODS ============

    suspend fun deactivateTeachingAssignmentsForUniversity(
        lecturerId: UUID,
        universityId: UUID,
        academicTermId: UUID
    ) = exposedTransaction {
        LecturerTeachingAssignmentsTable.update({
            (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                    (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                    (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId)
        }) {
            it[isActive] = false
        }
    }

    suspend fun deactivateTeachingAssignmentForUnit(
        lecturerId: UUID,
        programmeId: UUID,
        unitId: UUID,
        academicTermId: UUID
    ) = exposedTransaction {
        LecturerTeachingAssignmentsTable.update({
            (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                    (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                    (LecturerTeachingAssignmentsTable.unitId eq unitId) and
                    (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId)
        }) {
            it[isActive] = false
        }
    }

    suspend fun deactivateUniversityForLecturer(
        lecturerId: UUID,
        universityId: UUID
    ): Boolean = exposedTransaction {
        LecturerUniversitiesTable.update({
            (LecturerUniversitiesTable.lecturerId eq lecturerId) and
                    (LecturerUniversitiesTable.universityId eq universityId)
        }) {
            it[isActive] = false
        } > 0
    }

    /**
     * Check if university has attendance records in the current academic term
     */
    suspend fun hasAttendanceRecordsInCurrentTerm(universityId: UUID): Boolean = exposedTransaction {
        // Get active academic term for this university
        val activeTerm = getActiveAcademicTerm(universityId)

        // Check if there are any attendance records in the current term
        AttendanceRecordsTable
            .innerJoin(AttendanceSessionsTable) {
                AttendanceRecordsTable.sessionId eq AttendanceSessionsTable.id
            }
            .select(AttendanceRecordsTable.id)
            .where {
                (AttendanceSessionsTable.universityId eq universityId) and
                        (AttendanceSessionsTable.academicTermId eq activeTerm.id)
            }
            .limit(1)
            .any()
    }


    /**
     * Check if attendance exists for a programme in the current academic term
     */
    suspend fun hasAttendanceForProgrammeInCurrentTerm(
        programmeId: UUID,
        universityId: UUID
    ): Boolean = exposedTransaction {
        val activeTerm = getActiveAcademicTerm(universityId)

        AttendanceRecordsTable
            .innerJoin(AttendanceSessionsTable) {
                AttendanceRecordsTable.sessionId eq AttendanceSessionsTable.id
            }
            .innerJoin(SessionProgrammesTable) {
                AttendanceSessionsTable.id eq SessionProgrammesTable.sessionId
            }
            .select(AttendanceRecordsTable.id)
            .where {
                (SessionProgrammesTable.programmeId eq programmeId) and
                        (AttendanceSessionsTable.academicTermId eq activeTerm.id)
            }
            .limit(1)
            .any()
    }

    /**
     * Check if attendance exists for a unit in the current academic term
     */
    suspend fun hasAttendanceForUnitInCurrentTerm(
        unitId: UUID,
        universityId: UUID
    ): Boolean = exposedTransaction {
        val activeTerm = getActiveAcademicTerm(universityId)

        AttendanceRecordsTable
            .innerJoin(AttendanceSessionsTable) {
                AttendanceRecordsTable.sessionId eq AttendanceSessionsTable.id
            }
            .select(AttendanceRecordsTable.id)
            .where {
                (AttendanceSessionsTable.unitId eq unitId) and
                        (AttendanceSessionsTable.academicTermId eq activeTerm.id)
            }
            .limit(1)
            .any()
    }

    /**
     * Find academic term by university, year, and semester
     */
    suspend fun findAcademicTerm(
        universityId: UUID,
        academicYear: String,
        semester: Int
    ): ResolvedAcademicTerm? = exposedTransaction {
        AcademicTermsTable
            .selectAll()
            .where {
                (AcademicTermsTable.universityId eq universityId) and
                        (AcademicTermsTable.academicYear eq academicYear) and
                        (AcademicTermsTable.semester eq semester)
            }
            .map { row ->
                ResolvedAcademicTerm(
                    id = row[AcademicTermsTable.id],
                    academicYear = row[AcademicTermsTable.academicYear],
                    semester = row[AcademicTermsTable.semester],
                    isActive = row[AcademicTermsTable.isActive]
                )
            }
            .singleOrNull()
    }

    /**
     * Find teaching assignment by lecturer, programme, and term
     */
    suspend fun findTeachingAssignment(
        lecturerId: UUID,
        programmeId: UUID,
        academicTermId: UUID
    ): TeachingAssignment? = exposedTransaction {
        LecturerTeachingAssignmentsTable
            .selectAll()
            .where {
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                        (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId) and
                        (LecturerTeachingAssignmentsTable.isActive eq true)
            }
            .limit(1)
            .map { row ->
                TeachingAssignment(
                    id = row[LecturerTeachingAssignmentsTable.id],
                    lecturerId = row[LecturerTeachingAssignmentsTable.lecturerId],
                    universityId = row[LecturerTeachingAssignmentsTable.universityId],
                    programmeId = row[LecturerTeachingAssignmentsTable.programmeId],
                    unitId = row[LecturerTeachingAssignmentsTable.unitId],
                    academicTermId = row[LecturerTeachingAssignmentsTable.academicTermId],
                    yearOfStudy = row[LecturerTeachingAssignmentsTable.yearOfStudy],
                    expectedStudents = row[LecturerTeachingAssignmentsTable.expectedStudents],
                    lectureDay = row[LecturerTeachingAssignmentsTable.lectureDay],
                    lectureTime = row[LecturerTeachingAssignmentsTable.lectureTime],
                    lectureVenue = row[LecturerTeachingAssignmentsTable.lectureVenue],
                    isActive = row[LecturerTeachingAssignmentsTable.isActive],
                    createdAt = row[LecturerTeachingAssignmentsTable.createdAt]
                )
            }
            .singleOrNull()
    }

    /**
     * Update teaching assignments for a programme (when yearOfStudy or expectedStudents changes)
     */
    suspend fun updateTeachingAssignmentsForProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        academicTermId: UUID,
        yearOfStudy: Int?,
        expectedStudents: Int?
    ) = exposedTransaction {
        LecturerTeachingAssignmentsTable.update({
            (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                    (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                    (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId)
        }) {
            yearOfStudy?.let { year -> it[this.yearOfStudy] = year }
            expectedStudents?.let { students -> it[this.expectedStudents] = students }
        }
    }

    /**
     * Create academic term
     */
    suspend fun createAcademicTerm(
        universityId: UUID,
        academicYear: String,
        semester: Int,
        weekCount: Int
    ): ResolvedAcademicTerm = exposedTransaction {
        val id = UUID.randomUUID()
        AcademicTermsTable.insert {
            it[this.id] = id
            it[this.universityId] = universityId
            it[this.academicYear] = academicYear
            it[this.semester] = semester
            it[this.weekCount] = weekCount
            it[this.isActive] = true
        }

        ResolvedAcademicTerm(
            id = id,
            academicYear = academicYear,
            semester = semester,
            isActive = true
        )
    }


    /**
     * Find department by ID
     */
    suspend fun findDepartmentById(departmentId: UUID): Department? = exposedTransaction {
        DepartmentsTable
            .selectAll()
            .where { DepartmentsTable.id eq departmentId }
            .map { row ->
                Department(
                    id = row[DepartmentsTable.id],
                    universityId = row[DepartmentsTable.universityId],
                    name = row[DepartmentsTable.name]
                )
            }
            .singleOrNull()
    }

    /**
     * Find programme by ID
     */
    suspend fun findProgrammeById(programmeId: UUID): Programme? = exposedTransaction {
        ProgrammesTable
            .selectAll()
            .where { ProgrammesTable.id eq programmeId }
            .map { row ->
                Programme(
                    id = row[ProgrammesTable.id],
                    universityId = row[ProgrammesTable.universityId],
                    departmentId = row[ProgrammesTable.departmentId],
                    name = row[ProgrammesTable.name],
                    isActive = row[ProgrammesTable.isActive],
                    createdAt = row[ProgrammesTable.createdAt],
                    updatedAt = row[ProgrammesTable.updatedAt]
                )
            }
            .singleOrNull()
    }


    /**
     * Create programme
     */
    suspend fun createProgramme(
        universityId: UUID,
        departmentId: UUID,
        name: String,
        yearOfStudy: Int,
        expectedStudentCount: Int
    ): Programme = exposedTransaction {
        val id = UUID.randomUUID()
        ProgrammesTable.insert {
            it[this.id] = id
            it[this.universityId] = universityId
            it[this.departmentId] = departmentId
            it[this.name] = normalizeName(name)
            it[this.isActive] = true
        }

        Programme(
            id = id,
            universityId = universityId,
            departmentId = departmentId,
            name = name,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update programme
     */
    suspend fun updateProgramme(
        programmeId: UUID,
        name: String?,
        departmentId: UUID?,
        yearOfStudy: Int?,
        expectedStudentCount: Int?,
        isActive: Boolean?
    ): Programme = exposedTransaction {
        ProgrammesTable.update({ ProgrammesTable.id eq programmeId }) {
            name?.let { name -> it[this.name] = normalizeName(name) }
            departmentId?.let { department -> it[this.departmentId] = department }
            isActive?.let { isActive -> it[this.isActive] = isActive }
            it[updatedAt] = LocalDateTime.now()
        }

        findProgrammeById(programmeId) ?: throw InternalServerException("Failed to update programme")
    }

    // Add to LecturerAcademicRepository.kt

    /**
     * Deactivate all teaching assignments for a specific programme
     * This removes the lecturer's teaching responsibilities for this programme
     * The programme entity itself remains active in the master table
     */
    suspend fun deactivateTeachingAssignmentsForProgramme(
        lecturerId: UUID,
        programmeId: UUID,
        academicTermId: UUID
    ) = exposedTransaction {
        LecturerTeachingAssignmentsTable.update({
            (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                    (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                    (LecturerTeachingAssignmentsTable.academicTermId eq academicTermId)
        }) {
            it[isActive] = false
        }
    }
    /**
     * Deactivate programme
     */
    suspend fun deactivateProgramme(programmeId: UUID) = exposedTransaction {
        ProgrammesTable.update({ ProgrammesTable.id eq programmeId }) {
            it[isActive] = false
            it[updatedAt] = LocalDateTime.now()
        }
    }

    /**
     * Find or create unit
     */
    suspend fun findOrCreateUnit(
        universityId: UUID,
        departmentId: UUID,
        code: String,
        name: String
    ): DomainUnit = exposedTransaction {
        val normalizedCode = normalizeCode(code)
        val normalizedName = normalizeName(name)

        val existing = UnitsTable
            .selectAll()
            .where {
                (UnitsTable.universityId eq universityId) and
                        (UnitsTable.code eq normalizedCode)
            }
            .singleOrNull()

        if (existing != null) {
            return@exposedTransaction DomainUnit(
                id = existing[UnitsTable.id],
                universityId = existing[UnitsTable.universityId],
                departmentId = existing[UnitsTable.departmentId],
                code = existing[UnitsTable.code],
                name = existing[UnitsTable.name],
                isActive = existing[UnitsTable.isActive],
                createdAt = existing[UnitsTable.createdAt],
                updatedAt = existing[UnitsTable.updatedAt]
            )
        }

        val id = UUID.randomUUID()
        UnitsTable.insert {
            it[this.id] = id
            it[this.universityId] = universityId
            it[this.departmentId] = departmentId
            it[this.code] = normalizedCode
            it[this.name] = normalizedName
            it[this.isActive] = true
        }

        DomainUnit(
            id = id,
            universityId = universityId,
            departmentId = departmentId,
            code = code,
            name = name,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Link unit to programme
     */
    suspend fun linkUnitToProgramme(
        programmeId: UUID,
        unitId: UUID,
        yearOfStudy: Int,
        semester: Int
    ) = exposedTransaction {
        ProgrammeUnitsTable.upsert(
            keys = arrayOf(
                ProgrammeUnitsTable.programmeId,
                ProgrammeUnitsTable.unitId,
                ProgrammeUnitsTable.yearOfStudy
            )
        ) {
            it[this.programmeId] = programmeId
            it[this.unitId] = unitId
            it[this.yearOfStudy] = yearOfStudy
            it[this.semester] = semester
        }
    }


    /**
     * Find teaching assignment by ID
     */
    suspend fun findTeachingAssignmentById(assignmentId: UUID): TeachingAssignment? = exposedTransaction {
        LecturerTeachingAssignmentsTable
            .selectAll()
            .where { LecturerTeachingAssignmentsTable.id eq assignmentId }
            .map { row ->
                TeachingAssignment(
                    id = row[LecturerTeachingAssignmentsTable.id],
                    lecturerId = row[LecturerTeachingAssignmentsTable.lecturerId],
                    universityId = row[LecturerTeachingAssignmentsTable.universityId],
                    programmeId = row[LecturerTeachingAssignmentsTable.programmeId],
                    unitId = row[LecturerTeachingAssignmentsTable.unitId],
                    academicTermId = row[LecturerTeachingAssignmentsTable.academicTermId],
                    yearOfStudy = row[LecturerTeachingAssignmentsTable.yearOfStudy],
                    expectedStudents = row[LecturerTeachingAssignmentsTable.expectedStudents],
                    lectureDay = row[LecturerTeachingAssignmentsTable.lectureDay],
                    lectureTime = row[LecturerTeachingAssignmentsTable.lectureTime],
                    lectureVenue = row[LecturerTeachingAssignmentsTable.lectureVenue],
                    isActive = row[LecturerTeachingAssignmentsTable.isActive],
                    createdAt = row[LecturerTeachingAssignmentsTable.createdAt]
                )
            }
            .singleOrNull()
    }

    /**
     * Create teaching assignment
     */
    suspend fun createTeachingAssignment(
        lecturerId: UUID,
        universityId: UUID,
        programmeId: UUID,
        unitId: UUID,
        academicTermId: UUID,
        yearOfStudy: Int,
        expectedStudents: Int,
        lectureDay: String?,
        lectureTime: String?,
        lectureVenue: String?
    ) = exposedTransaction {
        val id = UUID.randomUUID()
        LecturerTeachingAssignmentsTable.insert {
            it[this.id] = id
            it[this.lecturerId] = lecturerId
            it[this.universityId] = universityId
            it[this.programmeId] = programmeId
            it[this.unitId] = unitId
            it[this.academicTermId] = academicTermId
            it[this.yearOfStudy] = yearOfStudy
            it[this.expectedStudents] = expectedStudents
            it[this.lectureDay] = lectureDay
            it[this.lectureTime] = lectureTime
            it[this.lectureVenue] = lectureVenue
            it[this.isActive] = true
        }

        //findTeachingAssignmentById(id) ?: throw InternalServerException("Failed to create teaching assignment")
    }

    /*------------------------
      SUGGESTIONS
   -------------------------*/
    suspend fun searchUniversities(query: String, limit: Int = 10): List<UniversitySuggestion> = exposedTransaction {

        if (query.isBlank()) return@exposedTransaction emptyList()

        val normalizedQuery = normalizeSearchQuery(query)

        UniversitiesTable
            .select(
                UniversitiesTable.id,
                UniversitiesTable.name
                )
            .where {
                UniversitiesTable.name.lowerCase() like "%${normalizedQuery.lowercase()}%"
            }
            .limit(limit)
            .map { row ->
                val name = row[UniversitiesTable.name]
                UniversitySuggestion(
                    id = row[UniversitiesTable.id].toString(),
                    name = name,
                    matchType = determineMatchType(normalizeName(name), normalizedQuery)
                )
            }
            .sortedBy { it.matchType } // Exact matches first
    }

    suspend fun searchDepartments(
        universityId: UUID,
        query: String,
        limit: Int = 10
    ): List<DepartmentSuggestion> = exposedTransaction {
        if (query.isBlank()) return@exposedTransaction emptyList()

        val normalizedQuery = normalizeSearchQuery(query)

        DepartmentsTable
            .innerJoin(UniversitiesTable)
            .select(
                DepartmentsTable.id,
                DepartmentsTable.name,
                UniversitiesTable.name
            )
            .where {
                (DepartmentsTable.universityId eq universityId) and
                        (DepartmentsTable.name.lowerCase() like "%${normalizedQuery.lowercase()}%")
            }
            .limit(limit)
            .map { row ->
                DepartmentSuggestion(
                    id = row[DepartmentsTable.id].toString(),
                    name = row[DepartmentsTable.name],
                    universityId = universityId.toString(),
                    universityName = row[UniversitiesTable.name]
                )
            }
    }

    suspend fun searchProgrammes(
        universityId: UUID,
        departmentId: UUID?,
        query: String,
        limit: Int = 10
    ): List<ProgrammeSuggestion> = exposedTransaction {
        if (query.isBlank()) return@exposedTransaction emptyList()

        val normalizedQuery = normalizeSearchQuery(query)

        var selectQuery = ProgrammesTable
            .innerJoin(UniversitiesTable)
            .leftJoin(DepartmentsTable)
            .select(
                ProgrammesTable.id,
                ProgrammesTable.name,
                UniversitiesTable.name,
                DepartmentsTable.id,
                DepartmentsTable.name
            )
            .where {
                (ProgrammesTable.universityId eq universityId) and
                        (ProgrammesTable.name.lowerCase() like "%${normalizedQuery.lowercase()}%")
            }

        departmentId?.let { deptId ->
            selectQuery = selectQuery.andWhere { ProgrammesTable.departmentId eq deptId }
        }

        selectQuery
            .limit(limit)
            .map { row ->
                ProgrammeSuggestion(
                    id = row[ProgrammesTable.id].toString(),
                    name = row[ProgrammesTable.name],
                    universityId = universityId.toString(),
                    universityName = row[UniversitiesTable.name],
                    departmentId = row[DepartmentsTable.id]?.toString(),
                    departmentName = row[DepartmentsTable.name]
                )
            }
    }

    suspend fun searchUnits(
        universityId: UUID,
        departmentId: UUID?,
        programmeId: UUID?,
        query: String,
        limit: Int = 10
    ): List<UnitSuggestion> = exposedTransaction {
        if (query.isBlank()) return@exposedTransaction emptyList()

        val normalizedQuery = normalizeSearchQuery(query)

        // Build query based on whether programmeId is provided
        val queryBuilder = if (programmeId != null) {
            // With programme filter - include semester from ProgrammeUnitsTable
            UnitsTable
                .innerJoin(UniversitiesTable)
                .leftJoin(DepartmentsTable)
                .innerJoin(
                    otherTable = ProgrammeUnitsTable,
                    onColumn = { UnitsTable.id },
                    otherColumn = { ProgrammeUnitsTable.unitId }
                )
                .select(
                    UnitsTable.id,
                    UnitsTable.code,
                    UnitsTable.name,
                    UniversitiesTable.name,
                    DepartmentsTable.id,
                    DepartmentsTable.name,
                    ProgrammeUnitsTable.semester
                )
                .where {
                    (UnitsTable.universityId eq universityId) and
                    (ProgrammeUnitsTable.programmeId eq programmeId) and
                    (  (UnitsTable.code.lowerCase() like "%${normalizedQuery.lowercase()}%") or
                       (UnitsTable.name.lowerCase() like "%${normalizedQuery.lowercase()}%")
                            )
                }
        } else {
            // Without programme filter
            UnitsTable
                .innerJoin(UniversitiesTable)
                .leftJoin(DepartmentsTable)
                .select(
                    UnitsTable.id,
                    UnitsTable.code,
                    UnitsTable.name,
                    UniversitiesTable.name,
                    DepartmentsTable.id,
                    DepartmentsTable.name
                )
                .where {
                    (UnitsTable.universityId eq universityId) and
                            (
                                    (UnitsTable.code.lowerCase() like "%${normalizedQuery.lowercase()}%") or
                                            (UnitsTable.name.lowerCase() like "%${normalizedQuery.lowercase()}%")
                                    )
                }
        }

        // Apply department filter if needed
        val filteredQuery = departmentId?.let { deptId ->
            queryBuilder.andWhere { UnitsTable.departmentId eq deptId }
        } ?: queryBuilder

        filteredQuery
            .limit(limit)
            .map { row ->
                UnitSuggestion(
                    id = row[UnitsTable.id].toString(),
                    code = row[UnitsTable.code],
                    name = row[UnitsTable.name],
                    semester = if (programmeId != null) {
                        row[ProgrammeUnitsTable.semester]
                    } else {
                        1 // Default when not linked to a programme
                    },
                    universityId = universityId.toString(),
                    universityName = row[UniversitiesTable.name],
                    departmentId = row[DepartmentsTable.id]?.toString(),
                    departmentName = row[DepartmentsTable.name]
                )
            }
    }

    // Helper functions
    private fun normalizeSearchQuery(query: String): String {
        return query.trim().replace(Regex("\\s+"), " ")
    }

    private fun determineMatchType(normalizedName: String, query: String): String {
        return when {
            normalizedName == query -> "exact"
            normalizedName.startsWith(query) -> "prefix"
            normalizedName.contains(query) -> "partial"
            else -> "similar"
        }
    }

    /*------------------------
    HELPER FUNCTIONS
    -------------------------*/
    // Enhanced normalization functions
    private fun normalizeName(name: String): String {
        return name
            .trim()
            .replace("\\s+".toRegex(), " ") // Replace multiple spaces with single space
            .uppercase()
    }

    private fun normalizeCode(code: String): String {
        return code
            .trim()
            .uppercase()
            .replace("\\s+".toRegex(), "") // Remove all spaces from codes
    }

    /*private fun unitKey(dto: UpdateUnitAssignmentDto): String =
        dto.unitId ?: dto.draft!!.code

    private fun programmeKey(dto: UpdateProgrammeSetupDto): String =
        dto.programmeId ?: dto.draft!!.name

    private fun academicTermKey(dto: UpdateAcademicTermDto): String =
        dto.academicTermId ?: dto.draft!!.let {
            "${it.academicYear}-${it.semester}"
        }

    private fun academicTermRefKey(ref: AcademicTermRef): String =
        ref.academicTermId ?: ref.draft!!.let {
            "${it.academicYear}-${it.semester}"
        }

     */


}