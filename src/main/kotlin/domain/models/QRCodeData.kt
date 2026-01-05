package com.amos_tech_code.domain.models


/**
 * Data class for QR code content
 */
data class QRCodeData(
    val sessionCode: String,
    val unitCode: String,
    val sessionId: String,
    val timestamp: Long,
    val version: String
)