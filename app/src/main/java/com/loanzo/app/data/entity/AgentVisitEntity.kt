package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_visits")
data class AgentVisitEntity(
    @PrimaryKey val visitId: String = "",
    val agentId: String = "",
    val loanId: String = "",
    val visitType: String = "", // "COLLATERAL_VERIFICATION", "BORROWER_VERIFICATION", "LENDER_VERIFICATION"
    val title: String = "",
    val borrowerName: String = "",
    val borrowerPhone: String = "",
    val borrowerAddress: String = "",
    val lenderName: String = "",
    val lenderPhone: String = "",
    val lenderAddress: String = "",
    val targetAddress: String = "",
    val targetLatitude: Double = 0.0,
    val targetLongitude: Double = 0.0,
    val scheduledDate: String = "",
    val scheduledTimeSlot: String = "",
    val payoutAmount: Double = 0.0,
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
    val createdAt: Long = System.currentTimeMillis(),

    // --- Dual-Agent Cross-Verification (Swapping Engine) Fields ---
    val crossVerificationPairId: String? = null, // Links paired reciprocal inspections
    val isCrossVerification: Boolean = false,
    val counterpartVisitId: String? = null,
    val verificationStage: String = "STAGE_1_PRIMARY", // "STAGE_1_PRIMARY" or "STAGE_2_SWAPPED"
    val appraisedValue: Double? = null, // Independent appraisal submitted by this officer
    val counterpartAppraisedValue: Double? = null, // Counterpart appraisal for admin side-by-side
    val valuationDiscrepancyPercent: Double? = null, // Computed discrepancy %
    val officerRecommendation: String? = null, // "RECOMMEND_APPROVAL", "FLAG_DISCREPANCY", "RECOMMEND_REJECTION"
    val isCounterpartAnonymous: Boolean = true, // Enforces blind anonymity
    val assignedAgentName: String? = null, // Officer display name for Admin consensus dashboard
    val agentPhone: String = "", // Direct contact phone number of the assigned field officer

    // --- Physical Presence & Real-Time Operational Tracking ---
    val handshakePin: String = "", // 4-digit security code shown only to the Borrower/User
    val isHandshakePinVerified: Boolean = false, // Must be verified in-person by officer
    val visitStageStatus: String = "SCHEDULED", // "SCHEDULED", "EN_ROUTE", "ARRIVED", "COMPLETED"
    val agentLatitude: Double? = null,
    val agentLongitude: Double? = null,
    val loanType: String = "PERSONAL",
    val distanceKm: Double? = null
)
