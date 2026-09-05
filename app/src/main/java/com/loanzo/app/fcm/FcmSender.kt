package com.loanzo.app.fcm

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.loanzo.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Utility to send FCM push notifications directly from the app using the HTTP v1 API.
 * WARNING: This requires a Service Account JSON file in res/raw/. 
 * In a real production environment, this logic should be on a backend server.
 */
class FcmSender {

    companion object {
        private const val TAG = "FcmSender"
        // Replace this with your actual Firebase Project ID!
        private const val PROJECT_ID = "loanzo-app-project" 
        private const val FCM_API_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
        
        private val client = OkHttpClient()
    }

    /**
     * Reads the service account json from res/raw/firebase_service_account
     * and generates an OAuth2 Bearer token required for the FCM v1 API.
     */
    private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.resources.openRawResource(R.raw.firebase_service_account)
            val googleCredentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            
            googleCredentials.refreshIfExpired()
            return@withContext googleCredentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate access token. Ensure res/raw/firebase_service_account.json exists.", e)
            null
        }
    }

    /**
     * Sends a push notification to a specific FCM token.
     */
    suspend fun sendPaymentReminder(context: Context, targetToken: String, loanAmount: String): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken(context)
        if (accessToken == null) {
            Log.e(TAG, "Cannot send notification: Access token is null")
            return@withContext false
        }

        try {
            val messageJson = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", targetToken)
                    put("notification", JSONObject().apply {
                        put("title", "Payment Reminder")
                        put("body", "Friendly reminder: A payment of ₹$loanAmount is due soon.")
                    })
                    put("data", JSONObject().apply {
                        put("type", "PAYMENT_REMINDER")
                        put("amount", loanAmount)
                    })
                })
            }

            val requestBody = messageJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FCM_API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully sent notification: $responseBody")
                true
            } else {
                Log.e(TAG, "Failed to send notification. HTTP ${response.code}: $responseBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification", e)
            false
        }
    }
}
