package com.loanzo.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.loanzo.app.data.dao.SyncQueueDao
import com.loanzo.app.data.entity.SyncQueueEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central offline-first sync orchestrator.
 * Queues local changes into the SyncQueue and triggers WorkManager
 * to flush them to Firebase when the network is available.
 *
 * Pattern inspired by ThreatLens CloudSyncManager.
 */
@Singleton
class AppSyncManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AppSyncManager"
    }

    /**
     * Enqueues a user entity for background sync to Firestore.
     * The data is serialized to JSON and stored in the sync_queue table.
     * WorkManager will pick it up and push it to Firebase.
     */
    suspend fun enqueueUserSync(user: UserEntity, operation: String = "UPDATE") {
        val userMap = buildUserPayload(user)
        val payload = gson.toJson(userMap)

        val syncItem = SyncQueueEntity(
            syncId = UUID.randomUUID().toString(),
            entityType = "USER",
            entityId = user.userId,
            operation = operation,
            payload = payload
        )

        syncQueueDao.insertSync(syncItem)
        Log.d(TAG, "Enqueued $operation for USER/${user.userId}")
        triggerSync()
    }

    /**
     * Generic enqueue for any entity type (LOAN, DISBURSEMENT, etc.)
     */
    suspend fun enqueueSync(entityType: String, entityId: String, operation: String, payload: String) {
        val syncItem = SyncQueueEntity(
            syncId = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload
        )

        syncQueueDao.insertSync(syncItem)
        Log.d(TAG, "Enqueued $operation for $entityType/$entityId")
        triggerSync()
    }

    /**
     * Triggers WorkManager to process the sync queue.
     * Uses CONNECTED constraint so it only runs when network is available.
     * Uses KEEP policy so it doesn't duplicate if already enqueued.
     */
    private fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "loanzo_sync",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

        Log.d(TAG, "WorkManager sync triggered")
    }

    /**
     * Builds a Firestore-compatible map from a UserEntity.
     * This mirrors the format used in FirebaseManager.saveUserToFirestore().
     */
    private fun buildUserPayload(user: UserEntity): Map<String, Any?> {
        return mapOf(
            "userId" to user.userId,
            "name" to user.name,
            "email" to user.email,
            "phone" to user.phone,
            "username" to user.username,
            "password" to user.password,
            "role" to user.role,
            "kycStatus" to user.kycStatus,
            "panNumber" to user.panNumber,
            "aadhaarNumber" to user.aadhaarNumber,
            "emailVerified" to user.emailVerified,
            "phoneVerified" to user.phoneVerified,
            "panVerified" to user.panVerified,
            "aadhaarVerified" to user.aadhaarVerified,
            "selfieVerified" to user.selfieVerified,
            "upiId" to user.upiId,
            "bankAccountNumber" to user.bankAccountNumber,
            "profilePhotoUri" to user.profilePhotoUri,
            "panImageUrl" to user.panImageUrl,
            "aadhaarImageUrl" to user.aadhaarImageUrl,
            "dateOfBirth" to user.dateOfBirth,
            "address" to user.address,
            "fcmToken" to user.fcmToken,
            "createdAt" to user.createdAt,
            "updatedAt" to System.currentTimeMillis(),
            "app" to "Loanzo Android"
        )
    }
}
