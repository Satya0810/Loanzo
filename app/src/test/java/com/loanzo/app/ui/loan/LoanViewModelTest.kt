package com.loanzo.app.ui.loan

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.loanzo.app.data.dao.MediationMeetingDao
import com.loanzo.app.data.dao.PayeeDao
import com.loanzo.app.data.drive.GoogleDriveManager
import com.loanzo.app.data.entity.*
import com.loanzo.app.data.network.LeegalityService
import com.loanzo.app.data.repository.*
import com.loanzo.app.domain.RuleEngine
import com.loanzo.app.testutil.MainDispatcherExtension
import com.loanzo.app.util.TelegramManager
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.RegisterExtension

class LoanViewModelTest {

    @RegisterExtension
    @JvmField
    val mainDispatcher = MainDispatcherExtension()

    private val loanRepository = mockk<LoanRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val payeeDao = mockk<PayeeDao>(relaxed = true)
    private val ruleEngine = mockk<RuleEngine>(relaxed = true)
    private val leegalityService = mockk<LeegalityService>(relaxed = true)
    private val googleDriveManager = mockk<GoogleDriveManager>(relaxed = true)
    private val documentVaultRepository = mockk<DocumentVaultRepository>(relaxed = true)
    private val telegramManager = mockk<TelegramManager>(relaxed = true)
    private val marketplaceRepository = mockk<MarketplaceRepository>(relaxed = true)
    private val agentRepository = mockk<AgentRepository>(relaxed = true)
    private val mediationMeetingDao = mockk<MediationMeetingDao>(relaxed = true)

    private val sampleBorrower = UserEntity(
        userId = "usr_borrower",
        name = "Kavita Rao",
        username = "kavita_r",
        email = "kavita@test.com",
        phone = "+919876543210",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    )

    private val sampleLender = UserEntity(
        userId = "usr_lender",
        name = "Anil Kapoor",
        username = "anil_k",
        email = "anil@test.com",
        phone = "+919811223344",
        role = "LENDER",
        kycStatus = "VERIFIED"
    )

    private val sampleLoan = LoanEntity(
        loanId = "loan_1001",
        borrowerId = "usr_borrower",
        lenderId = "usr_lender",
        sanctionedAmount = 100000.0,
        outstandingAmount = 100000.0,
        disbursedAmount = 50000.0,
        interestRate = 12.0,
        tenureMonths = 12,
        purpose = "Business Expansion",
        status = "ACTIVE",
        repaymentFrequency = "MONTHLY"
    )

    private val sampleDisbursement = DisbursementEntity(
        disbursementId = "disb_1001",
        loanId = "loan_1001",
        amount = 50000.0,
        payeeId = "payee_1",
        payeeName = "Apollo Hospitals",
        purpose = "Medical Bills",
        purposeCategory = "MEDICAL",
        verificationStatus = "VERIFIED",
        ruleEngineResult = "AUTO_APPROVED",
        approvalStatus = "APPROVED",
        timestamp = System.currentTimeMillis()
    )

    private val sampleRepayment = RepaymentEntity(
        repaymentId = "rep_1001",
        loanId = "loan_1001",
        dueDate = System.currentTimeMillis() + 86_400_000L * 30,
        amount = 8884.0,
        principalComponent = 7884.0,
        interestComponent = 1000.0,
        penalty = 500.0,
        penaltyWaived = false,
        status = "PAID",
        paidDate = System.currentTimeMillis()
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        mockkStatic(FirebaseAuth::class)
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockAuth

        mockkStatic(FirebaseFirestore::class)
        val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        coEvery { userRepository.getCurrentUserIdSync() } returns "usr_borrower"
        coEvery { userRepository.getUserById("usr_borrower") } returns sampleBorrower
        coEvery { userRepository.getUserById("usr_lender") } returns sampleLender
        coEvery { userRepository.getUserByUsername("kavita_r") } returns sampleBorrower
        coEvery { userRepository.getUserByUsername("anil_k") } returns sampleLender

        every { loanRepository.getAllLoansForUser("usr_borrower") } returns flowOf(listOf(sampleLoan))
        every { loanRepository.observeLoan("loan_1001") } returns flowOf(sampleLoan)
        every { loanRepository.getDisbursementsByLoan("loan_1001") } returns flowOf(listOf(sampleDisbursement))
        every { loanRepository.getRepaymentsByLoan("loan_1001") } returns flowOf(listOf(sampleRepayment))
        every { loanRepository.getPledgesByLoan("loan_1001") } returns flowOf(emptyList())
        every { loanRepository.getAuditTrail("LOAN", "loan_1001") } returns flowOf(emptyList())
        every { mediationMeetingDao.getMeetingsForLoan("loan_1001") } returns flowOf(emptyList())
        every { agentRepository.observeActiveVisitForLoan("loan_1001") } returns flowOf(null)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        unmockkStatic(FirebaseFirestore::class)
    }

