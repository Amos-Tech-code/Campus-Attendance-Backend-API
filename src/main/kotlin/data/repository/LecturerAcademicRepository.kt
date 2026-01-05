package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.dtos.requests.AcademicTermRef
import com.amos_tech_code.domain.dtos.requests.UnitSetupRequest
import com.amos_tech_code.domain.dtos.requests.UpdateAcademicSetupRequest
import com.amos_tech_code.domain.dtos.requests.UpdateAcademicTermDto
import com.amos_tech_code.domain.dtos.requests.UpdateProgrammeSetupDto
import com.amos_tech_code.domain.dtos.requests.UpdateUnitAssignmentDto
import com.amos_tech_code.domain.dtos.response.AcademicSetupResponse
import com.amos_tech_code.domain.dtos.response.AcademicTermResponse
import com.amos_tech_code.domain.dtos.response.DepartmentResponse
import com.amos_tech_code.domain.dtos.response.DepartmentSuggestion
import com.amos_tech_code.domain.dtos.response.LecturerProgrammeResponse
import com.amos_tech_code.domain.dtos.response.LecturerUnitResponse
import com.amos_tech_code.domain.dtos.response.ProgrammeResponse
import com.amos_tech_code.domain.dtos.response.ProgrammeSuggestion
import com.amos_tech_code.domain.dtos.response.UnitSuggestion
import com.amos_tech_code.domain.dtos.response.UniversityResponse
import com.amos_tech_code.domain.dtos.response.UniversitySuggestion
import com.amos_tech_code.domain.models.*
import com.amos_tech_code.utils.ValidationException
import data.database.entities.AcademicTermsTable
import data.database.entities.DepartmentsTable
import data.database.entities.LecturerTeachingAssignmentsTable
import data.database.entities.LecturerUniversitiesTable
import data.database.entities.LecturersTable
import data.database.entities.ProgrammeUnitsTable
import data.database.entities.ProgrammesTable
import data.database.entities.UnitsTable
import data.database.entities.UniversitiesTable
import org.jetbrains.exposed.sql.*
import java.util.*

class LecturerAcademicRepository() {

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


