package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.VerificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: VerificationEntity)

    @Update
    suspend fun updateVerification(verification: VerificationEntity)

    @Query("SELECT * FROM verifications ORDER BY createdAt DESC")
    fun getAllVerifications(): Flow<List<VerificationEntity>>

    @Query("SELECT * FROM verifications WHERE token = :token LIMIT 1")
    suspend fun getByToken(token: String): VerificationEntity?

    @Query("SELECT * FROM verifications WHERE phone = :phone ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForPhone(phone: String): VerificationEntity?

    @Query("SELECT * FROM verifications WHERE phone = :phone AND status = 'VERIFIED' LIMIT 1")
    suspend fun getVerifiedByPhone(phone: String): VerificationEntity?

    @Query("UPDATE verifications SET status = 'VERIFIED', verifiedAt = :verifiedAt WHERE token = :token OR phone = :phone")
    suspend fun markAsVerified(token: String, phone: String, verifiedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM verifications WHERE createdAt < :cutoff")
    suspend fun deleteExpired(cutoff: Long)
}
