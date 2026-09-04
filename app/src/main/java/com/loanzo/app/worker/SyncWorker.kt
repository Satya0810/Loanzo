package com.loanzo.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.loanzo.app.data.dao.SyncQueueDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * WorkManager worker that processes the offline sync queue.
 * Reads all PENDING items, attempts to upload each via the network,
 * and marks them as SYNCED or FAILED.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun syncQueueDao(): SyncQueueDao
        fun firebaseSyncManager(): com.loanzo.app.data.firebase.FirebaseSyncManager
    }

    companion object {
        const val TAG = "SyncWorker"
        const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync...")
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val syncQueueDao = entryPoint.syncQueueDao()
        val firebaseSyncManager = entryPoint.firebaseSyncManager()
        
        val pendingItems = syncQueueDao.getPendingSyncs()

        if (pendingItems.isEmpty()) {
            Log.d(TAG, "No pending items to sync.")
            return Result.success()
        }

        var hasFailures = false

        for (item in pendingItems) {
            if (item.retryCount >= MAX_RETRIES) {
                Log.w(TAG, "Skipping ${item.syncId} — max retries reached.")
                continue
            }

            syncQueueDao.markAsSyncing(item.syncId)

            try {
                // Sync with Firebase Firestore
                firebaseSyncManager.syncEntity(item)
                
                Log.d(TAG, "Synced ${item.entityType}/${item.entityId} (${item.operation})")
                syncQueueDao.markAsSynced(item.syncId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync ${item.syncId}: ${e.message}")
                syncQueueDao.markAsFailed(item.syncId)
                hasFailures = true
            }
        }

        // Clean up synced items
        syncQueueDao.clearSynced()

        return if (hasFailures) Result.retry() else Result.success()
    }
}
