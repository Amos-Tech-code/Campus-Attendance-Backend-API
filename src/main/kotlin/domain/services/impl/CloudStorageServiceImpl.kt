package domain.services.impl

import com.amos_tech_code.config.AppConfig
import com.amos_tech_code.domain.services.CloudStorageService
import com.amos_tech_code.utils.InternalServerException
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.slf4j.LoggerFactory

class CloudStorageServiceImpl : CloudStorageService {

    private val logger = LoggerFactory.getLogger(CloudStorageServiceImpl::class.java)

    private val cloudinary: Cloudinary

    init {
        val config = mapOf(
            "cloud_name" to AppConfig.CLOUD_NAME,
            "api_key" to AppConfig.API_KEY,
            "api_secret" to AppConfig.API_SECRET,
            "secure" to true
        )
        cloudinary = Cloudinary(config)
        logger.info("Cloudinary service initialized for cloud: ${AppConfig.CLOUD_NAME}")
    }

    override suspend fun uploadQRCode(imageBytes: ByteArray, fileName: String): String {
        validateImageInput(imageBytes, fileName)

        return try {
            logger.debug("Uploading QR code to Cloudinary: $fileName (${imageBytes.size} bytes)")

            val uploadResult = cloudinary.uploader().upload(
                imageBytes,
                ObjectUtils.asMap(
                    "public_id", getFileId(fileName),
                    "folder", "qr_codes",
                    "resource_type", "image",
                    "format", "png",
                    "quality", "auto:good", // Optimize for web/mobile
                    "fetch_format", "auto",
                    "overwrite", true,
                    "invalidate", true // CDN cache invalidation
                )
            )

            val secureUrl = uploadResult["secure_url"] as? String
                ?: throw InternalServerException("Cloudinary did not return secure URL")

            logger.info ("QR code uploaded successfully: $secureUrl")
            secureUrl

        } catch (e: Exception) {
            logger.error("Failed to upload QR code to Cloudinary: ${e.message}")
            throw InternalServerException("Failed to upload QR code: ${e.message}")
        }
    }

    override suspend fun deleteQRCode(fileUrl: String): Boolean {
        if (fileUrl.isBlank()) {
            logger.warn ( "Attempted to delete QR code with blank URL" )
            return false
        }

        return try {
            logger.debug("Deleting QR code from Cloudinary: $fileUrl" )

            // Extract public ID from Cloudinary URL
            val publicId = extractPublicIdFromUrl(fileUrl)

            val deleteResult = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
                )
            )

            val isDeleted = deleteResult["result"] as? String == "ok"

            if (isDeleted) {
                logger.info ( "QR code deleted successfully: $publicId" )
            } else {
                logger.warn ( "Failed to delete QR code: $publicId" )
            }

