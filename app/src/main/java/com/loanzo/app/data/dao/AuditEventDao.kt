package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.AuditEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AuditEventEntity)

    @Query("SELECT * FROM audit_events WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    fun getEventsForEntity(entityType: String, entityId: String): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events WHERE actor = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsByUser(userId: String, limit: Int = 50): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<AuditEventEntity>>

    @Query("DELETE FROM audit_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldEvents(beforeTimestamp: Long)
}
