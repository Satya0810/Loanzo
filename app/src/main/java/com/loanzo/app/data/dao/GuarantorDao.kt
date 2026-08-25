package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.GuarantorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuarantorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuarantor(guarantor: GuarantorEntity)

    @Update
    suspend fun updateGuarantor(guarantor: GuarantorEntity)

    @Query("SELECT * FROM guarantors WHERE loanId = :loanId ORDER BY createdAt DESC")
    fun getGuarantorsByLoan(loanId: String): Flow<List<GuarantorEntity>>

    @Query("SELECT * FROM guarantors WHERE guarantorId = :id")
    suspend fun getGuarantorById(id: String): GuarantorEntity?

    @Delete
    suspend fun deleteGuarantor(guarantor: GuarantorEntity)
}
