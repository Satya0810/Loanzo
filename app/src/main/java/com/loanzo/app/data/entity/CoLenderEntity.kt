package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a participating lender in a syndicated or co-lending loan.
 * Allows multiple capital providers (Lead Lender and Co-Lenders) to pool funds
 * for a single borrower loan, with automatic pro-rata interest and EMI distributions.
 */
@Entity(
    tableName = "co_lenders",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["loanId"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["loanId"]),
        Index(value = ["lenderId"])
    ]
)
data class CoLenderEntity(
    @PrimaryKey
    val coLenderId: String,               // Unique ID: e.g. "colender_loan123_romesh321"
    val loanId: String,                   // Linked Loan ID
    val lenderId: String,                 // Canonical User ID of Co-Lender
    val name: String,                     // Co-Lender display name
    val phone: String = "",               // Contact phone
    val contributedAmount: Double = 0.0,  // Capital committed: e.g. ₹30,000
    val sharePercentage: Double = 0.0,    // Equity percentage: e.g. 30.0 for 30%
    val role: String = "CO_LENDER",       // "LEAD_LENDER" or "CO_LENDER"
    val status: String = "ACTIVE",        // "PENDING_CONSENT", "ACTIVE", "COMPLETED"
    val signedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
