package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.admin.*
import com.amos_tech_code.data.database.utils.exposedTransaction
import data.database.entities.AcademicTermsTable
import data.database.entities.AttendanceSessionsTable
import data.database.entities.DepartmentsTable
import data.database.entities.LecturerTeachingAssignmentsTable
import data.database.entities.ProgrammeUnitsTable
import data.database.entities.ProgrammesTable
import data.database.entities.StudentEnrollmentsTable
import data.database.entities.UnitsTable
import data.database.entities.UniversitiesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class UniversityStructureRepository {

    // ========== UNIVERSITY CRUD ==========

    suspend fun getAllUniversities(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): Triple<List<UniversityResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = UniversitiesTable.selectAll()

        if (!search.isNullOrBlank()) {
            query = query.andWhere { UniversitiesTable.name like "%$search%" }
        }

        val total = query.count()

        val universities = query
            .orderBy(UniversitiesTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                val universityId = row[UniversitiesTable.id]

                val departmentCount = DepartmentsTable
                    .selectAll()
                    .where { DepartmentsTable.universityId eq universityId }
                    .count()

                val programmeCount = ProgrammesTable
                    .selectAll()
                    .where { ProgrammesTable.universityId eq universityId }
                    .count()

                val unitCount = UnitsTable
                    .selectAll()
                    .where { UnitsTable.universityId eq universityId }
                    .count()

                UniversityResponse(
                    id = universityId.toString(),
                    name = row[UniversitiesTable.name],
                    departmentCount = departmentCount.toInt(),
                    programmeCount = programmeCount.toInt(),
                    unitCount = unitCount.toInt(),
                    createdAt = row[UniversitiesTable.createdAt].toString()
                )
            }

        Triple(universities, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun getUniversityById(id: UUID): UniversityResponse? = exposedTransaction {
        val row = UniversitiesTable
            .selectAll()
            .where { UniversitiesTable.id eq id }
            .singleOrNull()

        row?.let {
            val departmentCount = DepartmentsTable
                .selectAll()
                .where { DepartmentsTable.universityId eq id }
                .count()

            val programmeCount = ProgrammesTable
                .selectAll()
                .where { ProgrammesTable.universityId eq id }
                .count()

            val unitCount = UnitsTable
                .selectAll()
                .where { UnitsTable.universityId eq id }
                .count()

            UniversityResponse(
                id = it[UniversitiesTable.id].toString(),
                name = it[UniversitiesTable.name],
                departmentCount = departmentCount.toInt(),
                programmeCount = programmeCount.toInt(),
                unitCount = unitCount.toInt(),
                createdAt = it[UniversitiesTable.createdAt].toString()
            )
        }
    }

    suspend fun updateUniversity(id: UUID, name: String): Boolean = exposedTransaction {
        UniversitiesTable.update({ UniversitiesTable.id eq id }) {
            it[UniversitiesTable.name] = name
            it[UniversitiesTable.updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteUniversity(id: UUID): Boolean = exposedTransaction {
        // Check if there are any departments, programmes, or units
        val hasDepartments = DepartmentsTable.selectAll().where { DepartmentsTable.universityId eq id }.count() > 0
        val hasProgrammes = ProgrammesTable.selectAll().where { ProgrammesTable.universityId eq id }.count() > 0
        val hasUnits = UnitsTable.selectAll().where { UnitsTable.universityId eq id }.count() > 0

        if (hasDepartments || hasProgrammes || hasUnits) {
            return@exposedTransaction false
        }

        UniversitiesTable.deleteWhere { UniversitiesTable.id eq id } > 0
    }

    // ========== DEPARTMENT CRUD ==========

    suspend fun getAllDepartments(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        search: String? = null
    ): Triple<List<DepartmentResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = DepartmentsTable
            .innerJoin(UniversitiesTable, { DepartmentsTable.universityId }, { UniversitiesTable.id })
            .select(
                DepartmentsTable.id,
                DepartmentsTable.universityId,
                UniversitiesTable.name,
                DepartmentsTable.name,
                DepartmentsTable.createdAt
            )

        universityId?.let {
            query = query.andWhere { DepartmentsTable.universityId eq it }
        }

        if (!search.isNullOrBlank()) {
            query = query.andWhere { DepartmentsTable.name like "%$search%" }
        }

        val total = query.count()

        val departments = query
            .orderBy(DepartmentsTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                val departmentId = row[DepartmentsTable.id]

                val programmeCount = ProgrammesTable
                    .selectAll()
                    .where { ProgrammesTable.departmentId eq departmentId }
                    .count()

                DepartmentResponse(
                    id = departmentId.toString(),
                    universityId = row[DepartmentsTable.universityId].toString(),
                    universityName = row[UniversitiesTable.name],
                    name = row[DepartmentsTable.name],
                    programmeCount = programmeCount.toInt(),
                    createdAt = row[DepartmentsTable.createdAt].toString()
                )
            }

        Triple(departments, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun createDepartment(universityId: UUID, name: String): DepartmentResponse? = exposedTransaction {
        val id = UUID.randomUUID()

        DepartmentsTable.insert {
            it[DepartmentsTable.id] = id
            it[DepartmentsTable.universityId] = universityId
            it[DepartmentsTable.name] = name
            it[DepartmentsTable.createdAt] = LocalDateTime.now()
            it[DepartmentsTable.updatedAt] = LocalDateTime.now()
        }

        getAllDepartments(page = 1, pageSize = 1, universityId = universityId).first.find { it.id == id.toString() }
    }

    suspend fun updateDepartment(id: UUID, name: String): Boolean = exposedTransaction {
        DepartmentsTable.update({ DepartmentsTable.id eq id }) {
            it[DepartmentsTable.name] = name
            it[DepartmentsTable.updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteDepartment(id: UUID): Boolean = exposedTransaction {
        val hasProgrammes = ProgrammesTable.selectAll().where { ProgrammesTable.departmentId eq id }.count() > 0

        if (hasProgrammes) {
            return@exposedTransaction false
        }

        DepartmentsTable.deleteWhere { DepartmentsTable.id eq id } > 0
    }

    // ========== PROGRAMME CRUD ==========

    suspend fun getAllProgrammes(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        departmentId: UUID? = null,
        search: String? = null,
        activeOnly: Boolean = false
    ): Triple<List<ProgrammeResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = ProgrammesTable
            .innerJoin(UniversitiesTable, { ProgrammesTable.universityId }, { UniversitiesTable.id })
            .leftJoin(DepartmentsTable, { ProgrammesTable.departmentId }, { DepartmentsTable.id })
            .select(
                ProgrammesTable.id,
                ProgrammesTable.universityId,
                UniversitiesTable.name,
                ProgrammesTable.departmentId,
                DepartmentsTable.name,
                ProgrammesTable.name,
                ProgrammesTable.isActive,
                ProgrammesTable.createdAt
            )

        universityId?.let {
            query = query.andWhere { ProgrammesTable.universityId eq it }
        }

        departmentId?.let {
            query = query.andWhere { ProgrammesTable.departmentId eq it }
        }

        if (activeOnly) {
            query = query.andWhere { ProgrammesTable.isActive eq true }
        }

        if (!search.isNullOrBlank()) {
            query = query.andWhere { ProgrammesTable.name like "%$search%" }
        }

        val total = query.count()

        val programmes = query
            .orderBy(ProgrammesTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                val programmeId = row[ProgrammesTable.id]

                val unitCount = ProgrammeUnitsTable
                    .selectAll()
                    .where { ProgrammeUnitsTable.programmeId eq programmeId }
                    .count()

                ProgrammeResponse(
                    id = programmeId.toString(),
                    universityId = row[ProgrammesTable.universityId].toString(),
                    universityName = row[UniversitiesTable.name],
                    departmentId = row[ProgrammesTable.departmentId].toString(),
                    departmentName = row[DepartmentsTable.name],
                    name = row[ProgrammesTable.name],
                    isActive = row[ProgrammesTable.isActive],
                    unitCount = unitCount.toInt(),
                    createdAt = row[ProgrammesTable.createdAt].toString()
                )
            }

        Triple(programmes, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun createProgramme(
        universityId: UUID,
        departmentId: UUID,
        name: String,
        isActive: Boolean
    ): ProgrammeResponse? = exposedTransaction {
        val id = UUID.randomUUID()

        ProgrammesTable.insert {
            it[ProgrammesTable.id] = id
            it[ProgrammesTable.universityId] = universityId
            it[ProgrammesTable.departmentId] = departmentId
            it[ProgrammesTable.name] = name
            it[ProgrammesTable.isActive] = isActive
            it[ProgrammesTable.createdAt] = LocalDateTime.now()
            it[ProgrammesTable.updatedAt] = LocalDateTime.now()
        }

        getAllProgrammes(page = 1, pageSize = 1, universityId = universityId).first.find { it.id == id.toString() }
    }

    suspend fun updateProgramme(
        id: UUID,
        name: String?,
        departmentId: UUID?,
        isActive: Boolean?
    ): Boolean = exposedTransaction {
        val updateCount = ProgrammesTable.update({ ProgrammesTable.id eq id }) {
            name?.let { name -> it[ProgrammesTable.name] = name }
            departmentId?.let { departmentId -> it[ProgrammesTable.departmentId] = departmentId }
            isActive?.let { isActive -> it[ProgrammesTable.isActive] = isActive }
            it[ProgrammesTable.updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    suspend fun deleteProgramme(id: UUID): Boolean = exposedTransaction {
        val hasEnrollments = StudentEnrollmentsTable
            .selectAll()
            .where { StudentEnrollmentsTable.programmeId eq id }
            .count() > 0

        if (hasEnrollments) {
            return@exposedTransaction false
        }

        ProgrammeUnitsTable.deleteWhere { ProgrammeUnitsTable.programmeId eq id }
        ProgrammesTable.deleteWhere { ProgrammesTable.id eq id } > 0
    }

    // ========== UNIT CRUD ==========

    suspend fun getAllUnits(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        departmentId: UUID? = null,
        search: String? = null,
        activeOnly: Boolean = false
    ): Triple<List<UnitResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = UnitsTable
            .innerJoin(UniversitiesTable, { UnitsTable.universityId }, { UniversitiesTable.id })
            .leftJoin(DepartmentsTable, { UnitsTable.departmentId }, { DepartmentsTable.id })
            .select(
                UnitsTable.id,
                UnitsTable.universityId,
                UniversitiesTable.name,
                UnitsTable.departmentId,
                DepartmentsTable.name,
                UnitsTable.code,
                UnitsTable.name,
                UnitsTable.isActive,
                UnitsTable.createdAt
            )

        universityId?.let {
            query = query.andWhere { UnitsTable.universityId eq it }
        }

        departmentId?.let {
            query = query.andWhere { UnitsTable.departmentId eq it }
        }

        if (activeOnly) {
            query = query.andWhere { UnitsTable.isActive eq true }
        }

        if (!search.isNullOrBlank()) {
            query = query.andWhere {
                (UnitsTable.code like "%$search%") or (UnitsTable.name like "%$search%")
            }
        }

        val total = query.count()

        val units = query
            .orderBy(UnitsTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                val unitId = row[UnitsTable.id]

                val programmes = ProgrammeUnitsTable
                    .innerJoin(ProgrammesTable, { ProgrammeUnitsTable.programmeId }, { ProgrammesTable.id })
                    .select(
                        ProgrammesTable.id,
                        ProgrammesTable.name,
                        ProgrammeUnitsTable.yearOfStudy
                    )
                    .where { ProgrammeUnitsTable.unitId eq unitId }
                    .map { progRow ->
                        ProgrammeInfo(
                            id = progRow[ProgrammesTable.id].toString(),
                            name = progRow[ProgrammesTable.name],
                            yearOfStudy = progRow[ProgrammeUnitsTable.yearOfStudy]
                        )
                    }

                UnitResponse(
                    id = unitId.toString(),
                    universityId = row[UnitsTable.universityId].toString(),
                    universityName = row[UniversitiesTable.name],
                    departmentId = row[UnitsTable.departmentId].toString(),
                    departmentName = row[DepartmentsTable.name],
                    code = row[UnitsTable.code],
                    name = row[UnitsTable.name],
                    isActive = row[UnitsTable.isActive],
                    programmes = programmes,
                    createdAt = row[UnitsTable.createdAt].toString()
                )
            }

        Triple(units, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun createUnit(
        universityId: UUID,
        departmentId: UUID,
        code: String,
        name: String,
        isActive: Boolean
    ): UnitResponse? = exposedTransaction {
        val id = UUID.randomUUID()

        UnitsTable.insert {
            it[UnitsTable.id] = id
            it[UnitsTable.universityId] = universityId
            it[UnitsTable.departmentId] = departmentId
            it[UnitsTable.code] = code
            it[UnitsTable.name] = name
            it[UnitsTable.isActive] = isActive
            it[UnitsTable.createdAt] = LocalDateTime.now()
            it[UnitsTable.updatedAt] = LocalDateTime.now()
        }

        getAllUnits(page = 1, pageSize = 1, universityId = universityId).first.find { it.id == id.toString() }
    }

    suspend fun updateUnit(
        id: UUID,
        code: String?,
        name: String?,
        departmentId: UUID?,
        isActive: Boolean?
    ): Boolean = exposedTransaction {
        val updateCount = UnitsTable.update({ UnitsTable.id eq id }) {
            code?.let { code -> it[UnitsTable.code] = code }
            name?.let { name -> it[UnitsTable.name] = name }
            departmentId?.let { departmentId -> it[UnitsTable.departmentId] = departmentId }
            isActive?.let { isActive -> it[UnitsTable.isActive] = isActive }
            it[UnitsTable.updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    suspend fun deleteUnit(id: UUID): Boolean = exposedTransaction {
        val hasAssignments = LecturerTeachingAssignmentsTable
            .selectAll()
            .where { LecturerTeachingAssignmentsTable.unitId eq id }
            .count() > 0

        if (hasAssignments) {
            return@exposedTransaction false
        }

        ProgrammeUnitsTable.deleteWhere { ProgrammeUnitsTable.unitId eq id }
        UnitsTable.deleteWhere { UnitsTable.id eq id } > 0
    }

    suspend fun linkUnitToProgramme(
        unitId: UUID,
        programmeId: UUID,
        yearOfStudy: Int,
        semester: Int
    ): Boolean = exposedTransaction {
        val existing = ProgrammeUnitsTable
            .selectAll()
            .where { (ProgrammeUnitsTable.unitId eq unitId) and (ProgrammeUnitsTable.programmeId eq programmeId) }
            .count() > 0

        if (existing) {
            return@exposedTransaction false
        }

        ProgrammeUnitsTable.insert {
            it[ProgrammeUnitsTable.id] = UUID.randomUUID()
            it[ProgrammeUnitsTable.unitId] = unitId
            it[ProgrammeUnitsTable.programmeId] = programmeId
            it[ProgrammeUnitsTable.yearOfStudy] = yearOfStudy
            it[ProgrammeUnitsTable.semester] = semester
            it[ProgrammeUnitsTable.createdAt] = LocalDateTime.now()
        }

        true
    }

    suspend fun unlinkUnitFromProgramme(unitId: UUID, programmeId: UUID): Boolean = exposedTransaction {
        ProgrammeUnitsTable.deleteWhere {
            (ProgrammeUnitsTable.unitId eq unitId) and (ProgrammeUnitsTable.programmeId eq programmeId)
        } > 0
    }


    // ========== ACADEMIC TERMS CRUD ==========

    suspend fun getAllAcademicTerms(
        page: Int = 1,
        pageSize: Int = 20,
        universityId: UUID? = null,
        search: String? = null,
        activeOnly: Boolean = false
    ): Triple<List<AcademicTermResponse>, Long, Int> = exposedTransaction {
        val offset = (page - 1) * pageSize

        var query = AcademicTermsTable
            .innerJoin(UniversitiesTable, { AcademicTermsTable.universityId }, { UniversitiesTable.id })
            .select(
                AcademicTermsTable.id,
                AcademicTermsTable.universityId,
                UniversitiesTable.name,
                AcademicTermsTable.academicYear,
                AcademicTermsTable.semester,
                AcademicTermsTable.weekCount,
                AcademicTermsTable.isActive,
                AcademicTermsTable.createdAt
            )

        universityId?.let {
            query = query.andWhere { AcademicTermsTable.universityId eq it }
        }

        if (activeOnly) {
            query = query.andWhere { AcademicTermsTable.isActive eq true }
        }

        if (!search.isNullOrBlank()) {
            query = query.andWhere {
                (AcademicTermsTable.academicYear like "%$search%") or
                        (AcademicTermsTable.semester.castTo<String>(VarCharColumnType()).like("%$search%"))
            }
        }

        val total = query.count()

        val terms = query
            .orderBy(AcademicTermsTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(offset.toLong())
            .map { row ->
                AcademicTermResponse(
                    id = row[AcademicTermsTable.id].toString(),
                    universityId = row[AcademicTermsTable.universityId].toString(),
                    universityName = row[UniversitiesTable.name],
                    academicYear = row[AcademicTermsTable.academicYear],
                    semester = row[AcademicTermsTable.semester],
                    weekCount = row[AcademicTermsTable.weekCount],
                    isActive = row[AcademicTermsTable.isActive],
                    createdAt = row[AcademicTermsTable.createdAt].toString()
                )
            }

        Triple(terms, total, ((total + pageSize - 1) / pageSize).toInt())
    }

    suspend fun getAcademicTermById(id: UUID): AcademicTermResponse? = exposedTransaction {
        val row = AcademicTermsTable
            .innerJoin(UniversitiesTable, { AcademicTermsTable.universityId }, { UniversitiesTable.id })
            .select(
                AcademicTermsTable.id,
                AcademicTermsTable.universityId,
                UniversitiesTable.name,
                AcademicTermsTable.academicYear,
                AcademicTermsTable.semester,
                AcademicTermsTable.weekCount,
                AcademicTermsTable.isActive,
                AcademicTermsTable.createdAt
            )
            .where { AcademicTermsTable.id eq id }
            .singleOrNull()

        row?.let {
            AcademicTermResponse(
                id = it[AcademicTermsTable.id].toString(),
                universityId = it[AcademicTermsTable.universityId].toString(),
                universityName = it[UniversitiesTable.name],
                academicYear = it[AcademicTermsTable.academicYear],
                semester = it[AcademicTermsTable.semester],
                weekCount = it[AcademicTermsTable.weekCount],
                isActive = it[AcademicTermsTable.isActive],
                createdAt = it[AcademicTermsTable.createdAt].toString()
            )
        }
    }

    suspend fun createAcademicTerm(
        universityId: UUID,
        academicYear: String,
        semester: Int,
        weekCount: Int,
        isActive: Boolean
    ): UUID? = exposedTransaction {
        // Check for duplicate (same university, year, and semester)
        val existing = AcademicTermsTable
            .selectAll()
            .where {
                (AcademicTermsTable.universityId eq universityId) and
                        (AcademicTermsTable.academicYear eq academicYear) and
                        (AcademicTermsTable.semester eq semester)
            }
            .count() > 0

        if (existing) {
            return@exposedTransaction null
        }

        val id = UUID.randomUUID()

        AcademicTermsTable.insert {
            it[AcademicTermsTable.id] = id
            it[AcademicTermsTable.universityId] = universityId
            it[AcademicTermsTable.academicYear] = academicYear
            it[AcademicTermsTable.semester] = semester
            it[AcademicTermsTable.weekCount] = weekCount
            it[AcademicTermsTable.isActive] = isActive
            it[AcademicTermsTable.createdAt] = LocalDateTime.now()
        }

        id
    }

    suspend fun updateAcademicTerm(
        id: UUID,
        academicYear: String?,
        semester: Int?,
        weekCount: Int?,
        isActive: Boolean?
    ): Boolean = exposedTransaction {
        val term = getAcademicTermById(id) ?: return@exposedTransaction false

        val updateCount = AcademicTermsTable.update({ AcademicTermsTable.id eq id }) {
            academicYear?.let { academicYear -> it[AcademicTermsTable.academicYear] = academicYear }
            semester?.let { semester -> it[AcademicTermsTable.semester] = semester }
            weekCount?.let { weekCount -> it[AcademicTermsTable.weekCount] = weekCount }
            isActive?.let { isActive -> it[AcademicTermsTable.isActive] = isActive }
        }
        updateCount > 0
    }

    suspend fun deleteAcademicTerm(id: UUID): Boolean = exposedTransaction {
        // Check if there are any attendance sessions using this term
        val hasSessions = AttendanceSessionsTable
            .selectAll()
            .where { AttendanceSessionsTable.academicTermId eq id }
            .count() > 0

        if (hasSessions) {
            return@exposedTransaction false
        }

        // Check if there are any enrollments using this term
        val hasEnrollments = StudentEnrollmentsTable
            .selectAll()
            .where { StudentEnrollmentsTable.academicTermId eq id }
            .count() > 0

        if (hasEnrollments) {
            return@exposedTransaction false
        }

        AcademicTermsTable.deleteWhere { AcademicTermsTable.id eq id } > 0
    }

    suspend fun setActiveAcademicTerm(universityId: UUID, termId: UUID): Boolean = exposedTransaction {
        // First deactivate all terms for this university
        AcademicTermsTable.update({ AcademicTermsTable.universityId eq universityId }) {
            it[AcademicTermsTable.isActive] = false
        }

        // Then activate the selected term
        AcademicTermsTable.update({ AcademicTermsTable.id eq termId }) {
            it[AcademicTermsTable.isActive] = true
        } > 0
    }

    suspend fun getActiveAcademicTerm(universityId: UUID): AcademicTermResponse? = exposedTransaction {
        val row = AcademicTermsTable
            .innerJoin(UniversitiesTable, { AcademicTermsTable.universityId }, { UniversitiesTable.id })
            .select(
                AcademicTermsTable.id,
                AcademicTermsTable.universityId,
                UniversitiesTable.name,
                AcademicTermsTable.academicYear,
                AcademicTermsTable.semester,
                AcademicTermsTable.weekCount,
                AcademicTermsTable.isActive,
                AcademicTermsTable.createdAt
            )
            .where { (AcademicTermsTable.universityId eq universityId) and (AcademicTermsTable.isActive eq true) }
            .singleOrNull()

        row?.let {
            AcademicTermResponse(
                id = it[AcademicTermsTable.id].toString(),
                universityId = it[AcademicTermsTable.universityId].toString(),
                universityName = it[UniversitiesTable.name],
                academicYear = it[AcademicTermsTable.academicYear],
                semester = it[AcademicTermsTable.semester],
                weekCount = it[AcademicTermsTable.weekCount],
                isActive = it[AcademicTermsTable.isActive],
                createdAt = it[AcademicTermsTable.createdAt].toString()
            )
        }
    }
}