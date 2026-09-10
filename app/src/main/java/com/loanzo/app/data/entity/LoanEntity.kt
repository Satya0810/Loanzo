package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["lenderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["borrowerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["lenderId"]),
        Index(value = ["borrowerId"])
    ]
)
data class LoanEntity(
    @PrimaryKey
    val loanId: String = "",
    val lenderId: String = "",
    val borrowerId: String = "",
    val sanctionedAmount: Double = 0.0,
    val disbursedAmount: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val purpose: String = "Personal Loan",
    val loanType: String = "PERSONAL", // PERSONAL, BUSINESS, EDUCATION, MEDICAL, AGRICULTURE, OTHER
    val interestRate: Double = 12.0,
    val interestModel: String = "SIMPLE", // SIMPLE, COMPOUND, FLAT, NONE
    val tenureMonths: Int = 12,
    val status: String = "CONTRACT_SIGNING", // DRAFT, MARKETPLACE, BID_ACCEPTED, COLLATERAL_VALUATION, CONTRACT_SIGNING, TRANCHE_DISBURSEMENT, ACTIVE_SERVICING, RESTRUCTURED, DELINQUENT, LEGAL_DISPUTE, COMPLETED, CLOSED, ACTIVE
    val repaymentFrequency: String = "MONTHLY", // MONTHLY, WEEKLY, BI_WEEKLY, CUSTOM
    val createdAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val notes: String = "",
    // eSign Fields
    val lenderSignedAt: Long? = null,
    val borrowerSignedAt: Long? = null,
    val lenderSignatureUrl: String = "",
    val borrowerSignatureUrl: String = "",
    val lenderSelfieUrl: String = "",
    val borrowerSelfieUrl: String = "",
    val agreementPdfUrl: String = "",
    val isAgreementSigned: Boolean = false,
    
    // Penalty engine fields (Feature 14)
    val penaltyRate: Double = 2.0,
    val penaltyModel: String = "PERCENTAGE", // PERCENTAGE, FLAT, NONE
    val penaltyGraceDays: Int = 3,
    val penaltyCapPercent: Double = 100.0,
    
    // Restructuring / Moratorium fields (Feature 16)
    val originalTenureMonths: Int = 0,
    val moratoriumMonths: Int = 0,
    val isRestructured: Boolean = false,
    val restructuredAt: Long? = null
)
