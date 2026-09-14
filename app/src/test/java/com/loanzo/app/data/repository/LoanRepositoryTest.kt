package com.loanzo.app.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.loanzo.app.data.dao.*
import com.loanzo.app.data.entity.*
import com.loanzo.app.domain.PenaltyEngine
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LoanRepositoryTest {

    private val loanDao = mockk<LoanDao>(relaxed = true)
    private val disbursementDao = mockk<DisbursementDao>(relaxed = true)
    private val repaymentDao = mockk<RepaymentDao>(relaxed = true)
    private val pledgeDao = mockk<PledgeDao>(relaxed = true)
    private val guarantorDao = mockk<GuarantorDao>(relaxed = true)
    private val auditEventDao = mockk<AuditEventDao>(relaxed = true)
    private val penaltyEngine = mockk<PenaltyEngine>(relaxed = true)
    private val firebaseManager = mockk<com.loanzo.app.data.firebase.FirebaseManager>(relaxed = true)

    private lateinit var repository: LoanRepository

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        repository = LoanRepository(
            loanDao = loanDao,
            disbursementDao = disbursementDao,
            repaymentDao = repaymentDao,
            pledgeDao = pledgeDao,
            guarantorDao = guarantorDao,
            auditEventDao = auditEventDao,
            penaltyEngine = penaltyEngine,
            firebaseManager = firebaseManager
        )
    }

    private fun createSampleLoan(
        loanId: String = "loan_123",
        status: String = "ACTIVE",
        outstanding: Double = 50000.0,
        sanctioned: Double = 50000.0
    ): LoanEntity {
        return LoanEntity(
            loanId = loanId,
            borrowerId = "borrower_1",
            lenderId = "lender_1",
            sanctionedAmount = sanctioned,
            disbursedAmount = sanctioned,
            outstandingAmount = outstanding,
            interestRate = 12.0,
            tenureMonths = 12,
            purpose = "Medical Emergency",
            status = status,
            createdAt = 1700000000000L
        )
    }

    private fun createSampleRepayment(
        repaymentId: String = "rep_1",
        loanId: String = "loan_123",
        amount: Double = 5000.0,
        status: String = "PENDING"
    ): RepaymentEntity {
        return RepaymentEntity(
            repaymentId = repaymentId,
            loanId = loanId,
            dueDate = 1702500000000L,
            amount = amount,
            principalComponent = amount * 0.8,
            interestComponent = amount * 0.2,
            status = status
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Loan Lifecycle Operations
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Loan Creation & Updates")
    inner class LoanLifecycleTests {

        @Test
        fun `createLoan inserts entity and writes CREATED audit event`() = runTest {
            val loan = createSampleLoan(status = "PENDING_APPROVAL")
            val actor = "borrower_1"

            repository.createLoan(loan, actor)

            coVerify { loanDao.insertLoan(loan) }
            val capturedEvent = slot<AuditEventEntity>()
            coVerify { auditEventDao.insertEvent(capture(capturedEvent)) }

            assertThat(capturedEvent.captured.entityType).isEqualTo("LOAN")
            assertThat(capturedEvent.captured.entityId).isEqualTo("loan_123")
            assertThat(capturedEvent.captured.event).isEqualTo("CREATED")
            assertThat(capturedEvent.captured.newState).isEqualTo("PENDING_APPROVAL")
            assertThat(capturedEvent.captured.actor).isEqualTo(actor)
        }

        @Test
        fun `updateLoan records oldState and newState in audit event`() = runTest {
            val existingLoan = createSampleLoan(status = "ACTIVE")
            coEvery { loanDao.getLoanById("loan_123") } returns existingLoan

            val updatedLoan = existingLoan.copy(status = "CLOSED")
            repository.updateLoan(updatedLoan, actorId = "admin_1", description = "Early payoff")

            coVerify { loanDao.updateLoan(updatedLoan) }
            val capturedEvent = slot<AuditEventEntity>()
            coVerify { auditEventDao.insertEvent(capture(capturedEvent)) }

            assertThat(capturedEvent.captured.oldState).isEqualTo("ACTIVE")
            assertThat(capturedEvent.captured.newState).isEqualTo("CLOSED")
            assertThat(capturedEvent.captured.description).isEqualTo("Early payoff")
        }

        @Test
        fun `updateLoanStatus updates loan status and propagates change`() = runTest {
            val existingLoan = createSampleLoan(status = "ACTIVE")
            coEvery { loanDao.getLoanById("loan_123") } returns existingLoan

            repository.updateLoanStatus("loan_123", "OVERDUE", actorId = "system")

            coVerify {
                loanDao.updateLoan(match { it.loanId == "loan_123" && it.status == "OVERDUE" })
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Repayment & Auto-Closure Operations
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Repayment Recording & Loan Closure")
    inner class RepaymentTests {

        @Test
        fun `recordRepayment reduces outstanding amount when partial payment made`() = runTest {
            val loan = createSampleLoan(outstanding = 50000.0, status = "ACTIVE")
            coEvery { loanDao.getLoanById("loan_123") } returns loan

            val repayment = createSampleRepayment(amount = 10000.0)
            repository.recordRepayment(repayment, actorId = "borrower_1")

            coVerify { repaymentDao.insertRepayment(repayment) }
            coVerify {
                loanDao.updateLoan(match {
                    it.outstandingAmount == 40000.0 && it.status == "ACTIVE" && it.closedAt == null
                })
            }
        }

        @Test
        fun `recordRepayment closes loan when repayment pays off balance completely`() = runTest {
            val loan = createSampleLoan(outstanding = 10000.0, status = "ACTIVE")
            coEvery { loanDao.getLoanById("loan_123") } returns loan

            val repayment = createSampleRepayment(amount = 10000.0)
            repository.recordRepayment(repayment, actorId = "borrower_1")

            coVerify {
                loanDao.updateLoan(match {
                    it.outstandingAmount == 0.0 && it.status == "CLOSED" && it.closedAt != null
                })
            }
        }

        @Test
        fun `recordRepayment clamps outstanding to zero and closes loan if overpaid`() = runTest {
            val loan = createSampleLoan(outstanding = 5000.0, status = "ACTIVE")
            coEvery { loanDao.getLoanById("loan_123") } returns loan

            val repayment = createSampleRepayment(amount = 6000.0)
            repository.recordRepayment(repayment, actorId = "borrower_1")

            coVerify {
                loanDao.updateLoan(match {
                    it.outstandingAmount == 0.0 && it.status == "CLOSED"
                })
            }
        }

        @Test
        fun `getRepaymentsByLoan delegates to penaltyEngine when loan exists`() = runTest {
            val loan = createSampleLoan()
            val repayments = listOf(createSampleRepayment(status = "PENDING"))
            val enrichedRepayments = listOf(createSampleRepayment(status = "OVERDUE"))

            every { repaymentDao.getRepaymentsByLoan("loan_123") } returns flowOf(repayments)
            every { loanDao.observeLoan("loan_123") } returns flowOf(loan)
            every { penaltyEngine.applyPenalties(repayments, loan) } returns enrichedRepayments

            repository.getRepaymentsByLoan("loan_123").test {
                val item = awaitItem()
                assertThat(item).isEqualTo(enrichedRepayments)
                awaitComplete()
            }

            verify { penaltyEngine.applyPenalties(repayments, loan) }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Disbursement Operations
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Disbursements")
    inner class DisbursementTests {

        @Test
        fun `createDisbursement inserts entity and writes audit event`() = runTest {
            val disbursement = DisbursementEntity(
                disbursementId = "disb_1",
                loanId = "loan_123",
                amount = 25000.0,
                payeeId = "payee_1",
                payeeName = "Apollo Pharmacy",
                purpose = "Medicines",
                purposeCategory = "MEDICAL",
                verificationStatus = "VERIFIED",
                ruleEngineResult = "AUTO_APPROVED",
                approvalStatus = "PENDING"
            )

            repository.createDisbursement(disbursement, actorId = "borrower_1")

            coVerify { disbursementDao.insertDisbursement(disbursement) }
            val capturedEvent = slot<AuditEventEntity>()
            coVerify { auditEventDao.insertEvent(capture(capturedEvent)) }

            assertThat(capturedEvent.captured.entityType).isEqualTo("DISBURSEMENT")
            assertThat(capturedEvent.captured.entityId).isEqualTo("disb_1")
            assertThat(capturedEvent.captured.event).isEqualTo("CREATED")
            assertThat(capturedEvent.captured.newState).isEqualTo("PENDING")
        }
    }
}
