package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_requests")
data class AdminRequestEntity(
    @PrimaryKey val requestId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val reason: String = "",
    val currentRole: String = "MEMBER",
    val requestedRole: String = "ADMIN", // "ADMIN", "STAFF", "AUDITOR"
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val adminNotes: String? = null
) {
    @androidx.room.Ignore
    val justification: String = reason
}
