package com.loanzo.app.data.repository

import com.loanzo.app.data.dao.*
import com.loanzo.app.data.entity.*
import com.loanzo.app.domain.PenaltyEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val disbursementDao: DisbursementDao,
    private val repaymentDao: RepaymentDao,
    private val pledgeDao: PledgeDao,
    private val auditEventDao: AuditEventDao,
    private val penaltyEngine: PenaltyEngine
) {
    // Loan operations
    suspend fun createLoan(loan: LoanEntity, actorId: String) {
        loanDao.insertLoan(loan)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "LOAN",
                entityId = loan.loanId,
                actor = actorId,
                event = "CREATED",
                newState = loan.status,
                description = "Loan created: ₹${loan.sanctionedAmount} for ${loan.purpose}"
            )
        )
    }

    suspend fun updateLoan(loan: LoanEntity, actorId: String, description: String = "") {
        val existing = loanDao.getLoanById(loan.loanId)
        loanDao.updateLoan(loan)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "LOAN",
                entityId = loan.loanId,
                actor = actorId,
                event = "UPDATED",
                oldState = existing?.status ?: "",
                newState = loan.status,
                description = description
            )
        )
    }

    suspend fun getLoanById(loanId: String): LoanEntity? = loanDao.getLoanById(loanId)
    fun observeLoan(loanId: String): Flow<LoanEntity?> = loanDao.observeLoan(loanId)
    fun getLoansByBorrower(userId: String): Flow<List<LoanEntity>> = loanDao.getLoansByBorrower(userId)
    fun getLoansByLender(userId: String): Flow<List<LoanEntity>> = loanDao.getLoansByLender(userId)
    fun getAllLoansForUser(userId: String): Flow<List<LoanEntity>> = loanDao.getAllLoansForUser(userId)
    fun getTotalDisbursed(userId: String): Flow<Double?> = loanDao.getTotalDisbursedForBorrower(userId)
    fun getTotalOutstanding(userId: String): Flow<Double?> = loanDao.getTotalOutstandingForBorrower(userId)

    // Disbursement operations
    suspend fun createDisbursement(disbursement: DisbursementEntity, actorId: String) {
        disbursementDao.insertDisbursement(disbursement)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "DISBURSEMENT",
                entityId = disbursement.disbursementId,
                actor = actorId,
                event = "CREATED",
                newState = disbursement.approvalStatus,
                description = "Tranche request: ₹${disbursement.amount} to ${disbursement.payeeName} for ${disbursement.purpose}"
            )
        )
    }

    suspend fun updateDisbursement(disbursement: DisbursementEntity, actorId: String, eventType: String = "UPDATED") {
        disbursementDao.updateDisbursement(disbursement)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "DISBURSEMENT",
                entityId = disbursement.disbursementId,
                actor = actorId,
                event = eventType,
                newState = disbursement.approvalStatus,
                description = "Disbursement ${eventType.lowercase()}: ₹${disbursement.amount}"
            )
        )
    }

    fun getDisbursementsByLoan(loanId: String): Flow<List<DisbursementEntity>> =
        disbursementDao.getDisbursementsByLoan(loanId)
    fun getVerifiedDisbursements(loanId: String): Flow<List<DisbursementEntity>> =
        disbursementDao.getVerifiedDisbursements(loanId)
    fun getTotalDisbursedForLoan(loanId: String): Flow<Double?> =
        disbursementDao.getTotalDisbursedForLoan(loanId)
    fun getTotalVerifiedForLoan(loanId: String): Flow<Double?> =
        disbursementDao.getTotalVerifiedForLoan(loanId)
    fun getPendingApprovalsForLender(lenderId: String): Flow<List<DisbursementEntity>> =
        disbursementDao.getPendingApprovalsForLender(lenderId)

    // Repayment operations
    suspend fun createRepayment(repayment: RepaymentEntity, actorId: String) {
        repaymentDao.insertRepayment(repayment)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "REPAYMENT",
                entityId = repayment.repaymentId,
                actor = actorId,
                event = "CREATED",
                newState = repayment.status,
                description = "Repayment scheduled: ₹${repayment.amount}"
            )
        )
    }

    suspend fun recordRepayment(repayment: RepaymentEntity, actorId: String) {
        repaymentDao.updateRepayment(repayment)
        // Update loan outstanding
        val loan = loanDao.getLoanById(repayment.loanId)
        if (loan != null) {
            val newOutstanding = (loan.outstandingAmount - repayment.amount).coerceAtLeast(0.0)
            val updatedLoan = loan.copy(
                outstandingAmount = newOutstanding,
                status = if (newOutstanding <= 0.0) "CLOSED" else loan.status,
                closedAt = if (newOutstanding <= 0.0) System.currentTimeMillis() else null
            )
            loanDao.updateLoan(updatedLoan)
        }
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "REPAYMENT",
                entityId = repayment.repaymentId,
                actor = actorId,
                event = "PAID",
                newState = "PAID",
                description = "Repayment recorded: ₹${repayment.amount}, Ref: ${repayment.transactionRef}"
            )
        )
    }

    fun getRepaymentsByLoan(loanId: String): Flow<List<RepaymentEntity>> {
        return repaymentDao.getRepaymentsByLoan(loanId).combine(loanDao.observeLoan(loanId)) { repayments, loan ->
            if (loan != null) {
                penaltyEngine.applyPenalties(repayments, loan)
            } else {
                repayments
            }
        }
    }
    
    fun getNextDueRepayment(loanId: String): Flow<RepaymentEntity?> =
        repaymentDao.getNextDueRepayment(loanId)
    fun getOverdueRepaymentsForBorrower(userId: String): Flow<List<RepaymentEntity>> =
        repaymentDao.getOverdueRepaymentsForBorrower(userId)
    fun getOverdueRepaymentsForLender(userId: String): Flow<List<RepaymentEntity>> =
        repaymentDao.getOverdueRepaymentsForLender(userId)
    fun getTotalPaidForLoan(loanId: String): Flow<Double?> =
        repaymentDao.getTotalPaidForLoan(loanId)

    // Pledge operations
    suspend fun createPledge(pledge: PledgeEntity, actorId: String) {
        pledgeDao.insertPledge(pledge)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "PLEDGE",
                entityId = pledge.pledgeId,
                actor = actorId,
                event = "CREATED",
                newState = pledge.receiptStatus,
                description = "Pledge added: ${pledge.assetDescription} worth ₹${pledge.estimatedValue}"
            )
        )
    }

    fun getPledgesByLoan(loanId: String): Flow<List<PledgeEntity>> = pledgeDao.getPledgesByLoan(loanId)
    fun getTotalPledgeValue(loanId: String): Flow<Double?> = pledgeDao.getTotalPledgeValueForLoan(loanId)

    // Audit
    fun getAuditTrail(entityType: String, entityId: String): Flow<List<AuditEventEntity>> =
        auditEventDao.getEventsForEntity(entityType, entityId)
    fun getRecentEvents(limit: Int = 100): Flow<List<AuditEventEntity>> =
        auditEventDao.getRecentEvents(limit)
}