    /*------------------------
    UPDATE ACADEMIC SETUP
    -------------------------*/
    fun assertLecturerBelongsToUniversity(
        lecturerId: UUID,
        universityId: UUID
    ) {
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

    fun resolveAcademicTerms(
        universityId: UUID,
        terms: List<UpdateAcademicTermDto>
    ): Map<String, UUID> {

        val map = mutableMapOf<String, UUID>()

        terms.forEach { dto ->

            val termId = when {
                dto.academicTermId != null ->
                    UUID.fromString(dto.academicTermId)

                dto.draft != null -> {
                    val id = UUID.randomUUID()
                    AcademicTermsTable.insert {
                        it[this.id] = id
                        it[this.universityId] = universityId
                        it[academicYear] = dto.draft.academicYear
                        it[semester] = dto.draft.semester
                        it[weekCount] = dto.draft.weekCount
                        it[isActive] = dto.isActive
                    }
                    id
                }

                else -> throw ValidationException("Academic term ref required")
            }

            map[academicTermKey(dto)] = termId
        }

        return map
    }

    fun resolveDepartments(
        universityId: UUID,
        programmes: List<UpdateProgrammeSetupDto>
    ): Map<String, UUID> {

        val departmentMap = mutableMapOf<String, UUID>()

        // Collect all referenced department IDs (from units)
        val referencedDepartmentIds = programmes
            .flatMap { it.units }
            .mapNotNull { it.draft?.department?.departmentId }
            .map { UUID.fromString(it) }
            .toSet()

        // Load existing departments
        DepartmentsTable
            .selectAll()
            .where {
                (DepartmentsTable.id inList referencedDepartmentIds) and
                        (DepartmentsTable.universityId eq universityId)
            }
            .forEach { row ->
                departmentMap[row[DepartmentsTable.id].toString()] = row[DepartmentsTable.id]
            }

        // Validate all references resolved
        referencedDepartmentIds.forEach { id ->
            if (!departmentMap.containsKey(id.toString())) {
                throw ValidationException("Department $id does not belong to this university")
            }
        }

        return departmentMap
    }


    fun resolveProgrammes(
        universityId: UUID,
        programmes: List<UpdateProgrammeSetupDto>,
        departmentMap: Map<String, UUID>
    ): Map<String, UUID> {

        val map = mutableMapOf<String, UUID>()

        programmes.forEach { dto ->
            val id = when {
                dto.programmeId != null ->
                    UUID.fromString(dto.programmeId)

                dto.draft != null -> {
                    val programmeId = UUID.randomUUID()
                    ProgrammesTable.insert {
                        it[this.id] = programmeId
                        it[this.universityId] = universityId
                        it[departmentId] =
                            departmentMap[dto.draft.department.departmentId
                                ?: dto.draft.department.draftName]!!
                        it[name] = dto.draft.name
                    }
                    programmeId
                }

                else -> throw ValidationException("Programme ref required")
            }

            // FIX: Use the same key logic as programmeKey()
            map[dto.programmeId ?: dto.draft!!.name] = id
        }

        return map
    }

    fun resolveUnits(
        universityId: UUID,
        programmes: List<UpdateProgrammeSetupDto>,
        departmentMap: Map<String, UUID>
    ): Map<String, UUID> {

        val unitMap = mutableMapOf<String, UUID>()

        programmes.flatMap { it.units }.forEach { dto ->

            val unitId = when {
                dto.unitId != null ->
                    UUID.fromString(dto.unitId)

                dto.draft != null -> {
                    val departmentId =
                        departmentMap[dto.draft.department.departmentId]
                            ?: throw ValidationException("Invalid department reference for unit ${dto.draft.code}")

                    val newUnitId = UUID.randomUUID()

                    UnitsTable.insert {
                        it[id] = newUnitId
                        it[this.universityId] = universityId
                        it[this.departmentId] = departmentId
                        it[code] = dto.draft.code
                        it[name] = dto.draft.name
                        it[isActive] = true
                    }

                    newUnitId
                }

                else -> throw ValidationException("Unit reference or draft is required")
            }

            unitMap[unitKey(dto)] = unitId
        }

        return unitMap
    }

    fun resolveProgrammeUnits(
        programmes: List<UpdateProgrammeSetupDto>,
        programmeMap: Map<String, UUID>,
        unitMap: Map<String, UUID>
    ) {
        programmes.forEach { programme ->

            val programmeId =
                programmeMap[programmeKey(programme)]
                    ?: error("Programme not resolved")

            programme.units.forEach { unit ->

                val unitId =
                    unitMap[unitKey(unit)]
                        ?: error("Unit not resolved")

                ProgrammeUnitsTable.upsert(
                    keys = arrayOf(
                        ProgrammeUnitsTable.programmeId,
                        ProgrammeUnitsTable.unitId,
                        ProgrammeUnitsTable.yearOfStudy
                    )
                ) {
                    it[this.programmeId] = programmeId
                    it[this.unitId] = unitId
                    it[this.yearOfStudy] = programme.yearOfStudy
                    it[this.semester] =
                        unit.academicTermRef.draft?.semester
                            ?: AcademicTermsTable
                                .select(AcademicTermsTable.semester)
                                .where {
                                    AcademicTermsTable.id eq
                                            UUID.fromString(unit.academicTermRef.academicTermId!!)
                                }
                                .single()[AcademicTermsTable.semester]
                }
            }
        }
    }

    fun resolveLecturerAssignments(
        lecturerId: UUID,
        universityId: UUID,
        programmes: List<UpdateProgrammeSetupDto>,
        programmeMap: Map<String, UUID>,
        unitMap: Map<String, UUID>,
        academicTermMap: Map<String, UUID>
    ) {

        programmes.forEach { programme ->
            programme.units.forEach { unit ->

                LecturerTeachingAssignmentsTable.upsert(
                    keys = arrayOf(
                        LecturerTeachingAssignmentsTable.lecturerId,
                        LecturerTeachingAssignmentsTable.unitId,
                        LecturerTeachingAssignmentsTable.academicTermId,
                        LecturerTeachingAssignmentsTable.yearOfStudy,
                        LecturerTeachingAssignmentsTable.programmeId
                    )
                ) {
                    it[this.lecturerId] = lecturerId
                    it[this.universityId] = universityId
                    it[this.programmeId] =
                        programmeMap[programme.programmeId ?: programme.draft!!.name]!!
                    it[this.unitId] =
                        unitMap[unit.unitId ?: unit.draft!!.code]!!
                    it[this.academicTermId] =
                        academicTermMap[academicTermRefKey(unit.academicTermRef)]
                            ?: throw IllegalStateException(
                                "Academic term not resolved for ${academicTermRefKey(unit.academicTermRef)}"
                            )
                    it[expectedStudents] = programme.expectedStudentCount
                    it[yearOfStudy] = programme.yearOfStudy
                    it[lectureDay] = unit.lectureDay
                    it[lectureTime] = unit.lectureTime
                    it[lectureVenue] = unit.lectureVenue
                    it[isActive] = unit.isActive
                }
            }
        }
    }

    fun deactivateMissingAssignments(
        lecturerId: UUID,
        universityId: UUID,
        request: UpdateAcademicSetupRequest
    ) {
        // Units explicitly marked inactive
        val inactiveUnitIds = request.programmes
            .flatMap { it.units }
            .filter { !it.isActive }
            .mapNotNull { it.unitId }
            .map(UUID::fromString)
            .toSet()

        if (inactiveUnitIds.isNotEmpty()) {
            LecturerTeachingAssignmentsTable.update({
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                        (LecturerTeachingAssignmentsTable.unitId inList inactiveUnitIds)
            }) {
                it[isActive] = false
            }
        }

        // Programmes marked inactive
        val inactiveProgrammeIds = request.programmes
            .filter { !it.isActive }
            .mapNotNull { it.programmeId }
            .map(UUID::fromString)

        if (inactiveProgrammeIds.isNotEmpty()) {
            LecturerTeachingAssignmentsTable.update({
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                        (LecturerTeachingAssignmentsTable.programmeId inList inactiveProgrammeIds)
            }) {
                it[isActive] = false
            }
        }

        // Academic terms marked inactive
        val inactiveTermIds = request.academicTerms
            .filter { !it.isActive }
            .mapNotNull { it.academicTermId }
            .map(UUID::fromString)

        if (inactiveTermIds.isNotEmpty()) {
            LecturerTeachingAssignmentsTable.update({
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.academicTermId inList inactiveTermIds)
            }) {
                it[isActive] = false
            }
        }

        // Entire university deactivation
        if (!request.isActive) {
            LecturerTeachingAssignmentsTable.update({
                (LecturerTeachingAssignmentsTable.lecturerId eq lecturerId) and
                        (LecturerTeachingAssignmentsTable.universityId eq universityId)
            }) {
                it[isActive] = false
            }

            LecturerUniversitiesTable.update({
                (LecturerUniversitiesTable.lecturerId eq lecturerId) and
                        (LecturerUniversitiesTable.universityId eq universityId)
            }) {
                it[isActive] = false
            }
        }
    }

