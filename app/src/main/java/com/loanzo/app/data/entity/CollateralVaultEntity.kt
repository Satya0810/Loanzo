package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collateral_vault")
data class CollateralVaultEntity(
    @PrimaryKey
    val vaultItemId: String,
    val loanId: String,
    val borrowerId: String,
    val borrowerName: String,
    val borrowerPhone: String = "",
    val assetDescription: String,
    val assetType: String, // GOLD, PROPERTY_DEED, VEHICLE_TITLE, EQUIPMENT, OTHER
    val estimatedValue: Double,
    val appraisedPurityOrCondition: String, // e.g. "22 Karat (91.6% Pure Gold)", "Original Title Deed"
    val vaultFacilityName: String = "Loanzo Central Vault - Delhi NCR",
    val lockerNumber: String = "PENDING_ALLOCATION",
    val barcodeTag: String = "",
    val tamperSealNumber: String = "",
    val custodyStatus: String, // PENDING_INTAKE, SECURED_IN_VAULT, ENCUMBERED, READY_FOR_RELEASE, RELEASED, DEFAULT_AUCTION
    val photoUri: String = "",
    val intakeAgentId: String? = null,
    val intakeDate: Long? = null,
    val releaseDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
