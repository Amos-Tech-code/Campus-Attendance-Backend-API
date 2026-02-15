package com.amos_tech_code.domain.services

import domain.models.ExportFormat

interface CloudStorageService {

    suspend fun uploadQRCode(imageBytes: ByteArray, fileName: String): String

    suspend fun deleteQRCode(fileUrl: String): Boolean

    suspend fun uploadAttendanceReport(
        fileBytes: ByteArray,
        fileName: String,
        format: ExportFormat
    ): String

}