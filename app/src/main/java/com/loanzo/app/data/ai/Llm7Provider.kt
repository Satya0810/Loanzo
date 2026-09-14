package com.loanzo.app.data.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class Llm7Provider @Inject constructor(
    private val configManager: AiConfigManager,
    private val gson: Gson
) : AiProvider {

    override val name: String = "LLM7.io Fast Engine"
    override val providerType: String = "LLM7"
    override val isEnabled: Boolean get() = configManager.llm7ApiKey.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "Llm7Provider"
        private const val API_URL = "https://api.llm7.io/v1/chat/completions"
    }

    override suspend fun generateResponse(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Double
    ): String? = suspendCancellableCoroutine { continuation ->
        val apiKey = configManager.llm7ApiKey
        if (apiKey.isBlank()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val messages = mutableListOf<Map<String, String>>()
        if (systemPrompt.isNotBlank()) {
            messages.add(mapOf("role" to "system", "content" to systemPrompt))
        }
        messages.add(mapOf("role" to "user", "content" to prompt))

        val payload = mapOf(
            "model" to configManager.llm7Model.ifBlank { "default" },
            "messages" to messages,
            "max_tokens" to maxTokens,
            "temperature" to temperature
        )

        val jsonBody = gson.toJson(payload)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) {
                    Log.w(TAG, "LLM7 call failed: ${e.message}")
                    continuation.resume(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "LLM7 returned HTTP ${resp.code}: ${resp.message}")
                        if (!continuation.isCancelled) continuation.resume(null)
                        return
                    }

                    val bodyString = resp.body?.string()
                    if (bodyString.isNullOrBlank()) {
                        if (!continuation.isCancelled) continuation.resume(null)
                        return
                    }

                    try {
                        val json = gson.fromJson(bodyString, JsonObject::class.java)
                        val choices = json.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val firstChoice = choices[0].asJsonObject
                            val message = firstChoice.getAsJsonObject("message")
                            val content = message?.get("content")?.asString
                            if (!content.isNullOrBlank() && !continuation.isCancelled) {
                                continuation.resume(content.trim())
                                return
                            }
                        }
                    } catch (err: Exception) {
                        Log.e(TAG, "Error parsing LLM7 response", err)
                    }

                    if (!continuation.isCancelled) continuation.resume(null)
                }
            }
        })
    }
}
