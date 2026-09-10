package com.loanzo.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.loanzo.app.MainActivity
import com.loanzo.app.R
import com.loanzo.app.data.LoanzoDatabase
import com.loanzo.app.data.entity.NotificationEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoanzoMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var database: LoanzoDatabase

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Extract data payload fields (deep-link info)
        val data = remoteMessage.data
        val actionRoute = data["actionRoute"] ?: data["navigate_to"]
        val relatedLoanId = data["relatedLoanId"]
        val notificationType = data["type"] ?: "SYSTEM"

        // Determine title & body — prefer notification payload, fallback to data payload.
        // Only process ONCE to prevent duplicate system alerts (Bug #4).
        val title: String
        val body: String
        if (remoteMessage.notification != null) {
            title = remoteMessage.notification?.title ?: "Loanzo"
            body = remoteMessage.notification?.body ?: ""
        } else if (data.isNotEmpty()) {
            title = data["title"] ?: "Loanzo"
            body = data["body"] ?: ""
        } else {
            return // Nothing to process
        }

        Log.d(TAG, "Notification: title=$title, body=$body, actionRoute=$actionRoute")

        // Bug #5: Persist incoming push to local Room DB so it appears in in-app inbox
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isNotBlank()) {
            val entity = NotificationEntity(
                notificationId = "push_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
                userId = userId,
                title = title,
                message = body,
                type = notificationType,
                relatedLoanId = relatedLoanId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                actionRoute = actionRoute
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    database.notificationDao().insertNotification(entity)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to persist push notification to Room: ${e.message}")
                }
            }
        }

        // Bug #3: Pass actionRoute to the intent for deep-linking
        sendNotification(title, body, actionRoute)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Bug #1: Cache token locally in case user is not authenticated yet
        cacheTokenLocally(token)
        sendRegistrationToServer(token)
    }

    /**
     * Caches FCM token in SharedPreferences so it can be uploaded after login.
     */
    private fun cacheTokenLocally(token: String) {
        try {
            val prefs = getSharedPreferences("loanzo_fcm_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("pending_fcm_token", token).apply()
            Log.d(TAG, "FCM token cached locally for deferred upload")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache FCM token: ${e.message}")
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token == null) return
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "User not authenticated yet — token cached for deferred upload")
            return
        }
        val firestore = FirebaseFirestore.getInstance()
        val updates = hashMapOf<String, Any>("fcmToken" to token)
        firestore.collection("users").document(user.uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated in Firestore for user ${user.uid}")
                // Clear cached token since it's been uploaded
                try {
                    getSharedPreferences("loanzo_fcm_prefs", Context.MODE_PRIVATE)
                        .edit().remove("pending_fcm_token").apply()
                } catch (_: Exception) {}
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating FCM token", e)
            }

        // Also update local Room DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localUser = database.userDao().getUserById(user.uid)
                if (localUser != null) {
                    database.userDao().updateUser(localUser.copy(fcmToken = token))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update local fcmToken: ${e.message}")
            }
        }
    }

    /**
     * Bug #3: Now accepts actionRoute for deep-link intent.
     * Bug #4: Called only once per message (not duplicated).
     */
    private fun sendNotification(title: String, messageBody: String, actionRoute: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!actionRoute.isNullOrBlank()) {
                putExtra("navigate_to", actionRoute)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code per notification
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        com.loanzo.app.util.NotificationChannelHelper.setupNotificationChannels(this)
        val channelId = com.loanzo.app.util.NotificationChannelHelper.CHANNEL_ALERTS
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.replace(Regex("[⏰⚡🔔⚠️📜🚨👑⚖️]"), "").trim())
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "LoanzoMessagingService"
        const val CHANNEL_ID = "loanzo_alerts"

        /**
         * Bug #1: Call this after login/signup to upload any cached FCM token
         * and retrieve current token if none was cached.
         */
        fun registerFcmToken(context: Context, userId: String) {
            val firestore = FirebaseFirestore.getInstance()

            // 1. Check for cached token from onNewToken
            val prefs = context.getSharedPreferences("loanzo_fcm_prefs", Context.MODE_PRIVATE)
            val cachedToken = prefs.getString("pending_fcm_token", null)

            if (!cachedToken.isNullOrBlank()) {
                uploadToken(firestore, userId, cachedToken, context)
            }

            // 2. Also actively retrieve current token
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (token.isNotBlank()) {
                            uploadToken(firestore, userId, token, context)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to retrieve FCM token: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseMessaging not available: ${e.message}")
            }
        }

        private fun uploadToken(firestore: FirebaseFirestore, userId: String, token: String, context: Context) {
            firestore.collection("users").document(userId)
                .set(hashMapOf<String, Any>("fcmToken" to token), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token uploaded for user $userId")
                    try {
                        context.getSharedPreferences("loanzo_fcm_prefs", Context.MODE_PRIVATE)
                            .edit().remove("pending_fcm_token").apply()
                    } catch (_: Exception) {}
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to upload FCM token: ${e.message}")
                }
        }
    }
}