    fun getAcademicSetupForUniversity(
        lecturerId: UUID,
        universityId: UUID
    ): AcademicSetupResponse {

        val university = UniversitiesTable
            .selectAll()
            .where { UniversitiesTable.id eq universityId }
            .single()

        // Check if lecturer-university link is active
        val isUniversityActiveForLecturer = LecturerUniversitiesTable
            .select(LecturerUniversitiesTable.isActive)
            .where {
                (LecturerUniversitiesTable.lecturerId eq lecturerId) and
                        (LecturerUniversitiesTable.universityId eq universityId)
            }
            .singleOrNull()?.get(LecturerUniversitiesTable.isActive) ?: false

        // If university is not active for this lecturer, return minimal response
        if (!isUniversityActiveForLecturer) {
            return AcademicSetupResponse(
                university = UniversityResponse(
                    id = university[UniversitiesTable.id].toString(),
                    name = university[UniversitiesTable.name]
                ),
                academicTerm = null,  // No active term when university is inactive
                programmes = emptyList(),
                isActive = false,  // IMPORTANT: Set to false
                createdAt = System.currentTimeMillis()
            )
        }

        val activeTerm = getActiveAcademicTerm(universityId)

        val assignments = LecturerTeachingAssignmentsTable
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
                        (LecturerTeachingAssignmentsTable.academicTermId eq activeTerm.id) and
                        (LecturerTeachingAssignmentsTable.isActive eq true) and
                        (ProgrammeUnitsTable.programmeId eq LecturerTeachingAssignmentsTable.programmeId) and
                        (ProgrammeUnitsTable.yearOfStudy eq LecturerTeachingAssignmentsTable.yearOfStudy)
            }

        val programmes = assignments
            .groupBy {
                Pair(
                    it[ProgrammesTable.id],
                    it[LecturerTeachingAssignmentsTable.yearOfStudy]
                )
            }
            .map { (_, rows) ->
                val first = rows.first()

                LecturerProgrammeResponse(
                    programmeId = first[ProgrammesTable.id].toString(),
                    programmeName = first[ProgrammesTable.name],
                    departmentId = first[DepartmentsTable.id].toString(),
                    departmentName = first[DepartmentsTable.name],
                    yearOfStudy = first[LecturerTeachingAssignmentsTable.yearOfStudy],
                    expectedStudentCount = first[LecturerTeachingAssignmentsTable.expectedStudents],
                    units = rows.map {
                        LecturerUnitResponse(
                            unitId = it[UnitsTable.id].toString(),
                            code = it[UnitsTable.code],
                            name = it[UnitsTable.name],
                            semester = it[ProgrammeUnitsTable.semester],
                            lectureDay = it[LecturerTeachingAssignmentsTable.lectureDay],
                            lectureTime = it[LecturerTeachingAssignmentsTable.lectureTime],
                            lectureVenue = it[LecturerTeachingAssignmentsTable.lectureVenue]
                        )
                    }
                )
            }

        return AcademicSetupResponse(
            university = UniversityResponse(
                id = university[UniversitiesTable.id].toString(),
                name = university[UniversitiesTable.name]
            ),
            academicTerm = AcademicTermResponse(
                id = activeTerm.id.toString(),
                academicYear = activeTerm.academicYear,
                semester = activeTerm.semester,
                isActive = activeTerm.isActive
            ),
            programmes = programmes,
            isActive = true,  // University is active for this lecturer
            createdAt = System.currentTimeMillis()
        )
    }

    fun getActiveAcademicTerm(universityId: UUID): ResolvedAcademicTerm =
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

    private fun unitKey(dto: UpdateUnitAssignmentDto): String =
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


}