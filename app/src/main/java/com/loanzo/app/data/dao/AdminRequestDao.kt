package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.AdminRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: AdminRequestEntity)

    @Update
    suspend fun updateRequest(request: AdminRequestEntity)

    @Query("SELECT * FROM admin_requests ORDER BY requestedAt DESC")
    fun getAllRequests(): Flow<List<AdminRequestEntity>>

    @Query("SELECT * FROM admin_requests WHERE status = 'PENDING' ORDER BY requestedAt ASC")
    fun getPendingRequests(): Flow<List<AdminRequestEntity>>

    @Query("SELECT * FROM admin_requests WHERE status = 'PENDING' ORDER BY requestedAt ASC")
    suspend fun getPendingRequestsSync(): List<AdminRequestEntity>

    @Query("SELECT * FROM admin_requests WHERE userId = :userId ORDER BY requestedAt DESC LIMIT 1")
    fun getLatestRequestByUserId(userId: String): Flow<AdminRequestEntity?>

    @Query("SELECT * FROM admin_requests WHERE requestId = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: String): AdminRequestEntity?

    @Query("UPDATE admin_requests SET status = :status, reviewedAt = :reviewedAt, reviewedBy = :reviewedBy, adminNotes = :adminNotes WHERE requestId = :requestId")
    suspend fun updateRequestStatus(
        requestId: String,
        status: String,
        reviewedAt: Long = System.currentTimeMillis(),
        reviewedBy: String,
        adminNotes: String?
    )

    @Query("DELETE FROM admin_requests WHERE requestId LIKE 'admin_req_demo_%'")
    suspend fun deleteDemoRequests()
}
