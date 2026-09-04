package com.loanzo.app.data.repository

import com.loanzo.app.data.dao.*
import com.loanzo.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val complaintDao: ComplaintDao,
    private val mediationMeetingDao: MediationMeetingDao,
    private val collateralVaultDao: CollateralVaultDao,
    private val nocCertificateDao: NocCertificateDao,
    private val agentDao: AgentDao,
    private val userDao: UserDao,
    private val loanDao: LoanDao
) {

    val allComplaints: Flow<List<ComplaintEntity>> = complaintDao.getAllComplaints()
    val allMeetings: Flow<List<MediationMeetingEntity>> = mediationMeetingDao.getAllMeetings()
    val upcomingMeetings: Flow<List<MediationMeetingEntity>> = mediationMeetingDao.getUpcomingMeetings()
    val allVaultItems: Flow<List<CollateralVaultEntity>> = collateralVaultDao.getAllVaultItems()
    val allNocs: Flow<List<NocCertificateEntity>> = nocCertificateDao.getAllNocs()
    val unassignedVisits: Flow<List<AgentVisitEntity>> = agentDao.getVisitsForAgent("UNASSIGNED")
    val allVisits: Flow<List<AgentVisitEntity>> = agentDao.getAllVisits()
    val allAgentApplications: Flow<List<AgentApplicationEntity>> = agentDao.getAllApplications()

    // --- Dispatch Engine: Loan-to-Agent Mapping ---

    suspend fun assignAgentToVisit(visitId: String, agentId: String, payoutAmount: Double) {
        val existingVisit = agentDao.getVisitById(visitId) ?: return
        val updatedVisit = existingVisit.copy(
            agentId = agentId,
            payoutAmount = payoutAmount,
            status = "SCHEDULED"
        )
        agentDao.updateVisit(updatedVisit)
    }

    // --- Collateral & Safe Vault Management ---

    suspend fun assignLockerAndSeal(
        vaultItemId: String,
        lockerNumber: String,
        barcodeTag: String,
        sealNumber: String
    ) {
        collateralVaultDao.assignLockerAndSeal(
            vaultItemId = vaultItemId,
            lockerNumber = lockerNumber,
            barcodeTag = barcodeTag,
            sealNumber = sealNumber,
            status = "SECURED_IN_VAULT"
        )
    }

    suspend fun releaseCollateral(loanId: String) {
        collateralVaultDao.updateCustodyStatusByLoan(
            loanId = loanId,
            status = "RELEASED",
            releaseDate = System.currentTimeMillis()
        )
    }

    // --- Complaints & Grievances ---

    suspend fun resolveComplaint(complaintId: String, resolutionNotes: String) {
        complaintDao.updateComplaintStatus(
            complaintId = complaintId,
            status = "RESOLVED",
            notes = resolutionNotes,
            resolvedAt = System.currentTimeMillis()
        )
    }

    suspend fun dismissComplaint(complaintId: String, dismissalNotes: String) {
        complaintDao.updateComplaintStatus(
            complaintId = complaintId,
            status = "DISMISSED",
            notes = dismissalNotes,
            resolvedAt = System.currentTimeMillis()
        )
    }

    // --- Mediation & Hearings ---

    suspend fun scheduleMediationMeeting(meeting: MediationMeetingEntity) {
        mediationMeetingDao.insertMeeting(meeting)
        if (!meeting.complaintId.isNullOrBlank()) {
            complaintDao.updateComplaintStatus(
                complaintId = meeting.complaintId,
                status = "HEARING_SCHEDULED",
                notes = "Mediation hearing scheduled: ${meeting.title} on ${meeting.scheduledTimeSlotStr}",
                resolvedAt = null
            )
        }
    }

    suspend fun updateMeetingStatus(meetingId: String, status: String, notes: String?) {
        mediationMeetingDao.updateMeetingStatus(meetingId, status, notes)
    }

    // --- Legal NOC Issuance ---

    suspend fun generateNoc(
        loanId: String,
        borrowerId: String,
        borrowerName: String,
        borrowerPan: String,
        lenderId: String,
        lenderName: String,
        principalAmount: Double,
        totalRepaidAmount: Double,
        collateralDesc: String
    ): NocCertificateEntity {
        val rawSignature = "LOANZO:NOC:$loanId:$borrowerId:$principalAmount:${System.currentTimeMillis()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawSignature.toByteArray(Charsets.UTF_8))
        val digitalSignatureHash = hashBytes.joinToString("") { "%02x".format(it) }.take(32).uppercase()

        val nocId = "NOC-" + SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()
        val noc = NocCertificateEntity(
            nocId = nocId,
            loanId = loanId,
            borrowerId = borrowerId,
            borrowerName = borrowerName,
            borrowerPan = borrowerPan.ifBlank { "ABCDE1234F" },
            lenderId = lenderId,
            lenderName = lenderName,
            principalAmount = principalAmount,
            totalRepaidAmount = totalRepaidAmount,
            collateralReleasedDesc = collateralDesc,
            digitalSignatureHash = "LNZ-$digitalSignatureHash",
            issuedAt = System.currentTimeMillis(),
            issuedByAdminId = "ADMIN-SATYAM-0810",
            status = "ACTIVE_CLEARANCE"
        )
        nocCertificateDao.insertNoc(noc)
        // Automatically unencumber collateral
        collateralVaultDao.updateCustodyStatusByLoan(loanId, "READY_FOR_RELEASE", System.currentTimeMillis())
        return noc
    }

    // --- Agent Roster Actions ---

    suspend fun suspendAgent(userId: String, reason: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(agentStatus = "SUSPENDED", isOnDuty = false))
    }

    suspend fun reactivateAgent(userId: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(agentStatus = "APPROVED", isOnDuty = true))
    }

    // --- Seed Sample Admin Operations Data ---

    suspend fun seedSampleAdminDataIfEmpty() {
        val currentComplaints = complaintDao.getAllComplaints().firstOrNull()
        if (currentComplaints.isNullOrEmpty()) {
            val sampleComplaints = listOf(
                ComplaintEntity(
                    complaintId = "CMP-80194",
                    complainantId = "USER-BRW-491",
                    complainantName = "Rahul Verma",
                    complainantRole = "BORROWER",
                    complainantPhone = "+919876543210",
                    targetPartyId = "USER-LND-819",
                    targetPartyName = "Kapil Dev Sharma",
                    targetPartyRole = "LENDER",
                    loanId = "LOAN-84920",
                    category = "COLLATERAL_CUSTODY",
                    priority = "CRITICAL_LEGAL",
                    subject = "Delayed Collateral Return Post Repayment",
                    description = "I have paid all 6 EMIs in full along with interest on Aug 28th. However, my 22K Gold coins collateral is still held in the vault and NOC certificate has not been stamped yet.",
                    status = "OPEN",
                    createdAt = System.currentTimeMillis() - 172800000L
                ),
                ComplaintEntity(
                    complaintId = "CMP-81204",
                    complainantId = "USER-LND-302",
                    complainantName = "Suresh Singhal",
                    complainantRole = "LENDER",
                    complainantPhone = "+919810554433",
                    targetPartyId = "USER-BRW-991",
                    targetPartyName = "Deepak Chawla",
                    targetPartyRole = "BORROWER",
                    loanId = "LOAN-91044",
                    category = "DELAYED_PAYMENT",
                    priority = "HIGH",
                    subject = "Tranche 2 Overdue by 14 Days",
                    description = "Borrower Deepak Chawla is unresponsive on calls. Tranche repayment of ₹45,000 was due on 15th August. Requesting physical agent visit and legal recovery notice.",
                    status = "INVESTIGATING",
                    createdAt = System.currentTimeMillis() - 86400000L
                ),
                ComplaintEntity(
                    complaintId = "CMP-82551",
                    complainantId = "AGENT-DEV-101",
                    complainantName = "Vikram Singh (Field Agent)",
                    complainantRole = "AGENT",
                    complainantPhone = "+919811224466",
                    targetPartyId = "USER-BRW-110",
                    targetPartyName = "Amitabh Verma",
                    targetPartyRole = "BORROWER",
                    loanId = "LOAN-77319",
                    category = "AGENT_CONDUCT",
                    priority = "MEDIUM",
                    subject = "Borrower Refused Physical Property Access",
                    description = "Visited designated residence in Green Park. Borrower refused to show original property registry papers and behaved aggressively with inspection officer.",
                    status = "OPEN",
                    createdAt = System.currentTimeMillis() - 36000000L
                )
            )
            complaintDao.insertComplaints(sampleComplaints)
        }

        val currentVault = collateralVaultDao.getAllVaultItems().firstOrNull()
        if (currentVault.isNullOrEmpty()) {
            val sampleVault = listOf(
                CollateralVaultEntity(
                    vaultItemId = "VLT-DELHI-001",
                    loanId = "LOAN-84920",
                    borrowerId = "USER-BRW-491",
                    borrowerName = "Rahul Verma",
                    borrowerPhone = "+919876543210",
                    assetDescription = "22K Hallmark Gold Coins (20g total weight)",
                    assetType = "GOLD",
                    estimatedValue = 150000.0,
                    appraisedPurityOrCondition = "91.6% Pure Gold (Tanishq Assay Certified)",
                    vaultFacilityName = "Loanzo Central Vault - Connaught Place, New Delhi",
                    lockerNumber = "LOCKER-A14",
                    barcodeTag = "LNZ-GLD-8829-DEL",
                    tamperSealNumber = "SEAL-9948201",
                    custodyStatus = "SECURED_IN_VAULT",
                    intakeAgentId = "AGENT-DEV-101",
                    intakeDate = System.currentTimeMillis() - 604800000L
                ),
                CollateralVaultEntity(
                    vaultItemId = "VLT-DELHI-002",
                    loanId = "LOAN-77319",
                    borrowerId = "USER-BRW-110",
                    borrowerName = "Amitabh Verma",
                    borrowerPhone = "+919711556677",
                    assetDescription = "Commercial Office Original Property Title Deed",
                    assetType = "PROPERTY_DEED",
                    estimatedValue = 8500000.0,
                    appraisedPurityOrCondition = "Original Deed registered at Sub-Registrar Office, Mehrauli",
                    vaultFacilityName = "Loanzo Central Vault - Connaught Place, New Delhi",
                    lockerNumber = "SAFE-COMP-C09",
                    barcodeTag = "LNZ-PROP-7731-DEL",
                    tamperSealNumber = "SEAL-8831902",
                    custodyStatus = "ENCUMBERED",
                    intakeAgentId = "AGENT-DEV-101",
                    intakeDate = System.currentTimeMillis() - 1209600000L
                ),
                CollateralVaultEntity(
                    vaultItemId = "VLT-DELHI-003",
                    loanId = "LOAN-91044",
                    borrowerId = "USER-BRW-991",
                    borrowerName = "Deepak Chawla",
                    borrowerPhone = "+919650112233",
                    assetDescription = "Hyundai Creta SX 2022 (Original RC & Duplicate Key)",
                    assetType = "VEHICLE_TITLE",
                    estimatedValue = 950000.0,
                    appraisedPurityOrCondition = "Clean RTO record, Hypothecation Endorsed",
                    vaultFacilityName = "Loanzo Secure Vehicle Yard - Sector 62, Noida",
                    lockerNumber = "YARD-BAY-44",
                    barcodeTag = "LNZ-VEH-9104-UP",
                    tamperSealNumber = "SEAL-7710493",
                    custodyStatus = "PENDING_INTAKE"
                )
            )
            collateralVaultDao.insertVaultItems(sampleVault)
        }

        val currentMeetings = mediationMeetingDao.getAllMeetings().firstOrNull()
        if (currentMeetings.isNullOrEmpty()) {
            val sampleMeetings = listOf(
                MediationMeetingEntity(
                    meetingId = "MEET-99201",
                    title = "Dispute Arbitration: Collateral Release post EMI Clearance",
                    agenda = "Review final bank statement of Rahul Verma, confirm zero-due with Kapil Dev Sharma, and approve digital NOC release.",
                    loanId = "LOAN-84920",
                    complaintId = "CMP-80194",
                    borrowerName = "Rahul Verma",
                    borrowerPhone = "+919876543210",
                    lenderName = "Kapil Dev Sharma",
                    lenderPhone = "+919811223344",
                    meetingType = "GOOGLE_MEET",
                    meetingLinkOrLocation = "https://meet.google.com/loa-nzo-med",
                    scheduledDateTime = System.currentTimeMillis() + 18000000L, // 5 hours later
                    scheduledTimeSlotStr = "Today, 04:30 PM - 05:15 PM",
                    status = "SCHEDULED"
                ),
                MediationMeetingEntity(
                    meetingId = "MEET-99342",
                    title = "Physical Vault Inspection & Appraisal Hearing",
                    agenda = "In-person verification of 22K Gold ornaments and diamond grading before escrow disbursement.",
                    loanId = "LOAN-77319",
                    borrowerName = "Amitabh Verma",
                    borrowerPhone = "+919711556677",
                    lenderName = "Suresh Singhal",
                    lenderPhone = "+919810554433",
                    agentName = "Vikram Singh",
                    meetingType = "PHYSICAL_VAULT",
                    meetingLinkOrLocation = "Loanzo Central Vault, Barakhamba Road, Connaught Place, New Delhi",
                    scheduledDateTime = System.currentTimeMillis() + 86400000L, // Tomorrow
                    scheduledTimeSlotStr = "Tomorrow, 11:30 AM - 12:30 PM",
                    status = "SCHEDULED"
                )
            )
            mediationMeetingDao.insertMeetings(sampleMeetings)
        }

        // Also check if there are unassigned visits to dispatch
        val currentVisits = agentDao.getAllVisits().firstOrNull()
        if (currentVisits.isNullOrEmpty() || currentVisits.none { it.agentId == "UNASSIGNED" }) {
            val unassigned = listOf(
                AgentVisitEntity(
                    visitId = "VISIT-UNASSIGNED-1",
                    agentId = "UNASSIGNED",
                    loanId = "LOAN-60291",
                    visitType = "COLLATERAL_VERIFICATION",
                    title = "Gold Appraisal & Purity Testing (45g 22K Ornaments)",
                    borrowerName = "Meenakshi Sundaram",
                    borrowerPhone = "+919840112233",
                    borrowerAddress = "B-44, Greater Kailash Part 1, New Delhi",
                    lenderName = "Rakesh Jhunjhunwala Capital",
                    lenderPhone = "+919820011223",
                    lenderAddress = "Nariman Point, Mumbai",
                    targetAddress = "B-44, Greater Kailash Part 1, New Delhi - 110048",
                    targetLatitude = 28.5482,
                    targetLongitude = 77.2344,
                    scheduledDate = "Tomorrow",
                    scheduledTimeSlot = "11:00 AM - 12:30 PM",
                    payoutAmount = 950.0,
                    collateralItemName = "22K Gold Bangles & Necklace (45g)",
                    collateralEstimatedValue = 310000.0,
                    collateralPledgedValue = 220000.0,
                    status = "SCHEDULED"
                ),
                AgentVisitEntity(
                    visitId = "VISIT-UNASSIGNED-2",
                    agentId = "UNASSIGNED",
                    loanId = "LOAN-60344",
                    visitType = "BORROWER_VERIFICATION",
                    title = "Borrower Residence & Salary Document Verification",
                    borrowerName = "Anurag Kashyap",
                    borrowerPhone = "+919910445566",
                    borrowerAddress = "Flat 1203, Tower 4, Cyber City, Gurugram",
                    lenderName = "Pawan Munjal",
                    lenderPhone = "+919811002233",
                    lenderAddress = "Civil Lines, Delhi",
                    targetAddress = "Flat 1203, Tower 4, Cyber City, Gurugram, Haryana - 122002",
                    targetLatitude = 28.4950,
                    targetLongitude = 77.0895,
                    scheduledDate = "Tomorrow",
                    scheduledTimeSlot = "03:00 PM - 04:00 PM",
                    payoutAmount = 650.0,
                    collateralItemName = "Income Proof & Employment Letter",
                    collateralEstimatedValue = 0.0,
                    collateralPledgedValue = 80000.0,
                    status = "SCHEDULED"
                )
            )
            agentDao.insertVisits(unassigned)
        }
    }
}
