package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "noc_certificates")
data class NocCertificateEntity(
    @PrimaryKey
    val nocId: String,
    val loanId: String,
    val borrowerId: String,
    val borrowerName: String,
    val borrowerPan: String,
    val lenderId: String,
    val lenderName: String,
    val principalAmount: Double,
    val totalRepaidAmount: Double,
    val collateralReleasedDesc: String,
    val digitalSignatureHash: String, // SHA-256 legal hash
    val issuedAt: Long = System.currentTimeMillis(),
    val issuedByAdminId: String = "ADMIN-SATYAM-0810",
    val status: String = "ACTIVE_CLEARANCE" // ACTIVE_CLEARANCE, ARCHIVED
)
