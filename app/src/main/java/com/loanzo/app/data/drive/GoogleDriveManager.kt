package com.loanzo.app.data.drive

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveManager @Inject constructor() {

    // The Google Apps Script Web App URL acting as a secure proxy for Google Drive uploads
    private val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbwapyq3jYJHgqeIuFOv5qcg0nJdIyMvVdxmZx5hXv7B5G4nsi5Ap8Mra_JPRDiAjKQB/exec"
    private val API_KEY = "LOANZO_KYC_SECURE_KEY_2026"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadFile(context: Context, uri: Uri, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (WEB_APP_URL == "YOUR_WEB_APP_URL_HERE") {
                return@withContext Result.failure(Exception("Please configure your Web App URL in GoogleDriveManager!"))
            }

            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/pdf"
            
            // Read the file into a ByteArray
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Failed to open file input stream"))
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            // Encode binary data to Base64
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            // Build the JSON payload for our Google Apps Script
            val jsonBody = JSONObject().apply {
                put("apiKey", API_KEY)
                put("fileName", fileName)
                put("mimeType", mimeType)
                put("base64Data", base64String)
            }
            
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(WEB_APP_URL)
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseString)
                if (jsonResponse.optBoolean("success", false)) {
                    val link = jsonResponse.optString("webViewLink", "")
                    return@withContext Result.success(link)
                } else {
                    return@withContext Result.failure(Exception(jsonResponse.optString("error", "Unknown proxy error")))
                }
            } else {
                return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Checks if the user already has KYC documents (PAN / Aadhaar) uploaded in Google Drive.
     * Returns Pair(panUrl, aadhaarUrl).
     */
    suspend fun fetchExistingKycDocuments(userId: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            if (WEB_APP_URL.contains("YOUR_WEB_APP_URL")) return@withContext Pair(null, null)
            val url = "$WEB_APP_URL?apiKey=$API_KEY&userId=$userId"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(responseString)
                if (json.optBoolean("success", false)) {
                    val pan = json.optString("panUrl", "").takeIf { it.isNotBlank() }
                    val aadhaar = json.optString("aadhaarUrl", "").takeIf { it.isNotBlank() }
                    return@withContext Pair(pan, aadhaar)
                }
            }
        } catch (_: Exception) {}
        Pair(null, null)
    }

    /**
     * Downloads a file from Google Drive or general HTTP URL and writes it to a local File.
     */
    suspend fun downloadFileToLocal(url: String, targetFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank()) return@withContext false
            val downloadUrl = if (url.contains("drive.google.com")) {
                val fileId = if (url.contains("/d/")) {
                    url.substringAfter("/d/").substringBefore("/")
                } else if (url.contains("id=")) {
                    url.substringAfter("id=").substringBefore("&")
                } else null
                if (!fileId.isNullOrBlank()) {
                    "https://drive.google.com/uc?export=view&id=$fileId"
                } else url
            } else url

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyBytes = response.body?.bytes()
                if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeBytes(bodyBytes)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }
}