    private fun createViewModel(): LoanViewModel {
        return LoanViewModel(
            loanRepository = loanRepository,
            userRepository = userRepository,
            payeeDao = payeeDao,
            ruleEngine = ruleEngine,
            leegalityService = leegalityService,
            googleDriveManager = googleDriveManager,
            documentVaultRepository = documentVaultRepository,
            telegramManager = telegramManager,
            marketplaceRepository = marketplaceRepository,
            agentRepository = agentRepository,
            mediationMeetingDao = mediationMeetingDao
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Loan Loading & Detail
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Loan Loading & Detail Tests")
    inner class LoadLoanTests {

        @Test
        fun `loadLoans populates loans in uiState for authenticated user`() = runTest {
            val viewModel = createViewModel()
            viewModel.loadLoans()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.loans).containsExactly(sampleLoan)
        }

        @Test
        fun `loadLoans clears loans if no user is currently authenticated`() = runTest {
            coEvery { userRepository.getCurrentUserIdSync() } returns null

            val viewModel = createViewModel()
            viewModel.loadLoans()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.loans).isEmpty()
        }

        @Test
        fun `loadLoanDetail computes financial statistics and amortization schedule`() = runTest {
            val viewModel = createViewModel()
            viewModel.loadLoanDetail("loan_1001")

            val state = viewModel.uiState.value
            assertThat(state.selectedLoan).isEqualTo(sampleLoan)
            assertThat(state.totalDisbursed).isEqualTo(50000.0)
            assertThat(state.totalVerified).isEqualTo(50000.0)
            assertThat(state.utilizationPercentage).isEqualTo(100f)
            assertThat(state.totalPaid).isEqualTo(8884.0)
            assertThat(state.totalPrincipalPaid).isEqualTo(7884.0)
            assertThat(state.totalInterestPaid).isEqualTo(1000.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Loan Creation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Loan Creation Tests")
    inner class CreateLoanTests {

        @Test
        fun `createLoan creates loan with schedule and marks loanCreated true`() = runTest {
            coEvery { loanRepository.createLoan(any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.createLoan(
                counterpartyId = "usr_lender",
                amount = 75000.0,
                purpose = "Inventory purchase",
                loanType = "BUSINESS",
                interestRate = 14.0,
                interestModel = "REDUCING_BALANCE",
                tenureMonths = 6,
                repaymentFrequency = "MONTHLY",
                isGrantMode = false
            )

            val state = viewModel.uiState.value
            assertThat(state.loanCreated).isTrue()
            assertThat(state.message).contains("Loan requested successfully")
            coVerify { loanRepository.createLoan(match { it.sanctionedAmount == 75000.0 && it.purpose == "Inventory purchase" }, "usr_borrower") }
        }

        @Test
        fun `resetLoanCreated resets state flag`() = runTest {
            coEvery { loanRepository.createLoan(any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.createLoan(
                counterpartyId = "usr_lender",
                amount = 25000.0,
                purpose = "Medical Emergency",
                loanType = "EMERGENCY",
                interestRate = 10.0,
                interestModel = "FLAT",
                tenureMonths = 3,
                repaymentFrequency = "MONTHLY"
            )
            assertThat(viewModel.uiState.value.loanCreated).isTrue()

            viewModel.resetLoanCreated()
            assertThat(viewModel.uiState.value.loanCreated).isFalse()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Disbursements & Tranches
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Disbursement & Tranche Tests")
    inner class DisbursementTests {

        @Test
        fun `requestTranche delegates to loanRepository`() = runTest {
            coEvery { loanRepository.getLoanById("loan_1001") } returns sampleLoan
            coEvery { loanRepository.createDisbursement(any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.requestTranche(
                loanId = "loan_1001",
                amount = 25000.0,
                payeeId = "payee_1",
                payeeName = "Apollo Hospitals",
                payeeUpiId = "apollo@okhdfcbank",
                purpose = "Surgery fees",
                purposeCategory = "MEDICAL"
            )

            val state = viewModel.uiState.value
            assertThat(state.message).contains("Tranche submitted for lender review")
            coVerify {
                loanRepository.createDisbursement(
                    match { it.loanId == "loan_1001" && it.amount == 25000.0 && it.payeeName == "Apollo Hospitals" },
                    "usr_borrower"
                )
            }
        }

        @Test
        fun `approveDisbursement approves tranche and sets confirmation message`() = runTest {
            val pendingDisbursement = sampleDisbursement.copy(disbursementId = "disb_pending", approvalStatus = "PENDING")
            every { loanRepository.getDisbursementsByLoan("loan_1001") } returns flowOf(listOf(pendingDisbursement))
            coEvery { loanRepository.getLoanById("loan_1001") } returns sampleLoan
            coEvery { loanRepository.updateDisbursement(any(), any(), any()) } returns Unit
            coEvery { loanRepository.updateLoan(any(), any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.loadLoanDetail("loan_1001")
            viewModel.approveDisbursement("disb_pending")

            val state = viewModel.uiState.value
            assertThat(state.message).contains("Tranche approved")
            coVerify { loanRepository.updateDisbursement(match { it.approvalStatus == "APPROVED" }, "usr_borrower", "APPROVED") }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Repayments & Penalty Waivers
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Repayment & Penalty Tests")
    inner class RepaymentTests {

        @Test
        fun `recordRepayment delegates to loanRepository with payment details`() = runTest {
            coEvery { loanRepository.getLoanById("loan_1001") } returns sampleLoan
            coEvery { loanRepository.recordRepayment(any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.recordRepayment(
                loanId = "loan_1001",
                amount = 8884.0,
                transactionRef = "UTR998877665544"
            )

            val state = viewModel.uiState.value
            assertThat(state.message).contains("recorded successfully")
            coVerify {
                loanRepository.recordRepayment(
                    match { it.amount == 8884.0 && it.transactionRef == "UTR998877665544" },
                    "usr_borrower"
                )
            }
        }

        @Test
        fun `waivePenalty waives repayment penalty via repository`() = runTest {
            coEvery { loanRepository.updateRepayment(any(), any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.waivePenalty(sampleRepayment)

            val state = viewModel.uiState.value
            assertThat(state.message).contains("Penalty waived successfully")
            coVerify { loanRepository.updateRepayment(match { it.penaltyWaived }, "usr_borrower", any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Guarantors & Payees
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Guarantor & Payee Tests")
    inner class GuarantorAndPayeeTests {

        @Test
        fun `addGuarantor creates guarantor entity and saves to repository`() = runTest {
            coEvery { loanRepository.createGuarantor(any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.addGuarantor(
                loanId = "loan_1001",
                name = "Sunil Kumar",
                phone = "9876500000",
                email = "sunil@test.com",
                panNumber = "ABCDE1234F",
                relationship = "BROTHER"
            )

            val state = viewModel.uiState.value
            assertThat(state.message).contains("Guarantor added")
            coVerify {
                loanRepository.createGuarantor(
                    match { it.loanId == "loan_1001" && it.name == "Sunil Kumar" && it.relationship == "BROTHER" },
                    "usr_borrower"
                )
            }
        }

        @Test
        fun `updateGuarantorConsent updates consent status`() = runTest {
            val sampleGuarantor = GuarantorEntity(
                guarantorId = "guar_101",
                loanId = "loan_1001",
                name = "Sunil Kumar",
                phone = "9876500000",
                relationship = "BROTHER"
            )
            coEvery { loanRepository.getGuarantorById("guar_101") } returns sampleGuarantor
            coEvery { loanRepository.updateGuarantor(any(), any(), any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.updateGuarantorConsent("guar_101", "ACCEPTED")

            coVerify { loanRepository.updateGuarantor(match { it.consentStatus == "ACCEPTED" }, "usr_borrower", any()) }
        }

        @Test
        fun `loadPayees fetches payees from payeeDao`() = runTest {
            val samplePayee = PayeeEntity(
                payeeId = "payee_1",
                name = "Apollo Hospitals",
                verificationStatus = "VERIFIED",
                category = "HOSPITAL"
            )
            every { payeeDao.getAllPayees() } returns flowOf(listOf(samplePayee))

            val viewModel = createViewModel()
            viewModel.loadPayees()

            val state = viewModel.uiState.value
            assertThat(state.payees).containsExactly(samplePayee)
        }
    }
}
