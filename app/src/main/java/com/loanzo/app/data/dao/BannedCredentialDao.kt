package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.BannedCredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BannedCredentialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(banned: BannedCredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bannedList: List<BannedCredentialEntity>)

    @Query("SELECT * FROM banned_credentials WHERE credentialValue = :value LIMIT 1")
    suspend fun getByValue(value: String): BannedCredentialEntity?

    @Query("SELECT * FROM banned_credentials WHERE credentialValue IN (:values) LIMIT 1")
    suspend fun findFirstBanned(values: List<String>): BannedCredentialEntity?

    @Query("SELECT * FROM banned_credentials WHERE userId = :userId")
    suspend fun getByUserId(userId: String): List<BannedCredentialEntity>

    @Query("DELETE FROM banned_credentials WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM banned_credentials WHERE credentialValue = :value")
    suspend fun deleteByValue(value: String)

    @Query("SELECT * FROM banned_credentials")
    fun getAll(): Flow<List<BannedCredentialEntity>>
}
