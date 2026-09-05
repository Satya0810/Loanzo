package com.loanzo.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramManager @Inject constructor() {

    companion object {
        private const val TAG = "TelegramManager"
        const val BOT_TOKEN = "8911421683:AAFpIQLIBY9USPni5Ylr1I5vx4zgh_BXTq0"
        const val BOT_USERNAME = "Loanzo_bot"
        const val BOT_URL = "https://t.me/$BOT_USERNAME"

        // Sole designated Administrator username and primary verified Chat ID for @satyam_081
        const val ADMIN_USERNAME = "satyam_081"
        val ADMIN_CHAT_IDS = listOf(8234574147L)

        private const val TELEGRAM_API_URL = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Opens Telegram with a deep link to link the user's account with the bot.
     */
    fun openBotForLinking(context: Context, userId: String) {
        try {
            val deepLink = "$BOT_URL?start=user_$userId"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open Telegram: ${e.message}")
        }
    }

    /**
     * Sends an alert to all registered Admin chat IDs.
     */
    suspend fun sendAdminAlert(
        messageHtml: String,
        actionButtonText: String? = null,
        actionButtonUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var anySuccess = false
        for (chatId in ADMIN_CHAT_IDS) {
            val ok = sendMessage(chatId.toString(), messageHtml, actionButtonText, actionButtonUrl)
            if (ok) anySuccess = true
        }
        anySuccess
    }

    /**
     * Sends a KYC upload notification to Admins with a direct link to view the document.
     */
    suspend fun notifyKycSubmission(
        userName: String,
        userId: String,
        documentType: String,
        documentUrl: String?
    ) {
        val html = """
            📋 <b>New KYC Document Uploaded</b>
            
            <b>User:</b> $userName
            <b>User ID:</b> <code>$userId</code>
            <b>Document:</b> $documentType
            <b>Status:</b> Pending Verification ⏳
        """.trimIndent()

        val buttonText = if (!documentUrl.isNullOrBlank()) "📄 View on Google Drive" else null
        sendAdminAlert(html, buttonText, documentUrl)
    }

    /**
     * Sends a Loan Request notification to Admins.
     */
    suspend fun notifyLoanRequested(
        borrowerName: String,
        loanId: String,
        amount: Double,
        purpose: String
    ) {
        val formattedAmount = "₹%,.2f".format(amount)
        val html = """
            💰 <b>New Loan Request Submitted</b>
            
            <b>Borrower:</b> $borrowerName
            <b>Loan ID:</b> <code>$loanId</code>
            <b>Amount:</b> <b>$formattedAmount</b>
            <b>Purpose:</b> $purpose
            <b>Status:</b> Awaiting Lender Approval ⏳
        """.trimIndent()

        sendAdminAlert(html)
    }

    /**
     * Sends a Signed Agreement notification to Admins.
     */
    suspend fun notifyAgreementSigned(
        borrowerName: String,
        lenderName: String,
        loanId: String,
        agreementUrl: String?
    ) {
        val html = """
            ✍️ <b>Loan Agreement Signed & Finalized</b>
            
            <b>Borrower:</b> $borrowerName
            <b>Lender:</b> $lenderName
            <b>Loan ID:</b> <code>$loanId</code>
            <b>Status:</b> Fully Executed (eSigned) ✅
        """.trimIndent()

        val buttonText = if (!agreementUrl.isNullOrBlank()) "📜 View Signed Agreement" else null
        sendAdminAlert(html, buttonText, agreementUrl)
    }

    /**
     * Sends a generic message via Telegram Bot API with optional inline URL button.
     */
    suspend fun sendMessage(
        chatId: String,
        messageHtml: String,
        buttonText: String? = null,
        buttonUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", messageHtml)
                put("parse_mode", "HTML")

                if (!buttonText.isNullOrBlank() && !buttonUrl.isNullOrBlank()) {
                    val inlineKeyboard = JSONArray().apply {
                        val row = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", buttonText)
                                put("url", buttonUrl)
                            })
                        }
                        put(row)
                    }
                    put("reply_markup", JSONObject().apply {
                        put("inline_keyboard", inlineKeyboard)
                    })
                }
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(TELEGRAM_API_URL)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Telegram message to $chatId", e)
            return@withContext false
        }
    }
}
