package data.repository

import api.dtos.response.StudentEnrollmentResponse
import com.amos_tech_code.data.database.utils.exposedTransaction
import com.amos_tech_code.domain.dtos.response.*
import com.amos_tech_code.domain.models.ActiveEnrollmentInfo
import com.amos_tech_code.domain.models.ExistingEnrollment
import com.amos_tech_code.domain.models.Lecturer
import com.amos_tech_code.domain.models.StudentEnrollment
import com.amos_tech_code.domain.models.StudentEnrollmentInfo
import domain.models.StudentEnrollmentSource
import com.amos_tech_code.domain.models.TeachingAssignmentInfo
import com.amos_tech_code.utils.ResourceNotFoundException
import com.amos_tech_code.utils.ValidationException
import data.database.entities.AcademicTermsTable
import data.database.entities.DepartmentsTable
import data.database.entities.LecturerTeachingAssignmentsTable
import data.database.entities.LecturersTable
import data.database.entities.ProgrammesTable
import data.database.entities.StudentEnrollmentsTable
import data.database.entities.StudentsTable
import data.database.entities.UniversitiesTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class StudentEnrollmentRepository {

    suspend fun findActiveEnrollment(
        studentId: UUID
    ): ActiveEnrollmentInfo? = exposedTransaction {

        StudentEnrollmentsTable
            .innerJoin(
                ProgrammesTable,
                onColumn = { StudentEnrollmentsTable.programmeId },
                otherColumn = { ProgrammesTable.id }
            )
            .innerJoin(
                UniversitiesTable,
                onColumn = { StudentEnrollmentsTable.universityId },
                otherColumn = { UniversitiesTable.id }
            )
            .select(
                StudentEnrollmentsTable.id,
                ProgrammesTable.name,
                ProgrammesTable.id,
                UniversitiesTable.id,
                UniversitiesTable.name
            )
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.isActive eq true)
            }
            .singleOrNull()
            ?.let { row ->
                ActiveEnrollmentInfo(
                    enrollmentId = row[StudentEnrollmentsTable.id],
                    programmeName = row[ProgrammesTable.name],
                    programmeId = row[ProgrammesTable.id],
                    universityId = row[UniversitiesTable.id],
                    universityName = row[UniversitiesTable.name]
                )
            }
    }

    // In StudentEnrollmentRepository.kt
    suspend fun findActiveEnrollments(studentId: UUID): List<StudentEnrollment> = exposedTransaction {
        StudentEnrollmentsTable
            .innerJoin(ProgrammesTable) {
                StudentEnrollmentsTable.programmeId eq ProgrammesTable.id
            }
            .innerJoin(AcademicTermsTable) {
                StudentEnrollmentsTable.academicTermId eq AcademicTermsTable.id
            }
            .innerJoin(UniversitiesTable) {
                StudentEnrollmentsTable.universityId eq UniversitiesTable.id
            }
            .selectAll()
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.isActive eq true)
            }
            .map { row ->
                StudentEnrollment(
                    id = row[StudentEnrollmentsTable.id],
                    studentId = row[StudentEnrollmentsTable.studentId],
                    universityId = row[StudentEnrollmentsTable.universityId],
                    universityName = row[UniversitiesTable.name],
                    programmeId = row[StudentEnrollmentsTable.programmeId],
                    programmeName = row[ProgrammesTable.name],
                    academicTermId = row[StudentEnrollmentsTable.academicTermId],
                    academicYear = row[AcademicTermsTable.academicYear],
                    semester = row[AcademicTermsTable.semester],
                    yearOfStudy = row[StudentEnrollmentsTable.yearOfStudy],
                    enrollmentDate = row[StudentEnrollmentsTable.enrollmentDate],
                    isActive = row[StudentEnrollmentsTable.isActive]
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

    suspend fun getStudentEnrollment(studentId: UUID): StudentEnrollmentResponse = exposedTransaction {

        val enrollmentId = StudentEnrollmentsTable
            .select(StudentEnrollmentsTable.id)
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.isActive eq true)
            }
            .singleOrNull()
            ?.get(StudentEnrollmentsTable.id) ?: throw ResourceNotFoundException("No active enrollment found")

        getEnrollmentById(enrollmentId)

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
                StudentEnrollmentsTable.yearOfStudy
            )
            .where {
                (StudentEnrollmentsTable.id eq enrollmentId) and
                        (StudentEnrollmentsTable.studentId eq studentId)
            }
            .singleOrNull()
            ?: throw ValidationException("Enrollment not found or does not belong to student")

        val currentYear = currentEnrollment[StudentEnrollmentsTable.yearOfStudy]

        if (newYearOfStudy < currentYear) {
            throw ValidationException("Year of study cannot be decreased")
        }

        if (newYearOfStudy > currentYear + 1) {
            throw ValidationException("Invalid year progression")
        }

        StudentEnrollmentsTable.update({ StudentEnrollmentsTable.id eq enrollmentId }) {
            it[yearOfStudy] = newYearOfStudy
            it[updatedAt] = LocalDateTime.now()
        }

        getEnrollmentById(enrollmentId)
    }

    private fun getEnrollmentById(enrollmentId: UUID): StudentEnrollmentResponse {

        val row = StudentEnrollmentsTable
            .innerJoin(
                StudentsTable,
                { StudentEnrollmentsTable.studentId },
                { StudentsTable.id }
            )
            .innerJoin(
                UniversitiesTable,
                { StudentEnrollmentsTable.universityId },
                { UniversitiesTable.id }
            )
            .innerJoin(
                ProgrammesTable,
                onColumn = { StudentEnrollmentsTable.programmeId },
                otherColumn = { ProgrammesTable.id },
                additionalConstraint = {
                    ProgrammesTable.universityId eq StudentEnrollmentsTable.universityId
                }
            )
            .innerJoin(
                AcademicTermsTable,
                { StudentEnrollmentsTable.academicTermId },
                { AcademicTermsTable.id }
            )
            .leftJoin(
                DepartmentsTable,
                { ProgrammesTable.departmentId },
                { DepartmentsTable.id }
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
            .singleOrNull()
            ?: throw ValidationException("Enrollment not found")

        return row.toEnrollmentResponse()
    }

    suspend fun getLecturersForStudent(studentId: UUID): List<Lecturer> = exposedTransaction {
        StudentEnrollmentsTable
            .innerJoin(LecturerTeachingAssignmentsTable) {
                (StudentEnrollmentsTable.programmeId eq LecturerTeachingAssignmentsTable.programmeId) and
                        (StudentEnrollmentsTable.yearOfStudy eq LecturerTeachingAssignmentsTable.yearOfStudy) and
                        (StudentEnrollmentsTable.academicTermId eq LecturerTeachingAssignmentsTable.academicTermId)
            }
            .innerJoin(LecturersTable) {
                LecturerTeachingAssignmentsTable.lecturerId eq LecturersTable.id
            }
            .select(LecturersTable.columns)
            .where {
                (StudentEnrollmentsTable.studentId eq studentId) and
                        (StudentEnrollmentsTable.isActive eq true) and
                        (LecturerTeachingAssignmentsTable.isActive eq true)
            }
            .map { row ->
                Lecturer(
                    id = row[LecturersTable.id],
                    email = row[LecturersTable.email],
                    name = row[LecturersTable.fullName],
                    fcmToken = row[LecturersTable.fcmToken],
                    isProfileComplete = row[LecturersTable.isProfileComplete],
                    createdAt = row[LecturersTable.createdAt],
                    updatedAt = row[LecturersTable.updatedAt]
                )
            }
            .distinctBy { it.id }
    }

    private fun ResultRow.toEnrollmentResponse(): StudentEnrollmentResponse =
        StudentEnrollmentResponse(
            enrollmentId = this[StudentEnrollmentsTable.id].toString(),
            registrationNumber = this[StudentsTable.registrationNumber],
            fullName = this[StudentsTable.fullName],
            university = UniversityResponse(
                id = this[UniversitiesTable.id].toString(),
                name = this[UniversitiesTable.name]
            ),
            programme = ProgrammeResponse(
                id = this[ProgrammesTable.id].toString(),
                name = this[ProgrammesTable.name]
            ),
            academicTerm = AcademicTermResponse(
                id = this[AcademicTermsTable.id].toString(),
                academicYear = this[AcademicTermsTable.academicYear],
                semester = this[AcademicTermsTable.semester],
                isActive = this[AcademicTermsTable.isActive]
            ),
            yearOfStudy = this[StudentEnrollmentsTable.yearOfStudy],
            enrollmentDate = this[StudentEnrollmentsTable.enrollmentDate]
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli(),
            enrollmentSource = this[StudentEnrollmentsTable.enrollmentSource].name,
            isActive = this[StudentEnrollmentsTable.isActive]
        )


}