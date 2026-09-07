package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marketplace_vouches",
    indices = [
        Index(value = ["postId"]),
        Index(value = ["voucherUserId"]),
        Index(value = ["postId", "voucherUserId"], unique = true)
    ]
)
data class MarketplaceVouchEntity(
    @PrimaryKey
    val vouchId: String,
    val postId: String,
    val voucherUserId: String,
    val voucherName: String,
    val vouchReason: String, // "BUSINESS_PEER", "PAST_REPAYMENT", "COMMUNITY_REFERENCE"
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // Voucher Verification Details
    val voucherAvatarUrl: String = "",
    val voucherKycVerified: Boolean = true,
    val voucherTrustScore: Int = 90,
    val voucherRole: String = "Verified Community Member"
)
