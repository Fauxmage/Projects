package com.example.pebtip.ui.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

//Sending login request to /tokengen
//@Serializable needed for Ktor to convert to/from JSON
@Serializable
data class TokenRequest(
    val participant_id: String,
    val project_id: String
)

//Response form /tokengen contains generatted token
@Serializable
data class TokenResponse(
    val token: String
)

//Request body for /user_auth -> only token
@Serializable
data class AuthRequest(
    val token: String
)

//Works for both iOS and Android
val httpClient = HttpClient {
    install(ContentNegotiation){
        json(Json { ignoreUnknownKeys = true})
    }
}

private const val BASE_URL = "https://www.ubr004.xyz/"

//Send inputs to get a token back, return null if failed or server is unreachable
suspend fun getToken(participantId: String, projectId: String): String? {
    return try {
        val response = httpClient.post("${BASE_URL}tokengen") {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(participantId, projectId))
        }
        response.body<TokenResponse>().token
    } catch (e: Exception) {
        null
    }
}

//Verify token against backend database, returns true if valid, or false if not found or request fails
//Used both during login and on app launch
suspend fun authenticateToken(token: String): Boolean {
    return try {
        val response = httpClient.post("${BASE_URL}user_auth") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(token))
        }
        response.body<Boolean>()
    } catch (e: Exception) {
        false
    }
}