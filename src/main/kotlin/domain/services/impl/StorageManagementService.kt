package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.api.dtos.admin.*
import com.amos_tech_code.data.repository.StorageManagementRepository
import com.amos_tech_code.domain.services.CloudStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import utils.toLocalDateTimeOrThrow
import utils.toUUID
import java.time.LocalDateTime
import java.util.*

class StorageManagementService(
    private val repository: StorageManagementRepository,
    private val cloudStorageService: CloudStorageService
) {

    private val logger = LoggerFactory.getLogger(StorageManagementService::class.java)

    suspend fun getAllFiles(): List<StoredFileResponse> = withContext(Dispatchers.IO) {
        repository.getAllStoredFiles()
    }

    suspend fun getStorageStats(): StorageStatsResponse = withContext(Dispatchers.IO) {
        repository.getStorageStats()
    }

    suspend fun getOrphanedFiles(): List<StoredFileResponse> = withContext(Dispatchers.IO) {
        repository.getOrphanedFiles()
    }

    suspend fun getExpiredFiles(): List<StoredFileResponse> = withContext(Dispatchers.IO) {
        repository.getExpiredFiles()
    }

    suspend fun cleanupFiles(request: CleanupRequest): CleanupResultResponse = withContext(Dispatchers.IO) {
        val filesToDelete = when (request.cleanupType) {
            "orphaned" -> repository.getOrphanedFiles()
            "expired" -> repository.getExpiredFiles()
            "all" -> repository.getAllStoredFiles().filter { !it.isActive || it.expiresAt?.toLocalDateTimeOrThrow()?.isBefore(LocalDateTime.now()) ?: false }
            "manual" -> {
                request.fileIds?.let { ids ->
                    repository.getAllStoredFiles().filter { it.id in ids }
                } ?: emptyList()
            }
            else -> emptyList()
        }

        // Apply age filter if specified
        val filteredFiles = if (request.olderThanDays != null && request.cleanupType != "manual") {
            val cutoffDate = LocalDateTime.now().minusDays(request.olderThanDays.toLong())
            filesToDelete.filter { it.createdAt?.toLocalDateTimeOrThrow()?.isBefore(cutoffDate) ?: true }
        } else {
            filesToDelete
        }

        if (filteredFiles.isEmpty()) {
            return@withContext CleanupResultResponse(
                deletedCount = 0,
                failedCount = 0,
                freedSpaceMB = 0.0,
                details = emptyList()
            )
        }

        logger.info("Starting cleanup of ${filteredFiles.size} files")

        val results = mutableListOf<CleanupDetail>()
        var deletedCount = 0
        var failedCount = 0
        var freedSpaceBytes = 0L

        coroutineScope {
            val deletionJobs = filteredFiles.map { file ->
                async {
                    try {
                        val deleted = cloudStorageService.deleteQRCode(file.fileUrl)
                        if (deleted) {
                            // If it's an export, also mark it as deleted in DB
                            if (file.associatedWith == "AttendanceExport" && file.associatedId != null) {
                                repository.markExportAsDeleted(file.associatedId.toUUID())
                            }
                            deletedCount++
                            freedSpaceBytes += file.fileSize ?: 0L
                            CleanupDetail(
                                fileName = file.fileName,
                                fileType = file.fileType,
                                fileUrl = file.fileUrl,
                                reason = "Cleanup requested",
                                success = true,
                                error = null
                            )
                        } else {
                            failedCount++
                            CleanupDetail(
                                fileName = file.fileName,
                                fileType = file.fileType,
                                fileUrl = file.fileUrl,
                                reason = "Cleanup requested",
                                success = false,
                                error = "Delete operation returned false"
                            )
                        }
                    } catch (e: Exception) {
                        failedCount++
                        logger.error("Failed to delete file: ${file.fileName}", e)
                        CleanupDetail(
                            fileName = file.fileName,
                            fileType = file.fileType,
                            fileUrl = file.fileUrl,
                            reason = "Cleanup requested",
                            success = false,
                            error = e.message
                        )
                    }
                }
            }
            results.addAll(deletionJobs.awaitAll())
        }

        CleanupResultResponse(
            deletedCount = deletedCount,
            failedCount = failedCount,
            freedSpaceMB = freedSpaceBytes / (1024.0 * 1024.0),
            details = results
        )
    }

    suspend fun deleteSingleFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val file = repository.getAllStoredFiles().find { it.id == fileId }
            ?: return@withContext false

        try {
            val deleted = cloudStorageService.deleteQRCode(file.fileUrl)
            if (deleted && file.associatedWith == "AttendanceExport" && file.associatedId != null) {
                repository.markExportAsDeleted(file.associatedId.toUUID())
            }
            deleted
        } catch (e: Exception) {
            logger.error("Failed to delete file: ${file.fileName}", e)
            false
        }
    }
}