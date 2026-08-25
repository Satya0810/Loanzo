package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSync(syncItem: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingSyncs(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED'")
    fun getPendingSyncCount(): Flow<Int>

    @Query("UPDATE sync_queue SET status = 'SYNCED' WHERE syncId = :syncId")
    suspend fun markAsSynced(syncId: String)

    @Query("UPDATE sync_queue SET status = 'FAILED', retryCount = retryCount + 1 WHERE syncId = :syncId")
    suspend fun markAsFailed(syncId: String)

    @Query("UPDATE sync_queue SET status = 'SYNCING' WHERE syncId = :syncId")
    suspend fun markAsSyncing(syncId: String)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSynced()
}
