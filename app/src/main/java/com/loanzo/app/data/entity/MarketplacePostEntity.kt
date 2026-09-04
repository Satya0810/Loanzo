package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marketplace_posts",
    indices = [
        Index(value = ["authorId"]),
        Index(value = ["postType"]),
        Index(value = ["purposeCategory"]),
        Index(value = ["status"])
    ]
)
data class MarketplacePostEntity(
    @PrimaryKey
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String = "",
    val authorKycVerified: Boolean = false,
    val authorTrustScore: Int = 85,
    val postType: String, // "OFFER_TO_LEND" or "SEEKING_LOAN"
    val title: String,
    val description: String,
    val minAmount: Double,
    val maxAmount: Double,
    val interestRate: Double,
    val interestModel: String = "SIMPLE",
    val tenureMonths: Int,
    val repaymentFrequency: String = "MONTHLY",
    val purposeCategory: String = "PERSONAL", // PERSONAL, BUSINESS, EDUCATION, MEDICAL, AGRICULTURE, EMERGENCY
    val locationCity: String = "",
    val collateralOffered: String = "",
    val incomeProofStatus: String = "VERIFIED",
    val vouchCount: Int = 0,
    val bidsCount: Int = 0,
    val status: String = "OPEN", // OPEN, IN_NEGOTIATION, FUNDED, CLOSED
    val createdAt: Long = System.currentTimeMillis()
)
