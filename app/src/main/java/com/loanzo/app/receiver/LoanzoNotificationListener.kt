package com.loanzo.app.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.loanzo.app.data.LoanzoDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoanzoNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var database: LoanzoDatabase

    companion object {
        private const val TAG = "LoanzoNotification"
        
        // WhatsApp packages
        private val WHATSAPP_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        )
        
        // Default SMS/messaging app packages
        private val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",  // Google Messages
            "com.samsung.android.messaging",       // Samsung Messages
            "com.android.mms",                     // Stock Android MMS
            "com.oneplus.mms",                     // OnePlus Messages
            "com.xiaomi.mms",                      // Xiaomi Messages
            "com.miui.smsextra"                    // MIUI SMS
        )
        
        // All packages we want to intercept
        private val TARGET_PACKAGES = WHATSAPP_PACKAGES + SMS_PACKAGES
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName
        
        // Only process notifications from WhatsApp or SMS apps
        if (packageName !in TARGET_PACKAGES) return
        
        val extras = sbn.notification.extras
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        
        // Determine the channel type
        val channel = when (packageName) {
            in WHATSAPP_PACKAGES -> "WhatsApp (Auto-Intercept)"
            else -> "SMS Notification (Auto-Intercept)"
        }
        
        Log.d(TAG, "Intercepted $channel from: $title, Content: $text")

        // Check if the message contains Loanzo verification keywords
        if (text.contains("Loanzo", ignoreCase = true) && text.contains("Token", ignoreCase = true)) {
            val tokenRegex = Regex("""\b(\d{6})\b""")
            val match = tokenRegex.find(text)
            val token = match?.value

            val phoneRegex = Regex("""(\+?\d{10,13})""")
            val phoneMatch = phoneRegex.find(text)
            // Also try to extract phone from the sender/title (useful for unsaved contacts)
            val senderPhoneMatch = phoneRegex.find(title)
            val phone = phoneMatch?.value ?: senderPhoneMatch?.value ?: title

            if (token != null) {
                Log.d(TAG, "Extracted token=$token, phone=$phone via $channel")
                processVerificationToken(token, phone, channel)
            }
        }
    }

    private fun processVerificationToken(token: String, phone: String, channel: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cleanPhone = phone.replace("\\D".toRegex(), "").takeLast(10)
                
                // Step 1: Check if we have a matching PENDING entry in local DB
                val existingByToken = database.verificationDao().getByToken(token)
                
                if (existingByToken != null && existingByToken.status == "PENDING") {
                    // Perfect match — this token was already registered as PENDING from Firestore sync
                    // Mark it verified with the matching phone
                    Log.d(TAG, "Token $token matched PENDING entry for phone ${existingByToken.phone}. Verifying...")
                    database.verificationDao().markAsVerified(
                        token = token,
                        phone = existingByToken.phone,
                        verifiedAt = System.currentTimeMillis()
                    )
                    // Update Firestore using the phone from the PENDING entry (more reliable)
                    val pendingCleanPhone = existingByToken.phone.replace("\\D".toRegex(), "").takeLast(10)
                    updateFirestoreVerification(pendingCleanPhone)
                } else if (existingByToken != null) {
                    // Already verified or processed, skip
                    Log.d(TAG, "Token $token already processed with status ${existingByToken.status}")
                } else {
                    // No existing entry — this token arrived before Firestore sync populated local DB
                    // Create a new VERIFIED entry directly so the dashboard sees it
                    Log.d(TAG, "Token $token not found locally. Creating new VERIFIED entry.")
                    database.verificationDao().insertVerification(
                        com.loanzo.app.data.entity.VerificationEntity(
                            token = token,
                            phone = phone,
                            channel = channel,
                            status = "VERIFIED",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    // Update Firestore
                    if (cleanPhone.isNotEmpty()) {
                        updateFirestoreVerification(cleanPhone)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing token", e)
                e.printStackTrace()
            }
        }
    }

    private fun updateFirestoreVerification(cleanPhone: String) {
        if (cleanPhone.isEmpty()) return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("verifications")
            .document(cleanPhone)
            .set(mapOf("status" to "VERIFIED"), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "Firestore updated to VERIFIED for phone $cleanPhone") }
            .addOnFailureListener { Log.w(TAG, "Failed to update Firestore for phone $cleanPhone", it) }
    }
}
