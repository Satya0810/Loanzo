package com.loanzo.app.data.digilocker

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Response from the backend's /api/kyc/digilocker/init endpoint.
 */
data class DigiLockerSessionResponse(
    val success: Boolean,
    val sessionId: String?,
    val authorizationUrl: String?
)

@Singleton
class DigiLockerVerificationService @Inject constructor() {

    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Initiates a DigiLocker session via our backend (which calls Sandbox.co.in).
     * Returns the session ID and authorization URL for the user to complete consent.
     */
    suspend fun initSession(userId: String): Result<DigiLockerSessionResponse> {
        return try {
            val payload = mapOf("userId" to userId)
            val jsonBody = gson.toJson(payload)
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(DigiLockerConfig.INIT_SESSION_URL)
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val session = gson.fromJson(body, DigiLockerSessionResponse::class.java)
                Result.success(session)
            } else {
                Result.failure(Exception("Session init failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches the DigiLocker consent flow in Chrome Custom Tabs
     * using the authorization URL returned by our backend.
     */
    fun launchDigiLockerFlow(context: Context, authorizationUrl: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(authorizationUrl))
    }

data class DigiLockerProfileData(
    val success: Boolean,
    val name: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val panNumber: String? = null,
    val aadhaarNumber: String? = null
)

    /**
     * Completes verification by telling backend to fetch documents from Sandbox.co.in.
     */
    suspend fun verifySession(sessionId: String, userId: String): Result<DigiLockerProfileData> {
        return try {
            val payload = mapOf("sessionId" to sessionId, "userId" to userId)
            val jsonBody = gson.toJson(payload)
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(DigiLockerConfig.VERIFY_URL)
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val json = org.json.JSONObject(body)
                val isSuccess = json.optBoolean("success", false) || json.optString("status") == "VERIFIED"
                val userObj = json.optJSONObject("user")
                
                val name = json.optString("name").ifBlank { userObj?.optString("name") ?: "" }
                val panNumber = json.optString("panNumber").ifBlank { userObj?.optString("panNumber") ?: "" }
                val aadhaarNumber = json.optString("aadhaarNumber").ifBlank { userObj?.optString("aadhaarNumber") ?: "" }
                val dateOfBirth = json.optString("dateOfBirth").ifBlank { userObj?.optString("dateOfBirth") ?: "" }
                val address = json.optString("address").ifBlank { userObj?.optString("address") ?: "" }

                Result.success(
                    DigiLockerProfileData(
                        success = isSuccess,
                        name = name.ifBlank { "Verified Citizen" },
                        panNumber = panNumber.ifBlank { "VERIFIED_ITD" },
                        aadhaarNumber = aadhaarNumber.ifBlank { "VERIFIED_UIDAI" },
                        dateOfBirth = dateOfBirth.ifBlank { null },
                        address = address.ifBlank { null }
                    )
                )
            } else {
                Result.failure(Exception("Verification failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
