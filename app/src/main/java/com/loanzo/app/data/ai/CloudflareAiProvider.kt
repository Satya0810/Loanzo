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
class CloudflareAiProvider @Inject constructor(
    private val configManager: AiConfigManager,
    private val gson: Gson
) : AiProvider {

    override val name: String = "Cloudflare Workers AI"
    override val providerType: String = "CLOUDFLARE"
    override val isEnabled: Boolean get() = configManager.cloudflareToken.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "CloudflareAiProvider"
    }

    override suspend fun generateResponse(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Double
    ): String? = suspendCancellableCoroutine { continuation ->
        val token = configManager.cloudflareToken
        if (token.isBlank()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val accountId = configManager.cloudflareAccountId
        if (accountId.isBlank()) {
            // Cloudflare Workers AI requires an Account ID in the URL path.
            // When blank, skip immediately so it does not waste network latency or fail with HTTP 400.
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val model = configManager.cloudflareModel.ifBlank { "@cf/meta/llama-3.1-8b-instruct" }
        val url = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/$model"

        val messages = mutableListOf<Map<String, String>>()
        if (systemPrompt.isNotBlank()) {
            messages.add(mapOf("role" to "system", "content" to systemPrompt))
        }
        messages.add(mapOf("role" to "user", "content" to prompt))

        val payload = mapOf(
            "messages" to messages,
            "max_tokens" to maxTokens,
            "temperature" to temperature
        )

        val jsonBody = gson.toJson(payload)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
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
                    Log.w(TAG, "Cloudflare call failed: ${e.message}")
                    continuation.resume(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "Cloudflare returned HTTP ${resp.code}: ${resp.message}")
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
                        // Support both direct Workers AI format (result.response or result.choices) and OpenAI format
                        if (json.has("result")) {
                            val resultObj = json.getAsJsonObject("result")
                            if (resultObj.has("response")) {
                                val responseText = resultObj.get("response")?.asString
                                if (!responseText.isNullOrBlank() && !continuation.isCancelled) {
                                    continuation.resume(responseText.trim())
                                    return
                                }
                            }
                            if (resultObj.has("choices")) {
                                val choices = resultObj.getAsJsonArray("choices")
                                if (choices != null && choices.size() > 0) {
                                    val content = choices[0].asJsonObject.getAsJsonObject("message")?.get("content")?.asString
                                    if (!content.isNullOrBlank() && !continuation.isCancelled) {
                                        continuation.resume(content.trim())
                                        return
                                    }
                                }
                            }
                        } else if (json.has("choices")) {
                            val choices = json.getAsJsonArray("choices")
                            if (choices != null && choices.size() > 0) {
                                val content = choices[0].asJsonObject.getAsJsonObject("message")?.get("content")?.asString
                                if (!content.isNullOrBlank() && !continuation.isCancelled) {
                                    continuation.resume(content.trim())
                                    return
                                }
                            }
                        }
                    } catch (err: Exception) {
                        Log.e(TAG, "Error parsing Cloudflare response", err)
                    }

                    if (!continuation.isCancelled) continuation.resume(null)
                }
            }
        })
    }
}
