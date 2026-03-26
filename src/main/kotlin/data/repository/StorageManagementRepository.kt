package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.admin.StorageStatsResponse
import data.database.entities.AttendanceExportsTable
import com.amos_tech_code.api.dtos.admin.StoredFileResponse
import com.amos_tech_code.data.database.entities.*
import com.amos_tech_code.data.database.utils.exposedTransaction
import data.database.entities.AttendanceSessionsTable
import domain.models.AttendanceSessionStatus
import org.jetbrains.exposed.sql.*
import utils.toIsoString
import utils.toLocalDateTimeOrThrow
import java.time.LocalDateTime
import java.util.*

class StorageManagementRepository {

    // Get all QR codes from attendance sessions
    suspend fun getAllQRCodes(): List<StoredFileResponse> = exposedTransaction {
        AttendanceSessionsTable
            .select(
                AttendanceSessionsTable.qrCodeUrl,
                AttendanceSessionsTable.createdAt,
                AttendanceSessionsTable.id,
                AttendanceSessionsTable.status
            )
            .where { AttendanceSessionsTable.qrCodeUrl.isNotNull() }
            .map { row ->
                val url = row[AttendanceSessionsTable.qrCodeUrl] ?: return@map null
                StoredFileResponse(
                    id = "qr_${row[AttendanceSessionsTable.id]}",
                    fileName = "qr_${row[AttendanceSessionsTable.id]}.png",
                    fileType = "QR_CODE",
                    fileUrl = url,
                    fileSize = null,
                    createdAt = row[AttendanceSessionsTable.createdAt].toIsoString(),
                    expiresAt = null,
                    associatedWith = "AttendanceSession",
                    associatedId = row[AttendanceSessionsTable.id].toString(),
                    isActive = row[AttendanceSessionsTable.status] != AttendanceSessionStatus.ENDED && row[AttendanceSessionsTable.status] != AttendanceSessionStatus.CANCELLED
                )
            }
            .filterNotNull()
    }

    // Get all exported reports
    suspend fun getAllExportedReports(): List<StoredFileResponse> = exposedTransaction {
        AttendanceExportsTable
            .selectAll()
            .map { row ->
                StoredFileResponse(
                    id = "export_${row[AttendanceExportsTable.id]}",
                    fileName = row[AttendanceExportsTable.fileName],
                    fileType = row[AttendanceExportsTable.exportType],
                    fileUrl = row[AttendanceExportsTable.fileUrl],
                    fileSize = row[AttendanceExportsTable.fileSize],
                    createdAt = row[AttendanceExportsTable.createdAt].toIsoString(),
                    expiresAt = row[AttendanceExportsTable.expiresAt].toIsoString(),
                    associatedWith = "AttendanceExport",
                    associatedId = row[AttendanceExportsTable.id].toString(),
                    isActive = row[AttendanceExportsTable.expiresAt].isAfter(LocalDateTime.now())
                )
            }
    }

    // Get all stored files
    suspend fun getAllStoredFiles(): List<StoredFileResponse> = exposedTransaction {
        val qrCodes = getAllQRCodes()
        val reports = getAllExportedReports()
        qrCodes + reports
    }

    // Get storage statistics
    suspend fun getStorageStats(): StorageStatsResponse = exposedTransaction {
        val qrCodes = getAllQRCodes()
        val reports = getAllExportedReports()

        val allFiles = qrCodes + reports
        val totalSizeBytes = reports.sumOf { it.fileSize ?: 0L }

        val orphaned = allFiles.filter { !it.isActive }.count()
        val expired = reports.filter {
            it.expiresAt?.let {
                try {
                    LocalDateTime.parse(it).isBefore(LocalDateTime.now())
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }.count()

        StorageStatsResponse(
            totalFiles = allFiles.size.toLong(),
            totalQRCodes = qrCodes.size.toLong(),
            totalReports = reports.size.toLong(),
            totalSizeBytes = totalSizeBytes,
            orphanedFiles = orphaned.toLong(),
            expiredFiles = expired.toLong(),
            storageUsedMB = totalSizeBytes / (1024.0 * 1024.0),
            byType = mapOf(
                "QR_CODE" to qrCodes.size.toLong(),
                "PDF" to reports.count { it.fileType == "PDF" }.toLong(),
                "CSV" to reports.count { it.fileType == "CSV" }.toLong(),
                "EXCEL" to reports.count { it.fileType == "EXCEL" }.toLong()
            )
        )
    }

    // Get orphaned files (files associated with ended sessions or expired exports)
    suspend fun getOrphanedFiles(): List<StoredFileResponse> = exposedTransaction {
        getAllStoredFiles().filter { !it.isActive }
    }

    // Get expired files
    suspend fun getExpiredFiles(): List<StoredFileResponse> = exposedTransaction {
        getAllStoredFiles().filter {
            it.expiresAt?.toLocalDateTimeOrThrow()?.isBefore(LocalDateTime.now()) ?: false
        }
    }

    // Mark export as deleted (soft delete)
    suspend fun markExportAsDeleted(exportId: UUID): Boolean = exposedTransaction {
        AttendanceExportsTable.update({ AttendanceExportsTable.id eq exportId }) {
            it[expiresAt] = LocalDateTime.now().minusDays(1) // Mark as expired
        } > 0
    }

    // Get file by URL
    suspend fun findFileByUrl(fileUrl: String): StoredFileResponse? = exposedTransaction {
        getAllStoredFiles().find { it.fileUrl == fileUrl }
    }
}