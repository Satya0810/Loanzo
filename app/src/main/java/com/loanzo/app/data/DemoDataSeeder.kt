package com.loanzo.app.data

import com.loanzo.app.data.entity.*
import com.loanzo.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataSeeder @Inject constructor(
    private val database: LoanzoDatabase,
    private val userRepository: UserRepository
) {
    companion object {
        // Consumer Counterparties
        const val DEMO_BORROWER_ID = "usr_demo_rahul"
        const val DEMO_LENDER_ID = "usr_demo_priya"

        // Additional Platform Counterparties for Admin & Marketplace
        const val DEMO_AMIT_ID = "usr_demo_amit"
        const val DEMO_SNEHA_ID = "usr_demo_sneha"
        const val DEMO_VIKRAM_ID = "usr_demo_vikram"
        const val DEMO_RAJESH_ID = "usr_demo_rajesh"

        // Demo User Accounts for direct login
        const val DEMO_ADMIN_ACCOUNT_ID = "usr_satyam_owner"
        const val DEMO_AGENT_ACCOUNT_ID = "usr_demo_agent_vikas"
        const val DEMO_USER_ACCOUNT_ID = "usr_demo_user_arjun"

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
                bankIfsc = "KKBK0004921"
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
                bankIfsc = "SBIN0004521"
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
                bankIfsc = "ICIC0002938"
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
                bankAccountNumber = "4019283746192",
                bankIfsc = "PUNB0003829"
            ),
            // Sneha Roy (Tech Freelancer)
            UserEntity(
                userId = DEMO_SNEHA_ID,
                name = "Sneha Roy",
                email = "sneha.roy@demo.loanzo.app",
                phone = "+91 99887 76655",
                username = "sneha_creatives",
                password = "password123",
                role = "BORROWER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "7019283849102",
                bankIfsc = "AXIS0001928"
            ),
            // Vikram Malhotra (Lender / Investor)
            UserEntity(
                userId = DEMO_VIKRAM_ID,
                name = "Vikram Malhotra",
                email = "vikram.malhotra@demo.loanzo.app",
                phone = "+91 98450 11223",
                username = "vikram_capital",
                password = "password123",
                role = "LENDER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "8019283746190",
                bankIfsc = "HDFC0009821"
            ),
            // Rajesh Gupta (Super Angel Lender)
            UserEntity(
                userId = DEMO_RAJESH_ID,
                name = "Rajesh Gupta",
                email = "rajesh.gupta@demo.loanzo.app",
                phone = "+91 97112 88990",
                username = "rajesh_wealth",
                password = "password123",
                role = "LENDER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "9019283746199",
                bankIfsc = "ICIC0004910"
            )
        )
        demoUsers.forEach { user ->
            val existing = database.userDao().getUserById(user.userId)
            if (existing == null) {
                database.userDao().insertUser(user)
            }
        }

        // ==========================================
        // 2. SEED CONSUMER & PLATFORM LOANS
        // ==========================================
        val allDemoLoans = listOf(
            // Loan A: LENT by Active User to Rahul (₹50,000, 6 Months, Active)
            LoanEntity(
                loanId = DEMO_LOAN_LENT_ID,
                lenderId = targetConsumerId,
                borrowerId = DEMO_BORROWER_ID,
                sanctionedAmount = 50000.0,
                disbursedAmount = 50000.0,
                outstandingAmount = 41666.0,
                purpose = "Inventory Stock Expansion",
                loanType = "BUSINESS",
                interestRate = 12.0,
                interestModel = "SIMPLE",
                tenureMonths = 6,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (30 * oneDayMs),
                notes = "GST-verified MSME retail business. Timely borrower history.",
                isAgreementSigned = true,
                lenderSignedAt = now - (30 * oneDayMs),
                borrowerSignedAt = now - (30 * oneDayMs)
            ),
            // Loan B: BORROWED by Active User from Priya (₹25,000, 12 Months, Active)
            LoanEntity(
                loanId = DEMO_LOAN_BORROWED_ID,
                lenderId = DEMO_LENDER_ID,
                borrowerId = targetConsumerId,
                sanctionedAmount = 25000.0,
                disbursedAmount = 25000.0,
                outstandingAmount = 22650.0,
                purpose = "Home Office Tech Equipment",
                loanType = "PERSONAL",
                interestRate = 10.5,
                interestModel = "SIMPLE",
                tenureMonths = 12,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (20 * oneDayMs),
                notes = "Purchase of ergonomic desk and multi-monitor setup.",
                isAgreementSigned = true,
                lenderSignedAt = now - (20 * oneDayMs),
                borrowerSignedAt = now - (20 * oneDayMs)
            ),
            // Loan C: CLOSED Loan (₹15,000, Repaid on schedule)
            LoanEntity(
                loanId = DEMO_LOAN_CLOSED_ID,
                lenderId = targetConsumerId,
                borrowerId = DEMO_BORROWER_ID,
                sanctionedAmount = 15000.0,
                disbursedAmount = 15000.0,
                outstandingAmount = 0.0,
                purpose = "Emergency Medical Equipment",
                loanType = "PERSONAL",
                interestRate = 11.0,
                interestModel = "SIMPLE",
                tenureMonths = 3,
                status = "CLOSED",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (120 * oneDayMs),
                notes = "All 3 installments repaid seamlessly via UPI. Full NOC issued.",
                isAgreementSigned = true,
                lenderSignedAt = now - (120 * oneDayMs),
                borrowerSignedAt = now - (120 * oneDayMs)
            ),
            // Platform Loan 1: Vikram Malhotra -> Amit Verma (₹1,50,000, 12 Months, Active)
            LoanEntity(
                loanId = DEMO_LOAN_PLATFORM_1,
                lenderId = DEMO_VIKRAM_ID,
                borrowerId = DEMO_AMIT_ID,
                sanctionedAmount = 150000.0,
                disbursedAmount = 150000.0,
                outstandingAmount = 137500.0,
                purpose = "CNC Lathe Machinery Upgrade",
                loanType = "BUSINESS",
                interestRate = 11.5,
                interestModel = "SIMPLE",
                tenureMonths = 12,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (45 * oneDayMs),
                notes = "Secured against original commercial property deed in central vault.",
                isAgreementSigned = true,
                lenderSignedAt = now - (45 * oneDayMs),
                borrowerSignedAt = now - (45 * oneDayMs)
            ),
            // Platform Loan 2: Rajesh Gupta -> Sneha Roy (₹80,000, 8 Months, Active)
            LoanEntity(
                loanId = DEMO_LOAN_PLATFORM_2,
                lenderId = DEMO_RAJESH_ID,
                borrowerId = DEMO_SNEHA_ID,
                sanctionedAmount = 80000.0,
                disbursedAmount = 80000.0,
                outstandingAmount = 70000.0,
                purpose = "Studio Production Workstation & Audio Gear",
                loanType = "EQUIPMENT",
                interestRate = 12.0,
                interestModel = "SIMPLE",
                tenureMonths = 8,
                status = "ACTIVE",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (25 * oneDayMs),
                notes = "Hardware encumbered in platform collateral registry.",
                isAgreementSigned = true,
                lenderSignedAt = now - (25 * oneDayMs),
                borrowerSignedAt = now - (25 * oneDayMs)
            ),
            // Platform Loan 3: Rajesh Gupta -> Rahul Sharma (₹2,00,000, 24 Months, Active)
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
                notes = "Escrow backed peer-to-peer verified institutional loan.",
                isAgreementSigned = true,
                lenderSignedAt = now - (60 * oneDayMs),
                borrowerSignedAt = now - (60 * oneDayMs)
            )
        )
        allDemoLoans.forEach { loan ->
            database.loanDao().insertLoan(loan)
        }

        // ==========================================
        // 3. SEED REPAYMENT SCHEDULES & TRANSACTIONS
        // ==========================================
        val demoRepayments = listOf(
            RepaymentEntity(
                repaymentId = "repay_demo_lent_1",
                loanId = DEMO_LOAN_LENT_ID,
                amount = 8334.0,
                transactionRef = "UPI/329481928491",
                status = "PAID",
                dueDate = now - (5 * oneDayMs),
                paidDate = now - (5 * oneDayMs),
                outstandingSnapshot = 41666.0,
                principalComponent = 7834.0,
                interestComponent = 500.0,
                penalty = 0.0
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_borrowed_1",
                loanId = DEMO_LOAN_BORROWED_ID,
                amount = 2350.0,
                transactionRef = "IMPS/901827364512",
                status = "SCHEDULED",
                dueDate = now + (5 * oneDayMs),
                paidDate = null,
                outstandingSnapshot = 22650.0,
                principalComponent = 2131.0,
                interestComponent = 219.0,
                penalty = 0.0
            ),
            RepaymentEntity(
                repaymentId = "repay_demo_platform_1",
                loanId = DEMO_LOAN_PLATFORM_1,
                amount = 12500.0,
                transactionRef = "NEFT/8019283741",
                status = "PAID",
                dueDate = now - (15 * oneDayMs),
                paidDate = now - (15 * oneDayMs),
                outstandingSnapshot = 137500.0,
                principalComponent = 11062.0,
                interestComponent = 1438.0,
                penalty = 0.0
            )
        )
        demoRepayments.forEach { database.repaymentDao().insertRepayment(it) }

        // ==========================================
        // 4. SEED FIELD AGENT DOORSTEP INSPECTION VISITS
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

        // Insert visits assigned to both target agent and current active user
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
                    lenderAddress = "Indirapuram, Ghaziabad",
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
                    visitType = "BORROWER_VERIFICATION",
                    title = "Business Premise & KYC Verification",
                    borrowerName = "Amit Verma",
                    borrowerPhone = "+91 98111 22334",
                    borrowerAddress = "Shop 14, Main Market, Connaught Place, New Delhi",
                    lenderName = "Vikram Malhotra",
                    lenderPhone = "+91 98450 11223",
                    lenderAddress = "Bandra West, Mumbai",
                    targetAddress = "Shop 14, Main Market, Connaught Place, New Delhi",
                    targetLatitude = 28.6304,
                    targetLongitude = 77.2177,
                    scheduledDate = "Today",
                    scheduledTimeSlot = "03:00 PM - 04:30 PM",
                    payoutAmount = 500.0,
                    status = "SCHEDULED",
                    agentRemarks = ""
                ),
                AgentVisitEntity(
                    visitId = "visit_demo_laptop_${agentId}",
                    agentId = agentId,
                    loanId = DEMO_LOAN_PLATFORM_2,
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Hardware Asset Inspection & Tagging",
                    borrowerName = "Sneha Roy",
                    borrowerPhone = "+91 99887 76655",
                    borrowerAddress = "12th Cross, 100ft Road, Indiranagar, Bengaluru",
                    lenderName = "Rajesh Gupta",
                    lenderPhone = "+91 97112 88990",
                    lenderAddress = "Kothrud, Pune",
                    targetAddress = "12th Cross, 100ft Road, Indiranagar, Bengaluru",
                    targetLatitude = 12.9784,
                    targetLongitude = 77.6408,
                    scheduledDate = "Tomorrow",
                    scheduledTimeSlot = "10:00 AM - 11:30 AM",
                    payoutAmount = 600.0,
                    collateralItemName = "Apple MacBook Pro 16\" M2 Max (64GB RAM)",
                    collateralEstimatedValue = 240000.0,
                    collateralPledgedValue = 80000.0,
                    status = "IN_PROGRESS",
                    agentRemarks = "Physical serial number checked. Hardware functional."
                ),
                AgentVisitEntity(
                    visitId = "visit_demo_vehicle_${agentId}",
                    agentId = agentId,
                    loanId = "loan_demo_bike_35k",
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Vehicle Registration & Physical Inspection",
                    borrowerName = "Vikram Malhotra",
                    borrowerPhone = "+91 98450 11223",
                    borrowerAddress = "B-12, Sector 62, Noida, UP",
                    lenderName = "Rahul Sharma",
                    lenderPhone = "+91 98765 43210",
                    lenderAddress = "Sector 100, Noida",
                    targetAddress = "B-12, Sector 62, Noida, UP",
                    targetLatitude = 28.6270,
                    targetLongitude = 77.3725,
                    scheduledDate = "Yesterday",
                    scheduledTimeSlot = "02:00 PM - 03:30 PM",
                    payoutAmount = 800.0,
                    collateralItemName = "Yamaha FZ-S FI 150cc (2023 Model) - RC Book & Physical Bike",
                    collateralEstimatedValue = 95000.0,
                    collateralPledgedValue = 35000.0,
                    status = "COMPLETED",
                    agentRemarks = "Physical chassis number matching with RC. Bike in pristine condition. Odometer reading 12,450 km. Verified authentic.",
                    isCollateralAuthentic = true,
                    isBorrowerIdentityVerified = true,
                    completedAt = now - (1 * oneDayMs)
                )
            )
            database.agentDao().insertVisits(demoVisits)
        }

        // ==========================================
        // 5. SEED MASTER ADMIN EXECUTIVE HUB ENTITIES
        // ==========================================
        // Collateral Vault Ledger Items
        val demoVaultItems = listOf(
            CollateralVaultEntity(
                vaultItemId = "vault_demo_1",
                loanId = DEMO_LOAN_LENT_ID,
                borrowerId = DEMO_BORROWER_ID,
                borrowerName = "Rahul Sharma",
                borrowerPhone = "+91 98765 43210",
                assetDescription = "24K Hallmarked Gold Necklace & Bangles (48.5g Net Weight)",
                assetType = "GOLD",
                estimatedValue = 285000.0,
                appraisedPurityOrCondition = "99.5% Purity Hallmarked Gold (Certified by Bureau of Indian Standards)",
                vaultFacilityName = "Loanzo Central Vault - Delhi NCR",
                lockerNumber = "DEL-VAULT-402",
                barcodeTag = "LZ-GLD-88219",
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
                assetDescription = "Original Commercial Property Title Deed - Khata No. 412/10",
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
                assetDescription = "Apple MacBook Pro 16\" M2 Max (Serial C02G4109MD6)",
                assetType = "EQUIPMENT",
                estimatedValue = 240000.0,
                appraisedPurityOrCondition = "Original Invoice & MDM Unlocked with Physical Tagging",
                vaultFacilityName = "Loanzo Regional Locker - Bengaluru",
                lockerNumber = "BLR-VAULT-208",
                barcodeTag = "LZ-EQP-91028",
                tamperSealNumber = "TS-229104",
                custodyStatus = "ENCUMBERED",
                intakeDate = now - (25 * oneDayMs)
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
                scheduledTimeSlotStr = "Tomorrow, 03:30 PM - 04:15 PM",
                status = "SCHEDULED",
                adminNotes = "Official session link active. Platform mediator assigned."
            )
        )
        database.mediationMeetingDao().insertMeetings(demoMeetings)

        // Digital NOC Certificate for closed loan
        val demoNoc = NocCertificateEntity(
            nocId = "noc_demo_1",
            loanId = DEMO_LOAN_CLOSED_ID,
            borrowerId = DEMO_BORROWER_ID,
            borrowerName = "Rahul Sharma",
            borrowerPan = "ABCDE1234F",
            lenderId = targetConsumerId,
            lenderName = "Satyam Kumar",
            principalAmount = 15000.0,
            totalRepaidAmount = 16650.0,
            collateralReleasedDesc = "Gold ring and medical deposit lien fully cleared",
            digitalSignatureHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            issuedByAdminId = "ADMIN-SATYAM-0810",
            status = "ACTIVE_CLEARANCE"
        )
        database.nocCertificateDao().insertNoc(demoNoc)

        // ==========================================
        // 6. SEED MARKETPLACE POSTS & BIDS
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
                description = "Offering instant micro-loans for salaried individuals and small merchants. Fast eSign process.",
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
                description = "Seeking funds for cloud architecture exam certification. 6-month repayment with verified income.",
                minAmount = 30000.0,
                maxAmount = 50000.0,
                interestRate = 12.0,
                tenureMonths = 6,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "EDUCATION",
                locationCity = "Noida",
                incomeProofStatus = "VERIFIED",
                vouchCount = 4,
                bidsCount = 1,
                status = "OPEN",
                createdAt = now - (1 * oneDayMs)
            ),
            MarketplacePostEntity(
                postId = "post_demo_offer_2",
                authorId = DEMO_RAJESH_ID,
                authorName = "Rajesh Gupta (Super Angel)",
                authorKycVerified = true,
                authorTrustScore = 99,
                postType = "OFFER_TO_LEND",
                title = "Strategic SME Growth & Working Capital Pool",
                description = "Direct private lending for profitable businesses looking for bridge finance or machinery purchase.",
                minAmount = 100000.0,
                maxAmount = 500000.0,
                interestRate = 10.0,
                tenureMonths = 24,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "BUSINESS",
                locationCity = "Mumbai / Pune",
                incomeProofStatus = "VERIFIED",
                vouchCount = 15,
                bidsCount = 4,
                status = "OPEN",
                createdAt = now - (3 * oneDayMs)
            )
        )
        database.marketplaceDao().insertPosts(demoPosts)

        val demoBids = listOf(
            MarketplaceBidEntity(
                bidId = "bid_demo_1",
                postId = "post_demo_req_1",
                bidderId = DEMO_LENDER_ID,
                bidderName = "Priya Patel",
                bidderKycVerified = true,
                bidderTrustScore = 95,
                proposedAmount = 35000.0,
                proposedInterestRate = 11.5,
                proposedTenureMonths = 6,
                proposedRepaymentFrequency = "MONTHLY",
                message = "Happy to fund your certification loan. eSign agreement today for instant transfer!",
                status = "PENDING",
                createdAt = now - (12 * 3600_000L)
            ),
            MarketplaceBidEntity(
                bidId = "bid_demo_2",
                postId = "post_demo_offer_1",
                bidderId = DEMO_BORROWER_ID,
                bidderName = "Rahul Sharma",
                bidderKycVerified = true,
                bidderTrustScore = 90,
                proposedAmount = 50000.0,
                proposedInterestRate = 11.5,
                proposedTenureMonths = 12,
                proposedRepaymentFrequency = "MONTHLY",
                message = "Requesting ₹50,000 from your capital pool for inventory upgrade. All documents ready.",
                status = "PENDING",
                createdAt = now - (18 * 3600_000L)
            )
        )
        database.marketplaceDao().insertBids(demoBids)

        // ==========================================
        // 7. SEED NOTIFICATIONS & AUDIT TRAIL
        // ==========================================
        val notifUser = currentUserId ?: DEMO_USER_ACCOUNT_ID
        val demoNotifications = listOf(
            NotificationEntity(
                notificationId = "notif_demo_1",
                userId = notifUser,
                title = "🔔 EMI Due in 5 Days",
                message = "Upcoming installment of ₹2,350 for 'Home Office Tech Equipment' is due on ${android.text.format.DateFormat.format("dd MMM yyyy", now + 5 * oneDayMs)}.",
                type = "DEADLINE",
                relatedLoanId = DEMO_LOAN_BORROWED_ID,
                timestamp = now - (2 * 3600_000L),
                isRead = false,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_2",
                userId = notifUser,
                title = "🤝 Loan Agreement Executed",
                message = "Loan of ₹50,000 to Rahul Sharma has been disbursed and agreement digitally signed.",
                type = "AGREEMENT",
                relatedLoanId = DEMO_LOAN_LENT_ID,
                timestamp = now - (1 * oneDayMs),
                isRead = false,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_3",
                userId = notifUser,
                title = "🕵️ New Doorstep Inspection Assigned",
                message = "Doorstep physical collateral verification assigned for Rahul Sharma (Gold Jewelry). Scheduled for Today 11:30 AM.",
                type = "SYSTEM",
                timestamp = now - (4 * 3600_000L),
                isRead = false,
                actionRoute = "home"
            ),
            NotificationEntity(
                notificationId = "notif_demo_4",
                userId = notifUser,
                title = "🛡️ Escrow Vault Custody Verified",
                message = "Commercial property deed Khata 412/10 successfully secured in Central Vault locker #DEL-VAULT-119.",
                type = "SYSTEM",
                timestamp = now - (10 * 3600_000L),
                isRead = true,
                actionRoute = "loans"
            ),
            NotificationEntity(
                notificationId = "notif_demo_5",
                userId = notifUser,
                title = "💳 Repayment Received: ₹8,334",
                message = "Rahul Sharma paid EMI #1 for 'Inventory Stock Expansion' via UPI.",
                type = "REPAYMENT",
                relatedLoanId = DEMO_LOAN_LENT_ID,
                timestamp = now - (15 * oneDayMs),
                isRead = true,
                actionRoute = "loans"
            )
        )
        database.notificationDao().insertNotifications(demoNotifications)

        database.auditEventDao().insertEvent(
            AuditEventEntity(
                eventId = "audit_demo_1",
                entityType = "USER",
                entityId = notifUser,
                actor = notifUser,
                event = "KYC_VERIFIED",
                newState = "VERIFIED",
                description = "DigiLocker sandbox verification completed successfully."
            )
        )
        database.auditEventDao().insertEvent(
            AuditEventEntity(
                eventId = "audit_demo_2",
                entityType = "LOAN",
                entityId = DEMO_LOAN_LENT_ID,
                actor = notifUser,
                event = "DISBURSED",
                newState = "ACTIVE",
                description = "Loan ₹50,000 disbursed to borrower Rahul Sharma."
            )
        )
        database.auditEventDao().insertEvent(
            AuditEventEntity(
                eventId = "audit_demo_3",
                entityType = "VAULT",
                entityId = "vault_demo_1",
                actor = targetAdminId,
                event = "SEALED",
                newState = "SECURED_IN_VAULT",
                description = "Gold collateral sealed with tamper evident tag TS-891024."
            )
        )
    }

    /**
     * Clears all demo entities cleanly from the database.
     */
    suspend fun clearDemoData(currentUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Delete demo notifications
            for (i in 1..5) {
                database.notificationDao().deleteNotification("notif_demo_$i")
            }

            // Delete demo marketplace posts
            database.marketplaceDao().deletePost("post_demo_offer_1")
            database.marketplaceDao().deletePost("post_demo_req_1")
            database.marketplaceDao().deletePost("post_demo_offer_2")

            // Delete demo repayments & loans
            database.repaymentDao().deleteRepayment(
                RepaymentEntity("repay_demo_lent_1", DEMO_LOAN_LENT_ID, 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
            )
            database.repaymentDao().deleteRepayment(
                RepaymentEntity("repay_demo_borrowed_1", DEMO_LOAN_BORROWED_ID, 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
            )
            database.repaymentDao().deleteRepayment(
                RepaymentEntity("repay_demo_platform_1", DEMO_LOAN_PLATFORM_1, 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
            )

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
