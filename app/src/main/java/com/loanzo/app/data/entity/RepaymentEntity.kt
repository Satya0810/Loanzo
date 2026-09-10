package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repayments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["loanId"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loanId"])]
)
data class RepaymentEntity(
    @PrimaryKey
    val repaymentId: String = "",
    val loanId: String = "",
    val amount: Double = 0.0,
    val transactionRef: String = "",
    val status: String = "SCHEDULED", // SCHEDULED, PAID, OVERDUE, PARTIAL
    val dueDate: Long = System.currentTimeMillis(),
    val paidDate: Long? = null,
    val outstandingSnapshot: Double = 0.0,
    val principalComponent: Double = 0.0,
    val interestComponent: Double = 0.0,
    val penalty: Double = 0.0,
    val penaltyWaived: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
