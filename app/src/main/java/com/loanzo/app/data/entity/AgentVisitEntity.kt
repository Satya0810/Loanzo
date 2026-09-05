package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_visits")
data class AgentVisitEntity(
    @PrimaryKey val visitId: String,
    val agentId: String,
    val loanId: String,
    val visitType: String, // "COLLATERAL_VERIFICATION", "BORROWER_VERIFICATION", "LENDER_VERIFICATION"
    val title: String,
    val borrowerName: String,
    val borrowerPhone: String,
    val borrowerAddress: String,
    val lenderName: String,
    val lenderPhone: String,
    val lenderAddress: String,
    val targetAddress: String,
    val targetLatitude: Double = 0.0,
    val targetLongitude: Double = 0.0,
    val scheduledDate: String,
    val scheduledTimeSlot: String,
    val payoutAmount: Double,
    val collateralItemName: String? = null,
    val collateralEstimatedValue: Double? = null,
    val collateralPledgedValue: Double? = null,
    val status: String = "SCHEDULED", // "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val agentRemarks: String = "",
    val isCollateralAuthentic: Boolean = false,
    val isBorrowerIdentityVerified: Boolean = false,
    val isLenderIdentityVerified: Boolean = false,
    val proofPhotoUris: String = "",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
