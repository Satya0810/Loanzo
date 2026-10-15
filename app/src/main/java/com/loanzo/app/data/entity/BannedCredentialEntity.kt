package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banned_credentials")
data class BannedCredentialEntity(
    @PrimaryKey
    val credentialValue: String = "", // Normalized phone, PAN, Aadhaar, email, or username
    val credentialType: String = "PHONE", // PHONE, PAN, AADHAAR, EMAIL, USERNAME
    val userId: String = "",
    val reason: String = "",
    val bannedAt: Long = System.currentTimeMillis()
)
