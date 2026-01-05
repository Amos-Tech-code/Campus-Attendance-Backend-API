package com.amos_tech_code.services

interface CloudStorageService {

    suspend fun uploadQRCode(imageBytes: ByteArray, fileName: String): String

    suspend fun deleteQRCode(fileUrl: String): Boolean

}