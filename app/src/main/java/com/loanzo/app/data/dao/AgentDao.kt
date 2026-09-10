package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.AgentApplicationEntity
import com.loanzo.app.data.entity.AgentVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {

    // --- Applications ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: AgentApplicationEntity)

    @Update
    suspend fun updateApplication(application: AgentApplicationEntity)

    @Query("SELECT * FROM agent_applications WHERE userId = :userId ORDER BY submittedAt DESC LIMIT 1")
    fun getApplicationByUserId(userId: String): Flow<AgentApplicationEntity?>

    @Query("SELECT * FROM agent_applications WHERE userId = :userId ORDER BY submittedAt DESC LIMIT 1")
    suspend fun getApplicationByUserIdSync(userId: String): AgentApplicationEntity?

    @Query("SELECT * FROM agent_applications WHERE applicationId = :applicationId LIMIT 1")
    suspend fun getApplicationById(applicationId: String): AgentApplicationEntity?

    @Query("SELECT * FROM agent_applications ORDER BY submittedAt DESC")
    fun getAllApplications(): Flow<List<AgentApplicationEntity>>

    @Query("SELECT * FROM agent_applications WHERE status = 'PENDING' ORDER BY submittedAt ASC")
    fun getPendingApplications(): Flow<List<AgentApplicationEntity>>

    @Query("SELECT * FROM agent_applications WHERE status = 'PENDING' ORDER BY submittedAt ASC")
    suspend fun getPendingApplicationsSync(): List<AgentApplicationEntity>

    // --- Visits ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: AgentVisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<AgentVisitEntity>)

    @Update
    suspend fun updateVisit(visit: AgentVisitEntity)

    @Query("SELECT * FROM agent_visits ORDER BY scheduledDate ASC, scheduledTimeSlot ASC")
    fun getAllVisits(): Flow<List<AgentVisitEntity>>

    @Query("SELECT * FROM agent_visits WHERE agentId = :agentId ORDER BY scheduledDate ASC, scheduledTimeSlot ASC")
    fun getVisitsForAgent(agentId: String): Flow<List<AgentVisitEntity>>

    @Query("SELECT * FROM agent_visits WHERE agentId = 'UNASSIGNED' OR agentId = '' OR agentId IS NULL ORDER BY createdAt DESC")
    fun getUnassignedVisits(): Flow<List<AgentVisitEntity>>

    @Query("SELECT * FROM agent_visits WHERE visitId = :visitId LIMIT 1")
    suspend fun getVisitById(visitId: String): AgentVisitEntity?

    @Query("SELECT * FROM agent_visits WHERE visitId = :visitId LIMIT 1")
    fun observeVisitById(visitId: String): Flow<AgentVisitEntity?>

    @Query("SELECT * FROM agent_visits WHERE agentId = :agentId AND status = :status ORDER BY scheduledTimeSlot ASC")
    fun getVisitsByStatus(agentId: String, status: String): Flow<List<AgentVisitEntity>>

    @Query("SELECT * FROM agent_visits WHERE agentId = :agentId AND (scheduledDate = 'Today' OR scheduledDate = :todayDate) ORDER BY scheduledTimeSlot ASC")
    fun getTodayVisits(agentId: String, todayDate: String): Flow<List<AgentVisitEntity>>

    @Delete
    suspend fun deleteVisit(visit: AgentVisitEntity)

    @Query("SELECT * FROM agent_visits WHERE crossVerificationPairId = :pairId ORDER BY verificationStage ASC")
    fun getVisitsByPairId(pairId: String): Flow<List<AgentVisitEntity>>

    @Query("SELECT * FROM agent_visits WHERE crossVerificationPairId = :pairId ORDER BY verificationStage ASC")
    suspend fun getVisitsByPairIdSync(pairId: String): List<AgentVisitEntity>

    @Query("SELECT * FROM agent_visits WHERE isCrossVerification = 1 ORDER BY createdAt DESC")
    fun getAllCrossVerificationVisits(): Flow<List<AgentVisitEntity>>

    @Query("SELECT * FROM agent_visits WHERE loanId = :loanId ORDER BY createdAt DESC")
    fun getVisitsByLoanId(loanId: String): Flow<List<AgentVisitEntity>>

    @Query("SELECT * FROM agent_visits WHERE loanId = :loanId AND status != 'CANCELLED' ORDER BY createdAt DESC LIMIT 1")
    fun observeActiveVisitForLoan(loanId: String): Flow<AgentVisitEntity?>

    @Query("SELECT * FROM agent_visits WHERE loanId = :loanId AND status != 'CANCELLED' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveVisitForLoanSync(loanId: String): AgentVisitEntity?

    @Query("SELECT * FROM agent_visits WHERE borrowerPhone = :phone OR lenderPhone = :phone ORDER BY createdAt DESC")
    fun getVisitsForUserPhone(phone: String): Flow<List<AgentVisitEntity>>

    @Query("DELETE FROM agent_applications WHERE applicationId LIKE 'app_agent_demo_%'")
    suspend fun deleteDemoApplications()

    @Query("DELETE FROM agent_visits WHERE visitId LIKE 'visit_demo_%' OR visitId LIKE 'cross_pair_%' OR crossVerificationPairId LIKE 'cross_pair_demo_%'")
    suspend fun deleteDemoVisits()
}
