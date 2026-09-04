package com.loanzo.app.util

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationHelper @Inject constructor() {

    // Fast HTTP client with reasonable timeouts
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // In-memory LRU cache to store up to 500 translated sentences (0ms instant lookup)
    private val translationCache = LruCache<String, String>(500)

    suspend fun translateText(text: String, targetLang: String, sourceLang: String = "auto"): String? = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext text

        val cacheKey = "${sourceLang}_${targetLang}_$trimmed"
        val cached = translationCache.get(cacheKey)
        if (cached != null) {
            return@withContext cached
        }

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@withContext null
                    val rootArray = JSONArray(responseBody)
                    val sentences = rootArray.optJSONArray(0)
                    if (sentences != null) {
                        val sb = StringBuilder()
                        for (i in 0 until sentences.length()) {
                            val part = sentences.optJSONArray(i)
                            if (part != null) {
                                sb.append(part.optString(0, ""))
                            }
                        }
                        val result = sb.toString()
                        if (result.isNotBlank()) {
                            translationCache.put(cacheKey, result)
                            return@withContext result
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
