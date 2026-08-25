package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "disbursements",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["loanId"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PayeeEntity::class,
            parentColumns = ["payeeId"],
            childColumns = ["payeeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["loanId"]),
        Index(value = ["payeeId"])
    ]
)
data class DisbursementEntity(
    @PrimaryKey
    val disbursementId: String,
    val loanId: String,
    val amount: Double,
    val payeeId: String?,
    val payeeName: String,
    val purpose: String,
    val purposeCategory: String, // MEDICAL, EDUCATION, BUSINESS, HOUSING, AGRICULTURE, OTHER
    val verificationStatus: String, // PENDING, VERIFIED, FLAGGED, MISMATCH, UNVERIFIED
    val ruleEngineResult: String, // CONSISTENT, MISMATCH, UNVERIFIED, AUTO_APPROVED, BLOCKED
    val approvalStatus: String, // PENDING, APPROVED, REJECTED, CLARIFICATION_NEEDED
    val transactionRef: String = "",
    val upiDeepLink: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val lenderNote: String = "",
    val borrowerNote: String = ""
)
