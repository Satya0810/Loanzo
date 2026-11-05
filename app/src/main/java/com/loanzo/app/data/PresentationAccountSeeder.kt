package com.loanzo.app.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.loanzo.app.data.entity.*
import com.loanzo.app.util.hashPassword
import com.loanzo.app.util.pdf.DemoDocumentGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresentationAccountSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LoanzoDatabase
) {
    companion object {
        private const val TAG = "PresentationSeeder"

        // 4 Key Presentation Accounts
        const val USER_DEVESH = "devesh123"
        const val USER_ROMESH = "romesh321"
        const val USER_SHIVAM = "surya111"
        const val USER_SUMAN  = "suman321"

        // Demonstration Loans
        const val LOAN_SHIVAM_MSME = "loan_shivam_msme_100k"
        const val LOAN_SUMAN_AGRI  = "loan_suman_agri_50k"
        const val LOAN_ROMESH_TECH = "loan_romesh_tech_35k"
    }

    suspend fun seedPresentationAccounts(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val existingDevesh = database.userDao().getUserById(USER_DEVESH)
            val existingLoan = database.loanDao().getLoanById(LOAN_SHIVAM_MSME)
            val existingSuryaVisit = database.agentDao().getVisitById("visit_surya_01")
            val existingCoLenders = database.coLenderDao().getCoLendersForLoanSync(LOAN_SHIVAM_MSME)
            if (existingDevesh != null && existingLoan != null && existingSuryaVisit != null && existingCoLenders.isNotEmpty()) {
                Log.d(TAG, "Presentation dataset already seeded in Room. Skipping duplicate startup seeding.")
                return@withContext Result.success("Already seeded.")
            }

            val now = System.currentTimeMillis()
            val oneDayMs = 86_400_000L

            database.withTransaction {
            // =========================================================================
            // 1. SEED 4 KEY USERS (Lender, Borrower, Co-Borrower, Guarantor)
            // =========================================================================
            val users = listOf(
                // 1. Devesh Singh — LENDER (Lead Capital Provider & P2P Investor)
                UserEntity(
                    userId = USER_DEVESH,
                    name = "Devesh Singh",
                    email = "devesh123@loanzo.app",
                    phone = "+919876512340",
                    username = "devesh123",
                    password = hashPassword("Devesh@0810"),
                    role = "LENDER",
                    kycStatus = "VERIFIED",
                    panNumber = "DVSSH1234K",
                    aadhaarNumber = "5412 8765 1234",
                    aadhaarVerified = true,
                    panVerified = true,
                    bankVerified = true,
                    bankAccountNumber = "5010049281928",
                    bankIfsc = "HDFC0001234",
                    upiId = "devesh123@okhdfc",
                    upiVerified = true,
                    emailVerified = true,
                    phoneVerified = true,
                    selfieVerified = true,
                    dateOfBirth = "14/07/1992",
                    address = "Plot 24, Sector 62, Noida, Uttar Pradesh 201309",
                    profilePhotoUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
                    agentStatus = "NONE",
                    isOnDuty = false,
                    totalAgentEarnings = 0.0,
                    createdAt = now - (90 * oneDayMs)
                ),

                // 2. Romesh Singh — CO-BORROWER, GUARANTOR & FIELD AGENT (Section 128 ICA Joint Obligor)
                UserEntity(
                    userId = USER_ROMESH,
                    name = "Romesh Singh",
                    email = "romesh321@loanzo.app",
                    phone = "+919823456789",
                    username = "romesh321",
                    password = hashPassword("Romesh@0810"),
                    role = "BORROWER", // Can operate as Member / Co-borrower / Guarantor / Agent
                    kycStatus = "VERIFIED",
                    panNumber = "RMSGH3210R",
                    aadhaarNumber = "6789 0123 4567",
                    aadhaarVerified = true,
                    panVerified = true,
                    bankVerified = true,
                    bankAccountNumber = "1029384756102",
                    bankIfsc = "ICIC0002938",
                    upiId = "romesh321@okicici",
                    upiVerified = true,
                    emailVerified = true,
                    phoneVerified = true,
                    selfieVerified = true,
                    dateOfBirth = "28/11/1994",
                    address = "Flat 402, Sector 18, Noida, Uttar Pradesh 201301",
                    profilePhotoUri = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
                    agentStatus = "APPROVED",
                    isOnDuty = true,
                    totalAgentEarnings = 3200.0,
                    createdAt = now - (85 * oneDayMs)
                ),

                // 3. Surya Yadav — BORROWER & CERTIFIED FIELD AGENT
                UserEntity(
                    userId = USER_SHIVAM,
                    name = "Surya Yadav",
                    email = "surya111@loanzo.app",
                    phone = "+919812345678",
                    username = "surya111",
                    password = hashPassword("Surya@0810"),
                    role = "BORROWER",
                    kycStatus = "VERIFIED",
                    panNumber = "SHVYD8811P",
                    aadhaarNumber = "9012 3456 7890",
                    aadhaarVerified = true,
                    panVerified = true,
                    bankVerified = true,
                    bankAccountNumber = "39840219482",
                    bankIfsc = "SBIN0005423",
                    upiId = "surya111@oksbi",
                    upiVerified = true,
                    emailVerified = true,
                    phoneVerified = true,
                    selfieVerified = true,
                    dateOfBirth = "19/04/1996",
                    address = "Weavers Colony, Chowk, Varanasi, Uttar Pradesh 221001",
                    profilePhotoUri = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&auto=format&fit=crop&q=80",
                    agentStatus = "APPROVED",
                    isOnDuty = true,
                    totalAgentEarnings = 4500.0,
                    createdAt = now - (80 * oneDayMs)
                ),

                // 4. Suman Yadav — BORROWER & GUARANTOR (Agro-Enterprise & Dairy)
                UserEntity(
                    userId = USER_SUMAN,
                    name = "Suman Yadav",
                    email = "suman321@loanzo.app",
                    phone = "+919834567890",
                    username = "suman321",
                    password = hashPassword("Suman@0810"),
                    role = "BORROWER",
                    kycStatus = "VERIFIED",
                    panNumber = "SMNYD3219M",
                    aadhaarNumber = "3456 7890 1234",
                    aadhaarVerified = true,
                    panVerified = true,
                    bankVerified = true,
                    bankAccountNumber = "4019283746501",
                    bankIfsc = "PUNB0012340",
                    upiId = "suman321@okpnb",
                    upiVerified = true,
                    emailVerified = true,
                    phoneVerified = true,
                    selfieVerified = true,
                    dateOfBirth = "05/09/1993",
                    address = "Krishi Vikas Kendra, Barabanki, Uttar Pradesh 225001",
                    profilePhotoUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&auto=format&fit=crop&q=80",
                    agentStatus = "NONE",
                    isOnDuty = false,
                    totalAgentEarnings = 0.0,
                    createdAt = now - (75 * oneDayMs)
                )
            )

            for (u in users) {
                val existing = database.userDao().getUserById(u.userId)
                if (existing != null) {
                    val photo = if (existing.profilePhotoUri.isNotBlank() && !existing.profilePhotoUri.contains("dicebear") && !existing.profilePhotoUri.contains("cartoon")) existing.profilePhotoUri else u.profilePhotoUri
                    database.userDao().updateUser(
                        u.copy(
                            profilePhotoUri = photo,
                            registeredDeviceId = existing.registeredDeviceId.ifBlank { u.registeredDeviceId },
                            registeredDeviceModel = existing.registeredDeviceModel.ifBlank { u.registeredDeviceModel },
                            fcmToken = existing.fcmToken.ifBlank { u.fcmToken },
                            telegramUsername = existing.telegramUsername.ifBlank { u.telegramUsername }
                        )
                    )
                } else {
                    database.userDao().insertUser(u)
                }
            }

            // =========================================================================
            // 2. SEED VERIFIED MERCHANT PAYEES (Purpose-Bound Tranches)
            // =========================================================================
            val payees = listOf(
                PayeeEntity(
                    payeeId = "payee_bharat_tex",
                    name = "Bharat Tex Fabrics Ltd",
                    upiId = "supplier.textiles@okaxis",
                    gstNumber = "09AAACB2109K1Z5",
                    businessName = "Bharat Tex Fabrics Private Limited",
                    verificationStatus = "VERIFIED",
                    category = "BUSINESS",
                    verifiedAt = now - (60 * oneDayMs),
                    addedBy = USER_SHIVAM,
                    createdAt = now - (60 * oneDayMs)
                ),
                PayeeEntity(
                    payeeId = "payee_kisan_agro",
                    name = "Kisan Agro Solar Equipment",
                    upiId = "kisan.agro.pumps@okaxis",
                    gstNumber = "09AABCK9921D1Z2",
                    businessName = "Kisan Agro Green Solutions",
                    verificationStatus = "VERIFIED",
                    category = "AGRICULTURE",
                    verifiedAt = now - (70 * oneDayMs),
                    addedBy = USER_SUMAN,
                    createdAt = now - (70 * oneDayMs)
                ),
                PayeeEntity(
                    payeeId = "payee_precision_instruments",
                    name = "Precision Instruments India Ltd",
                    upiId = "orders.precision@okaxis",
                    gstNumber = "07AABCP8841F1Z9",
                    businessName = "Precision Survey Instruments & Sensors Private Limited",
                    verificationStatus = "VERIFIED",
                    category = "BUSINESS",
                    verifiedAt = now - (65 * oneDayMs),
                    addedBy = USER_ROMESH,
                    createdAt = now - (65 * oneDayMs)
                )
            )
            for (p in payees) {
                database.payeeDao().insertPayee(p)
            }

            // =========================================================================
            // 3. SEED REALISTIC DEMONSTRATION LOANS
            // =========================================================================

            // --- LOAN 1: Shivam Yadav (Borrower) from Devesh Singh (Lender) ---
            // Co-Borrower: Romesh Singh, Guarantor: Suman Yadav
            // Purpose: MSME Working Capital & Fabric Sourcing
            // Milestone Escrow: Tranche 1 disbursed directly to Bharat Tex via UPI (0% cash in hand)
            val loanShivam = LoanEntity(
                loanId = LOAN_SHIVAM_MSME,
                lenderId = USER_DEVESH,
                borrowerId = USER_SHIVAM,
                sanctionedAmount = 100000.0,
                disbursedAmount = 50000.0,
                outstandingAmount = 43400.0,
                purpose = "MSME Working Capital & Fabric Sourcing",
                loanType = "BUSINESS",
                interestRate = 12.5,
                interestModel = "SIMPLE",
                tenureMonths = 12,
                status = "ACTIVE_SERVICING",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (45 * oneDayMs),
                notes = "Secured by 22K Gold & IT Equipment. Co-borrower: Romesh Singh (Joint Obligor). Guarantor: Suman Yadav.",
                lenderSignedAt = now - (44 * oneDayMs),
                borrowerSignedAt = now - (44 * oneDayMs),
                isAgreementSigned = true,
                agreementPdfUrl = "https://loanzo.app/agreements/loan_shivam_msme_100k.pdf",
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                penaltyGraceDays = 3,
                penaltyCapPercent = 100.0
            )
            database.loanDao().insertLoan(loanShivam)

            // Co-Lending Syndicate for Loan 1 (Devesh Singh 70%, Romesh Singh 30%)
            val coLenderLead = CoLenderEntity(
                coLenderId = "colend_${LOAN_SHIVAM_MSME}_lead",
                loanId = LOAN_SHIVAM_MSME,
                lenderId = USER_DEVESH,
                name = "Devesh Singh",
                phone = "+919876512340",
                contributedAmount = 70000.0,
                sharePercentage = 70.0,
                role = "LEAD_LENDER",
                status = "ACTIVE",
                createdAt = now - (45 * oneDayMs)
            )
            val coLenderRomesh = CoLenderEntity(
                coLenderId = "colend_${LOAN_SHIVAM_MSME}_co1",
                loanId = LOAN_SHIVAM_MSME,
                lenderId = USER_ROMESH,
                name = "Romesh Singh",
                phone = "+919876512342",
                contributedAmount = 30000.0,
                sharePercentage = 30.0,
                role = "CO_LENDER",
                status = "ACTIVE",
                createdAt = now - (45 * oneDayMs)
            )
            database.coLenderDao().insertCoLenders(listOf(coLenderLead, coLenderRomesh))

            // --- LOAN 2: Suman Yadav (Borrower) from Devesh Singh (Lender) ---
            // Guarantor: Romesh Singh
            // Purpose: Solar Irrigation Pump Installation
            // Fully repaid! Automated Golden NOC issued!
            val loanSuman = LoanEntity(
                loanId = LOAN_SUMAN_AGRI,
                lenderId = USER_DEVESH,
                borrowerId = USER_SUMAN,
                sanctionedAmount = 50000.0,
                disbursedAmount = 50000.0,
                outstandingAmount = 0.0,
                purpose = "Solar Irrigation Pump & Equipment Installation",
                loanType = "AGRICULTURE",
                interestRate = 10.5,
                interestModel = "SIMPLE",
                tenureMonths = 6,
                status = "COMPLETED",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (190 * oneDayMs),
                closedAt = now - (10 * oneDayMs),
                notes = "Guarantor: Romesh Singh. Disbursed directly to Kisan Agro Equipment. Fully repaid on schedule. Golden NOC issued.",
                lenderSignedAt = now - (189 * oneDayMs),
                borrowerSignedAt = now - (189 * oneDayMs),
                isAgreementSigned = true,
                agreementPdfUrl = "https://loanzo.app/agreements/loan_suman_agri_50k.pdf",
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                penaltyGraceDays = 3,
                penaltyCapPercent = 100.0
            )
            database.loanDao().insertLoan(loanSuman)

            // --- LOAN 3: Romesh Singh (Borrower) from Devesh Singh (Lender) ---
            // Co-Borrower: Suman Yadav, Guarantor: Shivam Yadav
            // Purpose: Field Inspection Equipment & Solar Radiation Toolkit
            // Disbursed directly to Precision Instruments India Ltd
            val loanRomesh = LoanEntity(
                loanId = LOAN_ROMESH_TECH,
                lenderId = USER_DEVESH,
                borrowerId = USER_ROMESH,
                sanctionedAmount = 35000.0,
                disbursedAmount = 35000.0,
                outstandingAmount = 28700.0,
                purpose = "Field Inspection Equipment & Solar Radiance Toolkit",
                loanType = "BUSINESS",
                interestRate = 11.0,
                interestModel = "SIMPLE",
                tenureMonths = 12,
                status = "ACTIVE_SERVICING",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (65 * oneDayMs),
                notes = "Field officer equipment toolkit loan. Guarantor: Shivam Yadav. Co-borrower: Suman Yadav. Disbursed directly to Precision Instruments India.",
                lenderSignedAt = now - (64 * oneDayMs),
                borrowerSignedAt = now - (64 * oneDayMs),
                isAgreementSigned = true,
                agreementPdfUrl = "https://loanzo.app/agreements/loan_romesh_tech_35k.pdf",
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                penaltyGraceDays = 3,
                penaltyCapPercent = 100.0
            )
            database.loanDao().insertLoan(loanRomesh)

            // =========================================================================
            // 4. SEED GUARANTORS & CO-BORROWERS (Section 128 ICA Legality)
            // =========================================================================
            val guarantors = listOf(
                // Romesh Singh as Co-Borrower / Legal Obligor on Shivam's MSME loan
                GuarantorEntity(
                    guarantorId = "guarantor_romesh_coborrower",
                    loanId = LOAN_SHIVAM_MSME,
                    name = "Romesh Singh",
                    phone = "+919823456789",
                    email = "romesh321@loanzo.app",
                    panNumber = "RMSGH3210R",
                    relationship = "BUSINESS_PARTNER",
                    consentStatus = "ACCEPTED",
                    consentTimestamp = now - (44 * oneDayMs),
                    createdAt = now - (45 * oneDayMs)
                ),
                // Suman Yadav as Social Guarantor on Shivam's MSME loan
                GuarantorEntity(
                    guarantorId = "guarantor_suman_social",
                    loanId = LOAN_SHIVAM_MSME,
                    name = "Suman Yadav",
                    phone = "+919834567890",
                    email = "suman321@loanzo.app",
                    panNumber = "SMNYD3219M",
                    relationship = "FRIEND",
                    consentStatus = "ACCEPTED",
                    consentTimestamp = now - (43 * oneDayMs),
                    createdAt = now - (45 * oneDayMs)
                ),
                // Romesh Singh as Guarantor on Suman's Agriculture loan
                GuarantorEntity(
                    guarantorId = "guarantor_romesh_agri",
                    loanId = LOAN_SUMAN_AGRI,
                    name = "Romesh Singh",
                    phone = "+919823456789",
                    email = "romesh321@loanzo.app",
                    panNumber = "RMSGH3210R",
                    relationship = "FRIEND",
                    consentStatus = "ACCEPTED",
                    consentTimestamp = now - (189 * oneDayMs),
                    createdAt = now - (190 * oneDayMs)
                ),
                // Shivam Yadav as Guarantor on Romesh's Equipment loan
                GuarantorEntity(
                    guarantorId = "guarantor_shivam_for_romesh",
                    loanId = LOAN_ROMESH_TECH,
                    name = "Shivam Yadav",
                    phone = "+919812345678",
                    email = "surya111@loanzo.app",
                    panNumber = "SHVYD8811P",
                    relationship = "BUSINESS_PARTNER",
                    consentStatus = "ACCEPTED",
                    consentTimestamp = now - (64 * oneDayMs),
                    createdAt = now - (65 * oneDayMs)
                ),
                // Suman Yadav as Co-Borrower on Romesh's Equipment loan
                GuarantorEntity(
                    guarantorId = "guarantor_suman_for_romesh",
                    loanId = LOAN_ROMESH_TECH,
                    name = "Suman Yadav",
                    phone = "+919834567890",
                    email = "suman321@loanzo.app",
                    panNumber = "SMNYD3219M",
                    relationship = "COMMUNITY_PEER",
                    consentStatus = "ACCEPTED",
                    consentTimestamp = now - (64 * oneDayMs),
                    createdAt = now - (65 * oneDayMs)
                )
            )
            database.guarantorDao().insertGuarantors(guarantors)

            // =========================================================================
            // 5. SEED PURPOSE-BOUND ESCROW DISBURSEMENTS (0% Cash Leakage)
            // =========================================================================
            val disbursements = listOf(
                // Milestone 1 Disbursed directly to vendor for Shivam
                DisbursementEntity(
                    disbursementId = "disb_shivam_tranche_1",
                    loanId = LOAN_SHIVAM_MSME,
                    amount = 50000.0,
                    payeeId = "payee_bharat_tex",
                    payeeName = "Bharat Tex Fabrics Ltd",
                    purpose = "Raw Fabric & Yarn Procurement (Milestone 1)",
                    purposeCategory = "BUSINESS",
                    verificationStatus = "VERIFIED",
                    ruleEngineResult = "AUTO_APPROVED",
                    approvalStatus = "APPROVED",
                    transactionRef = "UPI/2619904281/BHARATTEX",
                    timestamp = now - (42 * oneDayMs),
                    lenderNote = "Invoice #TX-8841 OCR verified. Funds disbursed directly to vendor VPA.",
                    borrowerNote = "Fabric shipment received at loom unit. Milestone 1 active."
                ),
                // Tranche Disbursed directly to vendor for Suman
                DisbursementEntity(
                    disbursementId = "disb_suman_tranche_1",
                    loanId = LOAN_SUMAN_AGRI,
                    amount = 50000.0,
                    payeeId = "payee_kisan_agro",
                    payeeName = "Kisan Agro Solar Equipment",
                    purpose = "5HP Solar Water Pump Installation",
                    purposeCategory = "AGRICULTURE",
                    verificationStatus = "VERIFIED",
                    ruleEngineResult = "AUTO_APPROVED",
                    approvalStatus = "APPROVED",
                    transactionRef = "UPI/2619901984/KISANAGRO",
                    timestamp = now - (187 * oneDayMs),
                    lenderNote = "Direct supplier settlement completed upon equipment invoice upload.",
                    borrowerNote = "Solar pump successfully installed and tested on irrigation plot."
                ),
                // Tranche Disbursed directly to vendor for Romesh
                DisbursementEntity(
                    disbursementId = "disb_romesh_tranche_1",
                    loanId = LOAN_ROMESH_TECH,
                    amount = 35000.0,
                    payeeId = "payee_precision_instruments",
                    payeeName = "Precision Instruments India Ltd",
                    purpose = "Field Inspection Tooling & Optical Sensors",
                    purposeCategory = "BUSINESS",
                    verificationStatus = "VERIFIED",
                    ruleEngineResult = "AUTO_APPROVED",
                    approvalStatus = "APPROVED",
                    transactionRef = "UPI/2619908821/PRECISION",
                    timestamp = now - (63 * oneDayMs),
                    lenderNote = "Direct supplier settlement for field testing kit. Invoice #PI-4412 verified.",
                    borrowerNote = "Equipment received and calibrated for field inspections."
                )
            )
            database.disbursementDao().insertDisbursements(disbursements)

            // =========================================================================
            // 6. SEED REPAYMENTS (Track Record, Paid EMIs & Upcoming Schedules)
            // =========================================================================
            val repayments = mutableListOf<RepaymentEntity>()

            // Shivam's MSME Loan: 3 Paid EMIs, 1 Upcoming, 8 Scheduled
            val emiAmountShivam = 9200.0
            repayments.add(
                RepaymentEntity(
                    repaymentId = "repay_shivam_01",
                    loanId = LOAN_SHIVAM_MSME,
                    amount = emiAmountShivam,
                    transactionRef = "UPI/LZ/SHIVAM/01",
                    status = "PAID",
                    dueDate = now - (30 * oneDayMs),
                    paidDate = now - (30 * oneDayMs),
                    principalComponent = 8158.0,
                    interestComponent = 1042.0,
                    outstandingSnapshot = 91842.0,
                    timestamp = now - (30 * oneDayMs),
                    note = "On-time EMI payment via UPI"
                )
            )
            repayments.add(
                RepaymentEntity(
                    repaymentId = "repay_shivam_02",
                    loanId = LOAN_SHIVAM_MSME,
                    amount = emiAmountShivam,
                    transactionRef = "UPI/LZ/SHIVAM/02",
                    status = "PAID",
                    dueDate = now - (15 * oneDayMs),
                    paidDate = now - (15 * oneDayMs),
                    principalComponent = 8243.0,
                    interestComponent = 957.0,
                    outstandingSnapshot = 83599.0,
                    timestamp = now - (15 * oneDayMs),
                    note = "On-time EMI payment via UPI"
                )
            )
            repayments.add(
                RepaymentEntity(
                    repaymentId = "repay_shivam_03",
                    loanId = LOAN_SHIVAM_MSME,
                    amount = emiAmountShivam,
                    transactionRef = "UPI/LZ/SHIVAM/03",
                    status = "PAID",
                    dueDate = now - (1 * oneDayMs),
                    paidDate = now - (1 * oneDayMs),
                    principalComponent = 8329.0,
                    interestComponent = 871.0,
                    outstandingSnapshot = 75270.0,
                    timestamp = now - (1 * oneDayMs),
                    note = "On-time EMI payment via UPI"
                )
            )
            // Upcoming due in 14 days
            repayments.add(
                RepaymentEntity(
                    repaymentId = "repay_shivam_04",
                    loanId = LOAN_SHIVAM_MSME,
                    amount = emiAmountShivam,
                    status = "SCHEDULED",
                    dueDate = now + (14 * oneDayMs),
                    principalComponent = 8416.0,
                    interestComponent = 784.0,
                    outstandingSnapshot = 66854.0,
                    timestamp = now
                )
            )
            for (i in 5..12) {
                repayments.add(
                    RepaymentEntity(
                        repaymentId = "repay_shivam_0$i",
                        loanId = LOAN_SHIVAM_MSME,
                        amount = emiAmountShivam,
                        status = "SCHEDULED",
                        dueDate = now + ((i * 15) * oneDayMs),
                        principalComponent = 8500.0,
                        interestComponent = 700.0,
                        outstandingSnapshot = (100000.0 - (i * 8200.0)).coerceAtLeast(0.0),
                        timestamp = now
                    )
                )
            }

            // Suman's Completed Agriculture Loan: All 6 EMIs PAID
            val emiAmountSuman = 8770.0
            for (m in 1..6) {
                val paidTime = now - ((190 - (m * 30)) * oneDayMs)
                repayments.add(
                    RepaymentEntity(
                        repaymentId = "repay_suman_0$m",
                        loanId = LOAN_SUMAN_AGRI,
                        amount = emiAmountSuman,
                        transactionRef = "UPI/LZ/SUMAN/0$m",
                        status = "PAID",
                        dueDate = paidTime,
                        paidDate = paidTime,
                        principalComponent = 8333.0,
                        interestComponent = 437.0,
                        outstandingSnapshot = (50000.0 - (m * 8333.0)).coerceAtLeast(0.0),
                        timestamp = paidTime,
                        note = "Installment #$m paid on schedule"
                    )
                )
            }

            // Romesh's Equipment Loan: 2 Paid EMIs, 1 Upcoming, 9 Scheduled
            val emiAmountRomesh = 3150.0
            repayments.add(
                RepaymentEntity(
                    repaymentId = "repay_romesh_01",
                    loanId = LOAN_ROMESH_TECH,
                    amount = emiAmountRomesh,
                    transactionRef = "UPI/LZ/ROMESH/01",
                    status = "PAID",
                    dueDate = now - (35 * oneDayMs),
                    paidDate = now - (35 * oneDayMs),
                    principalComponent = 2830.0,
                    interestComponent = 320.0,
                    outstandingSnapshot = 32170.0,
                    timestamp = now - (35 * oneDayMs),
                    note = "On-time EMI payment via UPI"
                )
            )
            repayments.add(
                RepaymentEntity(
                    repaymentId = "repay_romesh_02",
                    loanId = LOAN_ROMESH_TECH,
                    amount = emiAmountRomesh,
                    transactionRef = "UPI/LZ/ROMESH/02",
                    status = "PAID",
                    dueDate = now - (5 * oneDayMs),
                    paidDate = now - (5 * oneDayMs),
                    principalComponent = 2855.0,
                    interestComponent = 295.0,
                    outstandingSnapshot = 29315.0,
                    timestamp = now - (5 * oneDayMs),
                    note = "On-time EMI payment via UPI"
                )
            )
            repayments.add(
                RepaymentEntity(
                    repaymentId = "repay_romesh_03",
                    loanId = LOAN_ROMESH_TECH,
                    amount = emiAmountRomesh,
                    status = "SCHEDULED",
                    dueDate = now + (25 * oneDayMs),
                    principalComponent = 2880.0,
                    interestComponent = 270.0,
                    outstandingSnapshot = 26435.0,
                    timestamp = now
                )
            )
            for (k in 4..12) {
                repayments.add(
                    RepaymentEntity(
                        repaymentId = "repay_romesh_0$k",
                        loanId = LOAN_ROMESH_TECH,
                        amount = emiAmountRomesh,
                        status = "SCHEDULED",
                        dueDate = now + ((k * 30 - 35) * oneDayMs),
                        principalComponent = 2900.0,
                        interestComponent = 250.0,
                        outstandingSnapshot = (35000.0 - (k * 2900.0)).coerceAtLeast(0.0),
                        timestamp = now
                    )
                )
            }

            database.repaymentDao().insertRepayments(repayments)

            // =========================================================================
            // 7. SEED 5-CLASS COLLATERAL PLEDGES & CENTRAL VAULT ITEMS
            // =========================================================================
            val pledges = listOf(
                PledgeEntity(
                    pledgeId = "pledge_shivam_gold",
                    loanId = LOAN_SHIVAM_MSME,
                    assetDescription = "22K Hallmarked Gold Ornaments (30 grams)",
                    assetType = "GOLD",
                    estimatedValue = 150000.0,
                    weight = 30.0,
                    receiptStatus = "VERIFIED",
                    notes = "Appraised by certified assayer. 75% LTV applied. Secured in Central Locker #42.",
                    createdAt = now - (45 * oneDayMs)
                ),
                PledgeEntity(
                    pledgeId = "pledge_shivam_it",
                    loanId = LOAN_SHIVAM_MSME,
                    assetDescription = "Precision CAD Workstation & Jacquard Digital Card Reader",
                    assetType = "EQUIPMENT",
                    estimatedValue = 40000.0,
                    receiptStatus = "VERIFIED",
                    notes = "50% LTV dynamic haircut. Inspection completed on site.",
                    createdAt = now - (45 * oneDayMs)
                ),
                PledgeEntity(
                    pledgeId = "pledge_suman_enwr",
                    loanId = LOAN_SUMAN_AGRI,
                    assetDescription = "Electronic Negotiable Warehouse Receipt (e-NWR) - 200 Quintals Wheat",
                    assetType = "OTHER",
                    estimatedValue = 60000.0,
                    receiptStatus = "VERIFIED",
                    notes = "WDRA Accredited Warehouse Barabanki. 60% LTV.",
                    createdAt = now - (190 * oneDayMs)
                ),
                PledgeEntity(
                    pledgeId = "pledge_romesh_kit",
                    loanId = LOAN_ROMESH_TECH,
                    assetDescription = "Optical Spectrometer & Digital Density Scale",
                    assetType = "EQUIPMENT",
                    estimatedValue = 50000.0,
                    receiptStatus = "VERIFIED",
                    notes = "Precision field inspection toolkit. Serial #SS-99420.",
                    createdAt = now - (65 * oneDayMs)
                )
            )
            database.pledgeDao().insertPledges(pledges)

            // Central Collateral Vault Entries
            val vaultItems = listOf(
                CollateralVaultEntity(
                    vaultItemId = "vault_shivam_gold_01",
                    loanId = LOAN_SHIVAM_MSME,
                    borrowerId = USER_SHIVAM,
                    borrowerName = "Surya Yadav",
                    borrowerPhone = "+919812345678",
                    assetDescription = "22K Gold Ornaments (30g Hallmarked)",
                    assetType = "GOLD",
                    estimatedValue = 150000.0,
                    appraisedPurityOrCondition = "22 Karat (91.6% Pure Gold)",
                    vaultFacilityName = "Loanzo Secured Custody Vault - Noida Sector 62",
                    lockerNumber = "LOCKER-042",
                    barcodeTag = "LZ-GLD-7721",
                    tamperSealNumber = "SEAL-889012",
                    custodyStatus = "SECURED_IN_VAULT",
                    intakeAgentId = USER_ROMESH,
                    intakeDate = now - (44 * oneDayMs),
                    createdAt = now - (45 * oneDayMs)
                ),
                CollateralVaultEntity(
                    vaultItemId = "vault_suman_enwr_01",
                    loanId = LOAN_SUMAN_AGRI,
                    borrowerId = USER_SUMAN,
                    borrowerName = "Suman Yadav",
                    borrowerPhone = "+919834567890",
                    assetDescription = "WDRA Electronic Warehouse Receipt (200Q Wheat)",
                    assetType = "OTHER",
                    estimatedValue = 60000.0,
                    appraisedPurityOrCondition = "Grade A Milling Wheat (Moisture < 12%)",
                    vaultFacilityName = "WDRA Barabanki Central Silo 4",
                    lockerNumber = "DIGITAL-ENWR-291",
                    barcodeTag = "WHR-2026-98124",
                    tamperSealNumber = "WDRA-DIGITAL-SEAL",
                    custodyStatus = "RELEASED",
                    releaseDate = now - (10 * oneDayMs),
                    createdAt = now - (190 * oneDayMs)
                ),
                CollateralVaultEntity(
                    vaultItemId = "vault_romesh_kit_01",
                    loanId = LOAN_ROMESH_TECH,
                    borrowerId = USER_ROMESH,
                    borrowerName = "Romesh Singh",
                    borrowerPhone = "+919823456789",
                    assetDescription = "Optical Spectrometer & Digital Density Scale",
                    assetType = "EQUIPMENT",
                    estimatedValue = 50000.0,
                    appraisedPurityOrCondition = "Certified Precision Grade A (Calibrated)",
                    vaultFacilityName = "Loanzo Field Equipment Registry - Noida Sector 62",
                    lockerNumber = "FIELD-RIG-09",
                    barcodeTag = "LZ-EQP-5512",
                    tamperSealNumber = "SEAL-991204",
                    custodyStatus = "SECURED_IN_VAULT",
                    intakeAgentId = USER_ROMESH,
                    intakeDate = now - (64 * oneDayMs),
                    createdAt = now - (65 * oneDayMs)
                )
            )
            database.collateralVaultDao().insertVaultItems(vaultItems)

            // =========================================================================
            // 8. SEED AUTOMATED GOLDEN NOC CERTIFICATE (Full Settlement Proof)
            // =========================================================================
            val goldenNoc = NocCertificateEntity(
                nocId = "noc_suman_agri_50k",
                loanId = LOAN_SUMAN_AGRI,
                borrowerId = USER_SUMAN,
                borrowerName = "Suman Yadav",
                borrowerPan = "SMNYD3219M",
                lenderId = USER_DEVESH,
                lenderName = "Devesh Singh",
                principalAmount = 50000.0,
                totalRepaidAmount = 52625.0,
                collateralReleasedDesc = "WDRA e-NWR #WHR-2026-98124 Wheat Lien Discharged & Restored to Borrower",
                digitalSignatureHash = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
                issuedAt = now - (10 * oneDayMs),
                issuedByAdminId = "SYSTEM-NOC-ENGINE",
                status = "ACTIVE_CLEARANCE"
            )
            database.nocCertificateDao().insertNoc(goldenNoc)

            // =========================================================================
            // 9. SEED MARKETPLACE POSTS, BIDS & VOUCHES (Community Ecosystem)
            // =========================================================================
            val posts = listOf(
                // Post 1: Suman Yadav seeking capital for Dairy Chiller
                MarketplacePostEntity(
                    postId = "post_suman_dairy_75k",
                    authorId = USER_SUMAN,
                    authorName = "Suman Yadav",
                    authorKycVerified = true,
                    authorTrustScore = 96,
                    postType = "SEEKING_LOAN",
                    title = "Dairy Bulk Milk Chiller & Solar Setup",
                    description = "Seeking working capital for setting up a 500-liter bulk milk chiller for local dairy collection. 75% collateral ready via e-NWR warehouse receipt. Verified co-borrower Romesh Singh.",
                    minAmount = 50000.0,
                    maxAmount = 75000.0,
                    interestRate = 11.5,
                    interestModel = "SIMPLE",
                    tenureMonths = 12,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "AGRICULTURE",
                    locationCity = "Barabanki, Uttar Pradesh",
                    collateralOffered = "Electronic Negotiable Warehouse Receipt (e-NWR)",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 3,
                    bidsCount = 2,
                    status = "OPEN",
                    createdAt = now - (2 * oneDayMs),
                    coBorrowerName = "Romesh Singh",
                    coBorrowerRelationship = "Brother / Partner",
                    coBorrowerKycVerified = true,
                    coBorrowerTrustScore = 94
                ),
                // Post 2: Devesh Singh offering capital pool for MSMEs (₹2,00,000)
                MarketplacePostEntity(
                    postId = "post_devesh_lending_pool",
                    authorId = USER_DEVESH,
                    authorName = "Devesh Singh",
                    authorKycVerified = true,
                    authorTrustScore = 98,
                    postType = "OFFER_TO_LEND",
                    title = "Verified MSME & Agricultural Working Capital Syndicate",
                    description = "Syndicating liquidity for verified local enterprises, weavers, and agro-producers. Direct milestone UPI disbursements to suppliers with 0% fund leakage required.",
                    minAmount = 25000.0,
                    maxAmount = 200000.0,
                    interestRate = 12.0,
                    interestModel = "SIMPLE",
                    tenureMonths = 12,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "BUSINESS",
                    locationCity = "Noida / Delhi NCR",
                    collateralOffered = "Gold / Crops / Equipment",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 4,
                    bidsCount = 2,
                    status = "OPEN",
                    createdAt = now - (5 * oneDayMs)
                ),
                // Post 3: Devesh Singh offering rural solar & equipment capital line (₹1,50,000)
                MarketplacePostEntity(
                    postId = "post_devesh_rural_solar",
                    authorId = USER_DEVESH,
                    authorName = "Devesh Singh",
                    authorKycVerified = true,
                    authorTrustScore = 98,
                    postType = "OFFER_TO_LEND",
                    title = "Tier-2/3 Rural Solar, Cold Storage & Dairy Syndicate",
                    description = "Offering syndicate liquidity for Farmer Producer Organizations (FPOs) and rural agro-enterprises. Direct vendor milestone disbursements via UPI with 0% advance fees.",
                    minAmount = 30000.0,
                    maxAmount = 150000.0,
                    interestRate = 11.0,
                    interestModel = "SIMPLE",
                    tenureMonths = 12,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "AGRICULTURE",
                    locationCity = "Uttar Pradesh / NCR",
                    collateralOffered = "e-NWR Warehouse Receipts / Farmland Lease",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 3,
                    bidsCount = 1,
                    status = "OPEN",
                    createdAt = now - (3 * oneDayMs)
                ),
                // Post 4: Vikram Gupta (Artisan Borrower seeking capital)
                MarketplacePostEntity(
                    postId = "post_vikram_weaver_60k",
                    authorId = "user_vikram_gupta",
                    authorName = "Vikram Gupta",
                    authorKycVerified = true,
                    authorTrustScore = 93,
                    postType = "SEEKING_LOAN",
                    title = "Banarasi Silk Yarn Bulk Procurement",
                    description = "Seeking working capital for raw mulberry silk yarn procurement for festive order fulfillment. Ready to pledge handloom equipment and warehouse stock.",
                    minAmount = 40000.0,
                    maxAmount = 60000.0,
                    interestRate = 12.0,
                    interestModel = "SIMPLE",
                    tenureMonths = 6,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "BUSINESS",
                    locationCity = "Varanasi, Uttar Pradesh",
                    collateralOffered = "Handloom Equipment & Finished Silk Stock",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 2,
                    bidsCount = 1,
                    status = "OPEN",
                    createdAt = now - (1 * oneDayMs)
                )
            )
            database.marketplaceDao().insertPosts(posts)

            // Bids involving Devesh Singh
            val bids = listOf(
                // Bid 1: Devesh on Suman's Post
                MarketplaceBidEntity(
                    bidId = "bid_devesh_on_suman",
                    postId = "post_suman_dairy_75k",
                    bidderId = USER_DEVESH,
                    bidderName = "Devesh Singh",
                    bidderKycVerified = true,
                    bidderTrustScore = 98,
                    proposedAmount = 75000.0,
                    proposedInterestRate = 11.5,
                    proposedTenureMonths = 12,
                    proposedRepaymentFrequency = "MONTHLY",
                    message = "Ready to syndicate full amount. Disbursement directly to verified chiller manufacturer UPI VPA upon invoice OCR.",
                    status = "PENDING",
                    createdAt = now - (1 * oneDayMs)
                ),
                // Bid 2: Devesh on Vikram's Silk Weaving post
                MarketplaceBidEntity(
                    bidId = "bid_devesh_on_vikram",
                    postId = "post_vikram_weaver_60k",
                    bidderId = USER_DEVESH,
                    bidderName = "Devesh Singh",
                    bidderKycVerified = true,
                    bidderTrustScore = 98,
                    proposedAmount = 60000.0,
                    proposedInterestRate = 12.0,
                    proposedTenureMonths = 6,
                    proposedRepaymentFrequency = "MONTHLY",
                    message = "Syndicate funding approved. Direct escrow payout to your registered silk supplier via UPI with 0% platform deductions.",
                    status = "ACCEPTED",
                    createdAt = now - (12 * 3600_000L)
                ),
                // Bid 3: Prospective borrower bidding on Devesh's Solar Pool
                MarketplaceBidEntity(
                    bidId = "bid_sharma_dairy_on_devesh_pool",
                    postId = "post_devesh_rural_solar",
                    bidderId = "user_kisan_dairy",
                    bidderName = "Rameshwar Sharma",
                    bidderKycVerified = true,
                    bidderTrustScore = 94,
                    proposedAmount = 90000.0,
                    proposedInterestRate = 11.0,
                    proposedTenureMonths = 12,
                    proposedRepaymentFrequency = "MONTHLY",
                    message = "Applying for 10HP solar pump funding for organic dairy farm. WDRA electronic warehouse receipt collateral available.",
                    status = "PENDING",
                    createdAt = now - (6 * 3600_000L)
                )
            )
            for (b in bids) {
                database.marketplaceDao().insertBid(b)
            }

            // Community Vouches (Authored by Devesh and Received by Devesh)
            val vouches = listOf(
                MarketplaceVouchEntity(
                    vouchId = "vouch_devesh_suman",
                    postId = "post_suman_dairy_75k",
                    voucherUserId = USER_DEVESH,
                    voucherName = "Devesh Singh",
                    vouchReason = "PAST_REPAYMENT",
                    comment = "Suman has successfully closed a ₹50,000 agricultural loan with zero defaults. High integrity borrower.",
                    createdAt = now - (1 * oneDayMs),
                    voucherKycVerified = true,
                    voucherTrustScore = 98,
                    voucherRole = "Verified Institutional Lender"
                ),
                MarketplaceVouchEntity(
                    vouchId = "vouch_devesh_romesh",
                    postId = "post_devesh_rural_solar",
                    voucherUserId = USER_DEVESH,
                    voucherName = "Devesh Singh",
                    vouchReason = "COMMUNITY_REFERENCE",
                    comment = "Romesh Singh is a diligent senior field inspection officer. Verified precision equipment inspection with 100% adherence.",
                    createdAt = now - (2 * oneDayMs),
                    voucherKycVerified = true,
                    voucherTrustScore = 98,
                    voucherRole = "Lead Syndicate Lender"
                ),
                MarketplaceVouchEntity(
                    vouchId = "vouch_devesh_shivam",
                    postId = "post_devesh_lending_pool",
                    voucherUserId = USER_DEVESH,
                    voucherName = "Devesh Singh",
                    vouchReason = "PAST_REPAYMENT",
                    comment = "Shivam Yadav has paid 3 consecutive EMIs on-time with zero friction. Highly dependable artisan entrepreneur.",
                    createdAt = now - (1 * oneDayMs),
                    voucherKycVerified = true,
                    voucherTrustScore = 98,
                    voucherRole = "Lead Syndicate Lender"
                ),
                MarketplaceVouchEntity(
                    vouchId = "vouch_suman_for_devesh",
                    postId = "post_devesh_lending_pool",
                    voucherUserId = USER_SUMAN,
                    voucherName = "Suman Yadav",
                    vouchReason = "PAST_REPAYMENT",
                    comment = "Devesh ji is a fair and supportive lender. Granted me a 5-day harvest grace period with zero penalty, and issued Golden NOC within minutes of loan closure.",
                    createdAt = now - (2 * oneDayMs),
                    voucherKycVerified = true,
                    voucherTrustScore = 96,
                    voucherRole = "Verified Borrower (Completed Loan)"
                ),
                MarketplaceVouchEntity(
                    vouchId = "vouch_romesh_for_devesh",
                    postId = "post_devesh_rural_solar",
                    voucherUserId = USER_ROMESH,
                    voucherName = "Romesh Singh",
                    vouchReason = "COMMUNITY_REFERENCE",
                    comment = "I regularly inspect collateral and toolings for Devesh's loans. Complete compliance with RBI guidelines, zero hidden charges.",
                    createdAt = now - (2 * oneDayMs),
                    voucherKycVerified = true,
                    voucherTrustScore = 94,
                    voucherRole = "Senior Field Inspection Officer"
                ),
                MarketplaceVouchEntity(
                    vouchId = "vouch_romesh_suman",
                    postId = "post_suman_dairy_75k",
                    voucherUserId = USER_ROMESH,
                    voucherName = "Romesh Singh",
                    vouchReason = "COMMUNITY_REFERENCE",
                    comment = "Family dairy operation verified on ground. Highly viable cash flow.",
                    createdAt = now - (2 * oneDayMs),
                    voucherKycVerified = true,
                    voucherTrustScore = 94,
                    voucherRole = "Verified Field Officer & Member"
                )
            )
            database.marketplaceDao().insertVouches(vouches)

            // =========================================================================
            // 10. SEED FIELD AGENT APPLICATION & DUAL-OFFICER VISITS
            // =========================================================================
            val agentApp = AgentApplicationEntity(
                applicationId = "app_romesh_agent",
                userId = USER_ROMESH,
                applicantName = "Romesh Singh",
                applicantPhone = "+919823456789",
                applicantEmail = "romesh321@loanzo.app",
                experienceYears = "3-5 Years",
                priorDomain = "Banking / NBFC",
                policeVerificationNumber = "PV-NOIDA-2025-9921",
                policeStation = "Sector 20 Police Station Noida",
                policeVerificationDate = "15/01/2025",
                permanentAddress = "Flat 402, Sector 18, Noida, Uttar Pradesh 201301",
                operatingCity = "Noida / Varanasi / NCR",
                operatingPincode = "201301",
                serviceRadiusKm = 25,
                vehicleType = "Two-Wheeler",
                drivingLicenseNumber = "UP14-2016-0049281",
                status = "APPROVED",
                submittedAt = now - (84 * oneDayMs),
                reviewedAt = now - (83 * oneDayMs),
                adminRemarks = "Verified banking experience and background check. Appointed as Senior Field Verification Officer."
            )
            database.agentDao().insertApplication(agentApp)

            val agentVisits = listOf(
                // Completed physical inspection of Shivam's workshop & gold assay
                AgentVisitEntity(
                    visitId = "visit_romesh_shivam_gold",
                    agentId = USER_ROMESH,
                    loanId = LOAN_SHIVAM_MSME,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Physical Inspection & Gold Assay - Weavers Workshop",
                    borrowerName = "Shivam Yadav",
                    borrowerPhone = "+919812345678",
                    borrowerAddress = "Weavers Colony, Chowk, Varanasi, Uttar Pradesh 221001",
                    lenderName = "Devesh Singh",
                    lenderPhone = "+919876512340",
                    lenderAddress = "Plot 24, Sector 62, Noida",
                    targetAddress = "Weavers Colony, Chowk, Varanasi, Uttar Pradesh 221001",
                    targetLatitude = 25.3176,
                    targetLongitude = 82.9739,
                    scheduledDate = "15-08-2026",
                    scheduledTimeSlot = "11:00 AM - 12:30 PM",
                    payoutAmount = 1600.0,
                    collateralItemName = "22K Hallmarked Gold Ornaments (30g)",
                    collateralEstimatedValue = 150000.0,
                    collateralPledgedValue = 100000.0,
                    status = "COMPLETED",
                    agentRemarks = "22K hallmarking verified with acid scratch & digital density test. Workshop machinery in prime condition.",
                    isCollateralAuthentic = true,
                    isBorrowerIdentityVerified = true,
                    isLenderIdentityVerified = true,
                    completedAt = now - (43 * oneDayMs),
                    createdAt = now - (45 * oneDayMs),
                    appraisedValue = 150000.0,
                    officerRecommendation = "RECOMMEND_APPROVAL",
                    assignedAgentName = "Romesh Singh",
                    agentPhone = "+919823456789",
                    isHandshakePinVerified = true,
                    visitStageStatus = "COMPLETED"
                ),
                // Completed farmland & solar pump inspection for Suman
                AgentVisitEntity(
                    visitId = "visit_romesh_suman_solar",
                    agentId = USER_ROMESH,
                    loanId = LOAN_SUMAN_AGRI,
                    visitType = "BORROWER_VERIFICATION",
                    title = "Agricultural Farmland & Solar Pump Inspection",
                    borrowerName = "Suman Yadav",
                    borrowerPhone = "+919834567890",
                    borrowerAddress = "Krishi Vikas Kendra, Barabanki, Uttar Pradesh 225001",
                    lenderName = "Devesh Singh",
                    lenderPhone = "+919876512340",
                    lenderAddress = "Plot 24, Sector 62, Noida",
                    targetAddress = "Krishi Vikas Kendra, Barabanki, Uttar Pradesh 225001",
                    targetLatitude = 26.9272,
                    targetLongitude = 81.1834,
                    scheduledDate = "10-03-2026",
                    scheduledTimeSlot = "02:00 PM - 03:30 PM",
                    payoutAmount = 1600.0,
                    collateralItemName = "WDRA e-NWR Warehouse Receipt (200Q Wheat)",
                    collateralEstimatedValue = 60000.0,
                    collateralPledgedValue = 50000.0,
                    status = "COMPLETED",
                    agentRemarks = "Solar pump installation verified running on 5-acre farmland. Borewell yield confirmed.",
                    isCollateralAuthentic = true,
                    isBorrowerIdentityVerified = true,
                    isLenderIdentityVerified = true,
                    completedAt = now - (185 * oneDayMs),
                    createdAt = now - (190 * oneDayMs),
                    appraisedValue = 60000.0,
                    officerRecommendation = "RECOMMEND_APPROVAL",
                    assignedAgentName = "Romesh Singh",
                    agentPhone = "+919823456789",
                    isHandshakePinVerified = true,
                    visitStageStatus = "COMPLETED"
                )
            )
            database.agentDao().insertVisits(agentVisits)

            // =========================================================================
            // 11. SEED SUPPORT TICKETS (Help & Resolution Center)
            // =========================================================================
            val tickets = listOf(
                SupportTicketEntity(
                    ticketId = "ticket_devesh_01",
                    userId = USER_DEVESH,
                    userName = "Devesh Singh",
                    userPhone = "+919876512340",
                    userEmail = "devesh123@loanzo.app",
                    category = "GENERAL_QUERY",
                    priority = "MEDIUM",
                    subject = "Syndicate Lender Registration & TDS Certificate Schedule",
                    description = "Requesting automated generation of quarterly TDS statement under Section 194A for interest payouts.",
                    status = "RESOLVED",
                    resolutionNotes = "TDS Certificate and Form 16A statements are now automated and available in your Document Vault.",
                    resolvedAt = now - (15 * oneDayMs),
                    feedbackRating = 5,
                    feedbackComment = "Very swift and clear response from Loanzo team.",
                    feedbackSubmittedAt = now - (14 * oneDayMs),
                    createdAt = now - (20 * oneDayMs)
                ),
                SupportTicketEntity(
                    ticketId = "ticket_devesh_02",
                    userId = USER_DEVESH,
                    userName = "Devesh Singh",
                    userPhone = "+919876512340",
                    userEmail = "devesh123@loanzo.app",
                    category = "ACCOUNT_ISSUE",
                    priority = "LOW",
                    subject = "Automated Escrow Reinvestment & Compounding Options",
                    description = "Inquiring about automated interest re-allocation into vetted Tier-1 community borrower pools once monthly EMI interest credits clear escrow.",
                    status = "RESOLVED",
                    resolutionNotes = "Auto-sweep and liquidity compound triggers are configured. Recovered funds can be auto-allocated to open verified lending syndicate bids.",
                    resolvedAt = now - (8 * oneDayMs),
                    feedbackRating = 5,
                    feedbackComment = "Clear explanation and seamless auto-sweep workflow.",
                    feedbackSubmittedAt = now - (7 * oneDayMs),
                    createdAt = now - (10 * oneDayMs)
                ),
                SupportTicketEntity(
                    ticketId = "ticket_devesh_03",
                    userId = USER_DEVESH,
                    userName = "Devesh Singh",
                    userPhone = "+919876512340",
                    userEmail = "devesh123@loanzo.app",
                    category = "COLLATERAL",
                    priority = "HIGH",
                    subject = "Dual-Officer Collateral Inspection for ₹2L MSME Syndicate",
                    description = "Requisitioning certified field inspection and gold assayer visit for a prospective textile exporter expansion in the Varanasi node.",
                    status = "IN_PROGRESS",
                    adminNotes = "Senior Field Officer Romesh Singh assigned. Primary physical inspection scheduled for tomorrow.",
                    createdAt = now - (1 * oneDayMs)
                ),
                SupportTicketEntity(
                    ticketId = "ticket_shivam_01",
                    userId = USER_SHIVAM,
                    userName = "Shivam Yadav",
                    userPhone = "+919812345678",
                    userEmail = "surya111@loanzo.app",
                    category = "LOAN_ISSUE",
                    priority = "HIGH",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    subject = "Milestone 2 Fabric Sourcing Tranche Requisition",
                    description = "Milestone 1 fabric processing 100% complete. Requesting verification for Tranche 2 direct payment to yarn supplier.",
                    status = "IN_PROGRESS",
                    adminNotes = "Field verification report received. OCR invoice verification pending supplier GST reconciliation.",
                    createdAt = now - (3 * oneDayMs)
                ),
                SupportTicketEntity(
                    ticketId = "ticket_romesh_01",
                    userId = USER_ROMESH,
                    userName = "Romesh Singh",
                    userPhone = "+919823456789",
                    userEmail = "romesh321@loanzo.app",
                    category = "APP_BUG",
                    priority = "LOW",
                    subject = "Field Agent GPS Geofence Check-in Calibration",
                    description = "Query regarding 50m tolerance buffer when verifying rural agricultural plots with patchy 4G.",
                    status = "RESOLVED",
                    resolutionNotes = "Offline cryptographic timestamp and GPS fallback buffer configured for rural nodes.",
                    resolvedAt = now - (40 * oneDayMs),
                    feedbackRating = 5,
                    feedbackComment = "Works seamlessly offline now.",
                    feedbackSubmittedAt = now - (39 * oneDayMs),
                    createdAt = now - (42 * oneDayMs)
                ),
                SupportTicketEntity(
                    ticketId = "ticket_suman_01",
                    userId = USER_SUMAN,
                    userName = "Suman Yadav",
                    userPhone = "+919834567890",
                    userEmail = "suman321@loanzo.app",
                    category = "COLLATERAL",
                    priority = "MEDIUM",
                    relatedLoanId = LOAN_SUMAN_AGRI,
                    subject = "WDRA e-NWR Electronic Warehouse Receipt Lien Discharge",
                    description = "Loan completed. Kindly confirm WDRA portal lien removal status for 200 Quintals Wheat.",
                    status = "RESOLVED",
                    resolutionNotes = "Lien successfully discharged on WDRA repository. Golden NOC and release certificate available in Document Vault.",
                    resolvedAt = now - (9 * oneDayMs),
                    feedbackRating = 5,
                    feedbackComment = "Golden NOC and WDRA lien discharge received. Thank you!",
                    feedbackSubmittedAt = now - (8 * oneDayMs),
                    createdAt = now - (12 * oneDayMs)
                )
            )
            database.supportTicketDao().insertTickets(tickets)

            // =========================================================================
            // 12. SEED COMPLAINT & AMICABLE MEDIATION RESOLUTION
            // =========================================================================
            val complaint = ComplaintEntity(
                complaintId = "complaint_suman_reschedule",
                complainantId = USER_SUMAN,
                complainantName = "Suman Yadav",
                complainantRole = "BORROWER",
                complainantPhone = "+919834567890",
                targetPartyId = USER_DEVESH,
                targetPartyName = "Devesh Singh",
                targetPartyRole = "LENDER",
                loanId = LOAN_SUMAN_AGRI,
                category = "DELAYED_PAYMENT",
                priority = "LOW",
                subject = "Wheat Harvest Mandi Procurement Grace Extension",
                description = "Requested 5-day grace period for Installment #4 due to mandi procurement schedule delay.",
                status = "RESOLVED",
                resolutionNotes = "Lender Devesh Singh approved 5-day grace extension under zero penalty clause. Amicably settled.",
                resolvedAt = now - (60 * oneDayMs),
                createdAt = now - (62 * oneDayMs)
            )
            database.complaintDao().insertComplaint(complaint)

            val mediationMeeting = MediationMeetingEntity(
                meetingId = "meet_suman_devesh_amicable",
                title = "Consensual Repayment Alignment Meeting",
                agenda = "Review mandi harvest schedule and confirm 5-day extension without penalty.",
                loanId = LOAN_SUMAN_AGRI,
                complaintId = "complaint_suman_reschedule",
                borrowerId = USER_SUMAN,
                borrowerName = "Suman Yadav",
                borrowerPhone = "+919834567890",
                lenderId = USER_DEVESH,
                lenderName = "Devesh Singh",
                lenderPhone = "+919876512340",
                agentId = USER_ROMESH,
                agentName = "Romesh Singh",
                meetingType = "GOOGLE_MEET",
                meetingLinkOrLocation = "https://meet.google.com/loa-nzo-med",
                scheduledDateTime = now - (60 * oneDayMs),
                scheduledTimeSlotStr = "Completed, 04:00 PM - 04:30 PM",
                status = "COMPLETED",
                adminNotes = "Both parties reached mutual consensus. Grace period applied.",
                createdAt = now - (62 * oneDayMs)
            )
            database.mediationMeetingDao().insertMeeting(mediationMeeting)

            // =========================================================================
            // 13. SEED DOCUMENT VAULT ENTITIES & PHYSICAL PDFS
            // =========================================================================
            val vaultDir = File(context.filesDir, "vault_documents").apply { mkdirs() }
            val generatedDocs = try {
                DemoDocumentGenerator.generateDemoVaultDocuments(context, vaultDir)
            } catch (e: Exception) {
                Log.w(TAG, "DemoDocumentGenerator completed with partial: ${e.message}")
                emptyMap()
            }

            fun getOrCreateVaultPdf(fileName: String, sampleKey: String): File {
                val target = File(vaultDir, fileName)
                if (!target.exists() || target.length() == 0L) {
                    val sample = generatedDocs[sampleKey]?.first
                    if (sample != null && sample.exists()) {
                        sample.copyTo(target, overwrite = true)
                    } else {
                        target.writeBytes("LOANZO SECURE VAULT DOCUMENT: $fileName\nTamper-evident legal record under Section 65B Indian Evidence Act.\nDigital Certificate ID: LZ-CERT-2026".toByteArray())
                    }
                }
                return target
            }

            val deveshDossier = getOrCreateVaultPdf("Dossier_Devesh_Lending_2026.pdf", "Dossier_Devesh_Lending_2026.pdf")
            val deveshSanction = getOrCreateVaultPdf("Sanction_Shivam_MSME_100K.pdf", "Sanction_Letter_Kumar_50k.pdf")
            val deveshAgreement = getOrCreateVaultPdf("Agreement_Shivam_Devesh_100K.pdf", "Agreement_Shivam_Devesh_100K.pdf")
            val deveshNoc = getOrCreateVaultPdf("Golden_NOC_Suman_Devesh.pdf", "Golden_NOC_Suman_Devesh.pdf")
            val deveshTds = getOrCreateVaultPdf("Form16A_TDS_Devesh_2026.pdf", "Form16A_TDS_Devesh_2026.pdf")
            val deveshTdsQ1 = getOrCreateVaultPdf("Form16A_TDS_Devesh_FY2526_Q1.pdf", "Form16A_TDS_Devesh_FY2526_Q1.pdf")
            val deveshTdsQ2 = getOrCreateVaultPdf("Form16A_TDS_Devesh_FY2526_Q2.pdf", "Form16A_TDS_Devesh_FY2526_Q2.pdf")
            val deveshTdsQ3 = getOrCreateVaultPdf("Form16A_TDS_Devesh_FY2526_Q3.pdf", "Form16A_TDS_Devesh_FY2526_Q3.pdf")

            val romeshAgentCert = getOrCreateVaultPdf("Field_Officer_Credential_Romesh.pdf", "Sanction_LZ_EDU_25K.pdf")
            val romeshGuarantee = getOrCreateVaultPdf("Joint_Obligor_Guarantee_Romesh.pdf", "Agreement_Kumar_Manish.pdf")
            val romeshAgreement = getOrCreateVaultPdf("Agreement_Romesh_Precision_Tech.pdf", "Agreement_LZ_EDU_2026.pdf")
            val romeshProtocol = getOrCreateVaultPdf("Custody_Intake_Protocol_Noida.pdf", "Sanction_LZ_BIZ_50K.pdf")

            val shivamKyc = getOrCreateVaultPdf("DigiLocker_KYC_Dossier_Shivam.pdf", "Sanction_Letter_Prince_75k.pdf")
            val shivamAgreement = getOrCreateVaultPdf("Promissory_Note_Agreement_Shivam.pdf", "Agreement_Prince_Sharma.pdf")
            val shivamReceipt1 = getOrCreateVaultPdf("Disbursement_Tranche1_BharatTex.pdf", "Receipt_EMI1_LZ_BIZ_50K.pdf")
            val shivamGoldCert = getOrCreateVaultPdf("Gold_Assay_Valuation_Certificate.pdf", "Sanction_LZ_BIZ_50K.pdf")
            val shivamEmi3 = getOrCreateVaultPdf("Receipt_EMI3_Shivam_MSME.pdf", "Receipt_EMI1_LZ_BIZ_50K.pdf")

            val sumanGoldenNoc = getOrCreateVaultPdf("Golden_NOC_Certificate_Suman.pdf", "NOC_LZ_MED_15K.pdf")
            val sumanEnwrRelease = getOrCreateVaultPdf("WDRA_ENWR_Lien_Discharge_Suman.pdf", "Sanction_Letter_Kumar_50k.pdf")
            val sumanClosure = getOrCreateVaultPdf("Loan_Full_Closure_Certificate_Suman.pdf", "Receipt_EMI1_LZ_BIZ_50K.pdf")
            val sumanStatement = getOrCreateVaultPdf("Full_Repayment_Statement_Suman.pdf", "Agreement_LZ_BIZ_2026.pdf")

            val vaultDocs = listOf(
                // Devesh Singh Documents
                VaultDocumentEntity(
                    documentId = "doc_devesh_master_dossier",
                    userId = USER_DEVESH,
                    title = "Master P2P Creditor Dossier & Audit Certificate 2026",
                    documentType = "FINANCIAL_DOSSIER",
                    fileName = "Dossier_Devesh_Lending_2026.pdf",
                    filePath = deveshDossier.absolutePath,
                    fileSizeBytes = deveshDossier.length().coerceAtLeast(184200L),
                    checksumSha256 = "b84f3c8a912d091e4572ac419de01b2238129031efac8920194bcda891230491",
                    isEncrypted = true,
                    description = "Consolidated credit exposure, capital reserve allocation, and verified return ledger.",
                    generatedAt = now - (30 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_sanction_shivam",
                    userId = USER_DEVESH,
                    loanId = LOAN_SHIVAM_MSME,
                    title = "Official Sanction Letter - Shivam Yadav MSME (₹1,00,000)",
                    documentType = "SANCTION_LETTER",
                    fileName = "Sanction_Shivam_MSME_100K.pdf",
                    filePath = deveshSanction.absolutePath,
                    fileSizeBytes = deveshSanction.length().coerceAtLeast(145200L),
                    checksumSha256 = "e102938475610293847561029384756102938475610293847561029384756102",
                    isEncrypted = true,
                    description = "Loan sanction advice detailing interest rate, escrow milestones, and collateral lock.",
                    generatedAt = now - (44 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_agreement_shivam",
                    userId = USER_DEVESH,
                    loanId = LOAN_SHIVAM_MSME,
                    title = "Tripartite E-Contract & Section 4 Promissory Note",
                    documentType = "LOAN_AGREEMENT",
                    fileName = "Agreement_Shivam_Devesh_100K.pdf",
                    filePath = deveshAgreement.absolutePath,
                    fileSizeBytes = deveshAgreement.length().coerceAtLeast(210500L),
                    checksumSha256 = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4",
                    isEncrypted = true,
                    description = "Digitally executed contract under Information Technology Act, 2000 and Section 128 ICA.",
                    generatedAt = now - (44 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_noc_suman",
                    userId = USER_DEVESH,
                    loanId = LOAN_SUMAN_AGRI,
                    title = "Golden NOC Discharge Counterpart - Suman Yadav Agri Loan",
                    documentType = "NOC_CERTIFICATE",
                    fileName = "Golden_NOC_Suman_Devesh.pdf",
                    filePath = deveshNoc.absolutePath,
                    fileSizeBytes = deveshNoc.length().coerceAtLeast(168900L),
                    checksumSha256 = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
                    isEncrypted = true,
                    description = "Lien discharge acknowledgment confirming full principal and interest recovery.",
                    generatedAt = now - (10 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_tds_statement",
                    userId = USER_DEVESH,
                    title = "Quarterly TDS Credit Certificate (Form 16A)",
                    documentType = "ACCOUNT_STATEMENT",
                    fileName = "Form16A_TDS_Devesh_2026.pdf",
                    filePath = deveshTds.absolutePath,
                    fileSizeBytes = deveshTds.length().coerceAtLeast(95400L),
                    checksumSha256 = "c019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Annual consolidated tax deducted at source statement under Section 194A Income Tax Act.",
                    generatedAt = now - (15 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_tds_q1",
                    userId = USER_DEVESH,
                    title = "Form 16A TDS Statement • Q1 FY25-26 (Apr - Jun)",
                    documentType = "ACCOUNT_STATEMENT",
                    fileName = "Form16A_TDS_Devesh_FY2526_Q1.pdf",
                    filePath = deveshTdsQ1.absolutePath,
                    fileSizeBytes = deveshTdsQ1.length().coerceAtLeast(84200L),
                    checksumSha256 = "9019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Quarter 1 TDS deduction certificate: ₹840 TDS deposited under Challan #90218 (BSR 0210042).",
                    generatedAt = now - (75 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_tds_q2",
                    userId = USER_DEVESH,
                    title = "Form 16A TDS Statement • Q2 FY25-26 (Jul - Sep)",
                    documentType = "ACCOUNT_STATEMENT",
                    fileName = "Form16A_TDS_Devesh_FY2526_Q2.pdf",
                    filePath = deveshTdsQ2.absolutePath,
                    fileSizeBytes = deveshTdsQ2.length().coerceAtLeast(85100L),
                    checksumSha256 = "9145083746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Quarter 2 TDS deduction certificate: ₹1,120 TDS deposited under Challan #91450 (BSR 0210042).",
                    generatedAt = now - (45 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_tds_q3",
                    userId = USER_DEVESH,
                    title = "Form 16A TDS Statement • Q3 FY25-26 (Oct - Dec)",
                    documentType = "ACCOUNT_STATEMENT",
                    fileName = "Form16A_TDS_Devesh_FY2526_Q3.pdf",
                    filePath = deveshTdsQ3.absolutePath,
                    fileSizeBytes = deveshTdsQ3.length().coerceAtLeast(86300L),
                    checksumSha256 = "9283183746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Quarter 3 TDS deduction certificate: ₹1,285 TDS deposited under Challan #92831 (BSR 0210042).",
                    generatedAt = now - (15 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_devesh_agreement_romesh",
                    userId = USER_DEVESH,
                    loanId = LOAN_ROMESH_TECH,
                    title = "Toolkit Credit Agreement - Romesh Singh Precision (₹35,000)",
                    documentType = "LOAN_AGREEMENT",
                    fileName = "Agreement_Romesh_Precision_Tech.pdf",
                    filePath = romeshAgreement.absolutePath,
                    fileSizeBytes = romeshAgreement.length().coerceAtLeast(194500L),
                    checksumSha256 = "f019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Direct vendor escrow agreement with dual digital signatures and field officer KYC credentials.",
                    generatedAt = now - (64 * oneDayMs)
                ),

                // Romesh Singh Documents
                VaultDocumentEntity(
                    documentId = "doc_romesh_agent_cert",
                    userId = USER_ROMESH,
                    title = "Certified Field Inspection Officer Credential & ID",
                    documentType = "FINANCIAL_DOSSIER",
                    fileName = "Field_Officer_Credential_Romesh.pdf",
                    filePath = romeshAgentCert.absolutePath,
                    fileSizeBytes = romeshAgentCert.length().coerceAtLeast(132400L),
                    checksumSha256 = "a918273645019283746501928374650192837465019283746501928374650192",
                    isEncrypted = true,
                    description = "Verified field officer accreditation, background verification, and identity credential.",
                    generatedAt = now - (83 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_romesh_coborrower_guarantee",
                    userId = USER_ROMESH,
                    loanId = LOAN_SHIVAM_MSME,
                    title = "Joint Obligor Guarantee Agreement (Section 128 ICA)",
                    documentType = "LOAN_AGREEMENT",
                    fileName = "Joint_Obligor_Guarantee_Romesh.pdf",
                    filePath = romeshGuarantee.absolutePath,
                    fileSizeBytes = romeshGuarantee.length().coerceAtLeast(188300L),
                    checksumSha256 = "d872193840192837465019283746501928374650192837465019283746501928",
                    isEncrypted = true,
                    description = "Co-borrower legal covenant executed on Shivam Yadav's ₹1,00,000 working capital facility.",
                    generatedAt = now - (44 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_romesh_tech_agreement",
                    userId = USER_ROMESH,
                    loanId = LOAN_ROMESH_TECH,
                    title = "Toolkit Loan Agreement - Precision Instruments",
                    documentType = "LOAN_AGREEMENT",
                    fileName = "Agreement_Romesh_Precision_Tech.pdf",
                    filePath = romeshAgreement.absolutePath,
                    fileSizeBytes = romeshAgreement.length().coerceAtLeast(194500L),
                    checksumSha256 = "f019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Field toolkit purchase contract with direct vendor escrow disbursement to supplier.",
                    generatedAt = now - (64 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_romesh_custody_protocol",
                    userId = USER_ROMESH,
                    title = "Dual-Officer Custody Intake Protocol - Noida Vault",
                    documentType = "FINANCIAL_DOSSIER",
                    fileName = "Custody_Intake_Protocol_Noida.pdf",
                    filePath = romeshProtocol.absolutePath,
                    fileSizeBytes = romeshProtocol.length().coerceAtLeast(141200L),
                    checksumSha256 = "e910293847561029384756102938475610293847561029384756102938475610",
                    isEncrypted = true,
                    description = "Chain of custody and sealed locker intake documentation for physical assets.",
                    generatedAt = now - (44 * oneDayMs)
                ),

                // Shivam Yadav Documents
                VaultDocumentEntity(
                    documentId = "doc_shivam_kyc_dossier",
                    userId = USER_SHIVAM,
                    title = "DigiLocker Government Verified KYC Dossier",
                    documentType = "FINANCIAL_DOSSIER",
                    fileName = "DigiLocker_KYC_Dossier_Shivam.pdf",
                    filePath = shivamKyc.absolutePath,
                    fileSizeBytes = shivamKyc.length().coerceAtLeast(156700L),
                    checksumSha256 = "9182736450192837465019283746501928374650192837465019283746501928",
                    isEncrypted = true,
                    description = "Statutory Aadhaar and PAN verification snapshot from UIDAI and NSDL DigiLocker gateways.",
                    generatedAt = now - (79 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_shivam_promissory_agreement",
                    userId = USER_SHIVAM,
                    loanId = LOAN_SHIVAM_MSME,
                    title = "Section 4 Promissory Note & E-Contract - MSME 100K",
                    documentType = "LOAN_AGREEMENT",
                    fileName = "Promissory_Note_Agreement_Shivam.pdf",
                    filePath = shivamAgreement.absolutePath,
                    fileSizeBytes = shivamAgreement.length().coerceAtLeast(214800L),
                    checksumSha256 = "4019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Binding peer-to-peer digital credit agreement with OTP authentication and timestamp.",
                    generatedAt = now - (44 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_shivam_ocr_invoice_tranche1",
                    userId = USER_SHIVAM,
                    loanId = LOAN_SHIVAM_MSME,
                    title = "Milestone 1 Vendor Disbursement Voucher & Invoice OCR",
                    documentType = "PAYMENT_RECEIPT",
                    fileName = "Disbursement_Tranche1_BharatTex.pdf",
                    filePath = shivamReceipt1.absolutePath,
                    fileSizeBytes = shivamReceipt1.length().coerceAtLeast(112400L),
                    checksumSha256 = "2910293847561029384756102938475610293847561029384756102938475610",
                    isEncrypted = true,
                    description = "Escrow release proof: ₹50,000 disbursed directly to Bharat Tex Fabrics Ltd via UPI.",
                    generatedAt = now - (42 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_shivam_gold_valuation_cert",
                    userId = USER_SHIVAM,
                    loanId = LOAN_SHIVAM_MSME,
                    title = "Certified 22K Gold Assayer Appraisal Certificate (30g)",
                    documentType = "FINANCIAL_DOSSIER",
                    fileName = "Gold_Assay_Valuation_Certificate.pdf",
                    filePath = shivamGoldCert.absolutePath,
                    fileSizeBytes = shivamGoldCert.length().coerceAtLeast(139800L),
                    checksumSha256 = "6019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Independent hallmarked assay report validating 22 Karat gold ornaments purity and weight.",
                    generatedAt = now - (43 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_shivam_emi_receipt_03",
                    userId = USER_SHIVAM,
                    loanId = LOAN_SHIVAM_MSME,
                    title = "EMI Payment Receipt #3 (₹9,200 UPI Settled)",
                    documentType = "PAYMENT_RECEIPT",
                    fileName = "Receipt_EMI3_Shivam_MSME.pdf",
                    filePath = shivamEmi3.absolutePath,
                    fileSizeBytes = shivamEmi3.length().coerceAtLeast(88400L),
                    checksumSha256 = "5019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "UPI transaction receipt for installment #3. Principal: ₹8,329, Interest: ₹871.",
                    generatedAt = now - (1 * oneDayMs)
                ),

                // Suman Yadav Documents
                VaultDocumentEntity(
                    documentId = "doc_suman_golden_noc_cert",
                    userId = USER_SUMAN,
                    loanId = LOAN_SUMAN_AGRI,
                    title = "Official Golden NOC Certificate (Debt-Free Seal)",
                    documentType = "NOC_CERTIFICATE",
                    fileName = "Golden_NOC_Certificate_Suman.pdf",
                    filePath = sumanGoldenNoc.absolutePath,
                    fileSizeBytes = sumanGoldenNoc.length().coerceAtLeast(174200L),
                    checksumSha256 = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
                    isEncrypted = true,
                    description = "Statutory debt discharge certificate confirming 100% full loan repayment with zero liability.",
                    generatedAt = now - (10 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_suman_enwr_lien_discharge",
                    userId = USER_SUMAN,
                    loanId = LOAN_SUMAN_AGRI,
                    title = "WDRA e-NWR Electronic Warehouse Lien Discharge",
                    documentType = "FINANCIAL_DOSSIER",
                    fileName = "WDRA_ENWR_Lien_Discharge_Suman.pdf",
                    filePath = sumanEnwrRelease.absolutePath,
                    fileSizeBytes = sumanEnwrRelease.length().coerceAtLeast(151000L),
                    checksumSha256 = "3019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Lien removal certification registered on WDRA electronic repository for 200Q wheat.",
                    generatedAt = now - (10 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_suman_loan_closure_cert",
                    userId = USER_SUMAN,
                    loanId = LOAN_SUMAN_AGRI,
                    title = "Agricultural Micro-Credit Full Closure Certificate",
                    documentType = "ACCOUNT_STATEMENT",
                    fileName = "Loan_Full_Closure_Certificate_Suman.pdf",
                    filePath = sumanClosure.absolutePath,
                    fileSizeBytes = sumanClosure.length().coerceAtLeast(102400L),
                    checksumSha256 = "1019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Formal account closure advice issued by Loanzo P2P Ledger.",
                    generatedAt = now - (10 * oneDayMs)
                ),
                VaultDocumentEntity(
                    documentId = "doc_suman_full_repayment_stmt",
                    userId = USER_SUMAN,
                    loanId = LOAN_SUMAN_AGRI,
                    title = "Complete 6-Month On-Time Repayment Statement",
                    documentType = "ACCOUNT_STATEMENT",
                    fileName = "Full_Repayment_Statement_Suman.pdf",
                    filePath = sumanStatement.absolutePath,
                    fileSizeBytes = sumanStatement.length().coerceAtLeast(118500L),
                    checksumSha256 = "8019283746501928374650192837465019283746501928374650192837465019",
                    isEncrypted = true,
                    description = "Comprehensive ledger of all 6 installments cleared via UPI with timestamps and UTRs.",
                    generatedAt = now - (10 * oneDayMs)
                )
            )

            for (doc in vaultDocs) {
                database.vaultDocumentDao().insertDocument(doc)
            }

            // =========================================================================
            // 14. SEED NOTIFICATIONS FOR EACH ROLE
            // =========================================================================
            val notifications = listOf(
                // For Devesh (Lender)
                NotificationEntity(
                    notificationId = "notif_devesh_emi_shivam_03",
                    userId = USER_DEVESH,
                    title = "Repayment Received (₹9,200) • Shivam Yadav",
                    message = "Shivam Yadav paid EMI #3 of ₹9,200 via UPI. Principal credited: ₹8,329, Interest earned: ₹871. Outstanding: ₹43,400.",
                    type = "REPAYMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (1 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_bid_received_vikram",
                    userId = USER_DEVESH,
                    title = "Marketplace Syndicate Bid Accepted",
                    message = "Your syndicate loan bid of ₹60,000 for Vikram Gupta's Banarasi Silk Yarn project was accepted.",
                    type = "SYSTEM",
                    timestamp = now - (12 * 3600_000L),
                    isRead = false
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_vouch_suman",
                    userId = USER_DEVESH,
                    title = "Community Peer Vouch Received 🌟",
                    message = "Suman Yadav published a glowing 5-star peer vouch on your creditor profile endorsing your fair lending and grace policy.",
                    type = "SYSTEM",
                    timestamp = now - (2 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_tranche_requisition",
                    userId = USER_DEVESH,
                    title = "Tranche 2 Requisition Submitted",
                    message = "Shivam Yadav submitted Milestone 2 fabric supplier invoice (#TX-9014) for ₹50,000 direct payment verification.",
                    type = "DISBURSEMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (3 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_romesh_repay",
                    userId = USER_DEVESH,
                    title = "Repayment Received (₹3,150) • Romesh Singh",
                    message = "Romesh Singh paid EMI #2 of ₹3,150 via UPI for Field Equipment loan. Principal: ₹2,855, Interest: ₹295.",
                    type = "REPAYMENT",
                    relatedLoanId = LOAN_ROMESH_TECH,
                    timestamp = now - (5 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_golden_noc_issued",
                    userId = USER_DEVESH,
                    title = "Loan Facility Completed • Golden NOC Issued 🏆",
                    message = "Suman Yadav has settled all 6 installments (₹52,625 recovered). Golden NOC and WDRA lien discharge executed.",
                    type = "AGREEMENT",
                    relatedLoanId = LOAN_SUMAN_AGRI,
                    timestamp = now - (10 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_tds_q3_ready",
                    userId = USER_DEVESH,
                    title = "Quarterly Form 16A TDS Statement Ready (Q3)",
                    message = "Your Q3 FY25-26 TDS Credit Statement under Section 194A (₹1,285 tax deposited under Challan #92831) is available in your Document Vault.",
                    type = "SYSTEM",
                    timestamp = now - (15 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_emi_shivam_02",
                    userId = USER_DEVESH,
                    title = "Repayment Received (₹9,200) • Shivam Yadav",
                    message = "Shivam Yadav paid EMI #2 of ₹9,200 via UPI. Principal credited: ₹8,243, Interest earned: ₹957.",
                    type = "REPAYMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (15 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_emi_shivam_01",
                    userId = USER_DEVESH,
                    title = "Repayment Received (₹9,200) • Shivam Yadav",
                    message = "Shivam Yadav paid EMI #1 of ₹9,200 via UPI. Principal credited: ₹8,158, Interest earned: ₹1,042.",
                    type = "REPAYMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (30 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_tds_q2_ready",
                    userId = USER_DEVESH,
                    title = "Quarterly Form 16A TDS Statement Ready (Q2)",
                    message = "Your Q2 FY25-26 TDS Credit Statement under Section 194A (₹1,120 tax deposited under Challan #91450) is available in your Document Vault.",
                    type = "SYSTEM",
                    timestamp = now - (45 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_devesh_vouch_romesh",
                    userId = USER_DEVESH,
                    title = "Officer Peer Vouch Received 🛡️",
                    message = "Senior Field Officer Romesh Singh endorsed your syndicate pool for zero-harassment and 100% compliant escrow practices.",
                    type = "SYSTEM",
                    timestamp = now - (2 * oneDayMs),
                    isRead = true
                ),
                // For Shivam (Borrower)
                NotificationEntity(
                    notificationId = "notif_shivam_tranche_disbursed",
                    userId = USER_SHIVAM,
                    title = "Purpose-Bound Tranche Disbursed",
                    message = "Milestone 1 tranche of ₹50,000 was disbursed directly to Bharat Tex Fabrics Ltd via UPI (0% cash leakage).",
                    type = "DISBURSEMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (42 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_shivam_emi_success",
                    userId = USER_SHIVAM,
                    title = "EMI Payment Cleared (₹9,200) ✓",
                    message = "Your EMI #3 of ₹9,200 was successfully settled. Outstanding principal reduced to ₹43,400.",
                    type = "REPAYMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (1 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_shivam_coborrower_attached",
                    userId = USER_SHIVAM,
                    title = "Co-Borrower Verified",
                    message = "Romesh Singh has accepted Section 128 Joint Obligor terms for your loan.",
                    type = "AGREEMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (44 * oneDayMs),
                    isRead = true
                ),
                // For Romesh (Co-Borrower, Guarantor & Field Agent)
                NotificationEntity(
                    notificationId = "notif_romesh_inspection_payout",
                    userId = USER_ROMESH,
                    title = "Field Verification Fee Credited (₹1,600)",
                    message = "Payout of ₹1,600 credited for physical inspection and 22K gold assay of Shivam's workshop.",
                    type = "SYSTEM",
                    timestamp = now - (43 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_romesh_coborrower_linked",
                    userId = USER_ROMESH,
                    title = "Co-Borrower Agreement Executed",
                    message = "You are registered as Joint Obligor (Section 128 ICA) on Shivam Yadav's ₹1,00,000 MSME loan.",
                    type = "AGREEMENT",
                    relatedLoanId = LOAN_SHIVAM_MSME,
                    timestamp = now - (44 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_romesh_tech_loan_active",
                    userId = USER_ROMESH,
                    title = "Equipment Loan Active & Disbursed",
                    message = "Your ₹35,000 toolkit loan was disbursed to Precision Instruments India. EMI #2 settled.",
                    type = "DISBURSEMENT",
                    relatedLoanId = LOAN_ROMESH_TECH,
                    timestamp = now - (63 * oneDayMs),
                    isRead = true
                ),
                // For Suman (Completed Loan Borrower)
                NotificationEntity(
                    notificationId = "notif_suman_golden_noc",
                    userId = USER_SUMAN,
                    title = "Golden NOC Certificate Issued 🏆",
                    message = "Congratulations! Your Solar Irrigation loan is 100% repaid. Your Golden NOC Certificate is available in your vault.",
                    type = "SYSTEM",
                    relatedLoanId = LOAN_SUMAN_AGRI,
                    timestamp = now - (10 * oneDayMs),
                    isRead = false
                ),
                NotificationEntity(
                    notificationId = "notif_suman_bid_received",
                    userId = USER_SUMAN,
                    title = "New Syndicate Bid Received",
                    message = "Devesh Singh placed a bid of ₹75,000 @ 11.5% APR on your Dairy Bulk Milk Chiller post.",
                    type = "SYSTEM",
                    timestamp = now - (1 * oneDayMs),
                    isRead = true
                ),
                NotificationEntity(
                    notificationId = "notif_suman_enwr_discharged",
                    userId = USER_SUMAN,
                    title = "WDRA Warehouse Lien Released",
                    message = "Lien on 200 Quintals Wheat has been officially removed on the WDRA repository.",
                    type = "SYSTEM",
                    relatedLoanId = LOAN_SUMAN_AGRI,
                    timestamp = now - (10 * oneDayMs),
                    isRead = true
                )
            )
            for (n in notifications) {
                database.notificationDao().insertNotification(n)
            }

            // =========================================================================
            // 15. SEED AUDIT TRAIL LOGS (Section 65B Indian Evidence Act Admissibility)
            // =========================================================================
            val auditEvents = listOf(
                AuditEventEntity(
                    eventId = "audit_shivam_01",
                    entityType = "LOAN",
                    entityId = LOAN_SHIVAM_MSME,
                    actor = USER_SHIVAM,
                    event = "CREATED",
                    newState = "CONTRACT_SIGNING",
                    description = "Loan application initiated with 22K Gold & IT equipment collateral. Co-borrower Romesh Singh attached.",
                    timestamp = now - (45 * oneDayMs)
                ),
                AuditEventEntity(
                    eventId = "audit_shivam_02",
                    entityType = "LOAN",
                    entityId = LOAN_SHIVAM_MSME,
                    actor = USER_DEVESH,
                    event = "APPROVED",
                    newState = "ACTIVE_SERVICING",
                    description = "Devesh Singh approved loan terms and signed digital contract. Section 4 Promissory Note generated.",
                    timestamp = now - (44 * oneDayMs)
                ),
                AuditEventEntity(
                    eventId = "audit_shivam_03",
                    entityType = "DISBURSEMENT",
                    entityId = "disb_shivam_tranche_1",
                    actor = USER_DEVESH,
                    event = "DISBURSED",
                    newState = "APPROVED",
                    description = "Tranche 1 (₹50,000) released to Bharat Tex Fabrics Ltd UPI (supplier.textiles@okaxis). Zero cash leakage.",
                    timestamp = now - (42 * oneDayMs)
                ),
                AuditEventEntity(
                    eventId = "audit_suman_noc",
                    entityType = "LOAN",
                    entityId = LOAN_SUMAN_AGRI,
                    actor = "SYSTEM-NOC-ENGINE",
                    event = "STATUS_CHANGED",
                    oldState = "ACTIVE_SERVICING",
                    newState = "COMPLETED",
                    description = "Final EMI cleared. Automated Golden NOC Certificate #noc_suman_agri_50k generated with digital seal.",
                    timestamp = now - (10 * oneDayMs)
                ),
                AuditEventEntity(
                    eventId = "audit_romesh_tech_01",
                    entityType = "LOAN",
                    entityId = LOAN_ROMESH_TECH,
                    actor = USER_ROMESH,
                    event = "DISBURSED",
                    newState = "ACTIVE_SERVICING",
                    description = "Field toolkit loan disbursed directly to Precision Instruments India Ltd UPI. Pledged optical rig.",
                    timestamp = now - (63 * oneDayMs)
                ),
                AuditEventEntity(
                    eventId = "audit_dual_inspection_01",
                    entityType = "VERIFICATION",
                    entityId = "visit_romesh_shivam_gold",
                    actor = USER_ROMESH,
                    event = "VERIFIED",
                    newState = "COMPLETED",
                    description = "Dual-officer primary physical assay completed by Officer Romesh Singh. 22K gold authenticity confirmed.",
                    timestamp = now - (43 * oneDayMs)
                )
            )
            for (a in auditEvents) {
                database.auditEventDao().insertEvent(a)
            }

            // =========================================================================
            // 16. SEED FIELD AGENT OPERATIONS & DOORSTEP OTP VISITS FOR AGENT SURYA YADAV
            // =========================================================================
            val agentApplicationSurya = AgentApplicationEntity(
                applicationId = "app_agent_surya_01",
                userId = USER_SHIVAM,
                applicantName = "Surya Yadav",
                applicantPhone = "+919812345678",
                applicantEmail = "surya111@loanzo.app",
                experienceYears = "3-5 Years",
                priorDomain = "Gold Loan Valuer & NBFC",
                policeVerificationNumber = "PV-UP-VAR-99420",
                policeStation = "Chowk Police Station, Varanasi",
                policeVerificationDate = "15/01/2026",
                permanentAddress = "Weavers Colony, Chowk, Varanasi, Uttar Pradesh 221001",
                operatingCity = "Varanasi / Noida NCR",
                operatingPincode = "221001",
                serviceRadiusKm = 15,
                vehicleType = "Two-Wheeler",
                drivingLicenseNumber = "UP65-2018-0098412",
                status = "APPROVED",
                submittedAt = now - (80 * oneDayMs),
                reviewedAt = now - (75 * oneDayMs),
                adminRemarks = "Verified biometric credentials and gold assayer certification. Field operations authorized."
            )
            database.agentDao().insertApplication(agentApplicationSurya)

            val suryaVisits = listOf(
                // Visit 1: Active In-Progress Inspection for Romesh Singh's Field Toolkit
                AgentVisitEntity(
                    visitId = "visit_surya_01",
                    agentId = USER_SHIVAM,
                    loanId = LOAN_ROMESH_TECH,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Equipment & Optical Spectrometer Appraisal",
                    borrowerName = "Romesh Singh",
                    borrowerPhone = "+919823456789",
                    borrowerAddress = "Flat 402, Sector 18, Noida, Uttar Pradesh 201301",
                    lenderName = "Devesh Singh",
                    lenderPhone = "+919876512340",
                    lenderAddress = "Plot 24, Sector 62, Noida, Uttar Pradesh 201309",
                    targetAddress = "Flat 402, Sector 18, Noida, Uttar Pradesh 201301",
                    targetLatitude = 28.5708,
                    targetLongitude = 77.3260,
                    scheduledDate = "Today",
                    scheduledTimeSlot = "10:30 AM - 11:30 AM",
                    payoutAmount = 450.0,
                    collateralItemName = "Optical Spectrometer & Digital Density Scale",
                    collateralEstimatedValue = 50000.0,
                    collateralPledgedValue = 35000.0,
                    status = "SCHEDULED",
                    visitStageStatus = "EN_ROUTE",
                    handshakePin = "8821",
                    isHandshakePinVerified = false,
                    assignedAgentName = "Surya Yadav",
                    agentPhone = "+919812345678",
                    distanceKm = 2.4,
                    createdAt = now - (1 * oneDayMs)
                ),
                // Visit 2: Scheduled Borrower & Farmland Inspection for Suman Yadav
                AgentVisitEntity(
                    visitId = "visit_surya_02",
                    agentId = USER_SHIVAM,
                    loanId = LOAN_SUMAN_AGRI,
                    visitType = "BORROWER_VERIFICATION",
                    title = "Solar Chiller Installation & Farmland Verification",
                    borrowerName = "Suman Yadav",
                    borrowerPhone = "+919834567890",
                    borrowerAddress = "Krishi Vikas Kendra, Barabanki, Uttar Pradesh 225001",
                    lenderName = "Devesh Singh",
                    lenderPhone = "+919876512340",
                    lenderAddress = "Plot 24, Sector 62, Noida, Uttar Pradesh 201309",
                    targetAddress = "Krishi Vikas Kendra, Barabanki, Uttar Pradesh 225001",
                    targetLatitude = 26.9272,
                    targetLongitude = 81.1822,
                    scheduledDate = "Today",
                    scheduledTimeSlot = "02:00 PM - 03:00 PM",
                    payoutAmount = 350.0,
                    collateralItemName = "Bulk Milk Chiller Solar Rig",
                    collateralEstimatedValue = 75000.0,
                    collateralPledgedValue = 50000.0,
                    status = "SCHEDULED",
                    visitStageStatus = "SCHEDULED",
                    handshakePin = "4190",
                    isHandshakePinVerified = false,
                    assignedAgentName = "Surya Yadav",
                    agentPhone = "+919812345678",
                    distanceKm = 5.8,
                    createdAt = now - (1 * oneDayMs)
                ),
                // Visit 3: Attested / Completed Gold Hallmark Verification
                AgentVisitEntity(
                    visitId = "visit_surya_03",
                    agentId = USER_SHIVAM,
                    loanId = LOAN_SHIVAM_MSME,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "22K Hallmarked Gold Ornaments (30g) Assay",
                    borrowerName = "Surya Yadav",
                    borrowerPhone = "+919812345678",
                    borrowerAddress = "Weavers Colony, Chowk, Varanasi, Uttar Pradesh 221001",
                    lenderName = "Devesh Singh",
                    lenderPhone = "+919876512340",
                    lenderAddress = "Plot 24, Sector 62, Noida, Uttar Pradesh 201309",
                    targetAddress = "Loanzo Vault Facility, Sector 62, Noida, Uttar Pradesh 201309",
                    targetLatitude = 28.6280,
                    targetLongitude = 77.3649,
                    scheduledDate = "Today",
                    scheduledTimeSlot = "09:00 AM - 10:00 AM",
                    payoutAmount = 500.0,
                    collateralItemName = "22K Hallmarked Gold Ornaments (30g)",
                    collateralEstimatedValue = 150000.0,
                    collateralPledgedValue = 100000.0,
                    status = "COMPLETED",
                    visitStageStatus = "COMPLETED",
                    agentRemarks = "Physical hallmarking verified. Acid test and specific gravity passed. 22K (91.6% pure gold) confirmed.",
                    isCollateralAuthentic = true,
                    isBorrowerIdentityVerified = true,
                    isLenderIdentityVerified = true,
                    handshakePin = "7721",
                    isHandshakePinVerified = true,
                    appraisedValue = 150000.0,
                    officerRecommendation = "RECOMMEND_APPROVAL",
                    assignedAgentName = "Surya Yadav",
                    agentPhone = "+919812345678",
                    completedAt = now - (2 * 3600_000L),
                    createdAt = now - (2 * oneDayMs)
                ),
                // Visit 4: Tomorrow's Scheduled Custody Intake Visit
                AgentVisitEntity(
                    visitId = "visit_surya_04",
                    agentId = USER_SHIVAM,
                    loanId = LOAN_ROMESH_TECH,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Central Locker Intake Verification",
                    borrowerName = "Romesh Singh",
                    borrowerPhone = "+919823456789",
                    borrowerAddress = "Flat 402, Sector 18, Noida, Uttar Pradesh 201301",
                    lenderName = "Devesh Singh",
                    lenderPhone = "+919876512340",
                    lenderAddress = "Plot 24, Sector 62, Noida, Uttar Pradesh 201309",
                    targetAddress = "Central Vault Facility, Sector 62, Noida",
                    targetLatitude = 28.6280,
                    targetLongitude = 77.3649,
                    scheduledDate = "Tomorrow",
                    scheduledTimeSlot = "11:00 AM - 12:00 PM",
                    payoutAmount = 600.0,
                    collateralItemName = "Secondary Security Bag Verification",
                    collateralEstimatedValue = 40000.0,
                    status = "SCHEDULED",
                    visitStageStatus = "SCHEDULED",
                    handshakePin = "9023",
                    isHandshakePinVerified = false,
                    assignedAgentName = "Surya Yadav",
                    agentPhone = "+919812345678",
                    distanceKm = 3.1,
                    createdAt = now - (1 * oneDayMs)
                ),
                // Visit 5: Unassigned Dispatches in Master Admin Queue
                AgentVisitEntity(
                    visitId = "visit_unassigned_noida",
                    agentId = "UNASSIGNED",
                    loanId = LOAN_ROMESH_TECH,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Commercial Hardware & Inventory Check",
                    borrowerName = "Romesh Singh",
                    borrowerPhone = "+919823456789",
                    borrowerAddress = "Sector 62, Noida, Uttar Pradesh",
                    lenderName = "Devesh Singh",
                    lenderPhone = "+919876512340",
                    lenderAddress = "Plot 24, Sector 62, Noida, Uttar Pradesh 201309",
                    targetAddress = "Industrial Area, Phase 3, Noida",
                    targetLatitude = 28.6139,
                    targetLongitude = 77.3588,
                    scheduledDate = "Today",
                    scheduledTimeSlot = "04:30 PM - 05:30 PM",
                    payoutAmount = 400.0,
                    collateralItemName = "Commercial Invoice & Electronic Equipment",
                    collateralEstimatedValue = 35000.0,
                    status = "SCHEDULED",
                    visitStageStatus = "SCHEDULED",
                    handshakePin = "6201",
                    isHandshakePinVerified = false,
                    distanceKm = 4.2,
                    createdAt = now
                )
            )
            database.agentDao().insertVisits(suryaVisits)
            }

            Log.i(TAG, "Presentation accounts and multi-role demonstration dataset seeded successfully across all modules.")
            Result.success("Presentation accounts seeded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed presentation accounts: ${e.message}", e)
            Result.failure(e)
        }
    }
}
