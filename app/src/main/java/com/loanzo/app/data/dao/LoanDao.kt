package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE loanId = :loanId")
    suspend fun getLoanById(loanId: String): LoanEntity?

    @Query("SELECT * FROM loans WHERE loanId = :loanId")
    fun observeLoan(loanId: String): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE borrowerId = :userId ORDER BY createdAt DESC")
    fun getLoansByBorrower(userId: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE lenderId = :userId ORDER BY createdAt DESC")
    fun getLoansByLender(userId: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE (borrowerId = :userId OR lenderId = :userId) ORDER BY createdAt DESC")
    fun getAllLoansForUser(userId: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY createdAt DESC")
    fun getLoansByStatus(status: String): Flow<List<LoanEntity>>

    @Query("SELECT SUM(disbursedAmount) FROM loans WHERE borrowerId = :userId AND status = 'ACTIVE'")
    fun getTotalDisbursedForBorrower(userId: String): Flow<Double?>

    @Query("SELECT SUM(outstandingAmount) FROM loans WHERE borrowerId = :userId AND status = 'ACTIVE'")
    fun getTotalOutstandingForBorrower(userId: String): Flow<Double?>

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)
}
