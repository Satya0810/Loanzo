package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey
    val ticketId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val category: String = "GENERAL_QUERY", // LOAN_ISSUE, PAYMENT_DISPUTE, KYC_HELP, APP_BUG, ACCOUNT_ISSUE, COLLATERAL, AGENT_COMPLAINT, GENERAL_QUERY
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val subject: String = "",
    val description: String = "",
    val relatedLoanId: String? = null,
    val attachmentUris: String = "",
    val status: String = "OPEN", // OPEN, UNDER_REVIEW, CALLBACK_SCHEDULED, IN_PROGRESS, RESOLVED, CLOSED, ESCALATED, REJECTED
    val adminNotes: String? = null,
    val resolutionNotes: String? = null,
    val scheduledCallbackAt: Long? = null,
    val preferredCallbackAt: Long? = null,
    val resolvedAt: Long? = null,
    val feedbackRating: Int? = null, // 1-5 star rating
    val feedbackComment: String? = null,
    val feedbackSubmittedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
