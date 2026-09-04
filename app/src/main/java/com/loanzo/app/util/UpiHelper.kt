package com.loanzo.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * UPI Deep-link helper for payments.
 *
 * Generates UPI payment intents that launch the user's UPI app.
 * Note: This initiates a payment request — actual settlement confirmation
 * requires a separate mechanism (user enters transaction reference).
 */
object UpiHelper {

    /**
     * Create a UPI payment intent.
     *
     * @param payeeUpiId UPI VPA of the payee (e.g., "merchant@upi")
     * @param payeeName Display name of the payee
     * @param amount Payment amount in INR
     * @param transactionNote Description/note for the transaction
     * @param transactionRef Optional merchant transaction reference
     * @return Intent that launches UPI app chooser
     */
    fun createPaymentIntent(
        payeeUpiId: String,
        payeeName: String,
        amount: Double,
        transactionNote: String,
        transactionRef: String = ""
    ): Intent {
        val uri = buildString {
            append("upi://pay?")
            append("pa=$payeeUpiId")
            append("&pn=${Uri.encode(payeeName)}")
            append("&am=${String.format(java.util.Locale.US, "%.2f", amount)}")
            append("&cu=INR")
            append("&tn=${Uri.encode(transactionNote)}")
            if (transactionRef.isNotBlank()) {
                append("&tr=$transactionRef")
            }
        }

        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)
        }
    }

    /**
     * Check if any UPI app is available on the device.
     */
    fun isUpiAvailable(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("upi://pay")
        }
        return intent.resolveActivity(context.packageManager) != null
    }

    /**
     * Generate a UPI deep-link URL string for display purposes.
     */
    fun generateDeepLinkUrl(
        payeeUpiId: String,
        payeeName: String,
        amount: Double,
        transactionNote: String
    ): String {
        return buildString {
            append("upi://pay?")
            append("pa=$payeeUpiId")
            append("&pn=${Uri.encode(payeeName)}")
            append("&am=${String.format(java.util.Locale.US, "%.2f", amount)}")
            append("&cu=INR")
            append("&tn=${Uri.encode(transactionNote)}")
        }
    }
}
