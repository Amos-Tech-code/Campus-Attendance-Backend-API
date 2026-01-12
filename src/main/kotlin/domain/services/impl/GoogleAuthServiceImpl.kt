package domain.services.impl

import com.amos_tech_code.domain.models.GoogleUser
import com.amos_tech_code.services.GoogleAuthService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

class GoogleAuthServiceImpl(
    private val httpClient: HttpClient
) : GoogleAuthService {

    private val logger = LoggerFactory.getLogger(GoogleAuthServiceImpl::class.java)

    override suspend fun validateGoogleToken(idToken: String): GoogleUser? {
        return try {
            val response: HttpResponse = httpClient.get("https://oauth2.googleapis.com/tokeninfo") {
                parameter("id_token", idToken)
            }

            val responseBody = response.bodyAsText()
            println("Google OAuth Response: $responseBody")

            if (response.status == HttpStatusCode.OK) {
                val jsonObject = Json.parseToJsonElement(responseBody).jsonObject

                val email = jsonObject["email"]?.jsonPrimitive?.content
                val emailVerified = jsonObject["email_verified"]?.jsonPrimitive?.content == "true"
                val name = jsonObject["name"]?.jsonPrimitive?.content

                if (email != null && emailVerified) {
                    GoogleUser(
                        email = email,
                        name = name ?: "Unknown",
                        emailVerified = emailVerified
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Google token validation error: ${e.message}")
            null
        }
    }

}