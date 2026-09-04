package com.loanzo.app.data.repository

import com.loanzo.app.data.dao.AgentDao
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.entity.AgentApplicationEntity
import com.loanzo.app.data.entity.AgentVisitEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val agentDao: AgentDao,
    private val userDao: UserDao
) {

    // --- Applications ---

    suspend fun submitApplication(application: AgentApplicationEntity) {
        agentDao.insertApplication(application)
        val user = userDao.getUserById(application.userId)
        if (user != null) {
            userDao.updateUser(
                user.copy(
                    role = "AGENT",
                    agentStatus = "PENDING"
                )
            )
        }
    }

    fun getApplication(userId: String): Flow<AgentApplicationEntity?> {
        return agentDao.getApplicationByUserId(userId)
    }

    suspend fun getApplicationSync(userId: String): AgentApplicationEntity? {
        return agentDao.getApplicationByUserIdSync(userId)
    }

    fun getPendingApplications(): Flow<List<AgentApplicationEntity>> {
        return agentDao.getPendingApplications()
    }

    fun getAllApplications(): Flow<List<AgentApplicationEntity>> {
        return agentDao.getAllApplications()
    }

    suspend fun approveApplication(applicationId: String, adminRemarks: String? = null) {
        val app = agentDao.getApplicationById(applicationId) ?: return
        val updatedApp = app.copy(
            status = "APPROVED",
            reviewedAt = System.currentTimeMillis(),
            adminRemarks = adminRemarks ?: "Approved by Master Admin"
        )
        agentDao.updateApplication(updatedApp)

        val user = userDao.getUserById(app.userId)
        if (user != null) {
            userDao.updateUser(
                user.copy(
                    role = "AGENT",
                    agentStatus = "APPROVED",
                    isOnDuty = true
                )
            )
        }

        // Seed sample visits for this agent if they don't have any
        seedSampleVisitsForAgent(app.userId)
    }

    suspend fun rejectApplication(applicationId: String, adminRemarks: String) {
        val app = agentDao.getApplicationById(applicationId) ?: return
        val updatedApp = app.copy(
            status = "REJECTED",
            reviewedAt = System.currentTimeMillis(),
            adminRemarks = adminRemarks
        )
        agentDao.updateApplication(updatedApp)

        val user = userDao.getUserById(app.userId)
        if (user != null) {
            userDao.updateUser(
                user.copy(
                    agentStatus = "REJECTED"
                )
            )
        }
    }

    // --- Agent Visits ---

    fun getVisitsForAgent(agentId: String): Flow<List<AgentVisitEntity>> {
        return agentDao.getVisitsForAgent(agentId)
    }

    suspend fun getVisitById(visitId: String): AgentVisitEntity? {
        return agentDao.getVisitById(visitId)
    }

    fun observeVisitById(visitId: String): Flow<AgentVisitEntity?> {
        return agentDao.observeVisitById(visitId)
    }

    suspend fun setDutyStatus(userId: String, isOnDuty: Boolean) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(isOnDuty = isOnDuty))
    }

    suspend fun completeVisit(
        visitId: String,
        agentRemarks: String,
        isCollateralAuthentic: Boolean,
        isBorrowerIdentityVerified: Boolean,
        isLenderIdentityVerified: Boolean,
        proofPhotoUris: String
    ) {
        val visit = agentDao.getVisitById(visitId) ?: return
        val completedVisit = visit.copy(
            status = "COMPLETED",
            agentRemarks = agentRemarks,
            isCollateralAuthentic = isCollateralAuthentic,
            isBorrowerIdentityVerified = isBorrowerIdentityVerified,
            isLenderIdentityVerified = isLenderIdentityVerified,
            proofPhotoUris = proofPhotoUris,
            completedAt = System.currentTimeMillis()
        )
        agentDao.updateVisit(completedVisit)

        // Credit agent earnings
        val user = userDao.getUserById(visit.agentId)
        if (user != null) {
            val updatedEarnings = user.totalAgentEarnings + visit.payoutAmount
            userDao.updateUser(user.copy(totalAgentEarnings = updatedEarnings))
        }
    }

    suspend fun seedSampleVisitsForAgent(agentId: String) {
        val sampleVisits = listOf(
            AgentVisitEntity(
                visitId = "VISIT-" + UUID.randomUUID().toString().take(8).uppercase(),
                agentId = agentId,
                loanId = "LOAN-84920",
                visitType = "COLLATERAL_VERIFICATION",
                title = "Gold Collateral Physical Appraisal & Purity Check",
                borrowerName = "Vikram Sharma",
                borrowerPhone = "+919876543210",
                borrowerAddress = "Flat 402, Golden Heights, Sector 18, Noida",
                lenderName = "Rajesh Gupta",
                lenderPhone = "+919811223344",
                lenderAddress = "B-12, Kailash Colony, Greater Kailash, New Delhi",
                targetAddress = "Flat 402, Golden Heights, Sector 18, Noida, UP - 201301",
                targetLatitude = 28.5708,
                targetLongitude = 77.3271,
                scheduledDate = "Today",
                scheduledTimeSlot = "10:30 AM - 11:30 AM",
                payoutAmount = 850.0,
                collateralItemName = "22K Hallmark Gold Coins (20g)",
                collateralEstimatedValue = 150000.0,
                collateralPledgedValue = 100000.0,
                status = "SCHEDULED"
            ),
            AgentVisitEntity(
                visitId = "VISIT-" + UUID.randomUUID().toString().take(8).uppercase(),
                agentId = agentId,
                loanId = "LOAN-77319",
                visitType = "BORROWER_VERIFICATION",
                title = "Borrower Residence & Employment Verification",
                borrowerName = "Pooja Malhotra",
                borrowerPhone = "+919711556677",
                borrowerAddress = "House 15, Block C, Green Park Extension, New Delhi",
                lenderName = "Amitabh Verma",
                lenderPhone = "+919910998877",
                lenderAddress = "Tower 3, Apt 901, DLF Phase 5, Gurugram",
                targetAddress = "House 15, Block C, Green Park Extension, New Delhi - 110016",
                targetLatitude = 28.5589,
                targetLongitude = 77.2028,
                scheduledDate = "Today",
                scheduledTimeSlot = "02:00 PM - 03:00 PM",
                payoutAmount = 550.0,
                collateralItemName = "Personal Guarantee & Salary Proof",
                collateralEstimatedValue = 0.0,
                collateralPledgedValue = 50000.0,
                status = "SCHEDULED"
            ),
            AgentVisitEntity(
                visitId = "VISIT-" + UUID.randomUUID().toString().take(8).uppercase(),
                agentId = agentId,
                loanId = "LOAN-91044",
                visitType = "LENDER_VERIFICATION",
                title = "High-Value Lender Source & Physical KYC Verification",
                borrowerName = "Kunal Rawat",
                borrowerPhone = "+919650112233",
                borrowerAddress = "Pocket A, Sarita Vihar, New Delhi",
                lenderName = "Suresh Chand Singhal",
                lenderPhone = "+919810554433",
                lenderAddress = "Singhal Jewellers, Main Market, Chandni Chowk, Delhi",
                targetAddress = "Singhal Jewellers, Main Market, Chandni Chowk, Delhi - 110006",
                targetLatitude = 28.6506,
                targetLongitude = 77.2303,
                scheduledDate = "Today",
                scheduledTimeSlot = "04:30 PM - 05:30 PM",
                payoutAmount = 650.0,
                collateralItemName = "Commercial P2P Escrow Facility",
                collateralEstimatedValue = 0.0,
                collateralPledgedValue = 250000.0,
                status = "SCHEDULED"
            )
        )
        agentDao.insertVisits(sampleVisits)
    }
}
