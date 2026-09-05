package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey
    val complaintId: String,
    val complainantId: String,
    val complainantName: String,
    val complainantRole: String, // BORROWER, LENDER, AGENT
    val complainantPhone: String,
    val targetPartyId: String? = null,
    val targetPartyName: String? = null,
    val targetPartyRole: String? = null, // BORROWER, LENDER, AGENT
    val loanId: String? = null,
    val category: String, // DELAYED_PAYMENT, COLLATERAL_CUSTODY, AGENT_CONDUCT, INTEREST_DISPUTE, TECHNICAL_APP_BUG, OTHER
    val priority: String, // CRITICAL_LEGAL, HIGH, MEDIUM, LOW
    val subject: String,
    val description: String,
    val evidenceUris: String = "",
    val status: String, // OPEN, INVESTIGATING, HEARING_SCHEDULED, RESOLVED, DISMISSED
    val resolutionNotes: String? = null,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
