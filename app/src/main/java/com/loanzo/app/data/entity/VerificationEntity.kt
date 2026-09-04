package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verifications")
data class VerificationEntity(
    @PrimaryKey
    val token: String,
    val phone: String,
    val channel: String, // SMS, WHATSAPP, EMAIL
    val status: String, // PENDING, VERIFIED, EXPIRED
    val createdAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null,
    val note: String = "",
    val username: String = ""
)
