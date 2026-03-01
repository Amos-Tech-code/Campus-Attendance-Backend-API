package com.amos_tech_code.domain.services.impl

import com.amos_tech_code.config.AppConfig
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

object FirebaseService {

    private val logger = LoggerFactory.getLogger(FirebaseService::class.java)
    private var isInitialized = false

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val firebaseConfig = AppConfig.FIREBASE_CONFIG
                ?: throw IllegalStateException("FIREBASE_CONFIG environment variable not found")

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(firebaseConfig.toByteArray())))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                logger.info("Firebase initialized successfully")
                isInitialized = true
            }
        } catch (e: Exception) {
            logger.error("Firebase initialization failed: ${e.message}", e)
            isInitialized = false
        }
    }

    fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): NotificationResult {
        if (!isInitialized) {
            logger.error("Firebase not initialized")
            return NotificationResult.Failure("Firebase not initialized")
        }

        return try {
            val message = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putAllData(data)
                .build()

            val response = FirebaseMessaging.getInstance().send(message)
            logger.info("Notification sent successfully: $response")
            NotificationResult.Success(response)
        } catch (e: FirebaseMessagingException) {
            when (e.messagingErrorCode) {
                MessagingErrorCode.UNREGISTERED -> {
                    logger.warn("Token is unregistered: $token")
                    NotificationResult.Failure("Device token is no longer valid")
                }
                MessagingErrorCode.INVALID_ARGUMENT -> {
                    logger.error("Invalid token: $token")
                    NotificationResult.Failure("Invalid device token")
                }
                else -> {
                    logger.error("Firebase error: ${e.message}", e)
                    NotificationResult.Failure(e.message ?: "Firebase error")
                }
            }
        } catch (e: Exception) {
            logger.error("Error sending notification: ${e.message}", e)
            NotificationResult.Failure(e.message ?: "Unknown error")
        }
    }

    fun sendMulticast(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): MulticastResult {
        if (!isInitialized) {
            logger.error("Firebase not initialized")
            return MulticastResult.Failure("Firebase not initialized")
        }

        return try {
            val message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putAllData(data)
                .build()

            val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)

            val successCount = response.successCount
            val failureCount = response.failureCount

            logger.info("Multicast sent: $successCount successful, $failureCount failed")

            MulticastResult.Success(
                successCount = successCount,
                failureCount = failureCount,
                responses = response.responses.mapIndexed { index, sendResponse ->
                    if (sendResponse.isSuccessful) {
                        TokenResult(index, true, sendResponse.messageId, null)
                    } else {
                        TokenResult(index, false, null, sendResponse.exception?.message)
                    }
                }
            )
        } catch (e: Exception) {
            logger.error("Error sending multicast: ${e.message}", e)
            MulticastResult.Failure(e.message ?: "Unknown error")
        }
    }

    fun sendToTopic(
        topic: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): NotificationResult {
        if (!isInitialized) {
            logger.error("Firebase not initialized")
            return NotificationResult.Failure("Firebase not initialized")
        }

        return try {
            val message = Message.builder()
                .setTopic(topic)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putAllData(data)
                .build()

            val response = FirebaseMessaging.getInstance().send(message)
            logger.info("Topic notification sent: $response")
            NotificationResult.Success(response)
        } catch (e: Exception) {
            logger.error("Error sending topic notification: ${e.message}", e)
            NotificationResult.Failure(e.message ?: "Unknown error")
        }
    }

    fun subscribeToTopic(tokens: List<String>, topic: String): Boolean {
        return try {
            val response = FirebaseMessaging.getInstance().subscribeToTopic(tokens, topic)
            logger.info("Subscribed ${tokens.size} devices to topic: $topic")
            response.successCount == tokens.size
        } catch (e: Exception) {
            logger.error("Error subscribing to topic: ${e.message}", e)
            false
        }
    }

    fun unsubscribeFromTopic(tokens: List<String>, topic: String): Boolean {
        return try {
            val response = FirebaseMessaging.getInstance().unsubscribeFromTopic(tokens, topic)
            logger.info("Unsubscribed ${tokens.size} devices from topic: $topic")
            response.successCount == tokens.size
        } catch (e: Exception) {
            logger.error("Error unsubscribing from topic: ${e.message}", e)
            false
        }
    }
}

sealed class NotificationResult {
    data class Success(val messageId: String) : NotificationResult()
    data class Failure(val error: String) : NotificationResult()
}

data class MulticastResult(
    val successCount: Int,
    val failureCount: Int,
    val responses: List<TokenResult>
) {
    companion object {
        fun Success(successCount: Int, failureCount: Int, responses: List<TokenResult>) =
            MulticastResult(successCount, failureCount, responses)
        fun Failure(error: String) = MulticastResult(0, 0, emptyList())
    }
}

data class TokenResult(
    val index: Int,
    val success: Boolean,
    val messageId: String?,
    val error: String?
)