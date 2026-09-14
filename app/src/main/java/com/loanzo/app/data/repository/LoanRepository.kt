package com.loanzo.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.loanzo.app.data.dao.*
import com.loanzo.app.data.entity.*
import com.loanzo.app.domain.PenaltyEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val disbursementDao: DisbursementDao,
    private val repaymentDao: RepaymentDao,
    private val pledgeDao: PledgeDao,
    private val guarantorDao: GuarantorDao,
    private val auditEventDao: AuditEventDao,
    private val penaltyEngine: PenaltyEngine,
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager
) {
    private val firestore: FirebaseFirestore
        get() = com.loanzo.app.data.firebase.FirestoreProvider.get()

    // Loan operations
    fun loanToFirestoreMap(loan: LoanEntity): Map<String, Any?> {
        return hashMapOf(
            "loanId" to loan.loanId,
            "lenderId" to loan.lenderId,
            "borrowerId" to loan.borrowerId,
            "sanctionedAmount" to loan.sanctionedAmount,
            "disbursedAmount" to loan.disbursedAmount,
            "outstandingAmount" to loan.outstandingAmount,
            "purpose" to loan.purpose,
            "loanType" to loan.loanType,
            "interestRate" to loan.interestRate,
            "interestModel" to loan.interestModel,
            "tenureMonths" to loan.tenureMonths,
            "status" to loan.status,
            "repaymentFrequency" to loan.repaymentFrequency,
            "createdAt" to loan.createdAt,
            "closedAt" to loan.closedAt,
            "notes" to loan.notes,
            "lenderSignedAt" to loan.lenderSignedAt,
            "borrowerSignedAt" to loan.borrowerSignedAt,
            "lenderSignatureUrl" to loan.lenderSignatureUrl,
            "borrowerSignatureUrl" to loan.borrowerSignatureUrl,
            "lenderSelfieUrl" to loan.lenderSelfieUrl,
            "borrowerSelfieUrl" to loan.borrowerSelfieUrl,
            "agreementPdfUrl" to loan.agreementPdfUrl,
            "isAgreementSigned" to loan.isAgreementSigned,
            "penaltyRate" to loan.penaltyRate,
            "penaltyModel" to loan.penaltyModel,
            "penaltyGraceDays" to loan.penaltyGraceDays,
            "penaltyCapPercent" to loan.penaltyCapPercent,
            "originalTenureMonths" to loan.originalTenureMonths,
            "moratoriumMonths" to loan.moratoriumMonths,
            "isRestructured" to loan.isRestructured,
            "restructuredAt" to loan.restructuredAt
        )
    }

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
        // Cloud broadcast with explicit Map payload
        try {
            withContext(Dispatchers.IO) {
                firebaseManager.ensureFirebaseAuthSession()
                withTimeoutOrNull(8000L) {
                    firestore.collection("loans")
                        .document(loan.loanId)
                        .set(loanToFirestoreMap(loan), SetOptions.merge())
                        .await()
                }
            }
        } catch (e: Throwable) {
            Log.e("LoanRepository", "Cloud sync for createLoan failed: ${e.message}", e)
        }
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
        // Cloud update with explicit Map payload
        try {
            withContext(Dispatchers.IO) {
                firebaseManager.ensureFirebaseAuthSession()
                withTimeoutOrNull(8000L) {
                    firestore.collection("loans")
                        .document(loan.loanId)
                        .set(loanToFirestoreMap(loan), SetOptions.merge())
                        .await()
                }
            }
        } catch (e: Throwable) {
            Log.e("LoanRepository", "Cloud sync for updateLoan failed: ${e.message}", e)
        }
    }

    suspend fun getLoanById(loanId: String): LoanEntity? = loanDao.getLoanById(loanId)
    fun observeLoan(loanId: String): Flow<LoanEntity?> = loanDao.observeLoan(loanId)

    suspend fun updateLoanStatus(loanId: String, status: String, actorId: String, description: String = "") {
        val loan = loanDao.getLoanById(loanId) ?: return
        val updated = loan.copy(status = status)
        updateLoan(updated, actorId, description.ifBlank { "Loan status changed to $status" })
    }
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
        repaymentDao.insertRepayment(repayment)
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

    suspend fun updateRepayment(repayment: RepaymentEntity, actorId: String, description: String = "Repayment updated") {
        repaymentDao.updateRepayment(repayment)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "REPAYMENT",
                entityId = repayment.repaymentId,
                actor = actorId,
                event = "UPDATED",
                newState = repayment.status,
                description = description
            )
        )
    }

    // Guarantor operations
    suspend fun createGuarantor(guarantor: GuarantorEntity, actorId: String) {
        guarantorDao.insertGuarantor(guarantor)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "GUARANTOR",
                entityId = guarantor.guarantorId,
                actor = actorId,
                event = "CREATED",
                newState = guarantor.consentStatus,
                description = "Guarantor added: ${guarantor.name} (${guarantor.relationship})"
            )
        )
    }

    suspend fun updateGuarantor(guarantor: GuarantorEntity, actorId: String, eventDescription: String) {
        guarantorDao.updateGuarantor(guarantor)
        auditEventDao.insertEvent(
            AuditEventEntity(
                eventId = UUID.randomUUID().toString(),
                entityType = "GUARANTOR",
                entityId = guarantor.guarantorId,
                actor = actorId,
                event = "UPDATED",
                newState = guarantor.consentStatus,
                description = eventDescription
            )
        )
    }

    fun getGuarantorsByLoan(loanId: String): Flow<List<GuarantorEntity>> =
        guarantorDao.getGuarantorsByLoan(loanId)

    suspend fun getGuarantorById(id: String): GuarantorEntity? =
        guarantorDao.getGuarantorById(id)

    // Audit
    fun getAuditTrail(entityType: String, entityId: String): Flow<List<AuditEventEntity>> =
        auditEventDao.getEventsForEntity(entityType, entityId)
    fun getRecentEvents(limit: Int = 100): Flow<List<AuditEventEntity>> =
        auditEventDao.getRecentEvents(limit)

    // --- Real-time Bidirectional Loan Sync (Borrower ↔ Lender) ---

    private var borrowerLoansListener: ListenerRegistration? = null
    private var lenderLoansListener: ListenerRegistration? = null

    /**
     * Safely parses a Firestore document into a LoanEntity,
     * converting Longs to Doubles and handling missing/null fields.
     */
    fun parseDocumentToLoan(doc: DocumentSnapshot): LoanEntity? {
        return try {
            val d = doc.data ?: return null
            LoanEntity(
                loanId = doc.getString("loanId") ?: doc.id,
                lenderId = doc.getString("lenderId") ?: "",
                borrowerId = doc.getString("borrowerId") ?: "",
                sanctionedAmount = doc.getDouble("sanctionedAmount") ?: doc.getLong("sanctionedAmount")?.toDouble() ?: 0.0,
                disbursedAmount = doc.getDouble("disbursedAmount") ?: doc.getLong("disbursedAmount")?.toDouble() ?: 0.0,
                outstandingAmount = doc.getDouble("outstandingAmount") ?: doc.getLong("outstandingAmount")?.toDouble() ?: 0.0,
                purpose = doc.getString("purpose") ?: "Personal Loan",
                loanType = doc.getString("loanType") ?: "PERSONAL",
                interestRate = doc.getDouble("interestRate") ?: doc.getLong("interestRate")?.toDouble() ?: 12.0,
                interestModel = doc.getString("interestModel") ?: "SIMPLE",
                tenureMonths = (doc.getLong("tenureMonths") ?: 12L).toInt(),
                status = doc.getString("status") ?: "CONTRACT_SIGNING",
                repaymentFrequency = doc.getString("repaymentFrequency") ?: "MONTHLY",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                closedAt = doc.getLong("closedAt"),
                notes = doc.getString("notes") ?: "",
                lenderSignedAt = doc.getLong("lenderSignedAt"),
                borrowerSignedAt = doc.getLong("borrowerSignedAt"),
                lenderSignatureUrl = doc.getString("lenderSignatureUrl") ?: "",
                borrowerSignatureUrl = doc.getString("borrowerSignatureUrl") ?: "",
                lenderSelfieUrl = doc.getString("lenderSelfieUrl") ?: "",
                borrowerSelfieUrl = doc.getString("borrowerSelfieUrl") ?: "",
                agreementPdfUrl = doc.getString("agreementPdfUrl") ?: "",
                isAgreementSigned = doc.getBoolean("isAgreementSigned") ?: false,
                penaltyRate = doc.getDouble("penaltyRate") ?: doc.getLong("penaltyRate")?.toDouble() ?: 2.0,
                penaltyModel = doc.getString("penaltyModel") ?: "PERCENTAGE",
                penaltyGraceDays = (doc.getLong("penaltyGraceDays") ?: 3L).toInt()
            )
        } catch (e: Exception) {
            Log.w("LoanRepository", "Failed to parse loan document ${doc.id}: ${e.message}")
            null
        }
    }

    /**
     * Registers real-time Firestore listeners for all loans where the user is either
     * the borrower or the lender. Synced records are immediately written into local Room SQLite.
     */
    fun startRealtimeUserLoansSync(userId: String, scope: CoroutineScope) {
        if (userId.isBlank()) return

        // Stop any previous listeners
        stopRealtimeUserLoansSync()

        try {
            scope.launch(Dispatchers.IO) {
                try {
                    firebaseManager.ensureFirebaseAuthSession()
                } catch (_: Exception) {}
            }

            // 1. Listen to loans where user is Borrower
            borrowerLoansListener = firestore.collection("loans")
                .whereEqualTo("borrowerId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("LoanRepository", "Borrower loans listener notice: ${error?.message}")
                        return@addSnapshotListener
                    }
                    scope.launch(Dispatchers.IO) {
                        val loans = snapshot.documents.mapNotNull { parseDocumentToLoan(it) }
                        if (loans.isNotEmpty()) {
                            loanDao.insertLoans(loans)
                            Log.d("LoanRepository", "Synced ${loans.size} borrower loans into Room")
                        }
                    }
                }

            // 2. Listen to loans where user is Lender
            lenderLoansListener = firestore.collection("loans")
                .whereEqualTo("lenderId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("LoanRepository", "Lender loans listener notice: ${error?.message}")
                        return@addSnapshotListener
                    }
                    scope.launch(Dispatchers.IO) {
                        val loans = snapshot.documents.mapNotNull { parseDocumentToLoan(it) }
                        if (loans.isNotEmpty()) {
                            loanDao.insertLoans(loans)
                            Log.d("LoanRepository", "Synced ${loans.size} lender loans into Room")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("LoanRepository", "Failed to register real-time loans sync: ${e.message}")
        }
    }

    fun stopRealtimeUserLoansSync() {
        borrowerLoansListener?.remove()
        borrowerLoansListener = null
        lenderLoansListener?.remove()
        lenderLoansListener = null
    }

    /**
     * On-demand single loan retrieval from Firestore.
     * Caches the fetched loan into Room so local observers immediately receive it.
     */
    suspend fun fetchAndSyncLoanById(loanId: String): LoanEntity? = withContext(Dispatchers.IO) {
        if (loanId.isBlank()) return@withContext null
        try {
            firebaseManager.ensureFirebaseAuthSession()
            val doc = withTimeoutOrNull(8000L) {
                firestore
                    .collection("loans")
                    .document(loanId)
                    .get()
                    .await()
            }
            if (doc != null && doc.exists()) {
                val loan = parseDocumentToLoan(doc)
                if (loan != null) {
                    loanDao.insertLoan(loan)
                    Log.d("LoanRepository", "fetchAndSyncLoanById: Cached loan $loanId into Room")
                    return@withContext loan
                }
            }
        } catch (e: Exception) {
            Log.w("LoanRepository", "fetchAndSyncLoanById failed for $loanId: ${e.message}")
        }
        return@withContext loanDao.getLoanById(loanId)
    }

    /**
     * Pulls all loans associated with the user (as borrower or lender) from Cloud Firestore
     * and persists them into local Room SQLite. This guarantees complete data restoration
     * on fresh app install or reinstallation.
     */
    suspend fun syncUserLoansFromCloud(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.success(0)
        try {
            firebaseManager.ensureFirebaseAuthSession()
            val borrowerDocs = withTimeoutOrNull(10000L) {
                firestore.collection("loans").whereEqualTo("borrowerId", userId).get().await()
            }
            val lenderDocs = withTimeoutOrNull(10000L) {
                firestore.collection("loans").whereEqualTo("lenderId", userId).get().await()
            }

            val allDocs = buildList {
                if (borrowerDocs != null) addAll(borrowerDocs.documents)
                if (lenderDocs != null) addAll(lenderDocs.documents)
            }.distinctBy { it.id }

            val parsedLoans = allDocs.mapNotNull { parseDocumentToLoan(it) }
            if (parsedLoans.isNotEmpty()) {
                loanDao.insertLoans(parsedLoans)
                Log.i("LoanRepository", "syncUserLoansFromCloud: Restored ${parsedLoans.size} loans into local Room database.")
            }
            Result.success(parsedLoans.size)
        } catch (e: Exception) {
            Log.w("LoanRepository", "syncUserLoansFromCloud notice: ${e.message}")
            Result.failure(e)
        }
    }
}
