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
import com.itextpdf.layout.borders.SolidBorder
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

    // Optimized constants for A4 Landscape to fit 30+ rows
    private val MARGIN = 20f
    private val ROW_HEIGHT = 18f

    // Font sizes
    private val TITLE_FONT_SIZE = 13f
    private val HEADER_FONT_SIZE = 10f
    private val TABLE_HEADER_FONT_SIZE = 8f
    private val TABLE_CONTENT_FONT_SIZE = 7f

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
            val writer = PdfWriter(baos)
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4.rotate())
            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN)

            val boldFont = PdfFontFactory.createFont("Helvetica-Bold")
            val regularFont = PdfFontFactory.createFont("Helvetica")

            addCompactHeader(document, boldFont, regularFont, universityName, schoolName, departmentName,
                programmeName, unitCode, unitName, academicTerm, yearOfStudy, semester)

            val (startWeek, endWeek) = parseWeekRange(weekRange)
            val weeks = (startWeek..endWeek).toList()

            // Calculate total possible special and makeup sessions from the data
            val totalSpecialSessions = reportData.firstOrNull()?.specialAttendance?.size ?: 0
            val totalMakeupSessions = reportData.firstOrNull()?.makeupAttendance?.size ?: 0

            val completeTable = createAttendanceTableWithSignature(
                reportData, weeks, boldFont, regularFont,
                totalSpecialSessions, totalMakeupSessions
            )
            document.add(completeTable)

            document.close()

        } catch (e: Exception) {
            logger.error("Failed to generate PDF: ${e.message}", e)
            throw Exception("Failed to generate PDF: ${e.message}")
        }

        return baos.toByteArray()
    }

    private fun addCompactHeader(
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
        val university = Paragraph(universityName.uppercase())
            .setFont(boldFont)
            .setFontSize(TITLE_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(3f)
        document.add(university)

        val school = Paragraph(if(schoolName.isNotBlank())
            "SCHOOL OF ${schoolName.uppercase()}"
        else "SCHOOL OF _________________")
            .setFont(boldFont)
            .setFontSize(HEADER_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(2f)
        document.add(school)

        val department = Paragraph("DEPARTMENT OF ${departmentName.uppercase()}")
            .setFont(regularFont)
            .setFontSize(HEADER_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(5f)
        document.add(department)

        val semesterText = when (semester) {
            1 -> "SEM 1"
            2 -> "SEM 2"
            else -> "SEM $semester"
        }

        val courseDetails = Paragraph("$programmeName Y$yearOfStudy $academicTerm : $unitCode")
            .setFont(boldFont)
            .setFontSize(HEADER_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(2f)
        document.add(courseDetails)

        val unitLine = Paragraph("$unitName | $semesterText")
            .setFont(regularFont)
            .setFontSize(HEADER_FONT_SIZE - 1)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(8f)
        document.add(unitLine)
    }

    private fun createAttendanceTableWithSignature(
        reportData: List<AttendanceReportData>,
        weeks: List<Int>,
        boldFont: PdfFont,
        regularFont: PdfFont,
        totalSpecialSessions: Int,
        totalMakeupSessions: Int
    ): Table {
        val weekCount = weeks.size

        // Column definitions with SP and MK totals
        val columnCount = 5 + weekCount + 1
        val columnWidths = floatArrayOf(
            2.5f,  // NO
            9f,    // REG NO
            16f,   // NAME
            4f,    // SP Total
            4f,    // MK Total
            *FloatArray(weekCount) { 3.5f }, // W1-W13 (Regular attendance)
            4f     // % ATTE
        )

        val table = Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth()
        table.setMarginTop(5f)

        // ===== HEADER ROWS (Single unified header) =====

        // First header row
        listOf("NO", "REG NO", "NAME", "SP", "MK").forEach { header ->
            val cell = Cell(1, 1)
                .add(Paragraph(header).setFont(boldFont).setFontSize(TABLE_HEADER_FONT_SIZE))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(2f)
            table.addCell(cell)
        }

        // Week headers
        weeks.forEach { week ->
            val cell = Cell(1, 1)
                .add(Paragraph("W$week").setFont(boldFont).setFontSize(TABLE_HEADER_FONT_SIZE - 0.5f))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(2f)
            table.addCell(cell)
        }

        // % header
        val atteHeader = Cell(1, 1)
            .add(Paragraph("%").setFont(boldFont).setFontSize(TABLE_HEADER_FONT_SIZE))
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setPadding(2f)
        table.addCell(atteHeader)

        // Second row for SP and MK totals
        // Empty cells for first 3 columns (with borders)
        for (i in 1..3) {
            table.addCell(Cell()
                .setBorder(SolidBorder(0.5f))
                .setPadding(2f)
                .setHeight(ROW_HEIGHT))
        }

        // SP total (with borders)
        table.addCell(Cell()
            .add(Paragraph("($totalSpecialSessions)").setFont(regularFont).setFontSize(TABLE_HEADER_FONT_SIZE - 1))
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(SolidBorder(0.5f))
            .setPadding(2f)
            .setHeight(ROW_HEIGHT))

        // MK total (with borders)
        table.addCell(Cell()
            .add(Paragraph("($totalMakeupSessions)").setFont(regularFont).setFontSize(TABLE_HEADER_FONT_SIZE - 1))
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(SolidBorder(0.5f))
            .setPadding(2f)
            .setHeight(ROW_HEIGHT))

        // Empty cells for week columns and % ATTE (with borders)
        for (i in 1..weekCount + 1) {
            table.addCell(Cell()
                .setBorder(SolidBorder(0.5f))
                .setPadding(2f)
                .setHeight(ROW_HEIGHT))
        }

        // ===== STUDENT DATA ROWS =====
        if (reportData.isNotEmpty()) {
            reportData.forEachIndexed { index, student ->
                // Create maps for quick lookup by week number (REGULAR sessions only)
                val regularAttendanceMap = student.regularAttendance.associateBy { it.weekNumber }

                // NO
                table.addCell(createDataCell((index + 1).toString(), regularFont))

                // REG NO
                table.addCell(createDataCell(student.regNo, regularFont))

                // NAME
                val shortName = if (student.fullName.length > 20)
                    student.fullName.substring(0, 18) + ".."
                else student.fullName
                table.addCell(createDataCell(shortName, regularFont))

                // SP (special sessions attended count)
                table.addCell(createDataCell(student.specialTotal.toString(), regularFont, ColorConstants.BLUE))

                // MK (makeup sessions attended count)
                table.addCell(createDataCell(student.makeupTotal.toString(), regularFont, ColorConstants.ORANGE))

                // Weekly attendance (REGULAR sessions only)
                weeks.forEach { week ->
                    val attendance = regularAttendanceMap[week]
                    val attended = attendance?.attended == true
                    val mark = if (attended) "✓" else ""
                    table.addCell(createDataCell(mark, regularFont,
                        if (attended) ColorConstants.GREEN else null))
                }

                // % ATTE (overall attendance percentage)
                val percentage = String.format("%.0f", student.attendancePercentage)
                table.addCell(createDataCell("$percentage%", regularFont))
            }
        }

        // ===== SIGNATURE SECTION =====

        // CLASS REP row
        // First two columns combined for "CLASS REP NAME:"
        val classRepCell = Cell(1, 2)
            .add(Paragraph("CLASS REP NAME:").setFont(boldFont).setFontSize(8f).setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBorder(SolidBorder(0.5f))
            .setPadding(2f)
            .setHeight(ROW_HEIGHT)
        table.addCell(classRepCell)

        // Third column - SIGN
        table.addCell(createSignatureFieldCell("SIGN:", regularFont))

        // SP signature
        table.addCell(createSignatureFieldCell("", regularFont))
        // MK signature
        table.addCell(createSignatureFieldCell("", regularFont))
        // Week signatures
        weeks.forEach { _ ->
            table.addCell(createSignatureFieldCell("", regularFont))
        }
        // % ATTE column - Empty
        table.addCell(createSignatureFieldCell("", regularFont))

        // LECTURER row
        // First two columns combined for "LECTURER NAME:"
        val lecturerCell = Cell(1, 2)
            .add(Paragraph("LECTURER NAME:").setFont(boldFont).setFontSize(8f).setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBorder(SolidBorder(0.5f))
            .setPadding(2f)
            .setHeight(ROW_HEIGHT)
        table.addCell(lecturerCell)

        // Third column - SIGN
        table.addCell(createSignatureFieldCell("SIGN:", regularFont))

        // SP signature
        table.addCell(createSignatureFieldCell("", regularFont))
        // MK signature
        table.addCell(createSignatureFieldCell("", regularFont))
        weeks.forEach { _ ->
            table.addCell(createSignatureFieldCell("", regularFont))
        }
        table.addCell(createSignatureFieldCell("", regularFont))

        // COD row
        // First two columns combined for "COD NAME:"
        val codCell = Cell(1, 2)
            .add(Paragraph("COD NAME:").setFont(boldFont).setFontSize(8f).setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBorder(SolidBorder(0.5f))
            .setPadding(2f)
            .setHeight(ROW_HEIGHT)
        table.addCell(codCell)

        // Third column - SIGN
        table.addCell(createSignatureFieldCell("SIGN:", regularFont))

        // SP signature
        table.addCell(createSignatureFieldCell("", regularFont))
        // MK signature
        table.addCell(createSignatureFieldCell("", regularFont))
        weeks.forEach { _ ->
            table.addCell(createSignatureFieldCell("", regularFont))
        }
        table.addCell(createSignatureFieldCell("", regularFont))

        // DATE row
        // First two columns - Empty (since label goes in third column)
        val emptyCell1 = Cell(1, 2)
            .setBorder(SolidBorder(0.5f))
            .setPadding(2f)
            .setHeight(ROW_HEIGHT)
        table.addCell(emptyCell1)

        // Third column - DATE label
        table.addCell(createSignatureLabelCell("DATE", boldFont))

        // For the remaining columns (from column 4 to the last column)
        val dateSpanCell = Cell(1, columnCount - 3)  // -3 because we've used first 3 columns
            .add(Paragraph("____________________").setFont(regularFont).setFontSize(8f))
            .setBorder(SolidBorder(1f))
            .setPadding(4f)
            .setTextAlignment(TextAlignment.LEFT)
        table.addCell(dateSpanCell)

        // ===== NOTE SECTION (at the bottom) =====
        val noteCell = Cell(1, columnCount)
            .add(Paragraph("SP = Special Class Sessions, MK = Makeup Class Sessions")
                .setFont(regularFont)
                .setFontSize(7f)
                .setItalic())
            .setBorder(null)
            .setPaddingTop(5f)
            .setTextAlignment(TextAlignment.CENTER)
        table.addCell(noteCell)

        return table
    }

    private fun createDataCell(text: String, font: PdfFont, backgroundColor: com.itextpdf.kernel.colors.Color? = null): Cell {
        val cell = Cell()
            .add(Paragraph(text).setFont(font).setFontSize(TABLE_CONTENT_FONT_SIZE))
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(2f)
            .setHeight(ROW_HEIGHT)

        backgroundColor?.let {
            cell.setBackgroundColor(it, 0.1f)
        }

        return cell
    }

    private fun createSignatureLabelCell(text: String, font: PdfFont): Cell {
        return Cell()
            .add(Paragraph(text).setFont(font).setFontSize(8f).setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(2f)
            .setHeight(ROW_HEIGHT)
    }

    private fun createSignatureFieldCell(text: String, font: PdfFont): Cell {
        return Cell()
            .add(Paragraph(text).setFont(font).setFontSize(7f))
            .setTextAlignment(TextAlignment.LEFT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(2f)
            .setHeight(ROW_HEIGHT)
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