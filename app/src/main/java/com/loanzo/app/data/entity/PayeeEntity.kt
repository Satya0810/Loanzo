package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payees")
data class PayeeEntity(
    @PrimaryKey
    val payeeId: String,
    val name: String,
    val upiId: String = "",
    val gstNumber: String = "",
    val businessName: String = "",
    val verificationStatus: String, // VERIFIED, UNVERIFIED, PENDING
    val category: String, // HOSPITAL, PHARMACY, EDUCATION, ELECTRONICS, GROCERY, RENT, UTILITY, OTHER
    val verifiedAt: Long? = null,
    val addedBy: String = "", // userId of who added this payee
    val createdAt: Long = System.currentTimeMillis()
)
