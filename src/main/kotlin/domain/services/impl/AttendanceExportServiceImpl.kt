package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.requests.AttendanceExportRequest
import com.amos_tech_code.api.dtos.response.AttendanceExportRecordDto
import com.amos_tech_code.api.dtos.response.AttendanceExportResponseDto
import com.amos_tech_code.api.dtos.response.ExportsListResponseDto
import com.amos_tech_code.data.repository.AttendanceExportRepository
import com.amos_tech_code.domain.services.AttendanceExportService
import com.amos_tech_code.domain.services.CsvGeneratorService
import com.amos_tech_code.domain.services.CloudStorageService
import com.amos_tech_code.domain.services.PdfGeneratorService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.AuthorizationException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ResourceNotFoundException
import com.amos_tech_code.utils.ValidationException
import domain.models.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class AttendanceExportServiceImpl(
    private val exportRepository: AttendanceExportRepository,
    private val pdfGeneratorService: PdfGeneratorService,
    private val csvGeneratorService: CsvGeneratorService,
    private val cloudStorage: CloudStorageService
) : AttendanceExportService {

    private val logger = LoggerFactory.getLogger(AttendanceExportServiceImpl::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    override suspend fun generateAndExportAttendance(
        lecturerId: UUID,
        request: AttendanceExportRequest
    ): AttendanceExportResponseDto {

        try {
            // Validate request
            validateExportRequest(request)

            // Get or detect active academic term
            val academicTermId = if (request.semester > 0) {
                exportRepository.getActiveAcademicTerm(request.universityId)
            } else {
                exportRepository.getActiveAcademicTerm(request.universityId)
            } ?: throw IllegalArgumentException("No active academic term found")

            // Get teaching assignment
            val teachingAssignment = exportRepository.findByLecturerUnitAndProgrammeAndTerm(
                lecturerId = lecturerId,
                unitId = UUID.fromString(request.unitId),
                programmeId = UUID.fromString(request.programmeId),
                academicTermId = academicTermId
            ) ?: throw IllegalArgumentException("No teaching assignment found for this unit and term")

            // Get unit details with university and department names
            val unitWithDetails = exportRepository.getUnitWithDetails(UUID.fromString(request.unitId))
                ?: throw IllegalArgumentException("Unit not found")

            // Get programme details with department and university names
            val programmeWithDetails = exportRepository.getProgrammeWithDetails(UUID.fromString(request.programmeId))
                ?: throw IllegalArgumentException("Programme not found")

            // Fetch attendance data
            val attendanceData = exportRepository.getAttendanceData(
                teachingAssignmentId = teachingAssignment.id,
                academicTermId = academicTermId,
                weekRange = request.weekRange,
                sessionType = request.sessionType
            )

            if (attendanceData.isEmpty()) {
                throw IllegalArgumentException("No attendance data found for the specified criteria")
            }

            // Get academic term details for better reporting
            val academicTermDetails = exportRepository.getAcademicTermById(academicTermId)
                ?: throw IllegalArgumentException("Academic term not found")

            val academicTermString = "${academicTermDetails.academicYear} Semester ${academicTermDetails.semester}"

            // Generate file based on format
            val fileBytes = when (request.exportFormat) {
                ExportFormat.PDF -> pdfGeneratorService.generateAttendanceReportPdf(
                    reportData = attendanceData,
                    title = "Attendance Register - ${unitWithDetails.unitName}",
                    unitName = unitWithDetails.unitName,
                    unitCode = unitWithDetails.unitCode,
                    programmeName = programmeWithDetails.programmeName,
                    weekRange = request.weekRange,
                    academicTerm = academicTermString,
                    yearOfStudy = request.yearOfStudy,
                    semester = request.semester,
                    universityName = unitWithDetails.universityName,
                    schoolName = "",
                    departmentName = unitWithDetails.departmentName
                )
                ExportFormat.CSV -> csvGeneratorService.generateAttendanceReportCsv(
                    reportData = attendanceData,
                    unitName = unitWithDetails.unitName,
                    unitCode = unitWithDetails.unitCode,
                    programmeName = programmeWithDetails.programmeName,
                    weekRange = request.weekRange,
                    academicTerm = academicTermString,
                    yearOfStudy = request.yearOfStudy,
                    semester = request.semester,
                    universityName = unitWithDetails.universityName,
                    schoolName = "",
                    departmentName = unitWithDetails.departmentName
                )
            }

            // Generate filename
            val timestamp = LocalDateTime.now().format(dateFormatter)
            val fileName = "attendance_${unitWithDetails.unitCode}_${request.weekRange.replace("-", "_")}_$timestamp.${request.exportFormat.name.lowercase()}"

            // Upload to Cloudinary based on format
            val fileUrl = withContext(Dispatchers.IO) {
                when (request.exportFormat) {
                    ExportFormat.PDF -> cloudStorage.uploadPdfReport(
                        fileBytes = fileBytes,
                        fileName = fileName
                    )
                    ExportFormat.CSV -> cloudStorage.uploadCsvReport(
                        fileBytes = fileBytes,
                        fileName = fileName
                    )
                }
            }

            // Save export record
            val exportId = exportRepository.saveExportRecord(
                lecturerId = lecturerId,
                teachingAssignmentId = teachingAssignment.id,
                exportType = request.exportFormat,
                academicTermId = academicTermId,
                weekRange = request.weekRange,
                fileUrl = fileUrl,
                fileSize = fileBytes.size.toLong(),
                fileName = fileName,
                unitName = unitWithDetails.unitName,
                unitCode = unitWithDetails.unitCode,
                programmeName = programmeWithDetails.programmeName,
                academicTermName = academicTermString
            )

            logger.info("Attendance export generated successfully: $fileName")

            return AttendanceExportResponseDto(
                exportId = exportId.toString(),
                fileUrl = fileUrl,
                fileName = fileName,
                fileSize = fileBytes.size.toLong(),
                expiresAt = LocalDateTime.now().plusDays(7).toString(),
                message = "Attendance report generated successfully"
            )
        } catch (ex: Exception) {
            logger.error("Error while generating export", ex)
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Error while generating export: ${ex.message}")
            }
        }
    }

    override suspend fun getExportById(exportId: String, lecturerId: UUID): AttendanceExportRecordDto {
        try {
            val export = exportRepository.findExportById(exportId)
                ?: throw ResourceNotFoundException("Export not found with ID: $exportId")

            // Verify that this export belongs to the requesting lecturer
            if (export.lecturerId != lecturerId) {
                throw AuthorizationException("You don't have permission to access this export")
            }

            // Get additional details for display
            val teachingAssignment = exportRepository.getTeachingAssignmentDetails(export.teachingAssignmentId)

            return AttendanceExportRecordDto(
                exportId = export.id.toString(),
                fileName = export.fileName,
                fileUrl = export.fileUrl,
                fileSize = export.fileSize,
                exportFormat = export.exportType.name,
                weekRange = export.weekRange ?: "All Weeks",
                createdAt = export.createdAt.format(displayDateFormatter),
                expiresAt = export.expiresAt?.format(displayDateFormatter),
                unitName = teachingAssignment?.unitName ?: export.unitName,
                unitCode = teachingAssignment?.unitCode ?: export.unitCode,
                programmeName = teachingAssignment?.programmeName ?: export.programmeName,
                academicTerm = export.academicTermName
            )
        } catch (ex: Exception) {
            logger.error("Error while fetching export by ID: $exportId", ex)
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("Error while fetching export")
            }
        }
    }

    override suspend fun getExportsByLecturer(
        lecturerId: UUID,
        limit: Int,
        offset: Int
    ): ExportsListResponseDto {
        try {
            val exports = exportRepository.findExportsByLecturer(
                lecturerId = lecturerId,
                limit = limit,
                offset = offset
            )

            val total = exportRepository.countExportsByLecturer(lecturerId)

            val exportDtos = exports.map { export ->
                val teachingAssignment = exportRepository.getTeachingAssignmentDetails(export.teachingAssignmentId)

                AttendanceExportRecordDto(
                    exportId = export.id.toString(),
                    fileName = export.fileName,
                    fileUrl = export.fileUrl,
                    fileSize = export.fileSize,
                    exportFormat = export.exportType.name,
                    weekRange = export.weekRange ?: "All Weeks",
                    createdAt = export.createdAt.format(displayDateFormatter),
                    expiresAt = export.expiresAt?.format(displayDateFormatter),
                    unitName = teachingAssignment?.unitName ?: export.unitName,
                    unitCode = teachingAssignment?.unitCode ?: export.unitCode,
                    programmeName = teachingAssignment?.programmeName ?: export.programmeName,
                    academicTerm = export.academicTermName
                )
            }

            return ExportsListResponseDto(
                exports = exportDtos,
                total = total,
                limit = limit,
                offset = offset
            )
        } catch (ex: Exception) {
            logger.error("Error while fetching exports for lecturer: $lecturerId", ex)
            throw InternalServerException("Error while fetching exports")
        }
    }

    // Extension function for LocalDateTime formatting
    fun LocalDateTime.format(formatter: DateTimeFormatter): String = this.format(formatter)

    private fun validateExportRequest(request: AttendanceExportRequest) {
        if (request.universityId.isBlank()) {
            throw ValidationException("University ID is required")
        }
        if (request.programmeId.isBlank()) {
            throw ValidationException("Programme ID is required")
        }
        if (request.unitId.isBlank()) {
            throw ValidationException("Unit ID is required")
        }
        if (request.weekRange.isBlank()) {
            throw ValidationException("Week range is required")
        }
        if (request.yearOfStudy <= 0) {
            throw ValidationException("Year of study must be greater than 0")
        }
        if (request.semester !in 1..3) {
            throw ValidationException("Semester must be 1, 2 or 3")
        }
    }
}