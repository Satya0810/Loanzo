package com.loanzo.app.data

import android.util.Log
import com.loanzo.app.data.entity.*
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.util.TelegramManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.loanzo.app.util.pdf.DemoDocumentGenerator
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LoanzoDatabase,
    private val userRepository: UserRepository,
    private val telegramManager: TelegramManager
) {
    companion object {
        private const val TAG = "DemoDataSeeder"

        // Role-specific Demo Accounts
        const val DEMO_ADMIN_ACCOUNT_ID = "demo_admin_satyam"
        const val DEMO_AGENT_ACCOUNT_ID = "demo_agent_vikas"
        const val DEMO_USER_ACCOUNT_ID = "demo_user_arjun"

        // Counterparty IDs
        const val DEMO_BORROWER_ID = "demo_borrower_rahul"
        const val DEMO_LENDER_ID = "demo_lender_priya"
        const val DEMO_AMIT_ID = "demo_amit_verma"
        const val DEMO_SNEHA_ID = "demo_sneha_roy"
        const val DEMO_RAJESH_ID = "demo_rajesh_gupta"
        const val DEMO_VIKRAM_ID = "demo_vikram_malhotra"
        const val DEMO_GUARANTOR_NIRMALA_ID = "demo_guarantor_nirmala"
        const val DEMO_COBORROWER_ROHAN_ID = "demo_coborrower_rohan"

        // Consumer Loans
        const val DEMO_LOAN_LENT_ID = "loan_demo_lent_50k"
        const val DEMO_LOAN_BORROWED_ID = "loan_demo_borrowed_25k"
        const val DEMO_LOAN_CLOSED_ID = "loan_demo_closed_15k"

        // Platform Loans (for Admin & Network)
        const val DEMO_LOAN_PLATFORM_1 = "loan_demo_biz_150k"
        const val DEMO_LOAN_PLATFORM_2 = "loan_demo_gadget_80k"
        const val DEMO_LOAN_PLATFORM_3 = "loan_demo_super_200k"
    }

    /**
     * Seeds comprehensive demo data for the specified active user across Member, Agent, and Admin roles.
     */
    suspend fun seedAllDemoData(currentUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val oneDayMs = 86_400_000L

            // 1. Upgrade current user to Fully Verified KYC, Bank Account & Agent Status
            val currentUser = database.userDao().getUserById(currentUserId)
            if (currentUser != null) {
                val verifiedUser = currentUser.copy(
                    kycStatus = "VERIFIED",
                    aadhaarVerified = true,
                    panVerified = true,
                    bankVerified = true,
                    bankAccountNumber = currentUser.bankAccountNumber.ifBlank { "5010049281928" },
                    bankIfsc = currentUser.bankIfsc.ifBlank { "HDFC0001234" },
                    panNumber = currentUser.panNumber.ifBlank { "ABCDE1234F" },
                    aadhaarNumber = currentUser.aadhaarNumber.ifBlank { "9876 5432 1098" },
                    agentStatus = "APPROVED",
                    isOnDuty = true,
                    totalAgentEarnings = if (currentUser.totalAgentEarnings > 0.0) currentUser.totalAgentEarnings else 4250.0
                )
                database.userDao().updateUser(verifiedUser)
            }

            // 2. Seed all global counterparties, platform loans, and role records
            seedGlobalEntities(currentUserId, now, oneDayMs)

            // 3. Mark demo quest completed
            userRepository.markQuestStepDone(UserRepository.QUEST_DEMO_SEEDED)

            // 4. Send Telegram notification to admin
            try {
                telegramManager.sendAdminAlert(
                    """
                    <b>Demo Data Seeded Successfully</b>

                    <b>Users:</b> 10 (Admin, Agent, Member, Counterparties)
                    <b>Loans:</b> 6 (3 Active, 1 Closed, 2 Platform)
                    <b>Repayments:</b> 14 EMIs (10 Paid, 2 Scheduled, 1 Overdue, 1 Partial)
                    <b>Disbursements:</b> 5 verified transactions
                    <b>Guarantors:</b> 2 (Nirmala Devi, Vikram Malhotra)
                    <b>Marketplace:</b> 3 posts with co-borrowers + 5 vouchers
                    <b>Agent Visits:</b> 3 (1 Completed, 1 Scheduled, 1 Rescheduled)
                    <b>Complaints:</b> 3 | Mediations: 2
                    <b>Vault Documents:</b> 6 | NOC: 1
                    <b>Notifications:</b> 10 lifecycle alerts
                    <b>Audit Trail:</b> 8 events

                    Seeded by: <code>${currentUserId}</code>
                    """.trimIndent()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Telegram notification failed (non-blocking): ${e.message}")
            }

            Result.success("Demo data successfully pushed across Member, Field Agent, and Master Admin views!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Seeds baseline demo data on app startup so that whether the user logs in as
     * Member, Field Agent, or Master Admin, all role-specific views are pre-populated.
     */
    suspend fun seedGlobalDemoData(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val oneDayMs = 86_400_000L
            seedGlobalEntities(currentUserId = null, now = now, oneDayMs = oneDayMs)
            Result.success("Global demo data initialized successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedGlobalEntities(currentUserId: String?, now: Long, oneDayMs: Long) {
        val targetAgentId = currentUserId ?: DEMO_AGENT_ACCOUNT_ID
        val targetAdminId = currentUserId ?: DEMO_ADMIN_ACCOUNT_ID
        val targetConsumerId = currentUserId ?: DEMO_USER_ACCOUNT_ID

        // ==========================================
        // 1. SEED DEMO USERS (Counterparties & Pre-configured Accounts)
        // ==========================================
        val demoUsers = listOf(
            // Master Admin Account
            UserEntity(
                userId = DEMO_ADMIN_ACCOUNT_ID,
                name = "Satyam Kumar",
                email = "satyam@loanzo.app",
                phone = "+91 70615 59039",
                username = "satyam0810",
                password = "password123",
                role = "ADMIN",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "5010049281928",
                bankIfsc = "HDFC0001234",
                panNumber = "ADMKR7892L",
                aadhaarNumber = "4532 1098 7654",
                upiId = "satyam0810@okhdfc",
                dateOfBirth = "08/10/2003",
                address = "B-204, Prateek Laurel, Sector 120, Noida, UP 201301",
                agentStatus = "APPROVED",
                isOnDuty = true,
                totalAgentEarnings = 8500.0
            ),
            // Dedicated Field Agent Account
            UserEntity(
                userId = DEMO_AGENT_ACCOUNT_ID,
                name = "Vikas Sharma (Field Agent)",
                email = "vikas.agent@loanzo.app",
                phone = "+91 98100 12345",
                username = "agent_demo",
                password = "password123",
                role = "AGENT",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "3094829104821",
                bankIfsc = "SBIN0001122",
                panNumber = "BKPVS4521R",
                aadhaarNumber = "3219 8765 4321",
                upiId = "vikas.agent@oksbi",
                dateOfBirth = "14/03/1992",
                address = "C-42, Sector 18, Noida, UP 201301",
                agentStatus = "APPROVED",
                isOnDuty = true,
                totalAgentEarnings = 4250.0
            ),
            // Dedicated Member Account
            UserEntity(
                userId = DEMO_USER_ACCOUNT_ID,
                name = "Arjun Mehta (Member)",
                email = "arjun.mehta@demo.loanzo.app",
                phone = "+91 98200 54321",
                username = "user_demo",
                password = "password123",
                role = "USER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "6029384719284",
                bankIfsc = "KKBK0004921",
                panNumber = "CMPAM2109K",
                aadhaarNumber = "7654 3210 9876",
                upiId = "arjun.mehta@okkotak",
                dateOfBirth = "22/07/1995",
                address = "Flat 12B, Hiranandani Gardens, Powai, Mumbai 400076"
            ),
            // Rahul Sharma (Borrower Counterparty)
            UserEntity(
                userId = DEMO_BORROWER_ID,
                name = "Rahul Sharma",
                email = "rahul.sharma@demo.loanzo.app",
                phone = "+91 98765 43210",
                username = "rahul_sharma",
                password = "password123",
                role = "BORROWER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "9182736451029",
                bankIfsc = "SBIN0004521",
                panNumber = "DKPRS8876M",
                aadhaarNumber = "5678 1234 9012",
                upiId = "rahul.sharma@oksbi",
                dateOfBirth = "15/11/1993",
                address = "Flat 402, Lotus Boulevard, Sector 100, Noida, UP 201304"
            ),
            // Priya Patel (Lender Counterparty)
            UserEntity(
                userId = DEMO_LENDER_ID,
                name = "Priya Patel",
                email = "priya.patel@demo.loanzo.app",
                phone = "+91 91234 56789",
                username = "priya_invest",
                password = "password123",
                role = "LENDER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "1029384756102",
                bankIfsc = "ICIC0002938",
                panNumber = "EKPPP6543N",
                aadhaarNumber = "8901 2345 6789",
                upiId = "priya.invest@okicici",
                dateOfBirth = "03/04/1990",
                address = "A-7, Indirapuram, Ghaziabad, UP 201014"
            ),
            // Amit Verma (MSME Retailer)
            UserEntity(
                userId = DEMO_AMIT_ID,
                name = "Amit Verma",
                email = "amit.verma@demo.loanzo.app",
                phone = "+91 98111 22334",
                username = "amit_retail",
                password = "password123",
                role = "BORROWER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "4019283746501",
                bankIfsc = "PUNB0012340",
                panNumber = "FKPAV3210Q",
                aadhaarNumber = "1234 5678 9012",
                upiId = "amit.verma@okpnb",
                dateOfBirth = "28/09/1988",
                address = "Shop 14, Karol Bagh Market, New Delhi 110005"
            ),
            // Sneha Roy (Graphic Designer)
            UserEntity(
                userId = DEMO_SNEHA_ID,
                name = "Sneha Roy",
                email = "sneha.roy@demo.loanzo.app",
                phone = "+91 99887 76655",
                username = "sneha_design",
                password = "password123",
                role = "BORROWER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "7019283746502",
                bankIfsc = "UTIB0000123",
                panNumber = "GMPSR7654P",
                aadhaarNumber = "3456 7890 1234",
                upiId = "sneha.roy@okaxis",
                dateOfBirth = "12/06/1996",
                address = "305, Koramangala 4th Block, Bengaluru, KA 560034"
            ),
            // Rajesh Gupta (HNI Lender)
            UserEntity(
                userId = DEMO_RAJESH_ID,
                name = "Rajesh Gupta",
                email = "rajesh.gupta@demo.loanzo.app",
                phone = "+91 98765 00112",
                username = "rajesh_capital",
                password = "password123",
                role = "LENDER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "8192837465012",
                bankIfsc = "HDFC0000456",
                panNumber = "HKPRG9988R",
                aadhaarNumber = "6789 0123 4567",
                upiId = "rajesh.capital@okhdfc",
                dateOfBirth = "07/02/1978",
                address = "DLF Phase 3, Gurgaon, Haryana 122002"
            ),
            // Vikram Malhotra (Angel Investor)
            UserEntity(
                userId = DEMO_VIKRAM_ID,
                name = "Vikram Malhotra",
                email = "vikram.malhotra@demo.loanzo.app",
                phone = "+91 98450 11223",
                username = "vikram_angel",
                password = "password123",
                role = "LENDER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "2019283746503",
                bankIfsc = "YESB0000789",
                panNumber = "JMPVM1122S",
                aadhaarNumber = "9012 3456 7890",
                upiId = "vikram.angel@okyesbank",
                dateOfBirth = "19/12/1985",
                address = "Embassy Golf Links, Domlur, Bengaluru, KA 560071"
            ),
            // Nirmala Devi (Guarantor)
            UserEntity(
                userId = DEMO_GUARANTOR_NIRMALA_ID,
                name = "Nirmala Devi",
                email = "nirmala.devi@demo.loanzo.app",
                phone = "+91 99100 33445",
                username = "nirmala_devi",
                password = "password123",
                role = "USER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "1192837465504",
                bankIfsc = "SBIN0009876",
                panNumber = "KMPND5566T",
                aadhaarNumber = "2345 6789 0123",
                upiId = "nirmala.devi@oksbi",
                dateOfBirth = "30/01/1968",
                address = "House 18, Sector 15, Noida, UP 201301"
            ),
            // Dr. Rohan Patil (Co-Borrower)
            UserEntity(
                userId = DEMO_COBORROWER_ROHAN_ID,
                name = "Dr. Rohan Patil",
                email = "rohan.patil@demo.loanzo.app",
                phone = "+91 98765 88990",
                username = "dr_rohan_patil",
                password = "password123",
                role = "USER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "5592837465605",
                bankIfsc = "HDFC0007654",
                panNumber = "LMPRP7788U",
                aadhaarNumber = "4567 8901 2345",
                upiId = "dr.rohan@okhdfc",
                dateOfBirth = "05/08/1991",
                address = "B-22, Koregaon Park, Pune, MH 411001"
            )
        )
        demoUsers.forEach { user ->
            database.userDao().insertUser(user)
        }

        // ==========================================
        // 2. SEED DEMO LOANS (Consumer + Platform)
        // ==========================================
        val allDemoLoans = listOf(
            // Loan Lent: Current User -> Rahul Sharma (50K, 6 Months)
            LoanEntity(
                loanId = DEMO_LOAN_LENT_ID,
                lenderId = targetConsumerId,
                borrowerId = DEMO_BORROWER_ID,
                sanctionedAmount = 50000.0,
                disbursedAmount = 50000.0,
                outstandingAmount = 41666.0,
                purpose = "Inventory Stock Expansion for Retail Shop",
                loanType = "BUSINESS",
                interestRate = 12.0,
                interestModel = "SIMPLE",
                tenureMonths = 6,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (30 * oneDayMs),
                notes = "Collateral: 24K Gold Necklace & Bangles (48.5g). Secured in Loanzo Central Vault.",
                isAgreementSigned = true,
                lenderSignedAt = now - (30 * oneDayMs),
                borrowerSignedAt = now - (30 * oneDayMs),
                agreementPdfUrl = File(File(context.filesDir, "vault_documents"), "Agreement_LZ_BIZ_2026.pdf").absolutePath
            ),
            // Loan Borrowed: Priya Patel -> Current User (25K, 12 Months)
            LoanEntity(
                loanId = DEMO_LOAN_BORROWED_ID,
                lenderId = DEMO_LENDER_ID,
                borrowerId = targetConsumerId,
                sanctionedAmount = 25000.0,
                disbursedAmount = 25000.0,
                outstandingAmount = 22650.0,
                purpose = "Professional Certification & Online Course Fee",
                loanType = "EDUCATION",
                interestRate = 10.5,
                interestModel = "SIMPLE",
                tenureMonths = 12,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (60 * oneDayMs),
                notes = "Guarantor: Nirmala Devi (Mother). Income proof: 3 months salary slips verified.",
                isAgreementSigned = true,
                lenderSignedAt = now - (60 * oneDayMs),
                borrowerSignedAt = now - (60 * oneDayMs),
                agreementPdfUrl = File(File(context.filesDir, "vault_documents"), "Agreement_LZ_EDU_2026.pdf").absolutePath
            ),
            // Closed Loan: Current User -> Rahul Sharma (15K, Fully Repaid)
            LoanEntity(
                loanId = DEMO_LOAN_CLOSED_ID,
                lenderId = targetConsumerId,
                borrowerId = DEMO_BORROWER_ID,
                sanctionedAmount = 15000.0,
                disbursedAmount = 15000.0,
                outstandingAmount = 0.0,
                purpose = "Medical Emergency Fund - Post-Surgery Recovery",
                loanType = "MEDICAL",
                interestRate = 11.0,
                interestModel = "SIMPLE",
                tenureMonths = 4,
                status = "CLOSED",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (180 * oneDayMs),
                closedAt = now - (120 * oneDayMs),
                notes = "Fully repaid ahead of schedule. NOC issued. Collateral released.",
                isAgreementSigned = true,
                lenderSignedAt = now - (180 * oneDayMs),
                borrowerSignedAt = now - (180 * oneDayMs),
                agreementPdfUrl = File(File(context.filesDir, "vault_documents"), "NOC_LZ_MED_15K.pdf").absolutePath
            ),
            // Platform Loan 1: Vikram Malhotra -> Amit Verma (1,50,000, 12 Months, Active)
            LoanEntity(
                loanId = DEMO_LOAN_PLATFORM_1,
                lenderId = DEMO_VIKRAM_ID,
                borrowerId = DEMO_AMIT_ID,
                sanctionedAmount = 150000.0,
                disbursedAmount = 150000.0,
                outstandingAmount = 125000.0,
                purpose = "CNC Lathe Machinery Upgrade",
                loanType = "BUSINESS",
                interestRate = 11.5,
                interestModel = "SIMPLE",
                tenureMonths = 12,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (45 * oneDayMs),
                notes = "Secured against original commercial property deed in central vault. Guarantor: Vikram Malhotra.",
                isAgreementSigned = true,
                lenderSignedAt = now - (45 * oneDayMs),
                borrowerSignedAt = now - (45 * oneDayMs),
                agreementPdfUrl = "loanzo://vault/agreement_biz_150k.pdf"
            ),
            // Platform Loan 2: Rajesh Gupta -> Sneha Roy (80,000, 8 Months, Active)
            LoanEntity(
                loanId = DEMO_LOAN_PLATFORM_2,
                lenderId = DEMO_RAJESH_ID,
                borrowerId = DEMO_SNEHA_ID,
                sanctionedAmount = 80000.0,
                disbursedAmount = 80000.0,
                outstandingAmount = 60000.0,
                purpose = "Studio Production Workstation & Audio Gear",
                loanType = "EQUIPMENT",
                interestRate = 12.0,
                interestModel = "SIMPLE",
                tenureMonths = 8,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (90 * oneDayMs),
                notes = "Hardware encumbered in platform collateral registry. MacBook Pro M2 Max serial tagged.",
                isAgreementSigned = true,
                lenderSignedAt = now - (90 * oneDayMs),
                borrowerSignedAt = now - (90 * oneDayMs),
                agreementPdfUrl = "loanzo://vault/agreement_gadget_80k.pdf"
            ),
            // Platform Loan 3: Rajesh Gupta -> Rahul Sharma (2,00,000, 24 Months, Active)
            LoanEntity(
                loanId = DEMO_LOAN_PLATFORM_3,
                lenderId = DEMO_RAJESH_ID,
                borrowerId = DEMO_BORROWER_ID,
                sanctionedAmount = 200000.0,
                disbursedAmount = 200000.0,
                outstandingAmount = 188000.0,
                purpose = "Warehouse Lease Commercial Expansion",
                loanType = "BUSINESS",
                interestRate = 10.0,
                interestModel = "SIMPLE",
                tenureMonths = 24,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (60 * oneDayMs),
                notes = "Escrow backed peer-to-peer verified institutional loan. Property deed pledged.",
                isAgreementSigned = true,
                lenderSignedAt = now - (60 * oneDayMs),
                borrowerSignedAt = now - (60 * oneDayMs),
                agreementPdfUrl = "loanzo://vault/agreement_super_200k.pdf"
            )
        )
        allDemoLoans.forEach { loan ->
            database.loanDao().insertLoan(loan)
        }

        // ==========================================
        // 3. SEED PAYEES (for disbursement foreign keys)
        // ==========================================
        val demoPayees = listOf(
            PayeeEntity(
                payeeId = "payee_rahul_shop",
                name = "Rahul Sharma - Retail Shop",
                upiId = "rahul.sharma@oksbi",
                businessName = "Sharma General Store",
                verificationStatus = "VERIFIED",
                category = "GROCERY",
                verifiedAt = now - (30 * oneDayMs),
                addedBy = targetConsumerId
            ),
            PayeeEntity(
                payeeId = "payee_coursera_edu",
                name = "Coursera Inc. - Education",
                upiId = "",
                businessName = "Coursera Professional Certificates",
                verificationStatus = "VERIFIED",
                category = "EDUCATION",
                verifiedAt = now - (60 * oneDayMs),
                addedBy = targetConsumerId
            ),
            PayeeEntity(
                payeeId = "payee_hospital_med",
                name = "Max Super Speciality Hospital",
                upiId = "maxhospital@hdfcbank",
                businessName = "Max Healthcare Institute Ltd",
                gstNumber = "07AAACM4928B1ZP",
                verificationStatus = "VERIFIED",
                category = "HOSPITAL",
                verifiedAt = now - (180 * oneDayMs),
                addedBy = targetConsumerId
            ),
            PayeeEntity(
                payeeId = "payee_amit_cnc",
                name = "Amit Verma - CNC Machinery",
                upiId = "amit.verma@okpnb",
                businessName = "Verma Precision Engineering",
                gstNumber = "07AAIPV8812K1ZF",
                verificationStatus = "VERIFIED",
                category = "OTHER",
                verifiedAt = now - (45 * oneDayMs),
                addedBy = DEMO_VIKRAM_ID
            ),
            PayeeEntity(
                payeeId = "payee_sneha_studio",
                name = "Sneha Roy - Studio Equipment",
                upiId = "sneha.roy@okaxis",
                businessName = "Roy Creative Studio",
                verificationStatus = "VERIFIED",
                category = "ELECTRONICS",
                verifiedAt = now - (90 * oneDayMs),
                addedBy = DEMO_RAJESH_ID
            )
        )
        demoPayees.forEach { database.payeeDao().insertPayee(it) }

        // ==========================================
        // 4. SEED DISBURSEMENTS
        // ==========================================
        val demoDisbursements = listOf(
            DisbursementEntity(
                disbursementId = "disb_demo_lent_1",
                loanId = DEMO_LOAN_LENT_ID,
                amount = 50000.0,
                payeeId = "payee_rahul_shop",
                payeeName = "Rahul Sharma - Retail Shop",
                purpose = "Inventory Stock Expansion for Retail Shop",
                purposeCategory = "BUSINESS",
                verificationStatus = "VERIFIED",
                ruleEngineResult = "CONSISTENT",
                approvalStatus = "APPROVED",
                transactionRef = "UPI/429381028491/HDFC",
                timestamp = now - (29 * oneDayMs),
                lenderNote = "Disbursed via UPI. Verified borrower bank details.",
                borrowerNote = "Received full amount. Stock procurement initiated."
            ),
            DisbursementEntity(
                disbursementId = "disb_demo_borrowed_1",
                loanId = DEMO_LOAN_BORROWED_ID,
                amount = 25000.0,
                payeeId = "payee_coursera_edu",
                payeeName = "Coursera Inc. - Education",
                purpose = "Professional Certification & Online Course Fee",
                purposeCategory = "EDUCATION",
                verificationStatus = "VERIFIED",
                ruleEngineResult = "CONSISTENT",
                approvalStatus = "APPROVED",
                transactionRef = "NEFT/N091827364512/ICIC",
                timestamp = now - (59 * oneDayMs),
                lenderNote = "Education loan disbursed. Course enrollment confirmed.",
                borrowerNote = "Course fee paid. Starting AWS Solutions Architect prep."
            ),
            DisbursementEntity(
                disbursementId = "disb_demo_closed_1",
                loanId = DEMO_LOAN_CLOSED_ID,
                amount = 15000.0,
                payeeId = "payee_hospital_med",
                payeeName = "Max Super Speciality Hospital",
                purpose = "Medical Emergency - Post-Surgery Recovery",
                purposeCategory = "MEDICAL",
                verificationStatus = "VERIFIED",
                ruleEngineResult = "CONSISTENT",
                approvalStatus = "APPROVED",
                transactionRef = "IMPS/802918374651/SBIN",
                timestamp = now - (179 * oneDayMs),
                lenderNote = "Emergency medical disbursement. Hospital bill verified.",
                borrowerNote = "Hospital discharge completed. Recovery in progress."
            ),
            DisbursementEntity(
                disbursementId = "disb_demo_platform1_1",
                loanId = DEMO_LOAN_PLATFORM_1,
                amount = 150000.0,
                payeeId = "payee_amit_cnc",
                payeeName = "Amit Verma - CNC Machinery",
                purpose = "CNC Lathe Machinery Upgrade",
                purposeCategory = "BUSINESS",
                verificationStatus = "VERIFIED",
                ruleEngineResult = "CONSISTENT",
                approvalStatus = "APPROVED",
                transactionRef = "RTGS/R701928374651/YESB",
                timestamp = now - (44 * oneDayMs),
                lenderNote = "RTGS disbursement for CNC machine purchase. Invoice validated.",
                borrowerNote = "Machine ordered from Jyoti CNC. Delivery in 2 weeks."
            ),
            DisbursementEntity(
                disbursementId = "disb_demo_platform2_1",
                loanId = DEMO_LOAN_PLATFORM_2,
                amount = 80000.0,
                payeeId = "payee_sneha_studio",
                payeeName = "Sneha Roy - Studio Equipment",
                purpose = "Studio Production Workstation & Audio Gear",
                purposeCategory = "OTHER",
                verificationStatus = "VERIFIED",
                ruleEngineResult = "CONSISTENT",
                approvalStatus = "APPROVED",
                transactionRef = "NEFT/N601928374652/HDFC",
                timestamp = now - (89 * oneDayMs),
                lenderNote = "Equipment loan disbursed. Apple invoice serial matched.",
                borrowerNote = "MacBook Pro M2 Max received. Studio setup complete."
            )
        )
        demoDisbursements.forEach { database.disbursementDao().insertDisbursement(it) }

        // ==========================================
        // 5. SEED GUARANTORS
        // ==========================================
        val demoGuarantors = listOf(
            GuarantorEntity(
                guarantorId = "guar_demo_nirmala_1",
                loanId = DEMO_LOAN_BORROWED_ID,
                name = "Nirmala Devi",
                phone = "+91 99100 33445",
                email = "nirmala.devi@demo.loanzo.app",
                panNumber = "KMPND5566T",
                relationship = "PARENT",
                consentStatus = "ACCEPTED",
                consentTimestamp = now - (59 * oneDayMs),
                createdAt = now - (60 * oneDayMs)
            ),
            GuarantorEntity(
                guarantorId = "guar_demo_vikram_1",
                loanId = DEMO_LOAN_PLATFORM_1,
                name = "Vikram Malhotra",
                phone = "+91 98450 11223",
                email = "vikram.malhotra@demo.loanzo.app",
                panNumber = "JMPVM1122S",
                relationship = "BUSINESS_PARTNER",
                consentStatus = "ACCEPTED",
                consentTimestamp = now - (44 * oneDayMs),
                createdAt = now - (45 * oneDayMs)
            )
        )
        demoGuarantors.forEach { database.guarantorDao().insertGuarantor(it) }

        // ==========================================
        // 6. SEED REPAYMENT SCHEDULES & TRANSACTIONS (14 EMIs)
        // ==========================================
        val demoRepayments = listOf(
            // -- Loan Lent (50K, 6 months, 12% simple) EMI ~ 8,834 --
            RepaymentEntity(
                repaymentId = "repay_demo_lent_1",
                loanId = DEMO_LOAN_LENT_ID,
                amount = 8834.0,
                transactionRef = "UPI/329481928491",
                status = "PAID",
                dueDate = now - (25 * oneDayMs),
                paidDate = now - (25 * oneDayMs),
                outstandingSnapshot = 41666.0,
                principalComponent = 8334.0,
                interestComponent = 500.0,
                note = "EMI #1 paid on time via UPI"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_lent_2",
                loanId = DEMO_LOAN_LENT_ID,
                amount = 8834.0,
                transactionRef = "",
                status = "SCHEDULED",
                dueDate = now + (5 * oneDayMs),
                paidDate = null,
                outstandingSnapshot = 33332.0,
                principalComponent = 8334.0,
                interestComponent = 500.0,
                note = "EMI #2 upcoming"
            ),
            // -- Loan Borrowed (25K, 12 months, 10.5%) EMI ~ 2,297 --
            RepaymentEntity(
                repaymentId = "repay_demo_borrowed_1",
                loanId = DEMO_LOAN_BORROWED_ID,
                amount = 2297.0,
                transactionRef = "IMPS/901827364512",
                status = "PAID",
                dueDate = now - (55 * oneDayMs),
                paidDate = now - (55 * oneDayMs),
                outstandingSnapshot = 22917.0,
                principalComponent = 2083.0,
                interestComponent = 214.0,
                note = "EMI #1 paid via IMPS"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_borrowed_2",
                loanId = DEMO_LOAN_BORROWED_ID,
                amount = 2297.0,
                transactionRef = "UPI/801928374651",
                status = "PAID",
                dueDate = now - (25 * oneDayMs),
                paidDate = now - (24 * oneDayMs),
                outstandingSnapshot = 20834.0,
                principalComponent = 2083.0,
                interestComponent = 214.0,
                note = "EMI #2 paid 1 day late (within grace)"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_borrowed_3",
                loanId = DEMO_LOAN_BORROWED_ID,
                amount = 2297.0,
                transactionRef = "",
                status = "SCHEDULED",
                dueDate = now + (5 * oneDayMs),
                paidDate = null,
                outstandingSnapshot = 18751.0,
                principalComponent = 2083.0,
                interestComponent = 214.0,
                note = "EMI #3 upcoming"
            ),
            // -- Platform Loan 1 (1.5L, 12 months, 11.5%) EMI ~ 13,938 --
            RepaymentEntity(
                repaymentId = "repay_demo_platform_1",
                loanId = DEMO_LOAN_PLATFORM_1,
                amount = 13938.0,
                transactionRef = "NEFT/8019283741",
                status = "PAID",
                dueDate = now - (15 * oneDayMs),
                paidDate = now - (15 * oneDayMs),
                outstandingSnapshot = 137500.0,
                principalComponent = 12500.0,
                interestComponent = 1438.0,
                note = "EMI #1 paid on time via NEFT"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_platform_1b",
                loanId = DEMO_LOAN_PLATFORM_1,
                amount = 13938.0,
                transactionRef = "",
                status = "SCHEDULED",
                dueDate = now + (15 * oneDayMs),
                paidDate = null,
                outstandingSnapshot = 125000.0,
                principalComponent = 12500.0,
                interestComponent = 1438.0,
                note = "EMI #2 upcoming"
            ),
            // -- Platform Loan 2 (80K, 8 months, 12%) EMI ~ 10,800 --
            RepaymentEntity(
                repaymentId = "repay_demo_platform2_1",
                loanId = DEMO_LOAN_PLATFORM_2,
                amount = 10800.0,
                transactionRef = "UPI/710928374651",
                status = "PAID",
                dueDate = now - (60 * oneDayMs),
                paidDate = now - (60 * oneDayMs),
                outstandingSnapshot = 70000.0,
                principalComponent = 10000.0,
                interestComponent = 800.0,
                note = "EMI #1 paid on time"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_platform2_2",
                loanId = DEMO_LOAN_PLATFORM_2,
                amount = 10800.0,
                transactionRef = "UPI/710928374652",
                status = "PAID",
                dueDate = now - (30 * oneDayMs),
                paidDate = now - (28 * oneDayMs),
                outstandingSnapshot = 60000.0,
                principalComponent = 10000.0,
                interestComponent = 800.0,
                note = "EMI #2 paid 2 days late (within grace)"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_platform2_3",
                loanId = DEMO_LOAN_PLATFORM_2,
                amount = 10800.0,
                transactionRef = "PARTIAL/710928374653",
                status = "OVERDUE",
                dueDate = now - (2 * oneDayMs),
                paidDate = null,
                outstandingSnapshot = 50000.0,
                principalComponent = 10000.0,
                interestComponent = 800.0,
                penalty = 150.0,
                note = "EMI #3 overdue by 2 days. Penalty of Rs 150 applied."
            ),
            // -- Platform Loan 3 (2L, 24 months, 10%) EMI ~ 9,167 --
            RepaymentEntity(
                repaymentId = "repay_demo_platform3_1",
                loanId = DEMO_LOAN_PLATFORM_3,
                amount = 9167.0,
                transactionRef = "NEFT/9019283741",
                status = "PAID",
                dueDate = now - (30 * oneDayMs),
                paidDate = now - (30 * oneDayMs),
                outstandingSnapshot = 192500.0,
                principalComponent = 8334.0,
                interestComponent = 833.0,
                note = "EMI #1 paid on time via NEFT"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_platform3_2",
                loanId = DEMO_LOAN_PLATFORM_3,
                amount = 9167.0,
                transactionRef = "",
                status = "SCHEDULED",
                dueDate = now + (1 * oneDayMs),
                paidDate = null,
                outstandingSnapshot = 184166.0,
                principalComponent = 8334.0,
                interestComponent = 833.0,
                note = "EMI #2 due tomorrow"
            ),
            // -- Closed Loan (15K, 4 months) - All 4 EMIs PAID --
            RepaymentEntity(
                repaymentId = "repay_demo_closed_1",
                loanId = DEMO_LOAN_CLOSED_ID,
                amount = 4163.0,
                transactionRef = "UPI/501928374651",
                status = "PAID",
                dueDate = now - (170 * oneDayMs),
                paidDate = now - (170 * oneDayMs),
                outstandingSnapshot = 11250.0,
                principalComponent = 3750.0,
                interestComponent = 413.0,
                note = "EMI #1 - Medical loan"
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_closed_2",
                loanId = DEMO_LOAN_CLOSED_ID,
                amount = 11663.0,
                transactionRef = "NEFT/501928374652",
                status = "PAID",
                dueDate = now - (140 * oneDayMs),
                paidDate = now - (125 * oneDayMs),
                outstandingSnapshot = 0.0,
                principalComponent = 11250.0,
                interestComponent = 413.0,
                note = "Final lump-sum prepayment. Loan closed early."
            )
        )
        demoRepayments.forEach { database.repaymentDao().insertRepayment(it) }

        // ==========================================
        // 7. SEED FIELD AGENT DOORSTEP INSPECTION VISITS
        // ==========================================
        val agentApp = AgentApplicationEntity(
            applicationId = "app_agent_demo_${targetAgentId}",
            userId = targetAgentId,
            applicantName = "Vikas Sharma",
            applicantPhone = "+91 98100 12345",
            applicantEmail = "agent@loanzo.app",
            experienceYears = "3-5 Years",
            priorDomain = "Gold Loan Valuer",
            policeVerificationNumber = "PV/DEL/2023/88921",
            policeStation = "Connaught Place Police Station",
            policeVerificationDate = "15/01/2024",
            permanentAddress = "C-42, Sector 18, Noida",
            operatingCity = "Delhi NCR",
            operatingPincode = "110001",
            vehicleType = "Two-Wheeler",
            drivingLicenseNumber = "DL-0420110023456",
            status = "APPROVED",
            reviewedAt = now - (7 * oneDayMs)
        )
        database.agentDao().insertApplication(agentApp)

        val agentIdsToSeed = listOfNotNull(targetAgentId, currentUserId).distinct()
        agentIdsToSeed.forEach { agentId ->
            val demoVisits = listOf(
                AgentVisitEntity(
                    visitId = "visit_demo_gold_${agentId}",
                    agentId = agentId,
                    loanId = DEMO_LOAN_LENT_ID,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Gold Collateral Physical Verification",
                    borrowerName = "Rahul Sharma",
                    borrowerPhone = "+91 98765 43210",
                    borrowerAddress = "Flat 402, Lotus Boulevard, Sector 100, Noida, UP",
                    lenderName = "Priya Patel",
                    lenderPhone = "+91 91234 56789",
                    lenderAddress = "A-7, Indirapuram, Ghaziabad, UP",
                    targetAddress = "Flat 402, Lotus Boulevard, Sector 100, Noida, UP",
                    targetLatitude = 28.5355,
                    targetLongitude = 77.3910,
                    scheduledDate = "Today",
                    scheduledTimeSlot = "11:30 AM - 01:00 PM",
                    payoutAmount = 750.0,
                    collateralItemName = "24K Hallmarked Gold Necklace & Bangles (48.5g)",
                    collateralEstimatedValue = 285000.0,
                    collateralPledgedValue = 50000.0,
                    status = "SCHEDULED",
                    agentRemarks = ""
                ),
                AgentVisitEntity(
                    visitId = "visit_demo_premise_${agentId}",
                    agentId = agentId,
                    loanId = DEMO_LOAN_PLATFORM_1,
                    visitType = "PREMISE_INSPECTION",
                    title = "Business Premise & Machinery Verification",
                    borrowerName = "Amit Verma",
                    borrowerPhone = "+91 98111 22334",
                    borrowerAddress = "Shop 14, Karol Bagh Market, New Delhi 110005",
                    lenderName = "Vikram Malhotra",
                    lenderPhone = "+91 98450 11223",
                    lenderAddress = "Embassy Golf Links, Domlur, Bengaluru",
                    targetAddress = "Shop 14, Karol Bagh Market, New Delhi 110005",
                    targetLatitude = 28.6519,
                    targetLongitude = 77.1905,
                    scheduledDate = "Tomorrow",
                    scheduledTimeSlot = "02:00 PM - 03:30 PM",
                    payoutAmount = 1200.0,
                    collateralItemName = "CNC Lathe Machine (Jyoti VMC-640)",
                    collateralEstimatedValue = 1850000.0,
                    collateralPledgedValue = 150000.0,
                    status = "COMPLETED",
                    agentRemarks = "Machinery verified on-site. Serial numbers matched. Photos uploaded. Business operational with 5 employees.",
                    completedAt = now - (2 * oneDayMs),
                    isBorrowerIdentityVerified = true,
                    isCollateralAuthentic = true
                ),
                AgentVisitEntity(
                    visitId = "visit_demo_studio_${agentId}",
                    agentId = agentId,
                    loanId = DEMO_LOAN_PLATFORM_2,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Studio Equipment Inventory Check",
                    borrowerName = "Sneha Roy",
                    borrowerPhone = "+91 99887 76655",
                    borrowerAddress = "305, Koramangala 4th Block, Bengaluru, KA 560034",
                    lenderName = "Rajesh Gupta",
                    lenderPhone = "+91 98765 00112",
                    lenderAddress = "DLF Phase 3, Gurgaon, Haryana 122002",
                    targetAddress = "305, Koramangala 4th Block, Bengaluru, KA 560034",
                    targetLatitude = 12.9352,
                    targetLongitude = 77.6245,
                    scheduledDate = "Next Monday",
                    scheduledTimeSlot = "10:00 AM - 11:30 AM",
                    payoutAmount = 950.0,
                    collateralItemName = "Apple MacBook Pro 16\" M2 Max (C02G4109MD6)",
                    collateralEstimatedValue = 240000.0,
                    collateralPledgedValue = 80000.0,
                    status = "RESCHEDULED",
                    agentRemarks = "Borrower requested rescheduling due to studio relocation."
                )
            )
            database.agentDao().insertVisits(demoVisits)
        }

        // ==========================================
        // 8. SEED ADMIN HUB: Collateral Vault, Complaints, Mediations, NOC
        // ==========================================
        val demoVaultItems = listOf(
            CollateralVaultEntity(
                vaultItemId = "vault_demo_1",
                loanId = DEMO_LOAN_LENT_ID,
                borrowerId = DEMO_BORROWER_ID,
                borrowerName = "Rahul Sharma",
                borrowerPhone = "+91 98765 43210",
                assetDescription = "24K Hallmarked Gold Necklace (32g) + 22K Gold Bangles Pair (16.5g) - BIS 916 Certified",
                assetType = "GOLD",
                estimatedValue = 285000.0,
                appraisedPurityOrCondition = "BIS 916 Hallmarked, 99.5% Purity Verified by Assayer",
                vaultFacilityName = "Loanzo Central Vault - Delhi NCR",
                lockerNumber = "DEL-VAULT-042",
                barcodeTag = "LZ-GLD-88921",
                tamperSealNumber = "TS-891024",
                custodyStatus = "SECURED_IN_VAULT",
                intakeDate = now - (30 * oneDayMs)
            ),
            CollateralVaultEntity(
                vaultItemId = "vault_demo_2",
                loanId = DEMO_LOAN_PLATFORM_1,
                borrowerId = DEMO_AMIT_ID,
                borrowerName = "Amit Verma",
                borrowerPhone = "+91 98111 22334",
                assetDescription = "Original Commercial Property Title Deed - Khata No. 412/10, Karol Bagh",
                assetType = "PROPERTY_DEED",
                estimatedValue = 1850000.0,
                appraisedPurityOrCondition = "Encumbrance-Free Registered Title Deed with Legal Search Report",
                vaultFacilityName = "Loanzo Central Vault - Delhi NCR",
                lockerNumber = "DEL-VAULT-119",
                barcodeTag = "LZ-PRP-40192",
                tamperSealNumber = "TS-441029",
                custodyStatus = "SECURED_IN_VAULT",
                intakeDate = now - (45 * oneDayMs)
            ),
            CollateralVaultEntity(
                vaultItemId = "vault_demo_3",
                loanId = DEMO_LOAN_PLATFORM_2,
                borrowerId = DEMO_SNEHA_ID,
                borrowerName = "Sneha Roy",
                borrowerPhone = "+91 99887 76655",
                assetDescription = "Apple MacBook Pro 16\" M2 Max (Serial C02G4109MD6) + Audio-Technica ATH-M50x",
                assetType = "EQUIPMENT",
                estimatedValue = 240000.0,
                appraisedPurityOrCondition = "Original Invoice & MDM Unlocked with Physical Tagging",
                vaultFacilityName = "Loanzo Regional Locker - Bengaluru",
                lockerNumber = "BLR-VAULT-208",
                barcodeTag = "LZ-EQP-91028",
                tamperSealNumber = "TS-229104",
                custodyStatus = "ENCUMBERED",
                intakeDate = now - (90 * oneDayMs)
            ),
            CollateralVaultEntity(
                vaultItemId = "vault_demo_4",
                loanId = DEMO_LOAN_CLOSED_ID,
                borrowerId = DEMO_BORROWER_ID,
                borrowerName = "Rahul Sharma",
                borrowerPhone = "+91 98765 43210",
                assetDescription = "Gold Ring 22K (8g) + Fixed Deposit Receipt SBIN FD-2023-44521",
                assetType = "GOLD",
                estimatedValue = 52000.0,
                appraisedPurityOrCondition = "Cleared. Asset released post NOC issuance.",
                vaultFacilityName = "Loanzo Central Vault - Delhi NCR",
                lockerNumber = "DEL-VAULT-042",
                barcodeTag = "LZ-GLD-55012",
                tamperSealNumber = "TS-551012",
                custodyStatus = "RELEASED_TO_OWNER",
                intakeDate = now - (180 * oneDayMs)
            )
        )
        database.collateralVaultDao().insertVaultItems(demoVaultItems)

        // Regulatory Complaints
        val demoComplaints = listOf(
            ComplaintEntity(
                complaintId = "comp_demo_1",
                complainantId = DEMO_BORROWER_ID,
                complainantName = "Rahul Sharma",
                complainantRole = "BORROWER",
                complainantPhone = "+91 98765 43210",
                targetPartyId = targetConsumerId,
                targetPartyName = "Lender Partner",
                targetPartyRole = "LENDER",
                loanId = DEMO_LOAN_CLOSED_ID,
                category = "COLLATERAL_CUSTODY",
                priority = "HIGH",
                subject = "Digital NOC Certificate & Collateral Receipt Request",
                description = "Full repayment was credited on time. Requesting immediate digital clearance certificate in app.",
                status = "RESOLVED",
                resolutionNotes = "Digital NOC certificate generated automatically and verified by custodian. Sent to borrower email.",
                resolvedAt = now - (10 * oneDayMs)
            ),
            ComplaintEntity(
                complaintId = "comp_demo_2",
                complainantId = DEMO_AMIT_ID,
                complainantName = "Amit Verma",
                complainantRole = "BORROWER",
                complainantPhone = "+91 98111 22334",
                targetPartyId = DEMO_VIKRAM_ID,
                targetPartyName = "Vikram Malhotra",
                targetPartyRole = "LENDER",
                loanId = DEMO_LOAN_PLATFORM_1,
                category = "INTEREST_DISPUTE",
                priority = "MEDIUM",
                subject = "Advance Pre-payment Rebate Clarification",
                description = "Requesting calculation on interest waiver if principal is repaid 3 months ahead of schedule.",
                status = "OPEN",
                resolutionNotes = null,
                resolvedAt = null
            ),
            ComplaintEntity(
                complaintId = "comp_demo_3",
                complainantId = DEMO_SNEHA_ID,
                complainantName = "Sneha Roy",
                complainantRole = "BORROWER",
                complainantPhone = "+91 99887 76655",
                targetPartyId = DEMO_RAJESH_ID,
                targetPartyName = "Rajesh Gupta",
                targetPartyRole = "LENDER",
                loanId = DEMO_LOAN_PLATFORM_2,
                category = "HARASSMENT",
                priority = "HIGH",
                subject = "Excessive Follow-up Calls Outside Business Hours",
                description = "Lender's collection representative called at 10:45 PM on 3 consecutive nights. Requesting RBI guideline enforcement on recovery practices.",
                status = "ESCALATED",
                resolutionNotes = "Escalated to Platform Compliance Officer. Recovery calls restricted to 8 AM - 7 PM as per RBI circular.",
                resolvedAt = null
            )
        )
        database.complaintDao().insertComplaints(demoComplaints)

        // Dispute Mediation Meetings
        val demoMeetings = listOf(
            MediationMeetingEntity(
                meetingId = "meet_demo_1",
                title = "Loan Term & Amortization Mediation",
                agenda = "Review advance repayment interest recalculation under Platform Lending Bylaws.",
                loanId = DEMO_LOAN_PLATFORM_1,
                complaintId = "comp_demo_2",
                borrowerId = DEMO_AMIT_ID,
                borrowerName = "Amit Verma",
                borrowerPhone = "+91 98111 22334",
                lenderId = DEMO_VIKRAM_ID,
                lenderName = "Vikram Malhotra",
                lenderPhone = "+91 98450 11223",
                agentId = targetAgentId,
                agentName = "Field Officer",
                meetingType = "GOOGLE_MEET",
                meetingLinkOrLocation = "https://meet.google.com/loa-nzo-med",
                scheduledDateTime = now + (2 * oneDayMs),
                scheduledTimeSlotStr = "Day after tomorrow, 03:30 PM - 04:15 PM",
                status = "SCHEDULED",
                adminNotes = "Official session link active. Platform mediator assigned."
            ),
            MediationMeetingEntity(
                meetingId = "meet_demo_2",
                title = "Anti-Harassment Review & Recovery Protocol",
                agenda = "Investigate complaint #comp_demo_3 regarding after-hours collection calls. Establish recovery protocol per RBI circular DOR.ORG.REC.71/21.16.003.",
                loanId = DEMO_LOAN_PLATFORM_2,
                complaintId = "comp_demo_3",
                borrowerId = DEMO_SNEHA_ID,
                borrowerName = "Sneha Roy",
                borrowerPhone = "+91 99887 76655",
                lenderId = DEMO_RAJESH_ID,
                lenderName = "Rajesh Gupta",
                lenderPhone = "+91 98765 00112",
                agentId = targetAgentId,
                agentName = "Compliance Officer",
                meetingType = "GOOGLE_MEET",
                meetingLinkOrLocation = "https://meet.google.com/loa-nzo-arb",
                scheduledDateTime = now - (3 * oneDayMs),
                scheduledTimeSlotStr = "Completed",
                status = "COMPLETED",
                adminNotes = "Resolution: Recovery calls restricted to 8AM-7PM. Lender warned. Written undertaking obtained."
            )
        )
        database.mediationMeetingDao().insertMeetings(demoMeetings)

        // Digital NOC Certificate for closed loan
        val demoNoc = NocCertificateEntity(
            nocId = "noc_demo_1",
            loanId = DEMO_LOAN_CLOSED_ID,
            borrowerId = DEMO_BORROWER_ID,
            borrowerName = "Rahul Sharma",
            borrowerPan = "DKPRS8876M",
            lenderId = targetConsumerId,
            lenderName = "Satyam Kumar",
            principalAmount = 15000.0,
            totalRepaidAmount = 16326.0,
            collateralReleasedDesc = "Gold Ring 22K (8g) + FD receipt fully cleared and returned to borrower",
            digitalSignatureHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            issuedByAdminId = "ADMIN-SATYAM-0810",
            status = "ACTIVE_CLEARANCE"
        )
        database.nocCertificateDao().insertNoc(demoNoc)

        // ==========================================
        // 9. SEED MARKETPLACE POSTS & BIDS (in DemoDataSeeder)
        // ==========================================
        val demoPosts = listOf(
            MarketplacePostEntity(
                postId = "post_demo_offer_1",
                authorId = DEMO_LENDER_ID,
                authorName = "Priya Patel",
                authorKycVerified = true,
                authorTrustScore = 95,
                postType = "OFFER_TO_LEND",
                title = "Personal & MSME Capital Pool Available",
                description = "Offering instant micro-loans for salaried individuals and small merchants. Fast eSign process. Collateral optional for amounts under 50K.",
                minAmount = 25000.0,
                maxAmount = 100000.0,
                interestRate = 11.0,
                tenureMonths = 12,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "PERSONAL",
                locationCity = "Delhi NCR",
                incomeProofStatus = "VERIFIED",
                vouchCount = 8,
                bidsCount = 2,
                status = "OPEN",
                createdAt = now - (2 * oneDayMs)
            ),
            MarketplacePostEntity(
                postId = "post_demo_req_1",
                authorId = DEMO_BORROWER_ID,
                authorName = "Rahul Sharma",
                authorKycVerified = true,
                authorTrustScore = 90,
                postType = "SEEKING_LOAN",
                title = "Education & Skill Certification Fee",
                description = "Seeking 40K for AWS Solutions Architect Professional certification. Currently employed as DevOps Engineer with monthly take-home of Rs 58,000. Can provide 3 months salary slips + Form 16.",
                minAmount = 40000.0,
                maxAmount = 40000.0,
                interestRate = 10.0,
                tenureMonths = 8,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "EDUCATION",
                locationCity = "Noida",
                incomeProofStatus = "VERIFIED",
                collateralOffered = "PAN + Aadhaar + 3 months salary slips + Form 16",
                vouchCount = 5,
                bidsCount = 1,
                status = "OPEN",
                createdAt = now - (1 * oneDayMs),
                coBorrowerName = "Nirmala Devi",
                coBorrowerRelationship = "Mother (Guarantor & Co-Signer)",
                coBorrowerKycVerified = true,
                coBorrowerTrustScore = 88
            ),
            MarketplacePostEntity(
                postId = "post_demo_offer_2",
                authorId = DEMO_VIKRAM_ID,
                authorName = "Vikram Malhotra",
                authorKycVerified = true,
                authorTrustScore = 92,
                postType = "OFFER_TO_LEND",
                title = "Startup & MSME Growth Capital Fund",
                description = "Angel investor with 8+ years experience in P2P lending. Offering structured growth capital for verified MSME businesses with Udyam registration. Same-day approval for amounts up to 2L.",
                minAmount = 50000.0,
                maxAmount = 200000.0,
                interestRate = 11.5,
                tenureMonths = 18,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "BUSINESS",
                locationCity = "Bengaluru",
                incomeProofStatus = "VERIFIED",
                vouchCount = 12,
                bidsCount = 5,
                status = "OPEN",
                createdAt = now - (3 * oneDayMs)
            )
        )
        demoPosts.forEach { database.marketplaceDao().insertPost(it) }

        // ==========================================
        // 10. SEED NOTIFICATIONS (10 lifecycle alerts)
        // ==========================================
        val notifUser = currentUserId ?: targetConsumerId
        val demoNotifications = listOf(
            NotificationEntity(
                notificationId = "notif_demo_1",
                userId = notifUser,
                title = "KYC Verification Complete",
                message = "Your Aadhaar eKYC, PAN, and Bank Account have been verified. You are now eligible to lend and borrow on Loanzo.",
                type = "KYC",
                timestamp = now - (2 * oneDayMs),
                isRead = true,
                actionRoute = "profile"
            ),
            NotificationEntity(
                notificationId = "notif_demo_2",
                userId = notifUser,
                title = "Loan Agreement Executed",
                message = "Loan of Rs 50,000 to Rahul Sharma has been disbursed and agreement digitally signed by both parties.",
                type = "AGREEMENT",
                relatedLoanId = DEMO_LOAN_LENT_ID,
                timestamp = now - (30 * oneDayMs),
                isRead = true,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_3",
                userId = notifUser,
                title = "New Doorstep Inspection Assigned",
                message = "Doorstep physical collateral verification assigned for Rahul Sharma (Gold Jewelry). Scheduled for Today 11:30 AM.",
                type = "SYSTEM",
                timestamp = now - (4 * 3600_000L),
                isRead = false,
                actionRoute = "home"
            ),
            NotificationEntity(
                notificationId = "notif_demo_4",
                userId = notifUser,
                title = "Escrow Vault Custody Verified",
                message = "Commercial property deed Khata 412/10 successfully secured in Central Vault locker #DEL-VAULT-119 with tamper-evident seal.",
                type = "SYSTEM",
                timestamp = now - (10 * 3600_000L),
                isRead = true,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_5",
                userId = notifUser,
                title = "Repayment Received: Rs 8,834",
                message = "Rahul Sharma paid EMI #1 for 'Inventory Stock Expansion' via UPI. Outstanding: Rs 41,666.",
                type = "REPAYMENT",
                relatedLoanId = DEMO_LOAN_LENT_ID,
                timestamp = now - (25 * oneDayMs),
                isRead = true,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_6",
                userId = notifUser,
                title = "EMI Reminder: Rs 2,297 Due in 5 Days",
                message = "Your EMI #3 for Education Loan (Priya Patel) is due on ${java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(now + (5 * oneDayMs)))}. Pay via UPI for instant receipt.",
                type = "REPAYMENT",
                relatedLoanId = DEMO_LOAN_BORROWED_ID,
                timestamp = now - (1 * oneDayMs),
                isRead = false,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_7",
                userId = notifUser,
                title = "Marketplace Post Approved",
                message = "Your lending offer 'Personal & MSME Capital Pool' is now live on the P2P Marketplace with 8 community vouches.",
                type = "SYSTEM",
                timestamp = now - (2 * oneDayMs),
                isRead = true,
                actionRoute = "marketplace"
            ),
            NotificationEntity(
                notificationId = "notif_demo_8",
                userId = notifUser,
                title = "Community Vouch Received",
                message = "Kavita Rao (Trust Score: 96) vouched for your marketplace post. Reason: Past Repayment Track Record.",
                type = "SYSTEM",
                timestamp = now - (36 * 3600_000L),
                isRead = false,
                actionRoute = "marketplace"
            ),
            NotificationEntity(
                notificationId = "notif_demo_9",
                userId = notifUser,
                title = "NOC Certificate Issued",
                message = "Digital No Objection Certificate issued for Loan #${DEMO_LOAN_CLOSED_ID.takeLast(8)}. Collateral (Gold Ring + FD) released to borrower.",
                type = "AGREEMENT",
                relatedLoanId = DEMO_LOAN_CLOSED_ID,
                timestamp = now - (120 * oneDayMs),
                isRead = true,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_10",
                userId = notifUser,
                title = "Agent Visit Completed",
                message = "Field Agent completed business premise inspection for Amit Verma (CNC Machinery). All serial numbers verified. Photos uploaded.",
                type = "SYSTEM",
                timestamp = now - (2 * oneDayMs),
                isRead = false,
                actionRoute = "home"
            )
        )
        database.notificationDao().insertNotifications(demoNotifications)

        // ==========================================
        // 11. SEED AUDIT TRAIL (8 events)
        // ==========================================
        val auditEvents = listOf(
            AuditEventEntity(eventId = "audit_demo_1", entityType = "USER", entityId = notifUser, actor = notifUser, event = "KYC_VERIFIED", newState = "VERIFIED", description = "DigiLocker sandbox verification completed. Aadhaar, PAN, and bank account validated."),
            AuditEventEntity(eventId = "audit_demo_2", entityType = "LOAN", entityId = DEMO_LOAN_LENT_ID, actor = notifUser, event = "CREATED", newState = "DRAFT", description = "Loan created: Rs 50,000 to Rahul Sharma for Inventory Stock Expansion."),
            AuditEventEntity(eventId = "audit_demo_3", entityType = "LOAN", entityId = DEMO_LOAN_LENT_ID, actor = notifUser, event = "AGREEMENT_SIGNED", newState = "ACTIVE", description = "Both parties signed. Agreement PDF generated with digital signatures."),
            AuditEventEntity(eventId = "audit_demo_4", entityType = "LOAN", entityId = DEMO_LOAN_LENT_ID, actor = notifUser, event = "DISBURSED", newState = "ACTIVE", description = "Rs 50,000 disbursed via UPI to Rahul Sharma (UPI/429381028491/HDFC)."),
            AuditEventEntity(eventId = "audit_demo_5", entityType = "VAULT", entityId = "vault_demo_1", actor = targetAdminId, event = "SEALED", newState = "SECURED_IN_VAULT", description = "Gold collateral sealed with tamper-evident tag TS-891024 in DEL-VAULT-042."),
            AuditEventEntity(eventId = "audit_demo_6", entityType = "LOAN", entityId = DEMO_LOAN_CLOSED_ID, actor = notifUser, event = "CLOSED", newState = "CLOSED", description = "Loan fully repaid. Prepayment of Rs 11,663 cleared outstanding. NOC issued."),
            AuditEventEntity(eventId = "audit_demo_7", entityType = "VAULT", entityId = "vault_demo_4", actor = targetAdminId, event = "RELEASED", newState = "RELEASED_TO_OWNER", description = "Gold Ring 22K + FD receipt released to Rahul Sharma post NOC clearance."),
            AuditEventEntity(eventId = "audit_demo_8", entityType = "AGENT", entityId = "visit_demo_premise_${targetAgentId}", actor = targetAgentId, event = "INSPECTION_COMPLETE", newState = "COMPLETED", description = "Business premise inspection for Amit Verma completed. CNC machine serial verified. 5 employees confirmed.")
        )
        auditEvents.forEach { database.auditEventDao().insertEvent(it) }

        // ==========================================
        // 12. SEED VAULT DOCUMENTS (6 documents)
        // ==========================================
        val vaultDir = File(context.filesDir, "vault_documents").apply { mkdirs() }
        val generatedDocs = DemoDocumentGenerator.generateDemoVaultDocuments(context, vaultDir)

        val doc1Pair = generatedDocs["Sanction_LZ_EDU_25K.pdf"]
        val doc2Pair = generatedDocs["Agreement_LZ_EDU_2026.pdf"]
        val doc3Pair = generatedDocs["Sanction_LZ_BIZ_50K.pdf"]
        val doc4Pair = generatedDocs["Agreement_LZ_BIZ_2026.pdf"]
        val doc5Pair = generatedDocs["Receipt_EMI1_LZ_BIZ_50K.pdf"]
        val doc6Pair = generatedDocs["NOC_LZ_MED_15K.pdf"]

        val targetBorrowerId = currentUserId ?: DEMO_BORROWER_ID
        val vaultDocs = listOf(
            VaultDocumentEntity(
                documentId = "vault_doc_demo_1",
                userId = targetBorrowerId,
                loanId = DEMO_LOAN_BORROWED_ID,
                title = "Sanction Letter - Education Loan Rs 25,000",
                documentType = "SANCTION_LETTER",
                fileName = "Sanction_LZ_EDU_25K.pdf",
                filePath = doc1Pair?.first?.absolutePath ?: File(vaultDir, "Sanction_LZ_EDU_25K.pdf").absolutePath,
                fileSizeBytes = doc1Pair?.first?.length() ?: 145320L,
                checksumSha256 = doc1Pair?.second ?: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                isEncrypted = true,
                description = "Official loan sanction confirmation with interest model, tenure terms, and guarantor details.",
                generatedAt = now - (60 * oneDayMs)
            ),
            VaultDocumentEntity(
                documentId = "vault_doc_demo_2",
                userId = targetBorrowerId,
                loanId = DEMO_LOAN_BORROWED_ID,
                title = "Signed Loan Agreement - LZ-EDU-2026",
                documentType = "LOAN_AGREEMENT",
                fileName = "Agreement_LZ_EDU_2026.pdf",
                filePath = doc2Pair?.first?.absolutePath ?: File(vaultDir, "Agreement_LZ_EDU_2026.pdf").absolutePath,
                fileSizeBytes = doc2Pair?.first?.length() ?: 210450L,
                checksumSha256 = doc2Pair?.second ?: "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4",
                isEncrypted = true,
                description = "Peer-to-peer credit agreement with digital signatures from both parties and guarantor consent.",
                generatedAt = now - (59 * oneDayMs)
            ),
            VaultDocumentEntity(
                documentId = "vault_doc_demo_3",
                userId = notifUser,
                loanId = DEMO_LOAN_LENT_ID,
                title = "Sanction Letter - Business Loan Rs 50,000",
                documentType = "SANCTION_LETTER",
                fileName = "Sanction_LZ_BIZ_50K.pdf",
                filePath = doc3Pair?.first?.absolutePath ?: File(vaultDir, "Sanction_LZ_BIZ_50K.pdf").absolutePath,
                fileSizeBytes = doc3Pair?.first?.length() ?: 152890L,
                checksumSha256 = doc3Pair?.second ?: "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
                isEncrypted = true,
                description = "Sanction letter for Inventory Stock Expansion loan to Rahul Sharma with collateral details.",
                generatedAt = now - (30 * oneDayMs)
            ),
            VaultDocumentEntity(
                documentId = "vault_doc_demo_4",
                userId = notifUser,
                loanId = DEMO_LOAN_LENT_ID,
                title = "eSigned Loan Agreement - LZ-BIZ-2026",
                documentType = "LOAN_AGREEMENT",
                fileName = "Agreement_LZ_BIZ_2026.pdf",
                filePath = doc4Pair?.first?.absolutePath ?: File(vaultDir, "Agreement_LZ_BIZ_2026.pdf").absolutePath,
                fileSizeBytes = doc4Pair?.first?.length() ?: 234560L,
                checksumSha256 = doc4Pair?.second ?: "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3",
                isEncrypted = true,
                description = "Fully executed loan agreement with biometric selfie verification and gold collateral annexure.",
                generatedAt = now - (30 * oneDayMs)
            ),
            VaultDocumentEntity(
                documentId = "vault_doc_demo_5",
                userId = notifUser,
                loanId = DEMO_LOAN_LENT_ID,
                title = "Repayment Receipt - EMI #1 Rs 8,834",
                documentType = "RECEIPT",
                fileName = "Receipt_EMI1_LZ_BIZ_50K.pdf",
                filePath = doc5Pair?.first?.absolutePath ?: File(vaultDir, "Receipt_EMI1_LZ_BIZ_50K.pdf").absolutePath,
                fileSizeBytes = doc5Pair?.first?.length() ?: 89450L,
                checksumSha256 = doc5Pair?.second ?: "c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4",
                isEncrypted = true,
                description = "Digital receipt for EMI #1 payment of Rs 8,834 via UPI (Ref: UPI/329481928491).",
                generatedAt = now - (25 * oneDayMs)
            ),
            VaultDocumentEntity(
                documentId = "vault_doc_demo_6",
                userId = notifUser,
                loanId = DEMO_LOAN_CLOSED_ID,
                title = "NOC & Clearance Certificate - Loan Closed",
                documentType = "NOC_CERTIFICATE",
                fileName = "NOC_LZ_MED_15K.pdf",
                filePath = doc6Pair?.first?.absolutePath ?: File(vaultDir, "NOC_LZ_MED_15K.pdf").absolutePath,
                fileSizeBytes = doc6Pair?.first?.length() ?: 178920L,
                checksumSha256 = doc6Pair?.second ?: "d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5",
                isEncrypted = true,
                description = "Official No Objection Certificate confirming full repayment and collateral release for Medical loan.",
                generatedAt = now - (120 * oneDayMs)
            )
        )
        vaultDocs.forEach { database.vaultDocumentDao().insertDocument(it) }
    }

    /**
     * Clears all demo entities cleanly from the database.
     */
    suspend fun clearDemoData(currentUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Delete demo notifications
            for (i in 1..10) {
                database.notificationDao().deleteNotification("notif_demo_$i")
            }

            // Delete demo audit events
            for (i in 1..8) {
                try { database.auditEventDao().deleteEvent("audit_demo_$i") } catch (_: Exception) {}
            }

            // Delete demo vault documents
            for (i in 1..6) {
                try { database.vaultDocumentDao().deleteDocument("vault_doc_demo_$i") } catch (_: Exception) {}
            }
            try {
                val vaultDir = File(context.filesDir, "vault_documents")
                listOf(
                    "Sanction_LZ_EDU_25K.pdf",
                    "Agreement_LZ_EDU_2026.pdf",
                    "Sanction_LZ_BIZ_50K.pdf",
                    "Agreement_LZ_BIZ_2026.pdf",
                    "Receipt_EMI1_LZ_BIZ_50K.pdf",
                    "NOC_LZ_MED_15K.pdf"
                ).forEach { name ->
                    val f = File(vaultDir, name)
                    if (f.exists()) f.delete()
                }
            } catch (_: Exception) {}

            // Delete demo marketplace posts
            database.marketplaceDao().deletePost("post_demo_offer_1")
            database.marketplaceDao().deletePost("post_demo_req_1")
            database.marketplaceDao().deletePost("post_demo_offer_2")

            // Delete demo disbursements
            val disbIds = listOf("disb_demo_lent_1", "disb_demo_borrowed_1", "disb_demo_closed_1", "disb_demo_platform1_1", "disb_demo_platform2_1")
            disbIds.forEach { id ->
                val d = database.disbursementDao().getDisbursementById(id)
                if (d != null) database.disbursementDao().deleteDisbursement(d)
            }

            // Delete demo guarantors
            val guarIds = listOf("guar_demo_nirmala_1", "guar_demo_vikram_1")
            guarIds.forEach { id ->
                val g = database.guarantorDao().getGuarantorById(id)
                if (g != null) database.guarantorDao().deleteGuarantor(g)
            }

            // Delete demo payees
            val payeeIds = listOf("payee_rahul_shop", "payee_coursera_edu", "payee_hospital_med", "payee_amit_cnc", "payee_sneha_studio")
            payeeIds.forEach { id ->
                val p = database.payeeDao().getPayeeById(id)
                if (p != null) database.payeeDao().deletePayee(p)
            }

            // Delete demo repayments & loans
            val repayIds = listOf(
                "repay_demo_lent_1", "repay_demo_lent_2",
                "repay_demo_borrowed_1", "repay_demo_borrowed_2", "repay_demo_borrowed_3",
                "repay_demo_platform_1", "repay_demo_platform_1b",
                "repay_demo_platform2_1", "repay_demo_platform2_2", "repay_demo_platform2_3",
                "repay_demo_platform3_1", "repay_demo_platform3_2",
                "repay_demo_closed_1", "repay_demo_closed_2"
            )
            repayIds.forEach { id ->
                // Use a minimal entity for deletion
                database.repaymentDao().deleteRepayment(
                    RepaymentEntity(id, "", 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
                )
            }

            listOf(
                DEMO_LOAN_LENT_ID,
                DEMO_LOAN_BORROWED_ID,
                DEMO_LOAN_CLOSED_ID,
                DEMO_LOAN_PLATFORM_1,
                DEMO_LOAN_PLATFORM_2,
                DEMO_LOAN_PLATFORM_3
            ).forEach { loanId ->
                val loan = database.loanDao().getLoanById(loanId)
                if (loan != null) database.loanDao().deleteLoan(loan)
            }

            // Clear admin hub & agent demo entities
            database.complaintDao().deleteDemoComplaints()
            database.mediationMeetingDao().deleteDemoMeetings()
            database.collateralVaultDao().deleteDemoVaultItems()
            database.nocCertificateDao().deleteDemoNocs()
            database.agentDao().deleteDemoVisits()

            Result.success("Demo data cleared successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
