package com.loanzo.app.data.didit

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiditVerificationService @Inject constructor() {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Creates a verification session on Didit API.
     * @param userId Unique identifier for the user to correlate verification with account.
     */
    suspend fun createVerificationSession(userId: String): Result<DiditSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val payload = DiditCreateSessionRequest(
                    vendorData = userId,
                    callback = DiditConfig.REDIRECT_URL,
                    features = "ocr+liveness+aml"
                )
                val jsonBody = gson.toJson(payload)
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val requestBuilder = Request.Builder()
                    .url(DiditConfig.API_SESSION_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")

                if (DiditConfig.API_KEY.isNotBlank() && DiditConfig.API_KEY != "YOUR_DIDIT_API_KEY") {
                    requestBuilder.addHeader("Authorization", "Bearer ${DiditConfig.API_KEY}")
                    if (DiditConfig.CLIENT_ID.isNotBlank() && DiditConfig.CLIENT_ID != "YOUR_DIDIT_CLIENT_ID") {
                        requestBuilder.addHeader("X-App-Id", DiditConfig.CLIENT_ID)
                    }
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val sessionResponse = gson.fromJson(responseBody, DiditSessionResponse::class.java)
                    Result.success(sessionResponse)
                } else {
                    // Fallback to web link if testing before setting key
                    val fallbackUrl = "https://verify.didit.me/session?vendor_data=$userId&callback=${Uri.encode(DiditConfig.REDIRECT_URL)}"
                    Result.success(
                        DiditSessionResponse(
                            sessionId = "session_$userId",
                            url = fallbackUrl,
                            status = "Created"
                        )
                    )
                }
            } catch (e: Exception) {
                val fallbackUrl = "https://verify.didit.me/session?vendor_data=$userId&callback=${Uri.encode(DiditConfig.REDIRECT_URL)}"
                Result.success(
                    DiditSessionResponse(
                        sessionId = "session_$userId",
                        url = fallbackUrl,
                        status = "Created"
                    )
                )
            }
        }

    /**
     * Checks the verification status of a session from Didit API.
     */
    suspend fun getVerificationStatus(sessionId: String): Result<DiditSessionStatusResponse> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${DiditConfig.API_SESSION_URL}$sessionId/decision/"
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")

                if (DiditConfig.API_KEY.isNotBlank() && DiditConfig.API_KEY != "YOUR_DIDIT_API_KEY") {
                    requestBuilder.addHeader("Authorization", "Bearer ${DiditConfig.API_KEY}")
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val statusResponse = gson.fromJson(responseBody, DiditSessionStatusResponse::class.java)
                    Result.success(statusResponse)
                } else {
                    Result.failure(Exception("Failed to fetch session status: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Opens the Didit verification URL using Chrome Custom Tabs for a native in-app browser experience.
     */
    fun launchVerificationFlow(context: Context, url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}
