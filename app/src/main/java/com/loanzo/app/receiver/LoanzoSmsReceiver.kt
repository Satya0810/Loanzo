package com.loanzo.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.loanzo.app.data.LoanzoDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class LoanzoSmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: LoanzoDatabase

    companion object {
        private const val TAG = "LoanzoSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                val sender = sms.originatingAddress ?: ""

                if (body.contains("Loanzo", ignoreCase = true) && body.contains("Token", ignoreCase = true)) {
                    val tokenRegex = Regex("""\b(\d{6})\b""")
                    val match = tokenRegex.find(body)
                    val token = match?.value

                    val phoneRegex = Regex("""(\+?\d{10,13})""")
                    val phoneMatch = phoneRegex.find(body)
                    val phone = phoneMatch?.value ?: sender

                    if (token != null) {
                        Log.d(TAG, "Intercepted SMS token=$token, phone=$phone from sender=$sender")
                        processVerificationToken(token, phone)
                    }
                } else if ((body.contains("credited", ignoreCase = true) || body.contains("credit", ignoreCase = true)) 
                    && (body.contains("1.00") || body.contains("Rs 1") || body.contains("INR 1"))) {
                    // This looks like a Penny Drop!
                    Log.d(TAG, "Intercepted potential Penny Drop SMS: $body")
                    processPennyDrop(body)
                }
            }
        }
    }

    private fun processVerificationToken(token: String, phone: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cleanPhone = phone.replace("\\D".toRegex(), "").takeLast(10)

                // Check if we have a matching PENDING entry in local DB
                val existingByToken = database.verificationDao().getByToken(token)

                if (existingByToken != null && existingByToken.status == "PENDING") {
                    // Token matched a pending verification — mark as verified
                    Log.d(TAG, "Token $token matched PENDING entry for phone ${existingByToken.phone}. Verifying...")
                    database.verificationDao().markAsVerified(
                        token = token,
                        phone = existingByToken.phone,
                        verifiedAt = System.currentTimeMillis()
                    )
                    val pendingCleanPhone = existingByToken.phone.replace("\\D".toRegex(), "").takeLast(10)
                    updateFirestoreVerification(pendingCleanPhone)
                } else if (existingByToken != null) {
                    // Already processed
                    Log.d(TAG, "Token $token already processed with status ${existingByToken.status}")
                } else {
                    // No pending entry found — create a new VERIFIED entry directly
                    Log.d(TAG, "Token $token not found locally. Creating new VERIFIED entry.")
                    database.verificationDao().insertVerification(
                        com.loanzo.app.data.entity.VerificationEntity(
                            token = token,
                            phone = phone,
                            channel = "SMS (Auto-Intercept)",
                            status = "VERIFIED",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    if (cleanPhone.isNotEmpty()) {
                        updateFirestoreVerification(cleanPhone)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS token", e)
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

    private fun processPennyDrop(smsBody: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all users who have an unverified bank account attached
                val pendingUsers = database.userDao().getAllUsers().first()
                    .filter { it.bankAccountNumber.isNotBlank() && !it.bankVerified }

                for (user in pendingUsers) {
                    val accNumber = user.bankAccountNumber
                    // Get the last 4 digits (or the whole number if it's short)
                    val last4 = if (accNumber.length > 4) accNumber.takeLast(4) else accNumber

                    // If the SMS contains the last 4 digits of this user's account
                    if (smsBody.contains(last4)) {
                        Log.d(TAG, "Penny Drop Matched! Verifying account $last4 for User ${user.userId}")
                        
                        // Update Room Database
                        val updatedUser = user.copy(bankVerified = true)
                        database.userDao().insertUser(updatedUser)

                        // Update Firestore to sync the new verified state
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.userId)
                            .set(mapOf("bankVerified" to true), com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener { Log.d(TAG, "Firestore updated bankVerified for ${user.userId}") }
                        
                        // We found a match, no need to check other users
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Penny Drop SMS", e)
            }
        }
    }
}
