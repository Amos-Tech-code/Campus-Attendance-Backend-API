package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.domain.models.AttendanceReportData
import com.amos_tech_code.domain.services.PdfGeneratorService
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

class PdfGeneratorServiceImpl : PdfGeneratorService {

    private val logger = LoggerFactory.getLogger(PdfGeneratorServiceImpl::class.java)

    // Constants for layout
    private val MARGIN = 36f // 0.5 inch margin

    // Font sizes
    private val TITLE_FONT_SIZE = 14f
    private val HEADER_FONT_SIZE = 11f
    private val TABLE_HEADER_FONT_SIZE = 9f
    private val TABLE_CONTENT_FONT_SIZE = 8f

    override suspend fun generateAttendanceReportPdf(
        reportData: List<AttendanceReportData>,
        title: String,
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

        try {
            // Initialize PDF document
            val writer = PdfWriter(baos)
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4.rotate())
            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN)

            // Create fonts
            val boldFont = PdfFontFactory.createFont("Helvetica-Bold")
            val regularFont = PdfFontFactory.createFont("Helvetica")

            // Add header with institution details
            addHeader(document, boldFont, regularFont, universityName, schoolName, departmentName,
                programmeName, unitCode, unitName, academicTerm, yearOfStudy, semester)

            // Add spacing
            document.add(Paragraph("\n"))

            // Parse week range to determine number of weeks
            val (startWeek, endWeek) = parseWeekRange(weekRange)
            val weeks = (startWeek..endWeek).toList()

            // Create and add the attendance table with ACTUAL student data only
            val table = createAttendanceTable(reportData, weeks, boldFont, regularFont)
            document.add(table)

            // Add signature footer
            document.add(Paragraph("\n"))
            addSignatureFooter(document, boldFont, regularFont)

