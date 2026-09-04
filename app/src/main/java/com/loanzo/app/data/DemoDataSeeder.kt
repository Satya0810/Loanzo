package com.loanzo.app.data

import com.loanzo.app.data.entity.*
import com.loanzo.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataSeeder @Inject constructor(
    private val database: LoanzoDatabase,
    private val userRepository: UserRepository
) {
    companion object {
        const val DEMO_BORROWER_ID = "usr_demo_rahul"
        const val DEMO_LENDER_ID = "usr_demo_priya"

        const val DEMO_LOAN_LENT_ID = "loan_demo_lent_50k"
        const val DEMO_LOAN_BORROWED_ID = "loan_demo_borrowed_25k"
        const val DEMO_LOAN_CLOSED_ID = "loan_demo_closed_15k"
    }

    /**
     * Seeds comprehensive demo data for the specified active user.
     */
    suspend fun seedAllDemoData(currentUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val oneDayMs = 86_400_000L

            // 1. Upgrade current user to Fully Verified KYC & Bank Account
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
                    aadhaarNumber = currentUser.aadhaarNumber.ifBlank { "9876 5432 1098" }
                )
                database.userDao().updateUser(verifiedUser)
            }

            // 2. Insert counterparties to satisfy Foreign Key constraints
            val demoBorrower = UserEntity(
                userId = DEMO_BORROWER_ID,
                name = "Rahul Sharma",
                email = "rahul.sharma@demo.loanzo.app",
                phone = "+91 98765 43210",
                username = "rahul_sharma",
                role = "BORROWER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "9182736451029",
                bankIfsc = "SBIN0004521"
            )
            database.userDao().insertUser(demoBorrower)

            val demoLender = UserEntity(
                userId = DEMO_LENDER_ID,
                name = "Priya Patel",
                email = "priya.patel@demo.loanzo.app",
                phone = "+91 91234 56789",
                username = "priya_invest",
                role = "LENDER",
                kycStatus = "VERIFIED",
                aadhaarVerified = true,
                panVerified = true,
                bankVerified = true,
                bankAccountNumber = "1029384756102",
                bankIfsc = "ICIC0002938"
            )
            database.userDao().insertUser(demoLender)

            // 3. Insert 3 Realistic Loans
            // Loan A: LENT by Current User to Rahul (₹50,000, 6 Months, Active)
            val loanLent = LoanEntity(
                loanId = DEMO_LOAN_LENT_ID,
                lenderId = currentUserId,
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
            )
            database.loanDao().insertLoan(loanLent)

            // Loan B: BORROWED by Current User from Priya (₹25,000, 12 Months, Active)
            val loanBorrowed = LoanEntity(
                loanId = DEMO_LOAN_BORROWED_ID,
                lenderId = DEMO_LENDER_ID,
                borrowerId = currentUserId,
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
                notes = "Fast-track peer disbursal via UPI AutoPay.",
                isAgreementSigned = true,
                lenderSignedAt = now - (20 * oneDayMs),
                borrowerSignedAt = now - (20 * oneDayMs)
            )
            database.loanDao().insertLoan(loanBorrowed)

            // Loan C: CLOSED Loan (₹15,000, Fully Repaid)
            val loanClosed = LoanEntity(
                loanId = DEMO_LOAN_CLOSED_ID,
                lenderId = DEMO_LENDER_ID,
                borrowerId = currentUserId,
                sanctionedAmount = 15000.0,
                disbursedAmount = 15000.0,
                outstandingAmount = 0.0,
                purpose = "Medical Emergency Checkup",
                loanType = "MEDICAL",
                interestRate = 9.0,
                interestModel = "FLAT",
                tenureMonths = 3,
                status = "CLOSED",
                repaymentFrequency = "MONTHLY",
                createdAt = now - (90 * oneDayMs),
                closedAt = now - (5 * oneDayMs),
                notes = "Fully honored on time. Pristine credit standing.",
                isAgreementSigned = true
            )
            database.loanDao().insertLoan(loanClosed)

            // 4. Insert Repayments for Loans
            // Repayments for Loan A (Lent)
            database.repaymentDao().insertRepayment(
                RepaymentEntity(
                    repaymentId = "repay_demo_lent_1",
                    loanId = DEMO_LOAN_LENT_ID,
                    amount = 8334.0,
                    transactionRef = "UPI/DEMO/928374829101",
                    status = "PAID",
                    dueDate = now - (15 * oneDayMs),
                    paidDate = now - (15 * oneDayMs),
                    outstandingSnapshot = 41666.0,
                    principalComponent = 7834.0,
                    interestComponent = 500.0,
                    timestamp = now - (15 * oneDayMs),
                    note = "Paid on time via PhonePe UPI"
                )
            )
            database.repaymentDao().insertRepayment(
                RepaymentEntity(
                    repaymentId = "repay_demo_lent_2",
                    loanId = DEMO_LOAN_LENT_ID,
                    amount = 8333.0,
                    transactionRef = "SCHEDULED",
                    status = "SCHEDULED",
                    dueDate = now + (15 * oneDayMs),
                    outstandingSnapshot = 41666.0,
                    principalComponent = 7833.0,
                    interestComponent = 500.0,
                    note = "Upcoming installment #2"
                )
            )

            // Repayments for Loan B (Borrowed)
            database.repaymentDao().insertRepayment(
                RepaymentEntity(
                    repaymentId = "repay_demo_borrowed_1",
                    loanId = DEMO_LOAN_BORROWED_ID,
                    amount = 2350.0,
                    transactionRef = "UPI/DEMO/102938475612",
                    status = "PAID",
                    dueDate = now - (10 * oneDayMs),
                    paidDate = now - (10 * oneDayMs),
                    outstandingSnapshot = 22650.0,
                    principalComponent = 2130.0,
                    interestComponent = 220.0,
                    timestamp = now - (10 * oneDayMs),
                    note = "Paid on time via Google Pay"
                )
            )
            database.repaymentDao().insertRepayment(
                RepaymentEntity(
                    repaymentId = "repay_demo_borrowed_2",
                    loanId = DEMO_LOAN_BORROWED_ID,
                    amount = 2350.0,
                    transactionRef = "SCHEDULED",
                    status = "SCHEDULED",
                    dueDate = now + (5 * oneDayMs),
                    outstandingSnapshot = 22650.0,
                    principalComponent = 2130.0,
                    interestComponent = 220.0,
                    note = "Upcoming installment #2 due in 5 days"
                )
            )

            // 5. Insert 4 Community Loan Wall Posts with bids
            val demoPosts = listOf(
                MarketplacePostEntity(
                    postId = "post_demo_offer_1",
                    authorId = "usr_demo_vikram",
                    authorName = "Dr. Vikram Mehta",
                    authorKycVerified = true,
                    authorTrustScore = 96,
                    postType = "OFFER_TO_LEND",
                    title = "Capital Pool for Micro-Entrepreneurs & Students",
                    description = "Offering instant micro-credit up to ₹1,00,000 for verified tech students, artisans, and shop owners. Low APR 11.5% with flexible monthly EMIs.",
                    minAmount = 20000.0,
                    maxAmount = 100000.0,
                    interestRate = 11.5,
                    tenureMonths = 12,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "BUSINESS",
                    locationCity = "Bengaluru",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 14,
                    bidsCount = 3,
                    status = "OPEN",
                    createdAt = now - (2 * oneDayMs)
                ),
                MarketplacePostEntity(
                    postId = "post_demo_req_1",
                    authorId = "usr_demo_sneha",
                    authorName = "Sneha Rao",
                    authorKycVerified = true,
                    authorTrustScore = 92,
                    postType = "SEEKING_LOAN",
                    title = "Tuition & Cloud Certification Exam Fees",
                    description = "Need ₹35,000 for AWS Solutions Architect Certification & final semester course fee. DigiLocker Aadhaar/PAN verified, willing to repay in 6 tranches at 12% p.a.",
                    minAmount = 30000.0,
                    maxAmount = 35000.0,
                    interestRate = 12.0,
                    tenureMonths = 6,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "EDUCATION",
                    locationCity = "Hyderabad",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 8,
                    bidsCount = 2,
                    status = "OPEN",
                    createdAt = now - (1 * oneDayMs)
                ),
                MarketplacePostEntity(
                    postId = "post_demo_offer_2",
                    authorId = "usr_demo_rajesh",
                    authorName = "Rajesh Kapoor",
                    authorKycVerified = true,
                    authorTrustScore = 94,
                    postType = "OFFER_TO_LEND",
                    title = "Emergency Medical & Health Micro-Grants",
                    description = "Providing rapid disbursal loans between ₹15,000 and ₹50,000 for verified medical emergencies and hospital bills. Flat 9.5% p.a., same-day approval.",
                    minAmount = 15000.0,
                    maxAmount = 50000.0,
                    interestRate = 9.5,
                    tenureMonths = 6,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "MEDICAL",
                    locationCity = "Mumbai",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 22,
                    bidsCount = 4,
                    status = "OPEN",
                    createdAt = now - (3 * oneDayMs)
                ),
                MarketplacePostEntity(
                    postId = "post_demo_req_2",
                    authorId = "usr_demo_amit",
                    authorName = "Amit Verma",
                    authorKycVerified = true,
                    authorTrustScore = 89,
                    postType = "SEEKING_LOAN",
                    title = "Shop Inventory Stock for Festival Season",
                    description = "Looking for ₹60,000 working capital to stock retail electronic appliances for upcoming festival sales. Fast repayment in 4 months with 12.5% interest.",
                    minAmount = 50000.0,
                    maxAmount = 60000.0,
                    interestRate = 12.5,
                    tenureMonths = 4,
                    repaymentFrequency = "MONTHLY",
                    purposeCategory = "BUSINESS",
                    locationCity = "New Delhi",
                    incomeProofStatus = "VERIFIED",
                    vouchCount = 5,
                    bidsCount = 1,
                    status = "OPEN",
                    createdAt = now - (4 * oneDayMs)
                )
            )
            database.marketplaceDao().insertPosts(demoPosts)

            // Sample Bids on posts
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

            // 6. Insert 4 Interactive Notifications
            val demoNotifications = listOf(
                NotificationEntity(
                    notificationId = "notif_demo_1",
                    userId = currentUserId,
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
                    userId = currentUserId,
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
                    userId = currentUserId,
                    title = "✅ DigiLocker KYC Verified",
                    message = "Aadhaar UIDAI and PAN ITD documents verified successfully via Sandbox API.",
                    type = "SYSTEM",
                    timestamp = now - (3 * oneDayMs),
                    isRead = true,
                    actionRoute = "profile"
                ),
                NotificationEntity(
                    notificationId = "notif_demo_4",
                    userId = currentUserId,
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

            // 7. Insert Audit Events
            database.auditEventDao().insertEvent(
                AuditEventEntity(
                    eventId = "audit_demo_1",
                    entityType = "USER",
                    entityId = currentUserId,
                    actor = currentUserId,
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
                    actor = currentUserId,
                    event = "DISBURSED",
                    newState = "ACTIVE",
                    description = "Loan ₹50,000 disbursed to borrower Rahul Sharma."
                )
            )

            Result.success("Demo data successfully pushed across Home, Loans, Community Wall & Notifications!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears all demo entities cleanly from the database.
     */
    suspend fun clearDemoData(currentUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Delete demo notifications
            database.notificationDao().deleteNotification("notif_demo_1")
            database.notificationDao().deleteNotification("notif_demo_2")
            database.notificationDao().deleteNotification("notif_demo_3")
            database.notificationDao().deleteNotification("notif_demo_4")

            // Delete demo marketplace posts
            database.marketplaceDao().deletePost("post_demo_offer_1")
            database.marketplaceDao().deletePost("post_demo_req_1")
            database.marketplaceDao().deletePost("post_demo_offer_2")
            database.marketplaceDao().deletePost("post_demo_req_2")

            // Delete demo repayments & loans
            database.repaymentDao().deleteRepayment(
                RepaymentEntity("repay_demo_lent_1", DEMO_LOAN_LENT_ID, 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
            )
            database.repaymentDao().deleteRepayment(
                RepaymentEntity("repay_demo_lent_2", DEMO_LOAN_LENT_ID, 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
            )
            database.repaymentDao().deleteRepayment(
                RepaymentEntity("repay_demo_borrowed_1", DEMO_LOAN_BORROWED_ID, 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
            )
            database.repaymentDao().deleteRepayment(
                RepaymentEntity("repay_demo_borrowed_2", DEMO_LOAN_BORROWED_ID, 0.0, "", "", 0L, null, 0.0, 0.0, 0.0)
            )

            val loanLent = database.loanDao().getLoanById(DEMO_LOAN_LENT_ID)
            if (loanLent != null) database.loanDao().deleteLoan(loanLent)

            val loanBorrowed = database.loanDao().getLoanById(DEMO_LOAN_BORROWED_ID)
            if (loanBorrowed != null) database.loanDao().deleteLoan(loanBorrowed)

            val loanClosed = database.loanDao().getLoanById(DEMO_LOAN_CLOSED_ID)
            if (loanClosed != null) database.loanDao().deleteLoan(loanClosed)

            Result.success("Demo data cleared successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
