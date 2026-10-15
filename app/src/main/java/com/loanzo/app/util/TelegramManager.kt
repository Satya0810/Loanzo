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
        val BOT_TOKEN: String get() = com.loanzo.app.BuildConfig.TELEGRAM_BOT_TOKEN
        const val BOT_USERNAME = "Loanzo_bot"
        const val BOT_URL = "https://t.me/$BOT_USERNAME"

        // Sole designated Administrator username and primary verified Chat ID for @satyam_081
        const val ADMIN_USERNAME = "satyam_081"
        val ADMIN_CHAT_IDS = listOf(8234574147L)

        private val TELEGRAM_API_URL: String get() = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
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

        /**
         * Opens Telegram with a deep link to link the user's account with the bot.
         * Prefers direct app scheme (`tg://resolve?domain=Loanzo_bot&start=user_$userId`),
         * falling back smoothly to web browser (`https://t.me/Loanzo_bot?start=user_$userId`),
         * and shows a friendly Toast if no suitable app or browser is available.
         */
        fun openTelegramBot(context: Context, userId: String) {
            openBotForLinking(context, userId)
        }

        fun openBotForLinking(context: Context, userId: String, telegramUsername: String? = null) {
            val cleanUserId = userId.trim()
            val cleanUsername = telegramUsername?.trim()?.removePrefix("@") ?: ""
            val startParam = if (cleanUsername.isNotBlank()) {
                "user_${cleanUserId}_$cleanUsername"
            } else if (cleanUserId.isNotBlank()) {
                "user_$cleanUserId"
            } else {
                "app_launch"
            }
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
    }

    private val client: OkHttpClient get() = sharedClient

    fun openBotForLinking(context: Context, userId: String, telegramUsername: String? = null) {
        Companion.openBotForLinking(context, userId, telegramUsername)
    }

    fun openTelegramBot(context: Context, userId: String) {
        Companion.openBotForLinking(context, userId)
    }

    data class TelegramActionButton(
        val text: String,
        val url: String? = null,
        val callbackData: String? = null
    )

    /**
     * Sends an alert to all registered Admin chat IDs with multi-action interactive buttons.
     */
    suspend fun sendActionableAdminAlert(
        messageHtml: String,
        buttons: List<List<TelegramActionButton>>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var anySuccess = false
        for (chatId in ADMIN_CHAT_IDS) {
            val ok = sendMessage(chatId.toString(), messageHtml, buttons = buttons)
            if (ok) anySuccess = true
        }
        anySuccess
    }

    /**
     * Sends an alert to all registered Admin chat IDs (backward-compatible overload).
     */
    suspend fun sendAdminAlert(
        messageHtml: String,
        actionButtonText: String? = null,
        actionButtonUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val buttons = if (!actionButtonText.isNullOrBlank() && !actionButtonUrl.isNullOrBlank()) {
            listOf(listOf(TelegramActionButton(actionButtonText, url = actionButtonUrl)))
        } else null
        sendActionableAdminAlert(messageHtml, buttons)
    }

    /**
     * Sends a KYC upload notification to Admins with direct interactive approval & rejection actions.
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
        val buttons = mutableListOf<List<TelegramActionButton>>()
        buttons.add(
            listOf(
                TelegramActionButton("✅ Approve KYC", callbackData = "approve_kyc_$safeUserId"),
                TelegramActionButton("❌ Reject KYC", callbackData = "reject_kyc_$safeUserId")
            )
        )
        val row2 = mutableListOf<TelegramActionButton>()
        if (sanitizedUrl != null) {
            row2.add(TelegramActionButton("📄 View Document", url = sanitizedUrl))
        }
        row2.add(TelegramActionButton("👤 Inspect Profile", callbackData = "view_user_$safeUserId"))
        buttons.add(row2)

        sendActionableAdminAlert(html, buttons)
    }

    /**
     * Sends a Loan Request notification to Admins with direct interactive approval & rejection actions.
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

        val buttons = listOf(
            listOf(
                TelegramActionButton("✅ Approve Loan", callbackData = "approve_loan_$safeLoanId"),
                TelegramActionButton("❌ Reject Loan", callbackData = "reject_loan_$safeLoanId")
            ),
            listOf(
                TelegramActionButton("🔍 Inspect Collateral", callbackData = "inspect_loan_$safeLoanId")
            )
        )

        sendActionableAdminAlert(html, buttons)
    }

    /**
     * Sends a Signed Agreement notification to Admins with disbursal authorization actions.
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
        val row1 = mutableListOf<TelegramActionButton>()
        if (sanitizedUrl != null) {
            row1.add(TelegramActionButton("📜 View Agreement", url = sanitizedUrl))
        }
        row1.add(TelegramActionButton("💸 Authorize Disbursal", callbackData = "disburse_loan_$safeLoanId"))

        sendActionableAdminAlert(html, listOf(row1))
    }

    /**
     * Sends an Emergency Statutory SOS Alert with instant freeze & agent dispatch actions.
     */
    suspend fun notifyEmergencySos(
        loanId: String,
        borrowerName: String,
        borrowerPhone: String,
        lenderName: String,
        violationType: String,
        details: String
    ) {
        val safeLoanId = escapeHtml(loanId)
        val safeBorrower = escapeHtml(borrowerName)
        val safePhone = escapeHtml(borrowerPhone)
        val safeLender = escapeHtml(lenderName)
        val safeViolation = escapeHtml(violationType)
        val safeDetails = escapeHtml(details.ifBlank { "Immediate emergency triggered by borrower." })

        val html = """
            🚨 <b>STATUTORY HARASSMENT & COERCION SOS ALERT</b>
            
            <b>Loan ID:</b> <code>$safeLoanId</code>
            <b>Borrower:</b> $safeBorrower ($safePhone)
            <b>Lender:</b> $safeLender
            <b>Violation Type:</b> $safeViolation
            <b>Details:</b> $safeDetails
            
            <i>STATUTORY CITATION:</i>
            Recorded under <b>Bharatiya Nyaya Sanhita 2023 (§351/352)</b> and <b>RBI Digital Lending Guidelines (2022)</b>.
            Late fees & collection calls are administratively frozen pending inquiry.
        """.trimIndent()

        val buttons = listOf(
            listOf(
                TelegramActionButton("🛑 Freeze Recovery Calls", callbackData = "freeze_recovery_$safeLoanId"),
                TelegramActionButton("👮 Dispatch Field Agent", callbackData = "dispatch_agent_$safeLoanId")
            ),
            listOf(
                TelegramActionButton("⚖️ Legal Moratorium Notice", callbackData = "legal_notice_$safeLoanId")
            )
        )

        sendActionableAdminAlert(html, buttons)
    }

    /**
     * Sends an Incident Report or Grievance Alert with moderation actions.
     */
    suspend fun notifyDisputeReport(
        caseId: String,
        reporterName: String,
        reporterPhone: String,
        targetPerson: String,
        category: String,
        severity: String,
        details: String
    ) {
        val safeCaseId = escapeHtml(caseId)
        val safeReporter = escapeHtml(reporterName)
        val safeTarget = escapeHtml(targetPerson)
        val safeCategory = escapeHtml(category)
        val safeSeverity = escapeHtml(severity)
        val safeDetails = escapeHtml(details.ifBlank { "N/A" })

        val html = """
            ⚠️ <b>INCIDENT REPORT & COMPLAINT FILED</b>
            
            <b>Case ID:</b> <code>$safeCaseId</code>
            <b>Reporter:</b> $safeReporter ($reporterPhone)
            <b>Target Person:</b> $safeTarget
            <b>Category:</b> $safeCategory
            <b>Severity:</b> $safeSeverity
            <b>Details:</b> $safeDetails
        """.trimIndent()

        val buttons = listOf(
            listOf(
                TelegramActionButton("🚨 Restrict Bad Actor", callbackData = "ban_user_$safeTarget"),
                TelegramActionButton("✅ Resolve Dispute", callbackData = "resolve_dispute_$safeCaseId")
            ),
            listOf(
                TelegramActionButton("👤 Inspect Target", callbackData = "view_user_$safeTarget")
            )
        )

        sendActionableAdminAlert(html, buttons)
    }

    /**
     * Sends a message via Telegram Bot API with self-healing fallback mechanisms:
     * Supports both single legacy URL button and multi-row action buttons (with callback_data).
     */
    suspend fun sendMessage(
        chatId: String,
        messageHtml: String,
        buttonText: String? = null,
        buttonUrl: String? = null,
        buttons: List<List<TelegramActionButton>>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (chatId.isBlank() || messageHtml.isBlank()) return@withContext false

        // 1. Truncate text if needed
        val safeText = if (messageHtml.length > MAX_TELEGRAM_MESSAGE_LENGTH) {
            messageHtml.take(MAX_TELEGRAM_MESSAGE_LENGTH) + "\n\n<i>... [Message Truncated]</i>"
        } else {
            messageHtml
        }

        // 2. Normalise buttons
        val effectiveButtons = when {
            buttons != null -> buttons
            !buttonText.isNullOrBlank() && !buttonUrl.isNullOrBlank() -> {
                val sanitized = sanitizeButtonUrl(buttonUrl)
                if (sanitized != null) listOf(listOf(TelegramActionButton(buttonText.trim(), url = sanitized))) else null
            }
            else -> null
        }

        // 3. Primary attempt: HTML format
        val firstAttemptResult = executeSendMessage(
            chatId = chatId,
            text = safeText,
            parseMode = "HTML",
            buttons = effectiveButtons
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
                buttons = effectiveButtons
            )
            if (plainAttemptResult.isSuccess) {
                return@withContext true
            }
        }

        // 5. Self-Healing Fallback 2: Invalid button URL error -> retry without buttons
        if (errorDescription.contains("button_url_invalid") ||
            errorDescription.contains("unsupported url") ||
            errorDescription.contains("url")
        ) {
            Log.w(TAG, "Button URL invalid error detected from Telegram: ${firstAttemptResult.errorDescription}. Retrying without button.")
            val noButtonResult = executeSendMessage(
                chatId = chatId,
                text = safeText,
                parseMode = "HTML",
                buttons = null
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
            buttons = null
        )

        return@withContext ultimateResult.isSuccess
    }

    private data class SendResult(val isSuccess: Boolean, val errorDescription: String = "")

    private fun executeSendMessage(
        chatId: String,
        text: String,
        parseMode: String?,
        buttons: List<List<TelegramActionButton>>?
    ): SendResult {
        try {
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text)
                if (!parseMode.isNullOrBlank()) {
                    put("parse_mode", parseMode)
                }

                if (!buttons.isNullOrEmpty()) {
                    val inlineKeyboard = JSONArray().apply {
                        for (row in buttons) {
                            val rowArray = JSONArray()
                            for (btn in row) {
                                val btnJson = JSONObject().apply {
                                    put("text", btn.text)
                                    if (!btn.url.isNullOrBlank()) {
                                        put("url", btn.url)
                                    } else if (!btn.callbackData.isNullOrBlank()) {
                                        put("callback_data", btn.callbackData)
                                    }
                                }
                                rowArray.put(btnJson)
                            }
                            if (rowArray.length() > 0) {
                                put(rowArray)
                            }
                        }
                    }
                    if (inlineKeyboard.length() > 0) {
                        put("reply_markup", JSONObject().apply {
                            put("inline_keyboard", inlineKeyboard)
                        })
                    }
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
