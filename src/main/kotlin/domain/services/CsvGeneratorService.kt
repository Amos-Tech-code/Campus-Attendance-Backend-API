package com.amos_tech_code.domain.services

import com.amos_tech_code.domain.models.AttendanceReportData

interface CsvGeneratorService {

    suspend fun generateAttendanceReportCsv(
        reportData: List<AttendanceReportData>,
        unitName: String,
        unitCode: String,
        programmeName: String,
        weekRange: String,
        academicTerm: String,
        yearOfStudy: Int,
        semester: Int,
        universityName: String,
        schoolName: String = "N/A",
        departmentName: String
    ): ByteArray

}
