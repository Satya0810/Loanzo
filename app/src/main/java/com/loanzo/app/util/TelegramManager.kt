package com.loanzo.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
        const val BOT_TOKEN = "8911421683:AAEkc1ykoS-VIg_Dnl8deLnakd6nJE88pqc"
        const val BOT_USERNAME = "Loanzo_bot"
        const val BOT_URL = "https://t.me/$BOT_USERNAME"

        // Sole designated Administrator username and primary verified Chat ID for @satyam_081
        const val ADMIN_USERNAME = "satyam_081"
        val ADMIN_CHAT_IDS = listOf(8234574147L)

        private const val TELEGRAM_API_URL = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
        private const val MAX_TELEGRAM_MESSAGE_LENGTH = 4000

        // Shared static client to prevent multiple OkHttpClient thread-pool and socket leaks
        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        // Shared singleton instance for manual invocation in Composables and ViewModels
        val instance: TelegramManager by lazy { TelegramManager() }

        /**
         * Safely escapes raw user text for Telegram HTML parse mode.
         * Converts &, <, and > into compliant HTML entities.
         */
        fun escapeHtml(text: String?): String {
            if (text.isNullOrEmpty()) return ""
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }

        /**
         * Strips all HTML tags and decodes entities for resilient plain-text fallback.
         */
        fun stripHtml(html: String?): String {
            if (html.isNullOrEmpty()) return ""
            return html
                .replace(Regex("<[^>]*>"), "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim()
        }

        /**
         * Validates and sanitizes a URL for Telegram inline keyboard buttons.
         * Returns null if the URL is invalid, empty, or uses an unsupported local scheme (e.g. content:// or file://).
         */
        fun sanitizeButtonUrl(rawUrl: String?): String? {
            if (rawUrl.isNullOrBlank()) return null
            val trimmed = rawUrl.trim()
            return when {
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("tg://", ignoreCase = true) -> trimmed
                trimmed.startsWith("tel:", ignoreCase = true) -> {
                    val digits = trimmed.removePrefix("tel:").filter { it.isDigit() }
                    if (digits.isNotBlank()) "https://wa.me/$digits" else null
                }
                trimmed.startsWith("content://", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true) -> null
                trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.contains("\n") -> "https://$trimmed"
                else -> null
            }
        }
    }

    private val client: OkHttpClient get() = sharedClient

    /**
     * Opens Telegram with a deep link to link the user's account with the bot.
     * Prefers direct app scheme (`tg://resolve?domain=Loanzo_bot&start=user_$userId`),
     * falling back smoothly to web browser (`https://t.me/Loanzo_bot?start=user_$userId`),
     * and shows a friendly Toast if no suitable app or browser is available.
     */
    fun openBotForLinking(context: Context, userId: String) {
        val cleanUserId = userId.trim()
        val startParam = if (cleanUserId.isNotBlank()) "user_$cleanUserId" else "app_launch"
        val nativeAppUri = Uri.parse("tg://resolve?domain=$BOT_USERNAME&start=$startParam")
        val webUri = Uri.parse("$BOT_URL?start=$startParam")

        try {
            // Attempt 1: Direct native Telegram application resolution
            val nativeIntent = Intent(Intent.ACTION_VIEW, nativeAppUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(nativeIntent)
        } catch (_: ActivityNotFoundException) {
            // Attempt 2: Web Browser fallback
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not open Telegram link: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Could not open Telegram. Please ensure Telegram or a browser is installed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            // Attempt 3: Catch-all fallback
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed all attempts to open Telegram: ${e2.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Could not open Telegram. Please ensure Telegram or a browser is installed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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
        val safeName = escapeHtml(userName.ifBlank { "User $userId" })
        val safeUserId = escapeHtml(userId)
        val safeDocType = escapeHtml(documentType)

        val html = """
            📋 <b>New KYC Document Uploaded</b>
            
            <b>User:</b> $safeName
            <b>User ID:</b> <code>$safeUserId</code>
            <b>Document:</b> $safeDocType
            <b>Status:</b> Pending Verification ⏳
        """.trimIndent()

        val sanitizedUrl = sanitizeButtonUrl(documentUrl)
        val buttonText = if (sanitizedUrl != null) "📄 View Document" else null
        sendAdminAlert(html, buttonText, sanitizedUrl)
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
        val safeName = escapeHtml(borrowerName)
        val safeLoanId = escapeHtml(loanId)
        val safePurpose = escapeHtml(purpose.ifBlank { "General Financial Support" })
        val formattedAmount = "₹%,.2f".format(amount)

        val html = """
            💰 <b>New Loan Request Submitted</b>
            
            <b>Borrower:</b> $safeName
            <b>Loan ID:</b> <code>$safeLoanId</code>
            <b>Amount:</b> <b>$formattedAmount</b>
            <b>Purpose:</b> $safePurpose
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
        val safeBorrower = escapeHtml(borrowerName)
        val safeLender = escapeHtml(lenderName)
        val safeLoanId = escapeHtml(loanId)

        val html = """
            ✍️ <b>Loan Agreement Signed & Finalized</b>
            
            <b>Borrower:</b> $safeBorrower
            <b>Lender:</b> $safeLender
            <b>Loan ID:</b> <code>$safeLoanId</code>
            <b>Status:</b> Fully Executed (eSigned) ✅
        """.trimIndent()

        val sanitizedUrl = sanitizeButtonUrl(agreementUrl)
        val buttonText = if (sanitizedUrl != null) "📜 View Signed Agreement" else null
        sendAdminAlert(html, buttonText, sanitizedUrl)
    }

    /**
     * Sends a message via Telegram Bot API with self-healing fallback mechanisms:
     * - Safely sanitizes and validates inline button URLs (guards against BUTTON_URL_INVALID)
     * - Truncates payload to MAX_TELEGRAM_MESSAGE_LENGTH (guards against 400 message is too long)
     * - Self-healing fallback: If Telegram throws a 400 parse error ("can't parse entities"),
     *   it strips HTML tags and retries as clean plain text.
     * - Self-healing fallback: If button URL is invalid, it retries without the button.
     * - Self-healing fallback: Stripped plain text with no button as an ultimate resilience net.
     */
    suspend fun sendMessage(
        chatId: String,
        messageHtml: String,
        buttonText: String? = null,
        buttonUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (chatId.isBlank() || messageHtml.isBlank()) return@withContext false

        // 1. Truncate text if needed
        val safeText = if (messageHtml.length > MAX_TELEGRAM_MESSAGE_LENGTH) {
            messageHtml.take(MAX_TELEGRAM_MESSAGE_LENGTH) + "\n\n<i>... [Message Truncated]</i>"
        } else {
            messageHtml
        }

        // 2. Validate button
        val sanitizedButtonUrl = sanitizeButtonUrl(buttonUrl)
        val validButtonText = if (sanitizedButtonUrl != null && !buttonText.isNullOrBlank()) buttonText.trim() else null

        // 3. Primary attempt: HTML format
        val firstAttemptResult = executeSendMessage(
            chatId = chatId,
            text = safeText,
            parseMode = "HTML",
            buttonText = validButtonText,
            buttonUrl = sanitizedButtonUrl
        )

        if (firstAttemptResult.isSuccess) {
            return@withContext true
        }

        val errorDescription = firstAttemptResult.errorDescription.lowercase()

        // 4. Self-Healing Fallback 1: HTML entity parsing error
        if (errorDescription.contains("can't parse entities") ||
            errorDescription.contains("tag") ||
            errorDescription.contains("parse")
        ) {
            Log.w(TAG, "HTML entity parsing error detected from Telegram: ${firstAttemptResult.errorDescription}. Falling back to plain text.")
            val plainText = stripHtml(safeText)
            val plainAttemptResult = executeSendMessage(
                chatId = chatId,
                text = plainText,
                parseMode = null,
                buttonText = validButtonText,
                buttonUrl = sanitizedButtonUrl
            )
            if (plainAttemptResult.isSuccess) {
                return@withContext true
            }
        }

        // 5. Self-Healing Fallback 2: Invalid button URL error
        if (errorDescription.contains("button_url_invalid") ||
            errorDescription.contains("unsupported url") ||
            errorDescription.contains("url")
        ) {
            Log.w(TAG, "Button URL invalid error detected from Telegram: ${firstAttemptResult.errorDescription}. Retrying without button.")
            val noButtonResult = executeSendMessage(
                chatId = chatId,
                text = safeText,
                parseMode = "HTML",
                buttonText = null,
                buttonUrl = null
            )
            if (noButtonResult.isSuccess) {
                return@withContext true
            }
        }

        // 6. Self-Healing Fallback 3: Both stripped plain text and no button (Ultimate resilience)
        val ultimateResult = executeSendMessage(
            chatId = chatId,
            text = stripHtml(safeText),
            parseMode = null,
            buttonText = null,
            buttonUrl = null
        )

        return@withContext ultimateResult.isSuccess
    }

    private data class SendResult(val isSuccess: Boolean, val errorDescription: String = "")

    private fun executeSendMessage(
        chatId: String,
        text: String,
        parseMode: String?,
        buttonText: String?,
        buttonUrl: String?
    ): SendResult {
        try {
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text)
                if (!parseMode.isNullOrBlank()) {
                    put("parse_mode", parseMode)
                }

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
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Telegram API error (${response.code}): $responseStr")
                    var desc = ""
                    try {
                        val obj = JSONObject(responseStr)
                        desc = obj.optString("description", "")
                    } catch (_: Exception) {}
                    return SendResult(false, desc.ifBlank { responseStr })
                } else {
                    Log.d(TAG, "Telegram alert dispatched successfully to $chatId")
                    return SendResult(true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed network call to Telegram for $chatId", e)
            return SendResult(false, e.message ?: "Network error")
        }
    }
}
