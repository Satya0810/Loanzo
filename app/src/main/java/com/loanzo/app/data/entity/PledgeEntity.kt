package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pledges",
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
data class PledgeEntity(
    @PrimaryKey
    val pledgeId: String,
    val loanId: String,
    val assetDescription: String,
    val assetType: String, // GOLD, PROPERTY, VEHICLE, EQUIPMENT, OTHER
    val estimatedValue: Double,
    val weight: Double = 0.0, // for gold, in grams
    val photoUri: String = "",
    val receiptUri: String = "",
    val receiptStatus: String, // SUBMITTED, VERIFIED, PENDING
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
