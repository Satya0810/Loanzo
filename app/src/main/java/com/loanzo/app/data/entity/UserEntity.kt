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
    val username: String = "",
    val password: String = "",
    val role: String, // BORROWER, LENDER
    val kycStatus: String, // PENDING, IN_PROGRESS, VERIFIED, REJECTED
    val panNumber: String = "",
    val aadhaarNumber: String = "",
    val aadhaarVerified: Boolean = false,
    val selfieVerified: Boolean = false,
    val upiId: String = "",
    val upiVerified: Boolean = false,
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    val bankVerified: Boolean = false,
    val profilePhotoUri: String = "",
    val panImageUrl: String = "",
    val aadhaarImageUrl: String = "",
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val panVerified: Boolean = false,
    val dateOfBirth: String = "",
    val address: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
