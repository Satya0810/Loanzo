package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "guarantors",
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
data class GuarantorEntity(
    @PrimaryKey
    val guarantorId: String,
    val loanId: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val panNumber: String = "",
    val relationship: String, // SPOUSE, PARENT, SIBLING, FRIEND, BUSINESS_PARTNER
    val consentStatus: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val consentTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
