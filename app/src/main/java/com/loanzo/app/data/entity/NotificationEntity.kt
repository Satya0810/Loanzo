package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val notificationId: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // DEADLINE, OVERDUE, DISBURSEMENT, AGREEMENT, REPAYMENT, SYSTEM
    val relatedLoanId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionRoute: String? = null,
    val dayKey: String = "" // e.g. "2026-09-03_DEADLINE_loanId" to prevent duplicate daily alerts
)
