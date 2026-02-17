package com.amos_tech_code.domain.services

interface CloudStorageService {

    suspend fun uploadQRCode(imageBytes: ByteArray, fileName: String): String

    suspend fun deleteQRCode(fileUrl: String): Boolean

    suspend fun uploadPdfReport(fileBytes: ByteArray, fileName: String): String

    suspend fun uploadCsvReport(fileBytes: ByteArray, fileName: String): String

}