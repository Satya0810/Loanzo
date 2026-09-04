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
    val signUrl: String? = null
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val userRepository: UserRepository,
    private val payeeDao: PayeeDao,
    private val ruleEngine: RuleEngine,
    private val leegalityService: LeegalityService,
    private val googleDriveManager: GoogleDriveManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanUiState())
    val uiState: StateFlow<LoanUiState> = _uiState.asStateFlow()

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = userRepository.getCurrentUserIdSync()
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, loans = emptyList()) }
                return@launch
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
            
            if (borrower != null && borrower.fcmToken.isNotBlank()) {
                val fcmSender = com.loanzo.app.fcm.FcmSender()
                val success = fcmSender.sendPaymentReminder(context, borrower.fcmToken, nextDue.amount.toString())
                if (success) {
                    _uiState.update { it.copy(message = "Reminder sent successfully!") }
                } else {
                    _uiState.update { it.copy(message = "Failed to send reminder. Check service account.") }
                }
            } else {
                _uiState.update { it.copy(message = "Borrower has no FCM token registered.") }
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
            val currentUserId = userRepository.getCurrentUserIdSync() ?: return@launch
            val cleanInput = counterpartyId.trim()
            val resolvedUser = userRepository.getUserById(cleanInput)
                ?: userRepository.getUserByUsername(cleanInput.removePrefix("@"))
                ?: (if (cleanInput.contains("@")) userRepository.getUserByEmail(cleanInput) else null)
                ?: userRepository.getUserByPhone(cleanInput)
                ?: userRepository.getUserByPhone("+91$cleanInput")
                ?: userRepository.getUserByPhone(cleanInput.removePrefix("+91"))
            val finalCounterpartyId = resolvedUser?.userId ?: cleanInput

            val actualLenderId = if (isGrantMode) currentUserId else finalCounterpartyId
            val actualBorrowerId = if (isGrantMode) finalCounterpartyId else currentUserId

            val loan = LoanEntity(
                loanId = UUID.randomUUID().toString(),
                lenderId = actualLenderId,
                borrowerId = actualBorrowerId,
                sanctionedAmount = amount,
                outstandingAmount = amount,
                purpose = purpose,
                loanType = loanType,
                interestRate = interestRate,
                interestModel = interestModel,
                tenureMonths = tenureMonths,
                status = "ACTIVE",
                repaymentFrequency = repaymentFrequency,
                notes = notes,
                penaltyRate = penaltyRate,
                penaltyModel = penaltyModel,
                penaltyGraceDays = penaltyGraceDays
            )
            loanRepository.createLoan(loan, currentUserId)
            val successMsg = if (isGrantMode) "Loan granted successfully" else "Loan requested successfully"
            _uiState.update { it.copy(loanCreated = true, message = successMsg) }
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
                restructuredAt = System.currentTimeMillis()
            )
            loanRepository.updateLoan(
                updatedLoan,
                userId,
                "Loan restructured: tenure $newTenureMonths mos, moratorium $moratoriumMonths mos"
            )
            _uiState.update { it.copy(message = "Loan restructured successfully") }
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
            loanRepository.updateLoanStatus(loanId, "DRAFT_PENDING_SIGNATURE", userId, "Proposal accepted by counterparty")
            _uiState.update { it.copy(message = "Proposal accepted! You can now review and sign the agreement.") }
            loadLoanDetail(loanId)
        }
    }

    fun declineProposal(loanId: String) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            loanRepository.updateLoanStatus(loanId, "REJECTED", userId, "Proposal declined")
            _uiState.update { it.copy(message = "Proposal declined.") }
            loadLoanDetail(loanId)
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
            loanRepository.updateLoanStatus(loanId, "ACTIVE", userId, "Funds disbursed via UPI (UTR: $utr). Loan activated.")
            _uiState.update { it.copy(message = "Disbursement recorded! Loan is now ACTIVE.") }
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
}
