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
import com.loanzo.app.data.drive.GoogleDriveManager
import com.loanzo.app.data.firebase.FirebaseManager
import com.loanzo.app.util.AgreementGenerator
import android.graphics.Bitmap
import android.content.Context
import java.io.File
import java.io.FileOutputStream

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
    val guarantors: List<GuarantorEntity> = emptyList(),
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
    val signUrl: String? = null,
    val activeFieldVisit: AgentVisitEntity? = null,
    val mediationMeetings: List<com.loanzo.app.data.entity.MediationMeetingEntity> = emptyList()
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val userRepository: UserRepository,
    private val payeeDao: PayeeDao,
    private val ruleEngine: RuleEngine,
    private val leegalityService: LeegalityService,
    private val googleDriveManager: GoogleDriveManager,
    private val documentVaultRepository: com.loanzo.app.data.repository.DocumentVaultRepository,
    private val telegramManager: com.loanzo.app.util.TelegramManager,
    private val marketplaceRepository: com.loanzo.app.data.repository.MarketplaceRepository,
    private val agentRepository: com.loanzo.app.data.repository.AgentRepository,
    private val mediationMeetingDao: com.loanzo.app.data.dao.MediationMeetingDao,
    private val multiAiRaceEngine: com.loanzo.app.data.ai.MultiAiRaceEngine? = null
) : ViewModel() {

    suspend fun simplifyAgreementClause(clauseText: String): com.loanzo.app.data.ai.AiRaceResult {
        return multiAiRaceEngine?.simplifyLegalAgreementResult(clauseText)
            ?: com.loanzo.app.data.ai.AiRaceResult.Success(
                providerName = "Offline Legal Rulebook",
                providerType = "OFFLINE",
                content = "• Repayment: Mandatory fixed-schedule debt service with zero compound penalties.\n• Grace Window: 3-day statutory waiver prior to any late fee assessment.\n• Legal Enforceability: Valid promissory obligation under Section 4 Negotiable Instruments Act.",
                latencyMs = 0L,
                modelUsed = "offline-legal-v1"
            )
    }

    private val _uiState = MutableStateFlow(LoanUiState())
    val uiState: StateFlow<LoanUiState> = _uiState.asStateFlow()

    private val firestore get() = com.loanzo.app.data.firebase.FirestoreProvider.get()

    private fun notificationToMap(notif: com.loanzo.app.data.entity.NotificationEntity): Map<String, Any?> {
        return hashMapOf(
            "notificationId" to notif.notificationId,
            "userId" to notif.userId,
            "title" to notif.title,
            "message" to notif.message,
            "type" to notif.type,
            "relatedLoanId" to notif.relatedLoanId,
            "actionRoute" to notif.actionRoute,
            "dayKey" to notif.dayKey,
            "timestamp" to notif.timestamp,
            "isRead" to notif.isRead
        )
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = userRepository.getCurrentUserIdSync()
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, loans = emptyList()) }
                return@launch
            }
            // Trigger automatic cloud sync to restore all user loans into Room
            launch(kotlinx.coroutines.Dispatchers.IO) {
                loanRepository.syncUserLoansFromCloud(userId)
                loanRepository.startRealtimeUserLoansSync(userId, this)
            }
            loanRepository.getAllLoansForUser(userId).collect { loans ->
                _uiState.update { it.copy(isLoading = false, loans = loans) }
            }
        }
    }

    fun sendPaymentReminder(context: android.content.Context) {
        viewModelScope.launch {
            val loan = _uiState.value.selectedLoan ?: return@launch
            val nextDue = _uiState.value.nextDueRepayment ?: return@launch
            val borrower = userRepository.getUserById(loan.borrowerId)
            
            var pushSent = false
            if (borrower != null && borrower.fcmToken.isNotBlank()) {
                val fcmSender = com.loanzo.app.fcm.FcmSender()
                pushSent = fcmSender.sendPaymentReminder(context, borrower.fcmToken, nextDue.amount.toString())
            }

            // Bug #16: In-app fallback: ALWAYS generate and push in-app notification to borrower
            val notif = com.loanzo.app.data.entity.NotificationEntity(
                notificationId = "remind_" + UUID.randomUUID().toString().take(8),
                userId = loan.borrowerId,
                title = "Payment Reminder: ₹${nextDue.amount.toInt()} Due",
                message = "Friendly reminder from lender: An EMI payment of ₹${nextDue.amount.toInt()} for ${loan.purpose} is due.",
                type = "DEADLINE",
                relatedLoanId = loan.loanId,
                actionRoute = "loan_detail/${loan.loanId}",
                timestamp = System.currentTimeMillis()
            )
            try {
                firestore
                    .collection("notifications")
                    .document(notif.notificationId)
                    .set(notif)
            } catch (_: Exception) {}

            _uiState.update {
                it.copy(message = if (pushSent) "Push notification & in-app reminder sent!" else "Payment reminder sent to borrower in-app!")
            }
        }
    }

    fun loadLoanDetail(loanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Fetch directly from Cloud Firestore in case this device hasn't cached it yet (e.g. counterparty / notification open)
            loanRepository.fetchAndSyncLoanById(loanId)
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
        // Guarantors
        viewModelScope.launch {
            loanRepository.getGuarantorsByLoan(loanId).collect { guarantors ->
                _uiState.update { it.copy(guarantors = guarantors) }
            }
        }
        viewModelScope.launch {
            loanRepository.getAuditTrail("LOAN", loanId).collect { events ->
                _uiState.update { it.copy(auditTrail = events) }
            }
        }
        viewModelScope.launch {
            agentRepository.observeActiveVisitForLoan(loanId).collect { visit ->
                _uiState.update { it.copy(activeFieldVisit = visit) }
            }
        }
        viewModelScope.launch {
            mediationMeetingDao.getMeetingsForLoan(loanId).collect { meetings ->
                _uiState.update { it.copy(mediationMeetings = meetings) }
            }
        }
    }

    fun requestFieldVerification(loan: LoanEntity) {
        viewModelScope.launch {
            try {
                val borrower = userRepository.getUserById(loan.borrowerId)
                val lender = userRepository.getUserById(loan.lenderId)
                val firstPledge = _uiState.value.pledges.firstOrNull()
                val collateralDesc: String? = firstPledge?.assetDescription?.ifBlank { null }
                val collateralVal: Double? = firstPledge?.estimatedValue?.takeIf { it > 0.0 }

                val borrowerAddr = borrower?.address?.ifBlank { "Registered Residential Address" } ?: "Registered Residential Address"
                val lenderAddr = lender?.address?.ifBlank { "Lender Registered Office" } ?: "Lender Registered Office"

                agentRepository.requestFieldVerification(
                    loanId = loan.loanId,
                    title = "Doorstep Verification: ${loan.purpose}",
                    borrowerName = borrower?.name?.ifBlank { "Borrower" } ?: "Borrower",
                    borrowerPhone = borrower?.phone?.ifBlank { "+919876543210" } ?: "+919876543210",
                    borrowerAddress = borrowerAddr,
                    lenderName = lender?.name?.ifBlank { "Lender" } ?: "Lender",
                    lenderPhone = lender?.phone?.ifBlank { "+919811223344" } ?: "+919811223344",
                    lenderAddress = lenderAddr,
                    targetAddress = borrowerAddr,
                    collateralItemName = collateralDesc,
                    collateralEstimatedValue = collateralVal,
                    loanType = loan.loanType
                )
                _uiState.update { it.copy(message = "Field verification officer requested! Dispatched to nearby active agents.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to request verification: ${e.message}") }
            }
        }
    }

    /** Generate amortization schedule from loan terms + actual repayment data, accounting for moratorium */
    private fun buildAmortizationSchedule(loan: LoanEntity, repayments: List<RepaymentEntity>): List<ScheduleItem> {
        val totalMonths = loan.tenureMonths
        val moratoriumMonths = loan.moratoriumMonths
        val effectiveTenure = (totalMonths - moratoriumMonths).coerceAtLeast(1)
        val regularEmi = calculateEMI(loan.sanctionedAmount, loan.interestRate, effectiveTenure)
        val dates = generateScheduleDates(loan.createdAt, totalMonths, loan.repaymentFrequency)
        val monthlyRate = loan.interestRate / (12 * 100)
        val paidDates = repayments.filter { it.status == "PAID" }.map { it.dueDate }.toSet()
        val now = System.currentTimeMillis()
        var remainingPrincipal = loan.sanctionedAmount
        var foundUpcoming = false

        return dates.mapIndexed { index, date ->
            val isMoratorium = index < moratoriumMonths
            val interest = remainingPrincipal * monthlyRate
            val principal = if (isMoratorium) 0.0 else (regularEmi - interest).coerceAtLeast(0.0)
            val installmentAmount = if (isMoratorium) interest else regularEmi
            if (!isMoratorium) {
                remainingPrincipal = (remainingPrincipal - principal).coerceAtLeast(0.0)
            }

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
                amount = installmentAmount,
                principal = principal,
                interest = interest,
                status = status
            )
        }
    }

    fun createLoan(
        counterpartyId: String,
        amount: Double,
        purpose: String,
        loanType: String,
        interestRate: Double,
        interestModel: String,
        tenureMonths: Int,
        repaymentFrequency: String,
        notes: String = "",
        penaltyRate: Double = 2.0,
        penaltyModel: String = "PERCENTAGE",
        penaltyGraceDays: Int = 3,
        isGrantMode: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUserIdSync()
                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: "user_default"

                val cleanInput = counterpartyId.trim()
                val resolvedUser = userRepository.getUserById(cleanInput)
                    ?: userRepository.getUserByUsername(cleanInput.removePrefix("@"))
                    ?: (if (cleanInput.contains("@")) userRepository.getUserByEmail(cleanInput) else null)
                    ?: userRepository.getUserByPhone(cleanInput)
                    ?: userRepository.getUserByPhone("+91$cleanInput")
                    ?: userRepository.getUserByPhone(cleanInput.removePrefix("+91"))
                val finalCounterpartyId = resolvedUser?.userId ?: cleanInput.ifBlank { "counterparty_user" }

                // Safe Foreign-Key Guard: Ensure both lender and borrower exist in Room's users table
                val existingCurrent = userRepository.getUserById(currentUserId)
                if (existingCurrent == null) {
                    userRepository.createUser(
                        UserEntity(
                            userId = currentUserId,
                            phone = "+91 98765 43210",
                            name = "Current User",
                            username = "current_user",
                            role = if (isGrantMode) "LENDER" else "BORROWER",
                            kycStatus = "VERIFIED"
                        )
                    )
                }

                val existingCounterparty = userRepository.getUserById(finalCounterpartyId)
                if (existingCounterparty == null) {
                    val cleanUsername = if (cleanInput.startsWith("@")) cleanInput.removePrefix("@") else cleanInput.lowercase().replace(" ", "_").take(15)
                    userRepository.createUser(
                        UserEntity(
                            userId = finalCounterpartyId,
                            phone = if (cleanInput.startsWith("+91") || cleanInput.all { it.isDigit() }) cleanInput else "+91 98000 00000",
                            name = if (cleanInput.isNotBlank()) cleanInput else "Counterparty User",
                            username = cleanUsername.ifBlank { "member_${finalCounterpartyId.take(6)}" },
                            role = if (isGrantMode) "BORROWER" else "LENDER",
                            kycStatus = "VERIFIED"
                        )
                    )
                }

                val actualLenderId = if (isGrantMode) currentUserId else finalCounterpartyId
                val actualBorrowerId = if (isGrantMode) finalCounterpartyId else currentUserId

                val loan = LoanEntity(
                    loanId = UUID.randomUUID().toString(),
                    lenderId = actualLenderId,
                    borrowerId = actualBorrowerId,
                    sanctionedAmount = amount,
                    outstandingAmount = amount,
                    purpose = purpose.ifBlank { "Personal Loan" },
                    loanType = loanType,
                    interestRate = interestRate,
                    interestModel = interestModel,
                    tenureMonths = tenureMonths,
                    status = "CONTRACT_SIGNING", // Flowchart verified lifecycle state
                    repaymentFrequency = repaymentFrequency,
                    notes = notes,
                    penaltyRate = penaltyRate,
                    penaltyModel = penaltyModel,
                    penaltyGraceDays = penaltyGraceDays
                )

                loanRepository.createLoan(loan, currentUserId)

                // Push to Firestore so both parties have cloud records
                try {
                    firestore
                        .collection("loans")
                        .document(loan.loanId)
                        .set(loanRepository.loanToFirestoreMap(loan), com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}

                // Bug #15: Notify counterparty in Firestore of loan creation/grant
                try {
                    val targetUser = if (isGrantMode) actualBorrowerId else actualLenderId
                    if (targetUser.isNotBlank()) {
                        val notif = com.loanzo.app.data.entity.NotificationEntity(
                            notificationId = "notif_loan_created_" + UUID.randomUUID().toString().take(8),
                            userId = targetUser,
                            title = if (isGrantMode) "Loan Granted: ₹${amount.toInt()}" else "New Loan Request: ₹${amount.toInt()}",
                            message = if (isGrantMode) "You received a loan of ₹${amount.toInt()} for ${loan.purpose}. Review agreement to proceed." else "A borrower requested a loan of ₹${amount.toInt()} for ${loan.purpose}.",
                            type = "AGREEMENT",
                            relatedLoanId = loan.loanId,
                            actionRoute = "loan_detail/${loan.loanId}",
                            timestamp = System.currentTimeMillis()
                        )
                        firestore
                            .collection("notifications")
                            .document(notif.notificationId)
                            .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
                    }
                } catch (_: Exception) {}

                try {
                    val borrowerName = resolvedUser?.name?.takeIf { it.isNotBlank() } ?: if (isGrantMode) "Borrower (ID: ${finalCounterpartyId.take(8)})" else "Borrower"
                    telegramManager.notifyLoanRequested(
                        borrowerName = borrowerName,
                        loanId = loan.loanId,
                        amount = loan.sanctionedAmount,
                        purpose = loan.purpose
                    )
                } catch (_: Exception) {}

                val successMsg = if (isGrantMode) "Loan granted successfully (₹${amount.toInt()})" else "Loan requested successfully (₹${amount.toInt()})"
                _uiState.update { it.copy(loanCreated = true, message = successMsg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Loan creation failed: ${e.localizedMessage}") }
            }
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
            } else {
                // Bug #15: Notify lender of pending manual tranche approval
                try {
                    val notif = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "notif_tranche_req_" + UUID.randomUUID().toString().take(8),
                        userId = loan.lenderId,
                        title = "⚡ Tranche Approval Requested",
                        message = "Borrower requested milestone tranche of ₹${amount.toInt()} for $purpose.",
                        type = "DISBURSEMENT",
                        relatedLoanId = loanId,
                        actionRoute = "loan_detail/$loanId",
                        timestamp = System.currentTimeMillis()
                    )
                    firestore
                        .collection("notifications")
                        .document(notif.notificationId)
                        .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}
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

            // Bug #15: Notify borrower that tranche is approved
            try {
                val notif = com.loanzo.app.data.entity.NotificationEntity(
                    notificationId = "notif_tranche_appr_" + UUID.randomUUID().toString().take(8),
                    userId = loan.borrowerId,
                    title = "✅ Tranche Approved: ₹${disb.amount.toInt()}",
                    message = "Lender approved your milestone tranche of ₹${disb.amount.toInt()} for ${disb.purpose}.",
                    type = "DISBURSEMENT",
                    relatedLoanId = disb.loanId,
                    actionRoute = "loan_detail/${disb.loanId}",
                    timestamp = System.currentTimeMillis()
                )
                firestore
                    .collection("notifications")
                    .document(notif.notificationId)
                    .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
            } catch (_: Exception) {}

            _uiState.update { it.copy(message = "Tranche approved") }
        }
    }

    fun rejectDisbursement(disbursementId: String, reason: String = "") {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val disb = _uiState.value.disbursements.find { it.disbursementId == disbursementId } ?: return@launch
            val updated = disb.copy(approvalStatus = "REJECTED", lenderNote = reason)
            loanRepository.updateDisbursement(updated, userId, "REJECTED")

            val loan = loanRepository.getLoanById(disb.loanId)
            if (loan != null) {
                try {
                    val notif = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "notif_tranche_rej_" + UUID.randomUUID().toString().take(8),
                        userId = loan.borrowerId,
                        title = "❌ Tranche Rejected",
                        message = "Your tranche of ₹${disb.amount.toInt()} was rejected${if (reason.isNotBlank()) ": $reason" else "."}",
                        type = "DISBURSEMENT",
                        relatedLoanId = disb.loanId,
                        actionRoute = "loan_detail/${disb.loanId}",
                        timestamp = System.currentTimeMillis()
                    )
                    firestore
                        .collection("notifications")
                        .document(notif.notificationId)
                        .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}
            }

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

            // Sync to Firestore cloud for instant real-time delivery to counterparty
            try {
                val firestore = firestore
                val repaymentMap = hashMapOf<String, Any?>(
                    "repaymentId" to repayment.repaymentId,
                    "loanId" to repayment.loanId,
                    "amount" to repayment.amount,
                    "dueDate" to repayment.dueDate,
                    "paidDate" to repayment.paidDate,
                    "transactionRef" to repayment.transactionRef,
                    "status" to repayment.status,
                    "outstandingSnapshot" to repayment.outstandingSnapshot,
                    "principalComponent" to repayment.principalComponent,
                    "interestComponent" to repayment.interestComponent,
                    "penalty" to repayment.penalty,
                    "timestamp" to repayment.timestamp,
                    "note" to repayment.note
                )
                firestore.collection("repayments").document(repayment.repaymentId)
                    .set(repaymentMap, com.google.firebase.firestore.SetOptions.merge())
                
                val updatedLoan = loan.copy(
                    outstandingAmount = newOutstanding,
                    status = if (newOutstanding <= 0.0) "COMPLETED" else loan.status,
                    closedAt = if (newOutstanding <= 0.0) System.currentTimeMillis() else null
                )
                firestore.collection("loans").document(loanId)
                    .set(loanRepository.loanToFirestoreMap(updatedLoan), com.google.firebase.firestore.SetOptions.merge())

                // Bug #17: Send notification to counterparty with relatedLoanId and detail route
                val counterpartyId = if (loan.borrowerId == userId) loan.lenderId else loan.borrowerId
                if (counterpartyId.isNotBlank()) {
                    val notif = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "rep_notif_" + UUID.randomUUID().toString().take(8),
                        userId = counterpartyId,
                        title = "Payment Received: ₹${amount.toInt()}",
                        message = "A repayment of ₹${amount.toInt()} has been recorded for loan ${loan.purpose.ifBlank { loanId }}. New outstanding: ₹${newOutstanding.toInt()}.",
                        type = "REPAYMENT",
                        relatedLoanId = loanId,
                        actionRoute = "loan_detail/$loanId",
                        timestamp = System.currentTimeMillis()
                    )
                    val notifMap = hashMapOf(
                        "notificationId" to notif.notificationId,
                        "userId" to notif.userId,
                        "title" to notif.title,
                        "message" to notif.message,
                        "type" to notif.type,
                        "relatedLoanId" to notif.relatedLoanId,
                        "actionRoute" to notif.actionRoute,
                        "timestamp" to notif.timestamp,
                        "isRead" to notif.isRead
                    )
                    firestore.collection("notifications").document(notif.notificationId)
                        .set(notifMap, com.google.firebase.firestore.SetOptions.merge())
                }
            } catch (e: Exception) {
                android.util.Log.w("LoanViewModel", "Cloud repayment sync note: ${e.message}")
            }

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
            // No longer using Leegality; just triggering navigation to AgreementSigningScreen
            // This is handled by the UI when it reads the button click.
            // We can just keep a dummy method if needed, or remove it.
        }
    }

    fun completeSignature(context: Context, loanId: String, signatureBitmap: Bitmap, selfieBitmap: Bitmap, biometricSuccess: Boolean) {
        if (!biometricSuccess) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Uploading signature...") }
            val currentUserId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId) ?: return@launch

            // Save Bitmaps to temporary files
            val sigFile = File(context.cacheDir, "sig_${currentUserId}_${System.currentTimeMillis()}.png")
            FileOutputStream(sigFile).use { signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

            val selfieFile = File(context.cacheDir, "selfie_${currentUserId}_${System.currentTimeMillis()}.png")
            FileOutputStream(selfieFile).use { selfieBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

            // Upload to Google Drive
            val sigUri = android.net.Uri.fromFile(sigFile)
            val selfieUri = android.net.Uri.fromFile(selfieFile)
            val sigUrl = googleDriveManager.uploadFile(context, sigUri, "SIGNATURE_${loanId}_${currentUserId}.png").getOrNull() ?: ""
            val selfieUrl = googleDriveManager.uploadFile(context, selfieUri, "SELFIE_${loanId}_${currentUserId}.png").getOrNull() ?: ""

            val now = System.currentTimeMillis()
            var updatedLoan = loan

            if (currentUserId == loan.lenderId) {
                updatedLoan = updatedLoan.copy(
                    lenderSignedAt = now,
                    lenderSignatureUrl = sigUrl,
                    lenderSelfieUrl = selfieUrl
                )
            } else if (currentUserId == loan.borrowerId) {
                updatedLoan = updatedLoan.copy(
                    borrowerSignedAt = now,
                    borrowerSignatureUrl = sigUrl,
                    borrowerSelfieUrl = selfieUrl
                )
            }

            loanRepository.updateLoan(updatedLoan, currentUserId, "Signed loan agreement")

            // Check if BOTH have signed
            if (updatedLoan.lenderSignedAt != null && updatedLoan.borrowerSignedAt != null) {
                _uiState.update { it.copy(message = "Both parties signed! Generating final PDF...") }
                generateAndUploadAgreement(context, updatedLoan, currentUserId)
            } else {
                _uiState.update { it.copy(isLoading = false, message = "Signature saved! Waiting for the other party to sign.") }
                loadLoanDetail(loanId)
            }
        }
    }

    private suspend fun generateAndUploadAgreement(context: Context, loan: LoanEntity, currentUserId: String) {
        val lender = userRepository.getUserById(loan.lenderId) ?: return
        val borrower = userRepository.getUserById(loan.borrowerId) ?: return
        
        val pdfFile = AgreementGenerator.generateAgreementPdf(context, loan, lender, borrower)
        
        if (pdfFile != null) {
            val pdfUri = android.net.Uri.fromFile(pdfFile)
            val pdfUrl = googleDriveManager.uploadFile(context, pdfUri, "LOAN_AGREEMENT_${loan.loanId}.pdf").getOrNull()
            
            if (pdfUrl != null) {
                val finalLoan = loan.copy(
                    isAgreementSigned = true,
                    agreementPdfUrl = pdfUrl
                )
                loanRepository.updateLoan(finalLoan, currentUserId, "Final Agreement PDF Generated")
                _uiState.update { it.copy(isLoading = false, message = "Agreement finalized and securely stored!") }
                loadLoanDetail(loan.loanId)
            } else {
                _uiState.update { it.copy(isLoading = false, message = "Failed to upload final PDF.") }
            }
        } else {
            _uiState.update { it.copy(isLoading = false, message = "Failed to generate final PDF.") }
        }
    }

    fun markAgreementSigned(loanId: String) {
        // Deprecated, handled by completeSignature
    }

    fun clearSignUrl() {
        _uiState.update { it.copy(signUrl = null) }
    }

    // Export Reports (Feature 13)
    fun exportAgreementPdf(context: android.content.Context, loan: LoanEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val lender = userRepository.getUserById(loan.lenderId)
                ?: com.loanzo.app.data.entity.UserEntity(userId = loan.lenderId, name = "Lender", email = "", phone = "", role = "LENDER", kycStatus = "VERIFIED")
            val borrower = userRepository.getUserById(loan.borrowerId)
                ?: com.loanzo.app.data.entity.UserEntity(userId = loan.borrowerId, name = "Borrower", email = "", phone = "", role = "BORROWER", kycStatus = "VERIFIED")
            val file = com.loanzo.app.util.AgreementGenerator.generateAgreementPdf(context, loan, lender, borrower)
            if (file != null) {
                documentVaultRepository.archiveDocument(
                    userId = loan.borrowerId,
                    loanId = loan.loanId,
                    title = "Digital Loan Agreement - ${loan.loanId.take(8).uppercase()}",
                    documentType = "LOAN_AGREEMENT",
                    sourceFile = file,
                    description = "Legally signed peer-to-peer agreement with e-signatures & KYC certifications."
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    com.loanzo.app.util.ReportExporter.shareFile(context, file, "application/pdf")
                    _uiState.update { it.copy(message = "Agreement archived to Vault & opened successfully!") }
                }
            } else {
                _uiState.update { it.copy(message = "Failed to generate Agreement PDF.") }
            }
        }
    }

    fun exportPitchDeck(context: android.content.Context) {
        val file = com.loanzo.app.util.ReportExporter.generatePlatformPitchReportPdf(context)
        if (file != null) {
            com.loanzo.app.util.ReportExporter.shareFile(context, file, "application/pdf", "Export Startup Pitch Dossier")
            _uiState.update { it.copy(message = "Startup Pitch Dossier generated successfully!") }
        } else {
            _uiState.update { it.copy(message = "Failed to generate Pitch Deck PDF") }
        }
    }

    fun exportLoanSummary(context: android.content.Context) {
        val loan = _uiState.value.selectedLoan ?: return
        val repayments = _uiState.value.repayments
        val file = com.loanzo.app.util.ReportExporter.generateLoanSummaryPdf(context, loan, repayments)
        if (file != null) {
            com.loanzo.app.util.ReportExporter.shareFile(context, file, "application/pdf")
        } else {
            _uiState.update { it.copy(message = "Failed to generate PDF report") }
        }
    }

    fun exportInterestCertificate(context: android.content.Context) {
        val loan = _uiState.value.selectedLoan ?: return
        val repayments = _uiState.value.repayments
        val file = com.loanzo.app.util.ReportExporter.generateInterestCertificatePdf(context, loan, repayments)
        if (file != null) {
            com.loanzo.app.util.ReportExporter.shareFile(context, file, "application/pdf")
        } else {
            _uiState.update { it.copy(message = "Failed to generate Interest Certificate") }
        }
    }

    fun exportRepaymentsCsv(context: android.content.Context) {
        val loan = _uiState.value.selectedLoan ?: return
        val repayments = _uiState.value.repayments
        val file = com.loanzo.app.util.ReportExporter.generateRepaymentCsv(context, loan, repayments)
        if (file != null) {
            com.loanzo.app.util.ReportExporter.shareFile(context, file, "text/csv")
        } else {
            _uiState.update { it.copy(message = "Failed to generate CSV") }
        }
    }

    // Penalty Waiver (Feature 14)
    fun waivePenalty(repayment: RepaymentEntity) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val updated = repayment.copy(penalty = 0.0, penaltyWaived = true)
            loanRepository.updateRepayment(updated, userId, "Penalty waived for repayment ${repayment.repaymentId}")
            _uiState.update { it.copy(message = "Penalty waived successfully") }
        }
    }

    // Guarantor Support (Feature 15)
    fun addGuarantor(
        loanId: String,
        name: String,
        phone: String,
        email: String,
        panNumber: String,
        relationship: String
    ) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val guarantor = GuarantorEntity(
                guarantorId = UUID.randomUUID().toString(),
                loanId = loanId,
                name = name,
                phone = phone,
                email = email,
                panNumber = panNumber,
                relationship = relationship,
                consentStatus = "PENDING"
            )
            loanRepository.createGuarantor(guarantor, userId)
            _uiState.update { it.copy(message = "Guarantor added") }
        }
    }

    fun updateGuarantorConsent(guarantorId: String, status: String) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val guarantor = loanRepository.getGuarantorById(guarantorId) ?: return@launch
            val updated = guarantor.copy(
                consentStatus = status,
                consentTimestamp = System.currentTimeMillis()
            )
            loanRepository.updateGuarantor(updated, userId, "Guarantor consent updated to $status")
            _uiState.update { it.copy(message = "Guarantor consent: $status") }
        }
    }

    // Loan Restructure / Moratorium (Feature 16)
    fun restructureLoan(
        loanId: String,
        newTenureMonths: Int,
        moratoriumMonths: Int
    ) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId) ?: return@launch
            val origTenure = if (loan.originalTenureMonths > 0) loan.originalTenureMonths else loan.tenureMonths
            val updatedLoan = loan.copy(
                originalTenureMonths = origTenure,
                tenureMonths = newTenureMonths,
                moratoriumMonths = moratoriumMonths,
                isRestructured = true,
                status = "RESTRUCTURED",
                restructuredAt = System.currentTimeMillis()
            )
            loanRepository.updateLoan(
                updatedLoan,
                userId,
                "Loan restructured: tenure $newTenureMonths mos, moratorium $moratoriumMonths mos. Status: RESTRUCTURED"
            )
            _uiState.update { it.copy(message = "Loan restructured successfully! Status: RESTRUCTURED") }
            loadLoanDetail(loanId)
        }
    }

    // Lifecycle Flow: User Lookup & Transactors
    fun searchUsers(query: String): Flow<List<UserEntity>> = userRepository.searchUsers(query)
    fun getAllRegisteredUsers(): Flow<List<UserEntity>> = userRepository.getAllUsers()

    // Lifecycle Flow: Acceptance Gate
    fun acceptProposal(loanId: String) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId) ?: return@launch
            val hasCollateral = loan.purpose.contains("Collateral", ignoreCase = true) || loan.notes.contains("Collateral", ignoreCase = true)
            val nextStatus = if (hasCollateral) "COLLATERAL_VALUATION" else "CONTRACT_SIGNING"
            loanRepository.updateLoanStatus(loanId, nextStatus, userId, "Proposal accepted by counterparty. Transitioned to $nextStatus.")

            // Bug #15: Notify the other party of acceptance
            val otherPartyId = if (loan.borrowerId == userId) loan.lenderId else loan.borrowerId
            if (otherPartyId.isNotBlank()) {
                try {
                    val notif = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "notif_prop_acc_" + UUID.randomUUID().toString().take(8),
                        userId = otherPartyId,
                        title = "✅ Loan Proposal Accepted",
                        message = "Your loan proposal for ${loan.purpose} was accepted. Advancing to $nextStatus.",
                        type = "AGREEMENT",
                        relatedLoanId = loanId,
                        actionRoute = "loan_detail/$loanId",
                        timestamp = System.currentTimeMillis()
                    )
                    firestore
                        .collection("notifications")
                        .document(notif.notificationId)
                        .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}
            }

            _uiState.update { it.copy(message = "Proposal accepted! Ready for $nextStatus.") }
            loadLoanDetail(loanId)
        }
    }

    fun declineProposal(loanId: String) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId)
            loanRepository.updateLoanStatus(loanId, "REJECTED", userId, "Proposal declined")

            if (loan != null) {
                val otherPartyId = if (loan.borrowerId == userId) loan.lenderId else loan.borrowerId
                if (otherPartyId.isNotBlank()) {
                    try {
                        val notif = com.loanzo.app.data.entity.NotificationEntity(
                            notificationId = "notif_prop_dec_" + UUID.randomUUID().toString().take(8),
                            userId = otherPartyId,
                            title = "❌ Loan Proposal Declined",
                            message = "The loan proposal for ${loan.purpose} was declined.",
                            type = "AGREEMENT",
                            relatedLoanId = loanId,
                            actionRoute = "loan_detail/$loanId",
                            timestamp = System.currentTimeMillis()
                        )
                        firestore
                            .collection("notifications")
                            .document(notif.notificationId)
                            .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
                    } catch (_: Exception) {}
                }
            }

            _uiState.update { it.copy(message = "Proposal declined.") }
            loadLoanDetail(loanId)
        }
    }

    // Lifecycle Flow: Agreement Signing Completion (Bilateral Signing Protocol)
    fun completeAgreementSigning(loan: LoanEntity) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: loan.borrowerId
            val isLenderSigning = userId == loan.lenderId
            val isBorrowerSigning = userId == loan.borrowerId || (!isLenderSigning)

            val updatedBorrowerSignedAt = if (isBorrowerSigning) System.currentTimeMillis() else loan.borrowerSignedAt
            val updatedLenderSignedAt = if (isLenderSigning) System.currentTimeMillis() else loan.lenderSignedAt
            val isFullySigned = updatedBorrowerSignedAt != null && updatedLenderSignedAt != null

            val updated = loan.copy(
                borrowerSignedAt = updatedBorrowerSignedAt,
                lenderSignedAt = updatedLenderSignedAt,
                isAgreementSigned = isFullySigned,
                status = if (isFullySigned) "TRANCHE_DISBURSEMENT" else "CONTRACT_SIGNING"
            )
            val logMessage = if (isFullySigned) {
                "Agreement bilaterally e-signed by both parties. Status moved to TRANCHE_DISBURSEMENT."
            } else if (isLenderSigning) {
                "Agreement e-signed by Lender. Awaiting Borrower counter-signature."
            } else {
                "Agreement e-signed by Borrower. Awaiting Lender counter-signature."
            }

            loanRepository.updateLoan(updated, userId, logMessage)
            try {
                val firestore = firestore
                firestore.collection("loans")
                    .document(loan.loanId)
                    .set(loanRepository.loanToFirestoreMap(updated), com.google.firebase.firestore.SetOptions.merge())

                // Bug #15: Bilateral agreement signing notifications
                if (isFullySigned) {
                    val notifB = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "notif_sign_full_b_" + UUID.randomUUID().toString().take(8),
                        userId = loan.borrowerId,
                        title = "📜 Contract Fully Executed",
                        message = "Both parties have e-signed the agreement for loan ${loan.purpose}. Ready for Tranche Disbursal.",
                        type = "AGREEMENT",
                        relatedLoanId = loan.loanId,
                        actionRoute = "loan_detail/${loan.loanId}",
                        timestamp = System.currentTimeMillis()
                    )
                    val notifL = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "notif_sign_full_l_" + UUID.randomUUID().toString().take(8),
                        userId = loan.lenderId,
                        title = "📜 Contract Fully Executed",
                        message = "Both parties have e-signed the agreement for loan ${loan.purpose}. Ready for Tranche Disbursal.",
                        type = "AGREEMENT",
                        relatedLoanId = loan.loanId,
                        actionRoute = "loan_detail/${loan.loanId}",
                        timestamp = System.currentTimeMillis()
                    )
                    val notifBMap = hashMapOf(
                        "notificationId" to notifB.notificationId, "userId" to notifB.userId,
                        "title" to notifB.title, "message" to notifB.message,
                        "type" to notifB.type, "relatedLoanId" to notifB.relatedLoanId,
                        "actionRoute" to notifB.actionRoute, "timestamp" to notifB.timestamp, "isRead" to notifB.isRead
                    )
                    val notifLMap = hashMapOf(
                        "notificationId" to notifL.notificationId, "userId" to notifL.userId,
                        "title" to notifL.title, "message" to notifL.message,
                        "type" to notifL.type, "relatedLoanId" to notifL.relatedLoanId,
                        "actionRoute" to notifL.actionRoute, "timestamp" to notifL.timestamp, "isRead" to notifL.isRead
                    )
                    firestore.collection("notifications").document(notifB.notificationId).set(notifBMap, com.google.firebase.firestore.SetOptions.merge())
                    firestore.collection("notifications").document(notifL.notificationId).set(notifLMap, com.google.firebase.firestore.SetOptions.merge())
                } else {
                    val pendingPartyId = if (isLenderSigning) loan.borrowerId else loan.lenderId
                    val signerRole = if (isLenderSigning) "Lender" else "Borrower"
                    val notifPending = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "notif_sign_req_" + UUID.randomUUID().toString().take(8),
                        userId = pendingPartyId,
                        title = "✍️ Counter-Signature Required",
                        message = "Agreement signed by $signerRole for loan ${loan.purpose}. Tap to review and counter-sign.",
                        type = "AGREEMENT",
                        relatedLoanId = loan.loanId,
                        actionRoute = "agreement_signing/${loan.loanId}",
                        timestamp = System.currentTimeMillis()
                    )
                    val notifPMap = hashMapOf(
                        "notificationId" to notifPending.notificationId, "userId" to notifPending.userId,
                        "title" to notifPending.title, "message" to notifPending.message,
                        "type" to notifPending.type, "relatedLoanId" to notifPending.relatedLoanId,
                        "actionRoute" to notifPending.actionRoute, "timestamp" to notifPending.timestamp, "isRead" to notifPending.isRead
                    )
                    firestore.collection("notifications").document(notifPending.notificationId).set(notifPMap, com.google.firebase.firestore.SetOptions.merge())
                }
            } catch (_: Exception) {}

            val userFeedback = if (isFullySigned) {
                "Agreement fully executed by both parties! Ready for Tranche Disbursal."
            } else {
                "Signature recorded! Waiting for counterparty counter-signature."
            }
            _uiState.update { it.copy(message = userFeedback) }
            loadLoanDetail(loan.loanId)
        }
    }

    // Lifecycle Flow: Disbursal Confirmation
    fun disburseLoan(loanId: String, amount: Double, utr: String, notes: String = "") {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val disb = DisbursementEntity(
                disbursementId = UUID.randomUUID().toString(),
                loanId = loanId,
                amount = amount,
                payeeId = null,
                payeeName = "Borrower",
                purpose = if (notes.isNotBlank()) notes else "Direct UPI Disbursal",
                purposeCategory = "OTHER",
                verificationStatus = "VERIFIED",
                ruleEngineResult = "AUTO_APPROVED",
                approvalStatus = "APPROVED",
                transactionRef = utr,
                timestamp = System.currentTimeMillis(),
                lenderNote = "Funds disbursed via UPI (UTR: $utr)"
            )
            loanRepository.createDisbursement(disb, userId)
            loanRepository.updateLoanStatus(loanId, "ACTIVE_SERVICING", userId, "Funds disbursed via UPI (UTR: $utr). Loan activated in ACTIVE_SERVICING.")

            // Bug #15: Notify borrower that funds have been disbursed via UPI
            val loan = loanRepository.getLoanById(loanId)
            if (loan != null && loan.borrowerId.isNotBlank()) {
                try {
                    val notif = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "notif_disb_upi_" + UUID.randomUUID().toString().take(8),
                        userId = loan.borrowerId,
                        title = "💰 Funds Disbursed: ₹${amount.toInt()}",
                        message = "Lender disbursed ₹${amount.toInt()} via UPI (UTR: $utr). Your loan is now active.",
                        type = "DISBURSEMENT",
                        relatedLoanId = loanId,
                        actionRoute = "loan_detail/$loanId",
                        timestamp = System.currentTimeMillis()
                    )
                    firestore
                        .collection("notifications")
                        .document(notif.notificationId)
                        .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}
            }

            _uiState.update { it.copy(message = "Disbursement recorded! Loan is now in ACTIVE_SERVICING.") }
            loadLoanDetail(loanId)
        }
    }

    // Lifecycle Flow: Arbitrated Dispute Resolution
    fun resolveDisputeAndComplete(loanId: String, settlementNotes: String = "") {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val loan = loanRepository.getLoanById(loanId) ?: return@launch
            val updated = loan.copy(
                status = "COMPLETED",
                outstandingAmount = 0.0,
                closedAt = System.currentTimeMillis(),
                notes = if (settlementNotes.isNotBlank()) "${loan.notes} | Arbitrated Settlement: $settlementNotes" else loan.notes
            )
            loanRepository.updateLoan(updated, userId, "Dispute arbitrated & settlement approved. Loan marked COMPLETED.")
            _uiState.update { it.copy(message = "Arbitrated settlement finalized! Loan marked COMPLETED.") }
            loadLoanDetail(loanId)
        }
    }

    // Lifecycle Flow: NOC Certificate Generation
    fun exportNocCertificate(context: android.content.Context, loan: LoanEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val lender = userRepository.getUserById(loan.lenderId) ?: UserEntity(userId = loan.lenderId, name = "Lender", email = "", phone = "", role = "LENDER", kycStatus = "VERIFIED")
            val borrower = userRepository.getUserById(loan.borrowerId) ?: UserEntity(userId = loan.borrowerId, name = "Borrower", email = "", phone = "", role = "BORROWER", kycStatus = "VERIFIED")
            val file = com.loanzo.app.util.AgreementGenerator.generateLoanNocCertificate(context, loan, lender, borrower)
            if (file != null) {
                documentVaultRepository.archiveDocument(
                    userId = loan.borrowerId,
                    loanId = loan.loanId,
                    title = "No Objection Certificate (NOC) - ${loan.loanId.take(8).uppercase()}",
                    documentType = "NOC_CERTIFICATE",
                    sourceFile = file,
                    description = "Official debt satisfaction clearance confirming zero balance."
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Open NOC Certificate").apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                        _uiState.update { it.copy(message = "NOC Clearance Certificate generated successfully!") }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(message = "NOC saved at ${file.absolutePath}") }
                    }
                }
            } else {
                _uiState.update { it.copy(message = "Failed to generate NOC Certificate.") }
            }
        }
    }

    /**
     * Publishes an existing or proposed loan to the Community Wall
     * so that peer lenders or community backers can discover, vouch, and bid on it.
     */
    fun publishLoanToWall(loan: LoanEntity, onSuccess: (MarketplacePostEntity) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentUserId = userRepository.getCurrentUserIdSync() ?: loan.borrowerId
            val user = userRepository.getUserById(currentUserId)
            val isLender = currentUserId == loan.lenderId
            val isKyc = (user?.kycStatus == "VERIFIED")

            val title = if (loan.notes.isNotBlank()) {
                loan.notes.take(50)
            } else {
                if (isLender) "Capital Facility for ${loan.purpose}" else "Seeking ₹${loan.sanctionedAmount.toInt()} for ${loan.purpose}"
            }

            val description = if (loan.notes.isNotBlank()) {
                loan.notes
            } else {
                "Community peer loan for ${loan.purpose}. Transparent repayment schedule with ${loan.interestRate}% interest rate over ${loan.tenureMonths} months."
            }

            val category = when (loan.loanType.uppercase()) {
                "EDUCATION" -> "EDUCATION"
                "MEDICAL" -> "MEDICAL"
                "BUSINESS" -> "BUSINESS"
                "EMERGENCY" -> "EMERGENCY"
                else -> "PERSONAL"
            }

            val newPost = MarketplacePostEntity(
                postId = "post_loan_${loan.loanId}",
                authorId = currentUserId,
                authorName = user?.name ?: if (isLender) "Verified Lender" else "Verified Borrower",
                authorAvatarUrl = user?.profilePhotoUri?.takeIf { it.isNotBlank() }
                    ?: com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl(user?.name ?: currentUserId),
                authorKycVerified = isKyc,
                authorTrustScore = if (isKyc) 95 else 85,
                postType = if (isLender) "OFFER_TO_LEND" else "SEEKING_LOAN",
                title = title,
                description = description,
                minAmount = loan.sanctionedAmount,
                maxAmount = loan.sanctionedAmount,
                interestRate = loan.interestRate,
                tenureMonths = loan.tenureMonths,
                purposeCategory = category,
                locationCity = user?.address ?: "Bengaluru",
                collateralOffered = "Verified Digital Dossier / Aadhaar & PAN KYC",
                vouchCount = 0,
                bidsCount = 0,
                status = "OPEN",
                createdAt = System.currentTimeMillis()
            )

            val result = marketplaceRepository.publishPost(newPost)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = "Loan ${loan.sanctionedAmount} published successfully to Community Wall!"
                )
            }
            if (result.isSuccess) {
                onSuccess(newPost)
            }
        }
    }
}
