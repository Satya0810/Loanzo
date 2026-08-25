package com.loanzo.app.util

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationHelper @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    // Public LibreTranslate mirror. For production, host your own instance.
    private val apiUrl = "https://translate.argosopentech.com/translate" 

    suspend fun translateText(text: String, targetLang: String, sourceLang: String = "en"): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        
        try {
            val jsonMap = mapOf(
                "q" to text,
                "source" to sourceLang,
                "target" to targetLang,
                "format" to "text"
            )
            
            val jsonBody = gson.toJson(jsonMap)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val resultMap = gson.fromJson(responseBody, Map::class.java)
                        return@withContext resultMap["translatedText"] as? String
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
