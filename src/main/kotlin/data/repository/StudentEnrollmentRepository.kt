package com.amos_tech_code.data.repository

import com.amos_tech_code.data.database.entities.*
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.domain.models.ActiveEnrollmentInfo
import com.amos_tech_code.domain.models.ExistingEnrollment
import com.amos_tech_code.domain.models.StudentEnrollmentInfo
import com.amos_tech_code.domain.models.StudentEnrollmentSource
import com.amos_tech_code.domain.models.TeachingAssignmentInfo
import com.amos_tech_code.utils.ValidationException
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class StudentEnrollmentRepository {

    suspend fun findActiveEnrollmentInUniversity(
        studentId: UUID,
        universityId: UUID
    ): ActiveEnrollmentInfo? = exposedTransaction {
        StudentEnrollmentsTable
            .innerJoin(
                ProgrammesTable,
                { StudentEnrollmentsTable.programmeId },
                { ProgrammesTable.id }
            )
            .innerJoin(
                UniversitiesTable,
                { StudentEnrollmentsTable.universityId },
                { UniversitiesTable.id }
            )
            .select(
                StudentEnrollmentsTable.id,
                ProgrammesTable.name,
                UniversitiesTable.name
            )
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.universityId eq universityId) and
                        (StudentEnrollmentsTable.isActive eq true)
            }
            .singleOrNull()
            ?.let { row ->
                ActiveEnrollmentInfo(
                    enrollmentId = row[StudentEnrollmentsTable.id],
                    programmeName = row[ProgrammesTable.name],
                    universityName = row[UniversitiesTable.name]
                )
            }
    }

    suspend fun findActiveTeachingAssignment(
        universityId: UUID,
        programmeId: UUID
    ): TeachingAssignmentInfo? = exposedTransaction {

        LecturerTeachingAssignmentsTable
            .innerJoin(
                AcademicTermsTable,
                { LecturerTeachingAssignmentsTable.academicTermId },
                { AcademicTermsTable.id }
            )
            .innerJoin(
                LecturersTable,
                { LecturerTeachingAssignmentsTable.lecturerId },
                { LecturersTable.id }
            )
            .select(
                LecturerTeachingAssignmentsTable.academicTermId,
                LecturerTeachingAssignmentsTable.yearOfStudy,
                LecturerTeachingAssignmentsTable.lecturerId
            )
            .where {
                (LecturerTeachingAssignmentsTable.universityId eq universityId) and
                        (LecturerTeachingAssignmentsTable.programmeId eq programmeId) and
                        (LecturerTeachingAssignmentsTable.isActive eq true) and
                        (AcademicTermsTable.isActive eq true) and
                        (LecturersTable.isActive eq true)
            }
            .orderBy(
                AcademicTermsTable.academicYear to SortOrder.DESC,
                AcademicTermsTable.semester to SortOrder.DESC
            )
            .limit(1)
            .singleOrNull()
            ?.let { row ->
                TeachingAssignmentInfo(
                    academicTermId = row[LecturerTeachingAssignmentsTable.academicTermId],
                    yearOfStudy = row[LecturerTeachingAssignmentsTable.yearOfStudy],
                    lecturerId = row[LecturerTeachingAssignmentsTable.lecturerId]
                )
            }
    }

    suspend fun getStudentInfo(studentId: UUID): StudentEnrollmentInfo? = exposedTransaction {
        StudentsTable
            .select(
                StudentsTable.id,
                StudentsTable.registrationNumber,
                StudentsTable.fullName
            ).where { StudentsTable.id eq studentId }
            .singleOrNull()
            ?.let { row ->
                StudentEnrollmentInfo(
                    id = row[StudentsTable.id],
                    registrationNumber = row[StudentsTable.registrationNumber],
                    fullName = row[StudentsTable.fullName]
                )
            }
    }

    suspend fun findExistingEnrollment(
        studentId: UUID,
        programmeId: UUID,
        academicTermId: UUID
    ): ExistingEnrollment? = exposedTransaction {
        StudentEnrollmentsTable
            .select(
                StudentEnrollmentsTable.id,
                StudentEnrollmentsTable.isActive
            ).where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.programmeId eq programmeId) and
                        (StudentEnrollmentsTable.academicTermId eq academicTermId)
            }
            .singleOrNull()
            ?.let { row ->
                ExistingEnrollment(
                    id = row[StudentEnrollmentsTable.id],
                    isActive = row[StudentEnrollmentsTable.isActive]
                )
            }
    }

    suspend fun createEnrollment(
        studentId: UUID,
        universityId: UUID,
        programmeId: UUID,
        academicTermId: UUID,
        yearOfStudy: Int,
        enrollmentSource: StudentEnrollmentSource
    ): StudentEnrollmentResponse = exposedTransaction {

        // Validate academic term exists and is active
        AcademicTermsTable
            .select(AcademicTermsTable.id)
            .where {
                (AcademicTermsTable.id eq academicTermId) and
                        (AcademicTermsTable.universityId eq universityId) and
                        (AcademicTermsTable.isActive eq true)
            }
            .singleOrNull()
            ?: throw ValidationException("Academic term not found or not active in this university")

        val enrollmentId = UUID.randomUUID()

        StudentEnrollmentsTable.insert {
            it[StudentEnrollmentsTable.id] = enrollmentId
            it[StudentEnrollmentsTable.studentId] = studentId
            it[StudentEnrollmentsTable.universityId] = universityId
            it[StudentEnrollmentsTable.programmeId] = programmeId
            it[StudentEnrollmentsTable.academicTermId] = academicTermId
            it[StudentEnrollmentsTable.yearOfStudy] = yearOfStudy
            it[StudentEnrollmentsTable.enrollmentSource] = enrollmentSource
            it[StudentEnrollmentsTable.isActive] = true
        }

        // Get the complete enrollment info with joins
        getEnrollmentById(enrollmentId)
    }

    suspend fun getEnrollmentById(enrollmentId: UUID): StudentEnrollmentResponse = exposedTransaction {
        StudentEnrollmentsTable
            .innerJoin(
                otherTable = StudentsTable,
                onColumn = { StudentEnrollmentsTable.studentId },
                otherColumn = { StudentsTable.id }
            )
            .innerJoin(
                otherTable = UniversitiesTable,
                onColumn = { StudentEnrollmentsTable.universityId },
                otherColumn = { UniversitiesTable.id }
            )
            .innerJoin(
                otherTable = ProgrammesTable,
                onColumn = { StudentEnrollmentsTable.programmeId },
                otherColumn = { ProgrammesTable.id }
            )
            .innerJoin(
                otherTable = AcademicTermsTable,
                onColumn = { StudentEnrollmentsTable.academicTermId },
                otherColumn = { AcademicTermsTable.id }
            )
            .leftJoin(
                otherTable = DepartmentsTable,
                onColumn = { ProgrammesTable.departmentId },
                otherColumn = { DepartmentsTable.id }
            )
            .select(
                StudentEnrollmentsTable.id,
                StudentEnrollmentsTable.yearOfStudy,
                StudentEnrollmentsTable.enrollmentDate,
                StudentEnrollmentsTable.enrollmentSource,
                StudentEnrollmentsTable.isActive,
                StudentsTable.id,
                StudentsTable.registrationNumber,
                StudentsTable.fullName,
                UniversitiesTable.id,
                UniversitiesTable.name,
                ProgrammesTable.id,
                ProgrammesTable.name,
                DepartmentsTable.id,
                DepartmentsTable.name,
                AcademicTermsTable.id,
                AcademicTermsTable.academicYear,
                AcademicTermsTable.semester,
                AcademicTermsTable.isActive
            )
            .where { StudentEnrollmentsTable.id eq enrollmentId }
            .single()
            .let { row ->
                StudentEnrollmentResponse(
                    enrollmentId = row[StudentEnrollmentsTable.id].toString(),
                    studentId = row[StudentsTable.id].toString(),
                    registrationNumber = row[StudentsTable.registrationNumber],
                    fullName = row[StudentsTable.fullName],
                    university = UniversityResponse(
                        id = row[UniversitiesTable.id].toString(),
                        name = row[UniversitiesTable.name]
                    ),
                    programme = ProgrammeResponse(
                        id = row[ProgrammesTable.id].toString(),
                        name = row[ProgrammesTable.name]
                    ),
                    academicTerm = AcademicTermResponse(
                        id = row[AcademicTermsTable.id].toString(),
                        academicYear = row[AcademicTermsTable.academicYear],
                        semester = row[AcademicTermsTable.semester],
                        isActive = row[AcademicTermsTable.isActive]
                    ),
                    yearOfStudy = row[StudentEnrollmentsTable.yearOfStudy],
                    enrollmentDate = row[StudentEnrollmentsTable.enrollmentDate].toInstant(ZoneOffset.UTC).toEpochMilli(),
                    enrollmentSource = row[StudentEnrollmentsTable.enrollmentSource].name,
                    isActive = row[StudentEnrollmentsTable.isActive]
                )
            }
    }

    suspend fun getStudentEnrollments(studentId: UUID): List<StudentEnrollmentResponse> = exposedTransaction {
        StudentEnrollmentsTable
            .innerJoin(
                otherTable = StudentsTable,
                onColumn = { StudentEnrollmentsTable.studentId },
                otherColumn = { StudentsTable.id }
            )
            .innerJoin(
                otherTable = UniversitiesTable,
                onColumn = { StudentEnrollmentsTable.universityId },
                otherColumn = { UniversitiesTable.id }
            )
            .innerJoin(
                otherTable = ProgrammesTable,
                onColumn = { StudentEnrollmentsTable.programmeId },
                otherColumn = { ProgrammesTable.id }
            )
            .innerJoin(
                otherTable = AcademicTermsTable,
                onColumn = { StudentEnrollmentsTable.academicTermId },
                otherColumn = { AcademicTermsTable.id }
            )
            .leftJoin(
                otherTable = DepartmentsTable,
                onColumn = { ProgrammesTable.departmentId },
                otherColumn = { DepartmentsTable.id }
            )
            .select(
                StudentEnrollmentsTable.id,
                StudentEnrollmentsTable.yearOfStudy,
                StudentEnrollmentsTable.enrollmentDate,
                StudentEnrollmentsTable.enrollmentSource,
                StudentEnrollmentsTable.isActive,
                StudentsTable.id,
                StudentsTable.registrationNumber,
                StudentsTable.fullName,
                UniversitiesTable.id,
                UniversitiesTable.name,
                ProgrammesTable.id,
                ProgrammesTable.name,
                DepartmentsTable.id,
                DepartmentsTable.name,
                AcademicTermsTable.id,
                AcademicTermsTable.academicYear,
                AcademicTermsTable.semester,
                AcademicTermsTable.isActive
            )
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.isActive eq true)
            }
            .orderBy(StudentEnrollmentsTable.enrollmentDate to SortOrder.DESC)
            .map { row ->
                StudentEnrollmentResponse(
                    enrollmentId = row[StudentEnrollmentsTable.id].toString(),
                    studentId = row[StudentsTable.id].toString(),
                    registrationNumber = row[StudentsTable.registrationNumber],
                    fullName = row[StudentsTable.fullName],
                    university = UniversityResponse(
                        id = row[UniversitiesTable.id].toString(),
                        name = row[UniversitiesTable.name]
                    ),
                    programme = ProgrammeResponse(
                        id = row[ProgrammesTable.id].toString(),
                        name = row[ProgrammesTable.name]
                    ),
                    academicTerm = AcademicTermResponse(
                        id = row[AcademicTermsTable.id].toString(),
                        academicYear = row[AcademicTermsTable.academicYear],
                        semester = row[AcademicTermsTable.semester],
                        isActive = row[AcademicTermsTable.isActive]
                    ),
                    yearOfStudy = row[StudentEnrollmentsTable.yearOfStudy],
                    enrollmentDate = row[StudentEnrollmentsTable.enrollmentDate].toInstant(ZoneOffset.UTC).toEpochMilli(),
                    enrollmentSource = row[StudentEnrollmentsTable.enrollmentSource].name,
                    isActive = row[StudentEnrollmentsTable.isActive]
                )
            }
    }


    suspend fun reactivateEnrollment(
        enrollmentId: UUID,
        yearOfStudy: Int
    ): StudentEnrollmentResponse = exposedTransaction {
        StudentEnrollmentsTable.update({ StudentEnrollmentsTable.id eq enrollmentId }) {
            it[StudentEnrollmentsTable.isActive] = true
            it[StudentEnrollmentsTable.yearOfStudy] = yearOfStudy
            it[StudentEnrollmentsTable.updatedAt] = LocalDateTime.now()
        }

        getEnrollmentById(enrollmentId)
    }

    suspend fun deactivateEnrollment(
        studentId: UUID,
        enrollmentId: UUID
    ): StudentEnrollmentResponse = exposedTransaction {
        // Verify the enrollment belongs to the student
        StudentEnrollmentsTable
            .select(StudentEnrollmentsTable.id)
            .where {
                (StudentEnrollmentsTable.id eq enrollmentId) and
                        (StudentEnrollmentsTable.studentId eq studentId)
            }
            .singleOrNull()
            ?: throw ValidationException("Enrollment not found or does not belong to student")

        StudentEnrollmentsTable.update({ StudentEnrollmentsTable.id eq enrollmentId }) {
            it[StudentEnrollmentsTable.isActive] = false
            it[StudentEnrollmentsTable.updatedAt] = LocalDateTime.now()
        }

        getEnrollmentById(enrollmentId)
    }

    suspend fun updateEnrollmentYear(
        studentId: UUID,
        enrollmentId: UUID,
        newYearOfStudy: Int
    ): StudentEnrollmentResponse = exposedTransaction {

        val currentEnrollment = StudentEnrollmentsTable
            .select(
                StudentEnrollmentsTable.id,
                StudentEnrollmentsTable.studentId,
                StudentEnrollmentsTable.yearOfStudy
            )
            .where {
                (StudentEnrollmentsTable.id eq enrollmentId) and
                        (StudentEnrollmentsTable.studentId eq studentId)
            }
            .singleOrNull()
            ?: throw ValidationException("Enrollment not found or does not belong to student")

        val currentYear = currentEnrollment[StudentEnrollmentsTable.yearOfStudy]

        require(newYearOfStudy >= currentYear) {
            throw ValidationException("Year of study cannot be decreased")
        }

        require(newYearOfStudy <= currentYear + 1) {
            throw ValidationException("Invalid year progression")
        }

        StudentEnrollmentsTable.update({ StudentEnrollmentsTable.id eq enrollmentId }) {
            it[yearOfStudy] = newYearOfStudy
            it[updatedAt] = LocalDateTime.now()
        }

        getEnrollmentById(enrollmentId)
    }

}