package com.ryanthetechman.cherrycal.ai

import com.ryanthetechman.cherrycal.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object PromptAI {
    @Serializable
    data class ChatMessage(val role: String, val content: String)

    @Serializable
    data class SchemaDefinition(
        val schema: JsonObject
    )

    /** Request body for chat completions, optionally including schemas */
    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val schema: SchemaDefinition? = null
    )

    @Serializable
    data class ChatResponse(val choices: List<Choice>)

    @Serializable
    data class Choice(val message: ChatMessage)

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendAIRequest(modelId: String, prompt: String, systemPrompt: String? = null): String {
        val requestBody = ChatRequest(
            model = modelId,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt ?: "You are a calendar assistant for a product named \"CherryCal\". You are helpful and friendly."),
                ChatMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response: String = HttpClient().post("${AISecrets.PORTKEY_BASE_URL}/api/chat/completions") {
                contentType(ContentType.Application.Json)
                bearerAuth(AISecrets.PORTKEY_API_KEY)
                setBody(Json.encodeToString(requestBody))
            }.bodyAsText()

            val parsedResponse = json.decodeFromString<ChatResponse>(response)
            parsedResponse.choices.firstOrNull()?.message?.content ?: "No response from AI."
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

    suspend fun sendStructuredRequest(
        modelId: String,
        userPrompt: String,
        systemPrompt: String? = null,
        responseFormat: JsonObject
    ): JsonObject {
        val messages = buildJsonArray {
            systemPrompt?.let {
                add(buildJsonObject {
                    put("role",    JsonPrimitive("system"))
                    put("content", JsonPrimitive(it))
                })
            }
            add(buildJsonObject {
                put("role",    JsonPrimitive("user"))
                put("content", JsonPrimitive(userPrompt))
            })
        }

        val payload = buildJsonObject {
            put("model", JsonPrimitive(modelId))
            put("messages", messages)
            put("response_format", responseFormat)
        }

        val raw: String = try {
            HttpClient().post("${AISecrets.PORTKEY_BASE_URL}/chat/completions") {
                header("x-portkey-api-key", AISecrets.PORTKEY_API_KEY)
                header("x-portkey-virtual-key", AISecrets.PORTKEY_VIRTUAL_KEY)
                contentType(ContentType.Application.Json)
                timeout { requestTimeoutMillis = 10000 }
                setBody(payload.toString())
            }.bodyAsText().also { println("PromptAI: Response: $it") }
        } catch (e: Exception) {
            println("PromptAI: Error: ${e.message}")
            return buildJsonObject { put("error", e.message ?: "Network error") }
        }

        return runCatching {
            val root      = json.parseToJsonElement(raw).jsonObject
            val content   = root["choices"]!!
                .jsonArray[0].jsonObject["message"]!!
                .jsonObject["content"]!!.jsonPrimitive.content
            json.parseToJsonElement(content).jsonObject
        }.getOrElse { err ->
            buildJsonObject { put("error", err.message ?: "Parse error") }
        }
    }
}