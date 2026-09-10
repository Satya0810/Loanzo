package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marketplace_bids",
    indices = [
        Index(value = ["postId"]),
        Index(value = ["bidderId"]),
        Index(value = ["status"])
    ]
)
data class MarketplaceBidEntity(
    @PrimaryKey
    val bidId: String = "",
    val postId: String = "",
    val bidderId: String = "",
    val bidderName: String = "",
    val bidderAvatarUrl: String = "",
    val bidderKycVerified: Boolean = false,
    val bidderTrustScore: Int = 85,
    val proposedAmount: Double = 0.0,
    val proposedInterestRate: Double = 0.0,
    val proposedTenureMonths: Int = 6,
    val proposedRepaymentFrequency: String = "MONTHLY",
    val message: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, CONVERTED_TO_LOAN
    val createdAt: Long = System.currentTimeMillis()
)
