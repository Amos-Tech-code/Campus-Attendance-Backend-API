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
            val (startWeek, endWeek) = parseWeekRange(weekRange)
            val weeks = (startWeek..endWeek).toList()

            // Calculate total possible special and makeup sessions from the data
            val totalSpecialSessions = reportData.firstOrNull()?.specialAttendance?.size ?: 0
            val totalMakeupSessions = reportData.firstOrNull()?.makeupAttendance?.size ?: 0

            withContext(Dispatchers.IO) {
                // Write header row 1 - Column labels (matching PDF)
                writer.write("NO,REG NO,NAME,SP,MK")
                weeks.forEach { week ->
                    writer.write(",W$week")
                }
                writer.write(",% ATTE\n")

                // Write header row 2 - Totals in parentheses (matching PDF second header row)
                writer.write(",,,") // Empty for first 3 columns
                writer.write("($totalSpecialSessions),($totalMakeupSessions)") // SP and MK totals
                weeks.forEach { _ ->
                    writer.write(",") // Empty for week columns
                }
                writer.write("\n") // Empty for % ATTE column

                // Write student data
                if (reportData.isNotEmpty()) {
                    reportData.forEachIndexed { index, student ->
                        val regularAttendanceMap = student.regularAttendance.associateBy { it.weekNumber }

                        writer.write("${index + 1},")
                        writer.write("\"${student.regNo}\",")
                        writer.write("\"${student.fullName}\",")
                        writer.write("${student.specialTotal},")  // SP count
                        writer.write("${student.makeupTotal},")   // MK count

                        weeks.forEach { week ->
                            val attended = regularAttendanceMap[week]?.attended == true
                            writer.write("${if (attended) "✓" else ""},")
                        }

                        writer.write("${String.format("%.0f", student.attendancePercentage)}%\n")
                    }
                }

                // Add empty row as separator (optional, can remove if you want continuous)
                writer.write("\n")

                // ===== SIGNATURE SECTION (matching PDF structure) =====

                // CLASS REP row - First two columns merged conceptually for "CLASS REP NAME:"
                writer.write("CLASS REP NAME:,")  // First column
                writer.write(",")                  // Second column (empty for writing name)
                writer.write("SIGN:,")             // Third column
                writer.write(",")                   // SP signature
                writer.write(",")                   // MK signature
                weeks.forEach { _ ->
                    writer.write(",")               // Week signatures
                }
                writer.write("\n")                   // % ATTE column

                // LECTURER row
                writer.write("LECTURER NAME:,")
                writer.write(",")
                writer.write("SIGN:,")
                writer.write(",")
                writer.write(",")
                weeks.forEach { _ ->
                    writer.write(",")
                }
                writer.write("\n")

                // COD row
                writer.write("COD NAME:,")
                writer.write(",")
                writer.write("SIGN:,")
                writer.write(",")
                writer.write(",")
                weeks.forEach { _ ->
                    writer.write(",")
                }
                writer.write("\n")

                // DATE row - DATE in column 3, spanning rest
                writer.write(",")  // First column empty
                writer.write(",")  // Second column empty
                writer.write("DATE,")  // Third column - DATE label
                // Date field spanning remaining columns
                writer.write("____________________")
                weeks.forEach { _ ->
                    writer.write(",")
                }
                writer.write("\n")

                // Note at the bottom (without quotes, matching PDF)
                writer.write("SP = Special Class Sessions, MK = Makeup Class Sessions\n")

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
            1 to 13
        } else {
            val parts = weekRange.split("-")
            require(parts.size == 2) { "Invalid week range format. Use 'start-end' or 'ALL'" }
            parts[0].toInt() to parts[1].toInt()
        }
    }
}