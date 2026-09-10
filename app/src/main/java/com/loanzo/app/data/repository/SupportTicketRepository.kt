package com.loanzo.app.data.repository

import com.loanzo.app.data.dao.NotificationDao
import com.loanzo.app.data.dao.SupportTicketDao
import com.loanzo.app.data.entity.NotificationEntity
import com.loanzo.app.data.entity.SupportTicketEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.util.TelegramManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportTicketRepository @Inject constructor(
    private val supportTicketDao: SupportTicketDao,
    private val notificationDao: NotificationDao,
    private val telegramManager: TelegramManager
) {

    // --- Flows ---

    val allTickets: Flow<List<SupportTicketEntity>> = supportTicketDao.getAllTickets()

    fun getTicketsByStatus(status: String): Flow<List<SupportTicketEntity>> =
        supportTicketDao.getTicketsByStatus(status)

    fun getTicketsForUser(userId: String): Flow<List<SupportTicketEntity>> =
        supportTicketDao.getTicketsForUser(userId)

    fun getPendingFeedbackTickets(userId: String): Flow<List<SupportTicketEntity>> =
        supportTicketDao.getPendingFeedbackTickets(userId)

    fun getPendingFeedbackCount(userId: String): Flow<Int> =
        supportTicketDao.getPendingFeedbackCount(userId)

    fun getOpenTicketCount(): Flow<Int> = supportTicketDao.getOpenTicketCount()

    fun observeTicket(ticketId: String): Flow<SupportTicketEntity?> =
        supportTicketDao.observeTicketById(ticketId)

    // --- Ticket Creation ---

    suspend fun createTicket(
        user: UserEntity,
        category: String,
        priority: String,
        subject: String,
        description: String,
        relatedLoanId: String? = null,
        attachmentUris: String = "",
        preferredCallbackAt: Long? = null
    ): SupportTicketEntity {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val datePart = dateFormat.format(Date())
        val randomPart = (1000..9999).random()
        val ticketId = "TKT-$datePart-$randomPart"

        val ticket = SupportTicketEntity(
            ticketId = ticketId,
            userId = user.userId,
            userName = user.name,
            userPhone = user.phone,
            userEmail = user.email,
            category = category,
            priority = priority,
            subject = subject,
            description = description,
            relatedLoanId = relatedLoanId,
            attachmentUris = attachmentUris,
            preferredCallbackAt = preferredCallbackAt,
            status = "OPEN"
        )

        supportTicketDao.insertTicket(ticket)

        // 1. Push to Firestore for Cloud Admin Delivery
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("support_tickets")
                .document(ticket.ticketId)
                .set(ticket)
        } catch (_: Exception) {}

        // 2. Notify user of successful submission (Room + Firestore)
        val userNotif = NotificationEntity(
            notificationId = "notif_ticket_created_${ticket.ticketId}",
            userId = user.userId,
            title = "Ticket Raised Successfully",
            message = "Your support ticket $ticketId has been raised. Our team will review and call you back shortly.",
            type = "SYSTEM",
            actionRoute = "ticket_detail/${ticket.ticketId}",
            dayKey = "${datePart}_TICKET_CREATED_${ticket.ticketId}",
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(userNotif)
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(userNotif.notificationId)
                .set(userNotif)
        } catch (_: Exception) {}

        // 2b. Notify Admin in Cloud of incoming ticket
        val adminNotif = NotificationEntity(
            notificationId = "notif_adm_tkt_${ticket.ticketId}",
            userId = "ADMIN",
            title = "🎫 New Support Ticket: $ticketId",
            message = "${user.name} (@${user.username}) raised a $priority priority ticket: ${ticket.subject}",
            type = "COMPLAINT",
            actionRoute = "app_owner_hub?tab=2",
            dayKey = "${datePart}_ADMIN_TKT_${ticket.ticketId}",
            timestamp = System.currentTimeMillis()
        )
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(adminNotif.notificationId)
                .set(adminNotif)
        } catch (_: Exception) {}

        // 3. Send Telegram alert to Admin
        val categoryLabel = category.replace("_", " ")
        val priorityEmoji = when (priority) {
            "URGENT" -> "🔴"
            "HIGH" -> "🟠"
            "MEDIUM" -> "🟡"
            else -> "🟢"
        }
        try {
            val digits = user.phone.filter { it.isDigit() }
            val waUrl = if (digits.isNotBlank()) "https://wa.me/$digits" else null
            telegramManager.sendAdminAlert(
                messageHtml = """
                    🎫 <b>New Support Ticket Raised</b>
                    
                    <b>Ticket:</b> ${TelegramManager.escapeHtml(ticketId)}
                    <b>User:</b> ${TelegramManager.escapeHtml(user.name)} (@${TelegramManager.escapeHtml(user.username)})
                    <b>Phone:</b> ${TelegramManager.escapeHtml(user.phone)}
                    <b>Category:</b> ${TelegramManager.escapeHtml(categoryLabel)}
                    <b>Priority:</b> $priorityEmoji ${TelegramManager.escapeHtml(priority)}
                    <b>Subject:</b> ${TelegramManager.escapeHtml(subject)}
                    
                    <i>${TelegramManager.escapeHtml(description.take(200))}${if (description.length > 200) "..." else ""}</i>
                """.trimIndent(),
                actionButtonText = if (waUrl != null) "💬 WhatsApp User" else null,
                actionButtonUrl = waUrl
            )
        } catch (_: Exception) {
            // Telegram alert is best-effort, don't fail ticket creation
        }

        return ticket
    }

    // --- Admin Actions ---

    suspend fun markUnderReview(ticketId: String, adminNotes: String? = null) {
        supportTicketDao.updateTicketStatus(ticketId, "UNDER_REVIEW", adminNotes)
        syncTicketToCloud(ticketId, "UNDER_REVIEW", adminNotes)
        notifyUser(ticketId, "Ticket Under Review", "Your support ticket $ticketId is now being reviewed by our team.")
    }

    suspend fun scheduleCallback(ticketId: String, callbackAt: Long, adminNotes: String? = null) {
        supportTicketDao.scheduleCallback(ticketId, callbackAt, adminNotes)
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "status" to "CALLBACK_SCHEDULED",
                    "scheduledCallbackAt" to callbackAt,
                    "adminNotes" to (adminNotes ?: ""),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val callbackDate = dateFormat.format(Date(callbackAt))
        notifyUser(ticketId, "Callback Scheduled", "Our team will call you on $callbackDate regarding ticket $ticketId.")
    }

    suspend fun markInProgress(ticketId: String, adminNotes: String? = null) {
        supportTicketDao.updateTicketStatus(ticketId, "IN_PROGRESS", adminNotes)
        syncTicketToCloud(ticketId, "IN_PROGRESS", adminNotes)
    }

    suspend fun resolveTicket(ticketId: String, resolutionNotes: String) {
        supportTicketDao.resolveTicket(ticketId, resolutionNotes)
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "status" to "RESOLVED",
                    "resolutionNotes" to resolutionNotes,
                    "resolvedAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
        notifyUser(ticketId, "Ticket Resolved ✅", "Your support ticket $ticketId has been resolved. Please share your feedback!")
    }

    suspend fun escalateTicket(ticketId: String, adminNotes: String?) {
        supportTicketDao.updateTicketStatus(ticketId, "ESCALATED", adminNotes)
        syncTicketToCloud(ticketId, "ESCALATED", adminNotes)
        notifyUser(ticketId, "Ticket Escalated", "Your support ticket $ticketId has been escalated to senior management for priority resolution.")
    }

    suspend fun rejectTicket(ticketId: String, adminNotes: String?) {
        supportTicketDao.updateTicketStatus(ticketId, "REJECTED", adminNotes)
        syncTicketToCloud(ticketId, "REJECTED", adminNotes)
        notifyUser(ticketId, "Ticket Update", "Your support ticket $ticketId has been updated. Please check for details.")
    }

    // --- User Actions ---

    suspend fun submitFeedback(ticketId: String, rating: Int, comment: String?) {
        supportTicketDao.submitFeedback(ticketId, rating, comment)
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "feedbackRating" to rating,
                    "feedbackComment" to (comment ?: ""),
                    "feedbackSubmittedAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    private fun syncTicketToCloud(ticketId: String, status: String, adminNotes: String? = null) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            if (adminNotes != null) updates["adminNotes"] = adminNotes
            firestore.collection("support_tickets").document(ticketId).update(updates)
        } catch (_: Exception) {}
    }

    /**
     * Real-time listener: Continuously syncs tickets from Firestore into Room.
     * Guarantees tickets submitted by real users appear immediately in the Admin console.
     */
    fun listenToCloudTickets(scope: kotlinx.coroutines.CoroutineScope) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("support_tickets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val ticket = doc.toObject(SupportTicketEntity::class.java)
                            if (ticket != null && ticket.ticketId.isNotBlank()) {
                                supportTicketDao.insertTicket(ticket)
                            }
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    // --- Helper ---

    private suspend fun notifyUser(ticketId: String, title: String, message: String) {
        val ticket = supportTicketDao.getTicketById(ticketId) ?: return
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val datePart = dateFormat.format(Date())
        val notif = NotificationEntity(
            notificationId = "notif_ticket_${ticket.status}_${ticket.ticketId}",
            userId = ticket.userId,
            title = title,
            message = message,
            type = "SYSTEM",
            actionRoute = "ticket_detail/${ticket.ticketId}",
            dayKey = "${datePart}_TICKET_${ticket.status}_${ticket.ticketId}",
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notif.notificationId)
                .set(notif)
        } catch (_: Exception) {}
    }

    // --- Demo ---

    suspend fun deleteDemoTickets() {
        supportTicketDao.deleteDemoTickets()
    }

    suspend fun insertTickets(tickets: List<SupportTicketEntity>) {
        supportTicketDao.insertTickets(tickets)
    }
}
