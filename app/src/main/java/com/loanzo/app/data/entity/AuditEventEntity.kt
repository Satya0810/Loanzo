package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey
    val eventId: String,
    val entityType: String, // LOAN, DISBURSEMENT, REPAYMENT, USER, PAYEE, PLEDGE
    val entityId: String,
    val actor: String, // userId
    val event: String, // CREATED, UPDATED, STATUS_CHANGED, APPROVED, REJECTED, VERIFIED, etc.
    val oldState: String = "",
    val newState: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val reference: String = ""
)
