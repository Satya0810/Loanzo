package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val syncId: String,
    val entityType: String,     // LOAN, DISBURSEMENT, REPAYMENT, PLEDGE
    val entityId: String,
    val operation: String,      // CREATE, UPDATE, DELETE
    val payload: String,        // JSON serialized entity
    val status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
