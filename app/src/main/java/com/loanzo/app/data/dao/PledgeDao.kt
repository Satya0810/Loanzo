package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.PledgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PledgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPledge(pledge: PledgeEntity)

    @Update
    suspend fun updatePledge(pledge: PledgeEntity)

    @Query("SELECT * FROM pledges WHERE pledgeId = :id")
    suspend fun getPledgeById(id: String): PledgeEntity?

    @Query("SELECT * FROM pledges WHERE loanId = :loanId ORDER BY createdAt DESC")
    fun getPledgesByLoan(loanId: String): Flow<List<PledgeEntity>>

    @Query("SELECT SUM(estimatedValue) FROM pledges WHERE loanId = :loanId")
    fun getTotalPledgeValueForLoan(loanId: String): Flow<Double?>

    @Delete
    suspend fun deletePledge(pledge: PledgeEntity)
}
