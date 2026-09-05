package com.loanzo.app.data.repository

import android.content.Context
import android.util.Log
import com.loanzo.app.data.dao.LoanDao
import com.loanzo.app.data.dao.NotificationDao
import com.loanzo.app.data.dao.RepaymentDao
import com.loanzo.app.data.entity.NotificationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val loanDao: LoanDao,
    private val repaymentDao: RepaymentDao
) {
    companion object {
        private const val TAG = "NotificationRepo"
    }

    fun getNotifications(userId: String): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsForUser(userId)

    fun getUnreadCount(userId: String): Flow<Int> =
        notificationDao.getUnreadCount(userId)

    suspend fun markAsRead(notificationId: String) =
        notificationDao.markAsRead(notificationId)

    suspend fun markAllAsRead(userId: String) =
        notificationDao.markAllAsRead(userId)

    suspend fun deleteNotification(notificationId: String) =
        notificationDao.deleteNotification(notificationId)

    suspend fun clearAll(userId: String) =
        notificationDao.clearAllForUser(userId)

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

            // Get all active loans the user is involved in
            val allLoans = loanDao.getAllLoansForUser(userId).first()
            val activeLoans = allLoans.filter { it.status == "ACTIVE" }

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

    private fun postSystemNotification(title: String, body: String) {
        try {
            val channelId = "loanzo_deadline_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Loan Deadlines & Reminders",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Upcoming EMI deadlines, overdue payments, and loan alerts"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.loanzo.app.R.drawable.ic_launcher_foreground)
                .setContentTitle(title.replace(Regex("[⏰⚡🔔⚠️📜]"), "").trim())
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post system notification", e)
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatted = String.format(Locale.US, "%,.0f", amount)
        return "₹$formatted"
    }
}
