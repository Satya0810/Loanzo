package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.CoLenderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoLenderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoLender(coLender: CoLenderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoLenders(coLenders: List<CoLenderEntity>)

    @Update
    suspend fun updateCoLender(coLender: CoLenderEntity)

    @Query("SELECT * FROM co_lenders WHERE loanId = :loanId ORDER BY sharePercentage DESC")
    fun getCoLendersForLoan(loanId: String): Flow<List<CoLenderEntity>>

    @Query("SELECT * FROM co_lenders WHERE loanId = :loanId ORDER BY sharePercentage DESC")
    suspend fun getCoLendersForLoanSync(loanId: String): List<CoLenderEntity>

    @Query("SELECT * FROM co_lenders WHERE lenderId = :lenderId ORDER BY createdAt DESC")
    fun getLoansForCoLender(lenderId: String): Flow<List<CoLenderEntity>>

    @Query("SELECT * FROM co_lenders WHERE lenderId = :lenderId ORDER BY createdAt DESC")
    suspend fun getLoansForCoLenderSync(lenderId: String): List<CoLenderEntity>

    @Query("SELECT * FROM co_lenders WHERE coLenderId = :coLenderId")
    suspend fun getCoLenderById(coLenderId: String): CoLenderEntity?

    @Query("DELETE FROM co_lenders WHERE coLenderId = :coLenderId")
    suspend fun deleteCoLender(coLenderId: String)

    @Query("DELETE FROM co_lenders WHERE loanId = :loanId")
    suspend fun deleteCoLendersForLoan(loanId: String)

    @Query("SELECT COUNT(*) FROM co_lenders WHERE loanId = :loanId")
    fun observeCoLenderCount(loanId: String): Flow<Int>
}