            document.close()

        } catch (e: Exception) {
            logger.error("Failed to generate PDF: ${e.message}", e)
            throw Exception("Failed to generate PDF: ${e.message}")
        }

        return baos.toByteArray()
    }

    private fun addHeader(
        document: Document,
        boldFont: PdfFont,
        regularFont: PdfFont,
        universityName: String,
        schoolName: String,
        departmentName: String,
        programmeName: String,
        unitCode: String,
        unitName: String,
        academicTerm: String,
        yearOfStudy: Int,
        semester: Int
    ) {
        // University Name
        val university = Paragraph(universityName.uppercase())
            .setFont(boldFont)
            .setFontSize(TITLE_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(5f)
        document.add(university)

        // School Name
        val school = Paragraph(schoolName.uppercase())
            .setFont(boldFont)
            .setFontSize(HEADER_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(2f)
        document.add(school)

        // Department Name
        val department = Paragraph("DEPARTMENT OF ${departmentName.uppercase()}")
            .setFont(regularFont)
            .setFontSize(HEADER_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f)
        document.add(department)

        // Course Details
        val semesterText = when (semester) {
            1 -> "SEMESTER 1"
            2 -> "SEMESTER 2"
            else -> "SEMESTER $semester"
        }
        val courseDetails = Paragraph("$programmeName YEAR $yearOfStudy $academicTerm : $unitCode : $unitName")
            .setFont(boldFont)
            .setFontSize(HEADER_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(2f)
        document.add(courseDetails)

        // Semester
        val semesterInfo = Paragraph(semesterText)
            .setFont(regularFont)
            .setFontSize(HEADER_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f)
        document.add(semesterInfo)
    }

    private fun createAttendanceTable(
        reportData: List<AttendanceReportData>,
        weeks: List<Int>,
        boldFont: PdfFont,
        regularFont: PdfFont
    ): Table {
        val weekCount = weeks.size

        // Create table with column widths
        // Columns: NOS, REG NO, NAMES, Regular Class, Makeup Class, WK1-WK13, % ATTE
        val columnWidths = floatArrayOf(
            3f,    // NOS
            12f,   // REG NO
            20f,   // NAMES
            8f,    // Special Class
            12f,   // Makeup Class
            *FloatArray(weekCount) { 5f }, // WK1 to WK13
            8f     // % ATTE
        )

        val table = Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth()
        table.setMarginTop(10f)

        // Create main headers
        val headers = mutableListOf(
            "NOS", "REG NO", "NAMES", "Special Class", "Makeup Class"
        )
        weeks.forEach { headers.add("WK$it") }
        headers.add("% ATTE")

        // Add headers to table
        headers.forEachIndexed { index, header ->
            val cell = Cell()
                .add(Paragraph(header).setFont(boldFont).setFontSize(TABLE_HEADER_FONT_SIZE))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5f)

            // For Regular Class and Makeup Class, we'll add sub-headers in next row
            if (index == 3) { // Regular Class
                cell.setBorderBottom(null)
            } else if (index == 4) { // Makeup Class
                cell.setBorderBottom(null)
            }

            table.addCell(cell)
        }

        // Add sub-header row for Regular Class and Makeup Class
        // Add empty cells for first 3 columns
        for (i in 1..3) {
            table.addCell(Cell().setBorderTop(null).setBorderBottom(null).setPadding(3f))
        }

        // Special Class sub-header
        val regularClassSubCell = Cell(1, 1)
            .add(Paragraph("(✓)").setFont(regularFont).setFontSize(TABLE_HEADER_FONT_SIZE - 1))
            .setTextAlignment(TextAlignment.CENTER)
            .setBorderTop(null)
            .setPadding(3f)
        table.addCell(regularClassSubCell)

        // Makeup Class sub-header
        val makeupClassSubCell = Cell(1, 1)
            .add(Paragraph("Indicate Date").setFont(regularFont).setFontSize(TABLE_HEADER_FONT_SIZE - 1))
            .setTextAlignment(TextAlignment.CENTER)
            .setBorderTop(null)
            .setPadding(3f)
        table.addCell(makeupClassSubCell)

        // Add empty cells for week headers (since we already added them)
        for (i in 1..weekCount) {
            table.addCell(Cell().setBorderTop(null).setPadding(3f))
        }

        // Add empty cell for % ATTE header
        table.addCell(Cell().setBorderTop(null).setPadding(3f))

        // Add ONLY actual student data - NO empty rows
        if (reportData.isNotEmpty()) {
            reportData.forEachIndexed { index, student ->
                // Create a map of week number to attendance for quick lookup
                val attendanceMap = student.weeklyAttendance.associateBy { it.weekNumber }

                // NOS
                table.addCell(createDataCell((index + 1).toString(), regularFont))

                // REG NO
                table.addCell(createDataCell(student.regNo, regularFont))

                // NAMES
                table.addCell(createDataCell(student.fullName, regularFont))

                // Regular Class (✓) - we can mark this based on some criteria or leave empty
                // For now, leave empty as per the template
                table.addCell(createDataCell("", regularFont))

                // Makeup Class (leave empty for date)
                table.addCell(createDataCell("", regularFont))

                // Weekly attendance cells (WK1 to WK13) - show ✓ for attended sessions
                weeks.forEach { week ->
                    val attendance = attendanceMap[week]
                    // A student might have multiple sessions in a week, but we show ✓ if they attended at least one
                    val attended = attendance?.attended == true
                    val mark = if (attended) "✓" else ""
                    table.addCell(createDataCell(mark, regularFont,
                        if (attended) ColorConstants.GREEN else null))
                }

                // % ATTE - calculate based on actual attendance
                val percentage = String.format("%.1f", student.attendancePercentage)
                table.addCell(createDataCell("$percentage%", regularFont))
            }
        } else {
            // If no students, show a message row
            val cell = Cell(1, headers.size)
                .add(Paragraph("No students enrolled for this unit").setFont(regularFont))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(10f)
            table.addCell(cell)
        }

        return table
    }

    private fun createDataCell(text: String, font: PdfFont, backgroundColor: com.itextpdf.kernel.colors.Color? = null): Cell {
        val cell = Cell()
            .add(Paragraph(text).setFont(font).setFontSize(TABLE_CONTENT_FONT_SIZE))
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(5f)
            .setHeight(25f) // Set height for signature space (approx 0.88cm)

        backgroundColor?.let {
            cell.setBackgroundColor(it, 0.1f)
        }

        return cell
    }

    private fun addSignatureFooter(
        document: Document,
        boldFont: PdfFont,
        regularFont: PdfFont
    ) {
        val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 30f, 40f))).useAllAvailableWidth()
        footerTable.setMarginTop(20f)

        // First row: Class Rep
        footerTable.addCell(createFooterCell("CLASS REP NAME:", boldFont))
        footerTable.addCell(createFooterCell("SIGN:", boldFont))
        footerTable.addCell(createFooterCell("", regularFont))

        // Second row: Lecturer
        footerTable.addCell(createFooterCell("LECTURERS NAME:", boldFont))
        footerTable.addCell(createFooterCell("SIGN:", boldFont))
        footerTable.addCell(createFooterCell("", regularFont))

        // Third row: COD
        footerTable.addCell(createFooterCell("COD NAME:", boldFont))
        footerTable.addCell(createFooterCell("SIGN:", boldFont))
        footerTable.addCell(createFooterCell("", regularFont))

        // Fourth row: Date
        footerTable.addCell(createFooterCell("DATE:", boldFont))
        footerTable.addCell(createFooterCell("", regularFont))
        footerTable.addCell(createFooterCell("", regularFont))

        document.add(footerTable)
    }

    private fun createFooterCell(text: String, font: PdfFont): Cell {
        return Cell()
            .add(Paragraph(text).setFont(font).setFontSize(9f))
            .setBorder(null)
            .setPadding(3f)
            .setTextAlignment(if (text.endsWith(":")) TextAlignment.RIGHT else TextAlignment.LEFT)
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