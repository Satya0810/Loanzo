package com.loanzo.app.data.repository

import android.content.Context
import android.util.Log
import com.loanzo.app.data.dao.LoanDao
import com.loanzo.app.data.dao.NotificationDao
import com.loanzo.app.data.dao.RepaymentDao
import com.loanzo.app.data.entity.NotificationEntity
import com.loanzo.app.data.dao.UserDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val loanDao: LoanDao,
    private val repaymentDao: RepaymentDao,
    private val userDao: UserDao,
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager
) {
    companion object {
        private const val TAG = "NotificationRepo"
    }

    fun getNotifications(userId: String): Flow<List<NotificationEntity>> {
        if (userId.isBlank()) return flowOf(emptyList())
        val isDirectAdmin = com.loanzo.app.util.VerificationManager.isAppOwner(userId = userId)
        return userDao.observeUser(userId).flatMapLatest { user ->
            val isAdmin = isDirectAdmin || com.loanzo.app.util.VerificationManager.isAppOwner(user) || user?.role?.uppercase() == "ADMIN"
            notificationDao.getNotificationsForUser(userId, isAdmin)
        }
    }

    fun getUnreadCount(userId: String): Flow<Int> {
        if (userId.isBlank()) return flowOf(0)
        val isDirectAdmin = com.loanzo.app.util.VerificationManager.isAppOwner(userId = userId)
        return userDao.observeUser(userId).flatMapLatest { user ->
            val isAdmin = isDirectAdmin || com.loanzo.app.util.VerificationManager.isAppOwner(user) || user?.role?.uppercase() == "ADMIN"
            notificationDao.getUnreadCount(userId, isAdmin)
        }
    }

    suspend fun markAsRead(notificationId: String) =
        notificationDao.markAsRead(notificationId)

    suspend fun markAllAsRead(userId: String) {
        val user = userDao.getUserById(userId)
        val isDirectAdmin = com.loanzo.app.util.VerificationManager.isAppOwner(userId = userId)
        val isAdmin = isDirectAdmin || com.loanzo.app.util.VerificationManager.isAppOwner(user) || user?.role?.uppercase() == "ADMIN"
        notificationDao.markAllAsRead(userId, isAdmin)
    }

    suspend fun deleteNotification(notificationId: String) =
        notificationDao.deleteNotification(notificationId)

    suspend fun clearAll(userId: String) {
        val user = userDao.getUserById(userId)
        val isDirectAdmin = com.loanzo.app.util.VerificationManager.isAppOwner(userId = userId)
        val isAdmin = isDirectAdmin || com.loanzo.app.util.VerificationManager.isAppOwner(user) || user?.role?.uppercase() == "ADMIN"
        notificationDao.clearAllForUser(userId, isAdmin)
    }

    suspend fun insertNotification(notification: NotificationEntity) =
        notificationDao.insertNotification(notification)

    /**
     * Scans all active loans for a user and generates deadline/overdue notifications.
     * This avoids creating duplicate alerts for the same loan + day + type combination.
     */
    suspend fun scanAndGenerateDeadlineNotifications(userId: String) {
        try {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = todayCal.timeInMillis
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDayKey = dayKeyFormat.format(Date())

            // Get all active and overdue loans the user is involved in (Bug #12: include OVERDUE status)
            val allLoans = loanDao.getAllLoansForUser(userId).first()
            val activeLoans = allLoans.filter { it.status == "ACTIVE" || it.status == "OVERDUE" || it.status == "DEFAULTED" }

            val newNotifications = mutableListOf<NotificationEntity>()

            for (loan in activeLoans) {
                val isBorrower = loan.borrowerId == userId
                val roleLabel = if (isBorrower) "borrower" else "lender"

                // Check for upcoming scheduled repayments
                val repayments = repaymentDao.getRepaymentsByLoan(loan.loanId).first()
                val scheduledRepayments = repayments.filter { it.status == "SCHEDULED" }
                val overdueRepayments = repayments.filter { it.status == "OVERDUE" }

                // Calculate EMI amount
                val emiAmount = if (loan.outstandingAmount > 0 && loan.tenureMonths > 0) {
                    val paidCount = repayments.count { it.status == "PAID" }
                    val remainingMonths = (loan.tenureMonths - paidCount).coerceAtLeast(1)
                    loan.outstandingAmount / remainingMonths
                } else {
                    loan.sanctionedAmount / loan.tenureMonths.coerceAtLeast(1).toDouble()
                }

                val formattedEmi = formatCurrency(emiAmount)

                // --- SCHEDULED REPAYMENTS: Due in 3 days, tomorrow, today ---
                for (repayment in scheduledRepayments) {
                    val dueDate = repayment.dueDate
                    val daysUntilDue = ((dueDate - todayStart) / (24 * 60 * 60 * 1000)).toInt()
                    val dueDateStr = dateFormat.format(Date(dueDate))

                    when {
                        daysUntilDue == 3 -> {
                            val dayKey = "${todayDayKey}_DEADLINE_3D_${loan.loanId}"
                            if (!notificationDao.existsNotification(userId, "DEADLINE", loan.loanId, dayKey)) {
                                newNotifications.add(
                                    NotificationEntity(
                                        notificationId = UUID.randomUUID().toString(),
                                        userId = userId,
                                        title = "⏰ EMI Due in 3 Days",
                                        message = "Your installment of $formattedEmi for '${loan.purpose}' is due on $dueDateStr. Keep funds ready!",
                                        type = "DEADLINE",
                                        relatedLoanId = loan.loanId,
                                        actionRoute = "loan_detail/${loan.loanId}",
                                        dayKey = dayKey
                                    )
                                )
                            }
                        }
                        daysUntilDue == 1 -> {
                            val dayKey = "${todayDayKey}_DEADLINE_1D_${loan.loanId}"
                            if (!notificationDao.existsNotification(userId, "DEADLINE", loan.loanId, dayKey)) {
                                newNotifications.add(
                                    NotificationEntity(
                                        notificationId = UUID.randomUUID().toString(),
                                        userId = userId,
                                        title = "⚡ EMI Due Tomorrow",
                                        message = "Your $formattedEmi installment for '${loan.purpose}' is due tomorrow ($dueDateStr).",
                                        type = "DEADLINE",
                                        relatedLoanId = loan.loanId,
                                        actionRoute = "loan_detail/${loan.loanId}",
                                        dayKey = dayKey
                                    )
                                )
                            }
                        }
                        daysUntilDue == 0 -> {
                            val dayKey = "${todayDayKey}_DEADLINE_TODAY_${loan.loanId}"
                            if (!notificationDao.existsNotification(userId, "DEADLINE", loan.loanId, dayKey)) {
                                newNotifications.add(
                                    NotificationEntity(
                                        notificationId = UUID.randomUUID().toString(),
                                        userId = userId,
                                        title = "🔔 EMI Due Today!",
                                        message = "Today is the last day to pay $formattedEmi for '${loan.purpose}'. Pay now to avoid penalties.",
                                        type = "DEADLINE",
                                        relatedLoanId = loan.loanId,
                                        actionRoute = "loan_detail/${loan.loanId}",
                                        dayKey = dayKey
                                    )
                                )
                            }
                        }
                    }
                }

                // --- OVERDUE REPAYMENTS ---
                for (repayment in overdueRepayments) {
                    val daysOverdue = ((todayStart - repayment.dueDate) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                    val dayKey = "${todayDayKey}_OVERDUE_${loan.loanId}"
                    if (!notificationDao.existsNotification(userId, "OVERDUE", loan.loanId, dayKey)) {
                        val penaltyInfo = if (loan.penaltyModel != "NONE" && loan.penaltyRate > 0) {
                            " Penalty of ${loan.penaltyRate}% may apply."
                        } else ""
                        newNotifications.add(
                            NotificationEntity(
                                notificationId = UUID.randomUUID().toString(),
                                userId = userId,
                                title = "⚠️ Payment Overdue ($daysOverdue days)",
                                message = "Your installment of ${formatCurrency(repayment.amount)} for '${loan.purpose}' is $daysOverdue day(s) overdue.$penaltyInfo",
                                type = "OVERDUE",
                                relatedLoanId = loan.loanId,
                                actionRoute = "loan_detail/${loan.loanId}",
                                dayKey = dayKey
                            )
                        )
                    }
                }

                // --- UNSIGNED AGREEMENTS ---
                if (loan.isAgreementSigned.not() && loan.agreementPdfUrl.isNotBlank()) {
                    val dayKey = "${todayDayKey}_AGREEMENT_${loan.loanId}"
                    if (!notificationDao.existsNotification(userId, "AGREEMENT", loan.loanId, dayKey)) {
                        val signerLabel = if (isBorrower && loan.borrowerSignedAt == null) "your" 
                            else if (!isBorrower && loan.lenderSignedAt == null) "your" 
                            else null
                        if (signerLabel != null) {
                            newNotifications.add(
                                NotificationEntity(
                                    notificationId = UUID.randomUUID().toString(),
                                    userId = userId,
                                    title = "📜 Agreement Awaiting Signature",
                                    message = "The loan agreement for '${loan.purpose}' ($formattedEmi/month) needs $signerLabel eSign.",
                                    type = "AGREEMENT",
                                    relatedLoanId = loan.loanId,
                                    actionRoute = "loan_detail/${loan.loanId}",
                                    dayKey = dayKey
                                )
                            )
                        }
                    }
                }

                // --- If no scheduled repayments exist, compute next EMI date from loan creation ---
                if (scheduledRepayments.isEmpty() && overdueRepayments.isEmpty() && isBorrower) {
                    val paidCount = repayments.count { it.status == "PAID" }
                    if (paidCount < loan.tenureMonths) {
                        val nextEmiCal = Calendar.getInstance().apply {
                            timeInMillis = loan.createdAt
                            add(Calendar.MONTH, paidCount + 1)
                        }
                        val nextEmiDate = nextEmiCal.timeInMillis
                        val daysUntilNext = ((nextEmiDate - todayStart) / (24 * 60 * 60 * 1000)).toInt()
                        val dueDateStr = dateFormat.format(Date(nextEmiDate))

                        if (daysUntilNext in 0..3) {
                            val label = when (daysUntilNext) {
                                0 -> "🔔 EMI Due Today!"
                                1 -> "⚡ EMI Due Tomorrow"
                                else -> "⏰ EMI Due in $daysUntilNext Days"
                            }
                            val dayKey = "${todayDayKey}_COMPUTED_EMI_${loan.loanId}"
                            if (!notificationDao.existsNotification(userId, "DEADLINE", loan.loanId, dayKey)) {
                                newNotifications.add(
                                    NotificationEntity(
                                        notificationId = UUID.randomUUID().toString(),
                                        userId = userId,
                                        title = label,
                                        message = "Estimated installment of $formattedEmi for '${loan.purpose}' is due on $dueDateStr.",
                                        type = "DEADLINE",
                                        relatedLoanId = loan.loanId,
                                        actionRoute = "loan_detail/${loan.loanId}",
                                        dayKey = dayKey
                                    )
                                )
                            }
                        }

                        // Check for overdue computed EMI
                        if (daysUntilNext < 0) {
                            val daysOverdue = -daysUntilNext
                            val dayKey = "${todayDayKey}_COMPUTED_OVERDUE_${loan.loanId}"
                            if (!notificationDao.existsNotification(userId, "OVERDUE", loan.loanId, dayKey)) {
                                newNotifications.add(
                                    NotificationEntity(
                                        notificationId = UUID.randomUUID().toString(),
                                        userId = userId,
                                        title = "⚠️ Estimated EMI Overdue ($daysOverdue days)",
                                        message = "An estimated installment of $formattedEmi for '${loan.purpose}' was due on $dueDateStr. Consider logging a repayment.",
                                        type = "OVERDUE",
                                        relatedLoanId = loan.loanId,
                                        actionRoute = "loan_detail/${loan.loanId}",
                                        dayKey = dayKey
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Batch insert all new notifications
            if (newNotifications.isNotEmpty()) {
                notificationDao.insertNotifications(newNotifications)
                Log.d(TAG, "Generated ${newNotifications.size} new notifications for user $userId")

                // Post Android system status-bar notification for highest priority item
                val highestPriority = newNotifications.firstOrNull { it.type == "OVERDUE" }
                    ?: newNotifications.firstOrNull { it.type == "DEADLINE" }
                if (highestPriority != null) {
                    postSystemNotification(highestPriority.title, highestPriority.message)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning deadlines", e)
        }
    }

    fun postSystemNotification(title: String, body: String, actionRoute: String? = null) {
        try {
            com.loanzo.app.util.NotificationChannelHelper.setupNotificationChannels(context)
            val isAdminAlert = actionRoute?.contains("app_owner_hub") == true || actionRoute?.contains("agent_main") == true
            val channelId = if (isAdminAlert) com.loanzo.app.util.NotificationChannelHelper.CHANNEL_ADMIN else com.loanzo.app.util.NotificationChannelHelper.CHANNEL_ALERTS
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            val intent = android.content.Intent(context, com.loanzo.app.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!actionRoute.isNullOrBlank()) {
                    putExtra("navigate_to", actionRoute)
                }
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.loanzo.app.R.mipmap.ic_launcher)
                .setContentTitle(title.replace(Regex("[⏰⚡🔔⚠️📜🚨👑⚖️]"), "").trim())
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post system notification", e)
        }
    }

    /**
     * Actively listens to real-time Firestore cloud notifications for the user
     * and persists them to the local Room database, popping an OS notification.
     * If the user is an Admin, also listens to shared "ADMIN" cloud notifications.
     */
    fun listenToCloudNotifications(userId: String, scope: kotlinx.coroutines.CoroutineScope) {
        if (userId.isBlank()) return
        val firestore = com.loanzo.app.data.firebase.FirestoreProvider.get()

        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            firebaseManager.ensureFirebaseAuthSession()
            // 1. Listen for user-specific notifications
            try {
                firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Cloud notifications listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            for (doc in snapshots.documentChanges) {
                                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                                    doc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED
                                ) {
                                    try {
                                        val entity = doc.document.toNotificationEntity()
                                        if (entity != null) {
                                            val alreadyExists = notificationDao.existsNotificationById(entity.notificationId)
                                            notificationDao.insertNotification(entity)
                                            // Bug #10: Only post system notification for genuinely new, recent, unread alerts
                                            if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED
                                                && !entity.isRead
                                                && !alreadyExists
                                                && (now - entity.timestamp) < 5 * 60 * 1000L // Only if < 5 min old
                                            ) {
                                                postSystemNotification(entity.title, entity.message, entity.actionRoute)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to parse cloud notification: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore not available for cloud notifications: ${e.message}")
            }
        }

        // 2. If user is Admin, ALSO listen for shared "ADMIN" cloud notifications
        val isDirectAdmin = com.loanzo.app.util.VerificationManager.isAppOwner(userId = userId)
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val user = userDao.getUserById(userId)
            val isAdmin = isDirectAdmin || com.loanzo.app.util.VerificationManager.isAppOwner(user) || user?.role?.uppercase() == "ADMIN"
            if (isAdmin) {
                try {
                    firebaseManager.ensureFirebaseAuthSession()
                    firestore.collection("notifications")
                        .whereEqualTo("userId", "ADMIN")
                        .addSnapshotListener { snapshots, error ->
                            if (error != null) {
                                Log.w(TAG, "Admin cloud notifications listener error: ${error.message}")
                                return@addSnapshotListener
                            }
                            if (snapshots != null && !snapshots.isEmpty) {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val now = System.currentTimeMillis()
                                    for (doc in snapshots.documentChanges) {
                                        if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                                            doc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED
                                        ) {
                                            try {
                                                val entity = doc.document.toNotificationEntity()
                                                if (entity != null) {
                                                    val alreadyExists = notificationDao.existsNotificationById(entity.notificationId)
                                                    notificationDao.insertNotification(entity)
                                                    // Bug #10: Only post system notification for genuinely new, recent, unread alerts
                                                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED
                                                        && !entity.isRead
                                                        && !alreadyExists
                                                        && (now - entity.timestamp) < 5 * 60 * 1000L
                                                    ) {
                                                        postSystemNotification(entity.title, entity.message, entity.actionRoute)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.w(TAG, "Failed to parse admin cloud notification: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Admin cloud notifications listener error: ${e.message}")
                }
            }
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toNotificationEntity(): NotificationEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(NotificationEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            val ts = when (val t = d["timestamp"]) {
                is Number -> t.toLong()
                is com.google.firebase.Timestamp -> t.toDate().time
                is String -> t.toLongOrNull() ?: System.currentTimeMillis()
                else -> System.currentTimeMillis()
            }
            NotificationEntity(
                notificationId = (d["notificationId"] as? String) ?: this.id,
                userId = (d["userId"] as? String) ?: "",
                title = (d["title"] as? String) ?: "",
                message = (d["message"] as? String) ?: "",
                type = (d["type"] as? String) ?: "SYSTEM",
                relatedLoanId = d["relatedLoanId"] as? String,
                timestamp = ts,
                isRead = (d["isRead"] as? Boolean) ?: false,
                actionRoute = d["actionRoute"] as? String,
                dayKey = (d["dayKey"] as? String) ?: ""
            )
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatted = String.format(Locale.US, "%,.0f", amount)
        return "₹$formatted"
    }
}