            isDeleted

        } catch (e: Exception) {
            logger.error("Failed to delete QR code from Cloudinary: ${e.message}")
            false // Return false instead of throwing to allow session cleanup to continue
        }
    }

    override suspend fun uploadPdfReport(fileBytes: ByteArray, fileName: String): String {
        validateFileInput(fileBytes, fileName, "pdf")

        return try {
            logger.debug("Uploading PDF report to Cloudinary: $fileName (${fileBytes.size} bytes)")

            val baseName = fileName.substringBeforeLast(".")

            val uploadResult = cloudinary.uploader().upload(
                fileBytes,
                ObjectUtils.asMap(
                    "public_id", baseName,
                    "folder", "attendance_reports/pdf",
                    "resource_type", "image",  // PDFs work best as image type
                    "type", "upload",          // Public access
                    "format", "pdf",           // Explicit format
                    "overwrite", false,
                    "invalidate", true
                )
            )

            // Construct public URL with proper format
            val cloudName = AppConfig.CLOUD_NAME
            val publicId = uploadResult["public_id"] as? String ?: "attendance_reports/pdf/$baseName"
            val finalUrl = "https://res.cloudinary.com/$cloudName/image/upload/$publicId.pdf"

            logger.info("PDF report uploaded successfully: $finalUrl")
            finalUrl

        } catch (e: Exception) {
            logger.error("Failed to upload PDF report to Cloudinary: ${e.message}")
            throw InternalServerException("Failed to upload PDF report.")
        }
    }

    override suspend fun uploadCsvReport(fileBytes: ByteArray, fileName: String): String {
        validateFileInput(fileBytes, fileName, "csv")

        return try {
            logger.debug("Uploading CSV report to Cloudinary: $fileName (${fileBytes.size} bytes)")

            val baseName = fileName.substringBeforeLast(".")

            val uploadResult = cloudinary.uploader().upload(
                fileBytes,
                ObjectUtils.asMap(
                    "public_id", baseName,
                    "folder", "attendance_reports/csv",
                    "resource_type", "raw",     // CSV as raw
                    "type", "upload",            // Public access
                    "format", "csv",             // Explicit format
                    "overwrite", false,
                    "invalidate", true
                )
            )

            // Construct public URL with proper format
            val cloudName = AppConfig.CLOUD_NAME
            val publicId = uploadResult["public_id"] as? String ?: "attendance_reports/csv/$baseName"
            val finalUrl = "https://res.cloudinary.com/$cloudName/raw/upload/$publicId"

            logger.info("CSV report uploaded successfully: $finalUrl")
            finalUrl

        } catch (e: Exception) {
            logger.error("Failed to upload CSV report to Cloudinary: ${e.message}")
            throw InternalServerException("Failed to upload CSV report.")
        }
    }

    private fun validateFileInput(fileBytes: ByteArray, fileName: String, expectedExtension: String) {
        if (fileBytes.isEmpty()) {
            throw IllegalArgumentException("File bytes cannot be empty")
        }
        if (fileBytes.size > 50 * 1024 * 1024) {
            throw IllegalArgumentException("File size too large: ${fileBytes.size} bytes")
        }
        if (fileName.isBlank()) {
            throw IllegalArgumentException("File name cannot be blank")
        }
        if (!fileName.endsWith(".$expectedExtension", ignoreCase = true)) {
            throw IllegalArgumentException("File name must end with .$expectedExtension")
        }
    }

    /**
     * Check if a file exists in Cloudinary
     */
    fun checkFileExists(fileUrl: String): Boolean {
        if (fileUrl.isBlank()) return false

        return try {
            val publicId = extractPublicIdFromUrl(fileUrl)
            val result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap())
            result != null
        } catch (e: Exception) {
            logger.debug ( "File does not exist or cannot be accessed: $fileUrl" )
            false
        }
    }


    /**
     * Clean up old QR codes (useful for maintenance)
     */
    fun cleanupOldQRCodes(daysOld: Int = 30): Int {
        return try {
            logger.info("Cleaning up QR codes older than $daysOld days")

            // This would require a more complex implementation using Cloudinary Admin API
            // For now, we'll log that this feature is not fully implemented
            logger.warn("Bulk cleanup feature requires Cloudinary Admin API access")
            0
        } catch (e: Exception) {
            logger.error("Failed to cleanup old QR codes: ${e.message}")
            0
        }
    }

    // Helper Functions
    private fun validateImageInput(imageBytes: ByteArray, fileName: String) {
        if (imageBytes.isEmpty()) {
            throw IllegalArgumentException("Image bytes cannot be empty")
        }
        if (imageBytes.size > 10 * 1024 * 1024) { // 10MB limit
            throw IllegalArgumentException("Image size too large: ${imageBytes.size} bytes")
        }
        if (fileName.isBlank()) {
            throw IllegalArgumentException("File name cannot be blank")
        }
        if (!fileName.endsWith(".png", ignoreCase = true)) {
            throw IllegalArgumentException("File name must end with .png")
        }
    }

    private fun getFileId(fileName: String): String {
        // Remove extension and ensure valid public_id format
        val baseName = fileName.substringBeforeLast(".")
        return baseName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    }

    private fun extractPublicIdFromUrl(fileUrl: String): String {
        return try {
            // Match and extract everything after `/upload/v1234567/` and before the file extension
            val regex = Regex("""/upload/v\d+/(.+)\.\w+$""")
            val matchResult = regex.find(fileUrl)
            matchResult?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Invalid Cloudinary URL format")
        } catch (e: Exception) {
            logger.error("Failed to extract public ID from URL: $fileUrl")
            throw IllegalArgumentException("Invalid Cloudinary URL: $fileUrl")
        }
    }


}