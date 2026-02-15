package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.domain.models.AttendanceReportData
import com.amos_tech_code.domain.services.CsvGeneratorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class CsvGeneratorServiceImpl : CsvGeneratorService {

    private val logger = LoggerFactory.getLogger(CsvGeneratorServiceImpl::class.java)

    override suspend fun generateAttendanceReportCsv(
        reportData: List<AttendanceReportData>,
        unitName: String,
        unitCode: String,
        programmeName: String,
        weekRange: String,
        academicTerm: String,
        yearOfStudy: Int,
        semester: Int,
        universityName: String,
        schoolName: String,
        departmentName: String
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = OutputStreamWriter(baos, StandardCharsets.UTF_8)

        try {
            // Parse week range
            val (startWeek, endWeek) = parseWeekRange(weekRange)
            val weeks = (startWeek..endWeek).toList()

            withContext(Dispatchers.IO) {
                // Write institution header as comments
                writer.write("# ${universityName.uppercase()}\n")
                writer.write("# ${schoolName.uppercase()}\n")
                writer.write("# ${departmentName.uppercase()}\n")
                writer.write("#\n")

                // Write course details
                val semesterText = when (semester) {
                    1 -> "SEMESTER 1"
                    2 -> "SEMESTER 2"
                    else -> "SEMESTER $semester"
                }
                writer.write("# $programmeName YEAR $yearOfStudy $academicTerm : $unitCode : $unitName\n")
                writer.write("# $semesterText $academicTerm\n")
                writer.write("# WEEKS $startWeek-$endWeek\n")
                writer.write("# Generated: ${java.time.LocalDateTime.now()}\n")
                writer.write("#\n")

                // Write CSV headers
                writer.write("NOS,REG NO,NAMES,Regular Class,Makeup Class")
                weeks.forEach { week ->
                    writer.write(",WK$week")
                }
                writer.write(",% ATTE\n")

                // Write ONLY actual student data - NO empty rows
                if (reportData.isNotEmpty()) {
                    reportData.forEachIndexed { index, student ->
                        // Create map for quick lookup
                        val attendanceMap = student.weeklyAttendance.associateBy { it.weekNumber }

                        writer.write("${index + 1},")
                        writer.write("\"${student.regNo}\",")
                        writer.write("\"${student.fullName}\",")
                        writer.write(",") // Regular Class (empty)
                        writer.write(",") // Makeup Class (empty)

                        weeks.forEach { week ->
                            val attended = attendanceMap[week]?.attended == true
                            writer.write(",${if (attended) "✓" else ""}")
                        }

                        writer.write(",${String.format("%.1f", student.attendancePercentage)}%\n")
                    }
                } else {
                    // If no students, add a comment
                    writer.write("# No students enrolled for this unit\n")
                }

                // Add signature section as comments
                writer.write("\n# SIGNATURE SECTION\n")
                writer.write("# CLASS REP NAME: , SIGN: \n")
                writer.write("# LECTURERS NAME: , SIGN: \n")
                writer.write("# COD NAME: , SIGN: \n")
                writer.write("# DATE: \n")

                writer.flush()
            }

        } catch (e: Exception) {
            logger.error("Failed to generate CSV: ${e.message}", e)
            throw Exception("Failed to generate CSV: ${e.message}")
        }

        return baos.toByteArray()
    }

    private fun parseWeekRange(weekRange: String): Pair<Int, Int> {
        return if (weekRange.equals("ALL", ignoreCase = true)) {
            1 to 13 // Default to 13 weeks for a semester
        } else {
            val parts = weekRange.split("-")
            require(parts.size == 2) { "Invalid week range format. Use 'start-end' or 'ALL'" }
            parts[0].toInt() to parts[1].toInt()
        }
    }
}