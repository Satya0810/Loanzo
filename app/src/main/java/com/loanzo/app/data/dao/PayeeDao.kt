package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.PayeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayee(payee: PayeeEntity)

    @Update
    suspend fun updatePayee(payee: PayeeEntity)

    @Query("SELECT * FROM payees WHERE payeeId = :id")
    suspend fun getPayeeById(id: String): PayeeEntity?

    @Query("SELECT * FROM payees WHERE upiId = :upiId LIMIT 1")
    suspend fun getPayeeByUpiId(upiId: String): PayeeEntity?

    @Query("SELECT * FROM payees WHERE addedBy = :userId ORDER BY createdAt DESC")
    fun getPayeesByUser(userId: String): Flow<List<PayeeEntity>>

    @Query("SELECT * FROM payees WHERE verificationStatus = 'VERIFIED'")
    fun getVerifiedPayees(): Flow<List<PayeeEntity>>

    @Query("SELECT * FROM payees WHERE category = :category")
    fun getPayeesByCategory(category: String): Flow<List<PayeeEntity>>

    @Query("SELECT * FROM payees ORDER BY name ASC")
    fun getAllPayees(): Flow<List<PayeeEntity>>

    @Delete
    suspend fun deletePayee(payee: PayeeEntity)
}
