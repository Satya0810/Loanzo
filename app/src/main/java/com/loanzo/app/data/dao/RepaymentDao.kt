package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.RepaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: RepaymentEntity)

    @Update
    suspend fun updateRepayment(repayment: RepaymentEntity)

    @Query("SELECT * FROM repayments WHERE repaymentId = :id")
    suspend fun getRepaymentById(id: String): RepaymentEntity?

    @Query("SELECT * FROM repayments WHERE loanId = :loanId ORDER BY dueDate ASC")
    fun getRepaymentsByLoan(loanId: String): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM repayments WHERE loanId = :loanId AND status = 'SCHEDULED' ORDER BY dueDate ASC LIMIT 1")
    fun getNextDueRepayment(loanId: String): Flow<RepaymentEntity?>

    @Query("SELECT * FROM repayments WHERE status = 'OVERDUE' AND loanId IN (SELECT loanId FROM loans WHERE borrowerId = :userId)")
    fun getOverdueRepaymentsForBorrower(userId: String): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM repayments WHERE status = 'OVERDUE' AND loanId IN (SELECT loanId FROM loans WHERE lenderId = :userId)")
    fun getOverdueRepaymentsForLender(userId: String): Flow<List<RepaymentEntity>>

    @Query("SELECT SUM(amount) FROM repayments WHERE loanId = :loanId AND status = 'PAID'")
    fun getTotalPaidForLoan(loanId: String): Flow<Double?>

    @Query("SELECT * FROM repayments WHERE dueDate BETWEEN :startTime AND :endTime AND status = 'SCHEDULED'")
    fun getUpcomingRepayments(startTime: Long, endTime: Long): Flow<List<RepaymentEntity>>

    @Delete
    suspend fun deleteRepayment(repayment: RepaymentEntity)
}
