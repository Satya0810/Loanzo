package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // BORROWER, LENDER
    val kycStatus: String, // PENDING, IN_PROGRESS, VERIFIED, REJECTED
    val panNumber: String = "",
    val aadhaarVerified: Boolean = false,
    val selfieVerified: Boolean = false,
    val upiId: String = "",
    val bankAccountNumber: String = "",
    val profilePhotoUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
