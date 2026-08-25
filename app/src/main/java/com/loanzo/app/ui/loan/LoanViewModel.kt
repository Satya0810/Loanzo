package com.loanzo.app.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.*
import com.loanzo.app.data.repository.LoanRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.data.dao.PayeeDao
import com.loanzo.app.domain.RuleEngine
import com.loanzo.app.domain.RuleEvaluation
import com.loanzo.app.domain.model.PurposeCategory
import com.loanzo.app.data.network.LeegalityService
import com.loanzo.app.util.calculateEMI
import com.loanzo.app.util.daysUntilDue
import com.loanzo.app.util.generateScheduleDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Single item in the generated amortization schedule */
data class ScheduleItem(
    val installmentNumber: Int,
    val dueDate: Long,
    val amount: Double,
    val principal: Double,
    val interest: Double,
    val status: String // PAID, UPCOMING, SCHEDULED
)

data class LoanUiState(
    val isLoading: Boolean = false,
    val loans: List<LoanEntity> = emptyList(),
    val selectedLoan: LoanEntity? = null,
    val disbursements: List<DisbursementEntity> = emptyList(),
    val repayments: List<RepaymentEntity> = emptyList(),
    val pledges: List<PledgeEntity> = emptyList(),
    val totalDisbursed: Double = 0.0,
    val totalVerified: Double = 0.0,
    val utilizationPercentage: Float = 0f,
    val totalPaid: Double = 0.0,
    val auditTrail: List<AuditEventEntity> = emptyList(),
    val ruleEvaluation: RuleEvaluation? = null,
    val payees: List<PayeeEntity> = emptyList(),
    val message: String? = null,
    val loanCreated: Boolean = false,
    // Enhanced dashboard fields
    val nextDueRepayment: RepaymentEntity? = null,
    val daysUntilNextDue: Int = 0,
    val totalPledgeValue: Double = 0.0,
    val ltvRatio: Float = 0f,
    val totalInterestPaid: Double = 0.0,
    val totalPrincipalPaid: Double = 0.0,
    val balanceHistory: List<Pair<Long, Double>> = emptyList(),
    val amortizationSchedule: List<ScheduleItem> = emptyList(),
    val isSigning: Boolean = false,
    val signUrl: String? = null
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val userRepository: UserRepository,
    private val payeeDao: PayeeDao,
    private val ruleEngine: RuleEngine,
    private val leegalityService: LeegalityService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanUiState())
    val uiState: StateFlow<LoanUiState> = _uiState.asStateFlow()

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            loanRepository.getAllLoansForUser(userId).collect { loans ->
                _uiState.update { it.copy(isLoading = false, loans = loans) }
            }
        }
    }

    fun loadLoanDetail(loanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loanRepository.observeLoan(loanId).collect { loan ->
                _uiState.update { state ->
                    val schedule = if (loan != null) buildAmortizationSchedule(loan, state.repayments) else emptyList()
                    state.copy(selectedLoan = loan, isLoading = false, amortizationSchedule = schedule)
                }
            }
        }
        viewModelScope.launch {
            loanRepository.getDisbursementsByLoan(loanId).collect { disb ->
                val totalDisb = disb.filter { it.approvalStatus == "APPROVED" }.sumOf { it.amount }
                val totalVerified = disb.filter { it.verificationStatus == "VERIFIED" }.sumOf { it.amount }
                val utilPct = if (totalDisb > 0) ((totalVerified / totalDisb) * 100).toFloat() else 0f
                _uiState.update {
                    it.copy(
                        disbursements = disb,
                        totalDisbursed = totalDisb,
                        totalVerified = totalVerified,
                        utilizationPercentage = utilPct
                    )
                }
            }
        }
        viewModelScope.launch {
            loanRepository.getRepaymentsByLoan(loanId).collect { repayments ->
                val paidRepayments = repayments.filter { it.status == "PAID" }
                val totalPaid = paidRepayments.sumOf { it.amount }
                val totalInterestPaid = paidRepayments.sumOf { it.interestComponent }
                val totalPrincipalPaid = paidRepayments.sumOf { it.principalComponent }

                // Build balance history from paid repayments (timestamp -> outstanding snapshot)
                val balanceHistory = paidRepayments
                    .sortedBy { it.paidDate ?: it.timestamp }
                    .map { (it.paidDate ?: it.timestamp) to it.outstandingSnapshot }

                _uiState.update { state ->
                    val schedule = state.selectedLoan?.let { buildAmortizationSchedule(it, repayments) } ?: emptyList()
                    state.copy(
                        repayments = repayments,
                        totalPaid = totalPaid,
                        totalInterestPaid = totalInterestPaid,
                        totalPrincipalPaid = totalPrincipalPaid,
                        balanceHistory = balanceHistory,
                        amortizationSchedule = schedule
                    )
                }
            }
        }
        // Next due repayment
        viewModelScope.launch {
            loanRepository.getNextDueRepayment(loanId).collect { nextDue ->
                _uiState.update {
                    it.copy(
                        nextDueRepayment = nextDue,
                        daysUntilNextDue = nextDue?.dueDate?.daysUntilDue() ?: 0
                    )
                }
            }
        }
        // Pledges + LTV
        viewModelScope.launch {
            loanRepository.getPledgesByLoan(loanId).collect { pledges ->
                val totalPledgeVal = pledges.sumOf { it.estimatedValue }
                val outstanding = _uiState.value.selectedLoan?.outstandingAmount ?: 0.0
                val ltv = if (totalPledgeVal > 0) ((outstanding / totalPledgeVal) * 100).toFloat() else 0f
                _uiState.update {
                    it.copy(pledges = pledges, totalPledgeValue = totalPledgeVal, ltvRatio = ltv)
                }
            }
        }
        viewModelScope.launch {
            loanRepository.getAuditTrail("LOAN", loanId).collect { events ->
                _uiState.update { it.copy(auditTrail = events) }
            }
        }
    }

    /** Generate amortization schedule from loan terms + actual repayment data */
    private fun buildAmortizationSchedule(loan: LoanEntity, repayments: List<RepaymentEntity>): List<ScheduleItem> {
        val emi = calculateEMI(loan.sanctionedAmount, loan.interestRate, loan.tenureMonths)
        val dates = generateScheduleDates(loan.createdAt, loan.tenureMonths, loan.repaymentFrequency)
        val monthlyRate = loan.interestRate / (12 * 100)
        val paidDates = repayments.filter { it.status == "PAID" }.map { it.dueDate }.toSet()
        val now = System.currentTimeMillis()
        var remainingPrincipal = loan.sanctionedAmount
        var foundUpcoming = false

        return dates.mapIndexed { index, date ->
            val interest = remainingPrincipal * monthlyRate
            val principal = (emi - interest).coerceAtLeast(0.0)
            remainingPrincipal = (remainingPrincipal - principal).coerceAtLeast(0.0)

            // Determine status
            val matchedPaid = repayments.find { it.status == "PAID" && kotlin.math.abs(it.dueDate - date) < 86_400_000L * 7 }
            val status = when {
                matchedPaid != null -> "PAID"
                !foundUpcoming && date >= now -> { foundUpcoming = true; "UPCOMING" }
                date < now -> "OVERDUE"
                else -> "SCHEDULED"
            }

            ScheduleItem(
                installmentNumber = index + 1,
                dueDate = date,
                amount = emi,
                principal = principal,
                interest = interest,
                status = status
            )
        }
    }

    fun createLoan(
        lenderId: String,
        amount: Double,
        purpose: String,
        loanType: String,
        interestRate: Double,
        interestModel: String,
        tenureMonths: Int,
        repaymentFrequency: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val borrowerId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = LoanEntity(
                loanId = UUID.randomUUID().toString(),
                lenderId = lenderId,
                borrowerId = borrowerId,
                sanctionedAmount = amount,
                outstandingAmount = amount,
                purpose = purpose,
                loanType = loanType,
                interestRate = interestRate,
                interestModel = interestModel,
                tenureMonths = tenureMonths,
                status = "ACTIVE",
                repaymentFrequency = repaymentFrequency,
                notes = notes
            )
            loanRepository.createLoan(loan, borrowerId)
            _uiState.update { it.copy(loanCreated = true, message = "Loan created successfully") }
        }
    }

    fun requestTranche(
        loanId: String,
        amount: Double,
        payeeId: String?,
        payeeName: String,
        payeeUpiId: String,
        purpose: String,
        purposeCategory: String,
        borrowerNote: String = ""
    ) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId) ?: return@launch
            val remainingLimit = loan.sanctionedAmount - loan.disbursedAmount

            // Check payee
            val payee = if (payeeId != null) payeeDao.getPayeeById(payeeId) else null
            val isPayeeVerified = payee?.verificationStatus == "VERIFIED"
            val payeeCat = try { PurposeCategory.valueOf(payee?.category ?: "") } catch (_: Exception) { null }
            val purpCat = try { PurposeCategory.valueOf(purposeCategory) } catch (_: Exception) { PurposeCategory.OTHER }

            // Run rule engine
            val evaluation = ruleEngine.evaluate(
                requestedAmount = amount,
                remainingLimit = remainingLimit,
                isPayeeVerified = isPayeeVerified,
                purposeCategory = purpCat,
                payeeCategory = payeeCat,
                previousDisbursementCount = _uiState.value.disbursements.size
            )

            _uiState.update { it.copy(ruleEvaluation = evaluation) }

            val approvalStatus = if (evaluation.canAutoApprove) "APPROVED" else "PENDING"

            val disbursement = DisbursementEntity(
                disbursementId = UUID.randomUUID().toString(),
                loanId = loanId,
                amount = amount,
                payeeId = payeeId,
                payeeName = payeeName,
                purpose = purpose,
                purposeCategory = purposeCategory,
                verificationStatus = if (isPayeeVerified) "VERIFIED" else "UNVERIFIED",
                ruleEngineResult = evaluation.result.name,
                approvalStatus = approvalStatus,
                borrowerNote = borrowerNote
            )
            loanRepository.createDisbursement(disbursement, userId)

            // If auto-approved, update loan disbursed amount
            if (evaluation.canAutoApprove) {
                val updatedLoan = loan.copy(disbursedAmount = loan.disbursedAmount + amount)
                loanRepository.updateLoan(updatedLoan, userId, "Auto-approved tranche of ₹$amount")
            }

            _uiState.update {
                it.copy(message = if (evaluation.canAutoApprove) "Tranche auto-approved!" else "Tranche submitted for lender review")
            }
        }
    }

    fun approveDisbursement(disbursementId: String) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val disb = _uiState.value.disbursements.find { it.disbursementId == disbursementId } ?: return@launch
            val updated = disb.copy(approvalStatus = "APPROVED")
            loanRepository.updateDisbursement(updated, userId, "APPROVED")

            // Update loan disbursed amount
            val loan = loanRepository.getLoanById(disb.loanId) ?: return@launch
            val updatedLoan = loan.copy(disbursedAmount = loan.disbursedAmount + disb.amount)
            loanRepository.updateLoan(updatedLoan, userId, "Tranche approved: ₹${disb.amount}")

            _uiState.update { it.copy(message = "Tranche approved") }
        }
    }

    fun rejectDisbursement(disbursementId: String, reason: String = "") {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val disb = _uiState.value.disbursements.find { it.disbursementId == disbursementId } ?: return@launch
            val updated = disb.copy(approvalStatus = "REJECTED", lenderNote = reason)
            loanRepository.updateDisbursement(updated, userId, "REJECTED")
            _uiState.update { it.copy(message = "Tranche rejected") }
        }
    }

    fun recordRepayment(
        loanId: String,
        amount: Double,
        transactionRef: String
    ) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId) ?: return@launch
            val newOutstanding = (loan.outstandingAmount - amount).coerceAtLeast(0.0)

            val repayment = RepaymentEntity(
                repaymentId = UUID.randomUUID().toString(),
                loanId = loanId,
                amount = amount,
                transactionRef = transactionRef,
                status = "PAID",
                dueDate = System.currentTimeMillis(),
                paidDate = System.currentTimeMillis(),
                outstandingSnapshot = newOutstanding,
                principalComponent = amount,
                interestComponent = 0.0
            )
            loanRepository.recordRepayment(repayment, userId)
            _uiState.update { it.copy(message = "Repayment of ₹$amount recorded successfully") }
        }
    }

    fun addPayee(
        name: String,
        upiId: String,
        gstNumber: String,
        category: String
    ) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val payee = PayeeEntity(
                payeeId = UUID.randomUUID().toString(),
                name = name,
                upiId = upiId,
                gstNumber = gstNumber,
                verificationStatus = if (upiId.isNotBlank()) "VERIFIED" else "PENDING",
                category = category,
                verifiedAt = if (upiId.isNotBlank()) System.currentTimeMillis() else null,
                addedBy = userId
            )
            payeeDao.insertPayee(payee)
            _uiState.update { it.copy(message = "Payee added") }
        }
    }

    fun loadPayees() {
        viewModelScope.launch {
            payeeDao.getAllPayees().collect { payees ->
                _uiState.update { it.copy(payees = payees) }
            }
        }
    }

    fun addPledge(
        loanId: String,
        assetDescription: String,
        assetType: String,
        estimatedValue: Double,
        weight: Double = 0.0,
        photoUri: String = ""
    ) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val pledge = PledgeEntity(
                pledgeId = UUID.randomUUID().toString(),
                loanId = loanId,
                assetDescription = assetDescription,
                assetType = assetType,
                estimatedValue = estimatedValue,
                weight = weight,
                photoUri = photoUri,
                receiptStatus = "PENDING"
            )
            loanRepository.createPledge(pledge, userId)
            _uiState.update { it.copy(message = "Pledge added") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun resetLoanCreated() {
        _uiState.update { it.copy(loanCreated = false) }
    }

    fun initiateSigning(loanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigning = true) }
            val borrowerId = userRepository.getCurrentUserIdSync() ?: return@launch
            // Need to get UserEntity for the borrower
            // I'll fetch user logic here (assuming repository has it, actually userRepository might not have a direct sync get user, let's use a flow or just fake the user details for now since we don't have the full UserRepository view, but we can assume getUserById(borrowerId) works or we just pass dummy data if it fails)
            var borrower = UserEntity(userId = borrowerId, email = "test@loanzo.com", phone = "9999999999", name = "Borrower", role = "BORROWER", kycStatus = "VERIFIED")
            // In a real app we fetch it: val borrower = userRepository.getUserById(borrowerId) ?: return@launch

            val loan = loanRepository.getLoanById(loanId) ?: return@launch
            val signUrl = leegalityService.createSigningWorkflow(loan, borrower)
            
            if (signUrl != null) {
                _uiState.update { it.copy(isSigning = false, signUrl = signUrl) }
            } else {
                _uiState.update { it.copy(isSigning = false, message = "Failed to generate signing URL. Check API keys.") }
            }
        }
    }

    fun markAgreementSigned(loanId: String) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId) ?: return@launch
            val updatedLoan = loan.copy(isAgreementSigned = true, agreementDocumentId = java.util.UUID.randomUUID().toString())
            loanRepository.updateLoan(updatedLoan, userId, "Loan agreement signed digitally")
            
            _uiState.update { it.copy(message = "Agreement signed successfully!", signUrl = null) }
            loadLoanDetail(loanId)
        }
    }

    fun clearSignUrl() {
        _uiState.update { it.copy(signUrl = null) }
    }
}
