package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.DisbursementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DisbursementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisbursement(disbursement: DisbursementEntity)

    @Update
    suspend fun updateDisbursement(disbursement: DisbursementEntity)

    @Query("SELECT * FROM disbursements WHERE disbursementId = :id")
    suspend fun getDisbursementById(id: String): DisbursementEntity?

    @Query("SELECT * FROM disbursements WHERE loanId = :loanId ORDER BY timestamp DESC")
    fun getDisbursementsByLoan(loanId: String): Flow<List<DisbursementEntity>>

    @Query("SELECT * FROM disbursements WHERE loanId = :loanId AND verificationStatus = 'VERIFIED'")
    fun getVerifiedDisbursements(loanId: String): Flow<List<DisbursementEntity>>

    @Query("SELECT SUM(amount) FROM disbursements WHERE loanId = :loanId AND approvalStatus = 'APPROVED'")
    fun getTotalDisbursedForLoan(loanId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM disbursements WHERE loanId = :loanId AND verificationStatus = 'VERIFIED'")
    fun getTotalVerifiedForLoan(loanId: String): Flow<Double?>

    @Query("SELECT * FROM disbursements WHERE approvalStatus = 'PENDING' AND loanId IN (SELECT loanId FROM loans WHERE lenderId = :lenderId)")
    fun getPendingApprovalsForLender(lenderId: String): Flow<List<DisbursementEntity>>

    @Query("SELECT COUNT(*) FROM disbursements WHERE loanId = :loanId AND verificationStatus = 'FLAGGED'")
    fun getFlaggedCountForLoan(loanId: String): Flow<Int>

    @Query("SELECT purposeCategory, SUM(amount) as total FROM disbursements WHERE loanId IN (SELECT loanId FROM loans WHERE borrowerId = :userId) AND approvalStatus = 'APPROVED' GROUP BY purposeCategory")
    fun getCategoryBreakdownForUser(userId: String): Flow<List<CategoryBreakdown>>

    @Query("""
        SELECT 
            CAST(strftime('%Y', timestamp / 1000, 'unixepoch') AS TEXT) || '-' || 
            CAST(strftime('%m', timestamp / 1000, 'unixepoch') AS TEXT) as monthLabel,
            SUM(amount) as total
        FROM disbursements 
        WHERE loanId IN (SELECT loanId FROM loans WHERE borrowerId = :userId) 
        AND approvalStatus = 'APPROVED'
        GROUP BY strftime('%Y-%m', timestamp / 1000, 'unixepoch')
        ORDER BY strftime('%Y-%m', timestamp / 1000, 'unixepoch') ASC
    """)
    fun getMonthlySpendingTrend(userId: String): Flow<List<MonthlySpending>>

    @Delete
    suspend fun deleteDisbursement(disbursement: DisbursementEntity)
}
