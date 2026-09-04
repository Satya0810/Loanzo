package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_applications")
data class AgentApplicationEntity(
    @PrimaryKey val applicationId: String,
    val userId: String,
    val applicantName: String,
    val applicantPhone: String,
    val applicantEmail: String = "",
    val experienceYears: String, // "1-2 Years", "3-5 Years", "5+ Years"
    val priorDomain: String, // "Banking / NBFC", "Microfinance (MFI)", "Gold Loan Valuer", "Real Estate & Vehicle Appraiser", "Legal Recovery"
    val policeVerificationNumber: String,
    val policeStation: String,
    val policeVerificationDate: String,
    val policeDocUri: String = "",
    val permanentAddress: String,
    val operatingCity: String,
    val operatingPincode: String,
    val serviceRadiusKm: Int = 10,
    val vehicleType: String, // "Two-Wheeler", "Four-Wheeler", "Public Transit"
    val drivingLicenseNumber: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val adminRemarks: String? = null
)
