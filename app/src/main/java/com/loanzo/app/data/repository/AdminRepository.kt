package com.loanzo.app.data.repository

import com.loanzo.app.data.dao.*
import com.loanzo.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

import com.loanzo.app.util.TelegramManager
import com.google.firebase.firestore.FirebaseFirestore

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.tasks.await

@Singleton
class AdminRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val complaintDao: ComplaintDao,
    private val mediationMeetingDao: MediationMeetingDao,
    private val collateralVaultDao: CollateralVaultDao,
    private val nocCertificateDao: NocCertificateDao,
    private val agentDao: AgentDao,
    private val userDao: UserDao,
    private val loanDao: LoanDao,
    private val notificationDao: NotificationDao,
    private val adminRequestDao: AdminRequestDao,
    private val telegramManager: TelegramManager
) {

    val allComplaints: Flow<List<ComplaintEntity>> = complaintDao.getAllComplaints()
    val allMeetings: Flow<List<MediationMeetingEntity>> = mediationMeetingDao.getAllMeetings()
    val upcomingMeetings: Flow<List<MediationMeetingEntity>> = mediationMeetingDao.getUpcomingMeetings()
    val allVaultItems: Flow<List<CollateralVaultEntity>> = collateralVaultDao.getAllVaultItems()
    val allNocs: Flow<List<NocCertificateEntity>> = nocCertificateDao.getAllNocs()
    val unassignedVisits: Flow<List<AgentVisitEntity>> = agentDao.getUnassignedVisits()
    val allVisits: Flow<List<AgentVisitEntity>> = agentDao.getAllVisits()
    val allAgentApplications: Flow<List<AgentApplicationEntity>> = agentDao.getAllApplications()
    val allAdminRequests: Flow<List<AdminRequestEntity>> = adminRequestDao.getAllRequests()
    val pendingAdminRequests: Flow<List<AdminRequestEntity>> = adminRequestDao.getPendingRequests()
    val allCrossVerificationVisits: Flow<List<AgentVisitEntity>> = agentDao.getAllCrossVerificationVisits()

    // --- Dispatch Engine: Loan-to-Agent Mapping ---

    suspend fun getLoanById(loanId: String): LoanEntity? = loanDao.getLoanById(loanId)

    suspend fun assignAgentToVisit(visitId: String, agentId: String, payoutAmount: Double) {
        val existingVisit = agentDao.getVisitById(visitId) ?: return
        val agentUser = userDao.getUserById(agentId)
        val agentName = agentUser?.name?.ifBlank { null } ?: agentUser?.username?.ifBlank { null } ?: "Field Officer"
        val agentPhone = agentUser?.phone?.ifBlank { null } ?: "+919876543210"
        val pin = if (existingVisit.handshakePin.isNotBlank()) existingVisit.handshakePin else String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)
        val updatedVisit = existingVisit.copy(
            agentId = agentId,
            assignedAgentName = agentName,
            agentPhone = agentPhone,
            payoutAmount = payoutAmount,
            status = "SCHEDULED",
            visitStageStatus = "DISPATCHED",
            handshakePin = pin,
            isHandshakePinVerified = false
        )
        agentDao.updateVisit(updatedVisit)
        try {
            FirebaseFirestore.getInstance().collection("agent_visits")
                .document(visitId)
                .set(updatedVisit)
        } catch (_: Exception) {}

        sendUserNotification(
            userId = agentId,
            title = "📋 New Inspection Assigned!",
            message = "You have been assigned to verify ${updatedVisit.title}. Scheduled payout: ₹${payoutAmount.toInt()}.",
            actionRoute = "agent_main"
        )

        // Notify borrower that officer is assigned and PIN is ready
        if (existingVisit.loanId.isNotBlank()) {
            val loan = loanDao.getLoanById(existingVisit.loanId)
            val borrowerId = loan?.borrowerId
            if (!borrowerId.isNullOrBlank()) {
                sendUserNotification(
                    userId = borrowerId,
                    title = "Field Verification Officer Assigned 🛡️",
                    message = "Officer $agentName has been assigned to conduct physical verification. Handshake PIN is ready in your loan details.",
                    actionRoute = "loan_detail/${existingVisit.loanId}"
                )
            }
        }
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
        try {
            val item = collateralVaultDao.getVaultItemById(vaultItemId)
            if (item != null) {
                FirebaseFirestore.getInstance().collection("collateral_vault")
                    .document(vaultItemId)
                    .set(item)
            }
        } catch (_: Exception) {}
    }

    suspend fun releaseCollateral(loanId: String) {
        collateralVaultDao.updateCustodyStatusByLoan(
            loanId = loanId,
            status = "RELEASED",
            releaseDate = System.currentTimeMillis()
        )
        try {
            val item = collateralVaultDao.getVaultItemByLoanId(loanId)
            if (item != null) {
                FirebaseFirestore.getInstance().collection("collateral_vault")
                    .document(item.vaultItemId)
                    .set(item)
            }
        } catch (_: Exception) {}
    }

    // --- Complaints & Grievances ---

    suspend fun resolveComplaint(complaintId: String, resolutionNotes: String) {
        complaintDao.updateComplaintStatus(
            complaintId = complaintId,
            status = "RESOLVED",
            notes = resolutionNotes,
            resolvedAt = System.currentTimeMillis()
        )
        try {
            val comp = complaintDao.getComplaintById(complaintId)
            if (comp != null) {
                FirebaseFirestore.getInstance().collection("complaints")
                    .document(complaintId)
                    .set(comp)

                sendUserNotification(
                    userId = comp.complainantId,
                    title = "Grievance Resolved: ${comp.subject}",
                    message = "Your grievance has been officially resolved: $resolutionNotes",
                    actionRoute = "profile"
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun dismissComplaint(complaintId: String, dismissalNotes: String) {
        complaintDao.updateComplaintStatus(
            complaintId = complaintId,
            status = "DISMISSED",
            notes = dismissalNotes,
            resolvedAt = System.currentTimeMillis()
        )
        try {
            val comp = complaintDao.getComplaintById(complaintId)
            if (comp != null) {
                FirebaseFirestore.getInstance().collection("complaints")
                    .document(complaintId)
                    .set(comp)
            }
        } catch (_: Exception) {}
    }

    suspend fun submitComplaint(complaint: ComplaintEntity) {
        complaintDao.insertComplaint(complaint)
        try {
            FirebaseFirestore.getInstance()
                .collection("complaints")
                .document(complaint.complaintId)
                .set(complaint)
        } catch (_: Exception) {}

        // Create In-App Notification for Admin
        val notifId = "notif_complaint_${complaint.complaintId}"
        val notif = NotificationEntity(
            notificationId = notifId,
            userId = "ADMIN",
            title = "⚖️ New Grievance / Dispute Filed",
            message = "${complaint.complainantName} reported ${complaint.targetPartyName}: ${complaint.subject}",
            type = "COMPLAINT",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = "app_owner_hub?tab=5"
        )
        try {
            notificationDao.insertNotification(notif)
            FirebaseFirestore.getInstance().collection("notifications")
                .document(notifId)
                .set(notif)
        } catch (_: Exception) {}
    }

    suspend fun authorizeDeviceTransfer(complaintId: String, usernameOrId: String, newDeviceId: String, newDeviceModel: String) {
        val user = userDao.getUserByUsername(usernameOrId) ?: userDao.getUserById(usernameOrId)
        if (user != null) {
            val updated = user.copy(
                registeredDeviceId = newDeviceId,
                registeredDeviceModel = newDeviceModel
            )
            userDao.updateUser(updated)
            try {
                FirebaseFirestore.getInstance().collection("users")
                    .document(user.userId)
                    .set(updated)
            } catch (_: Exception) {}
        }
        complaintDao.updateComplaintStatus(
            complaintId = complaintId,
            status = "RESOLVED",
            notes = "Device transfer authorized by Master Admin to $newDeviceModel ($newDeviceId)",
            resolvedAt = System.currentTimeMillis()
        )
        try {
            val comp = complaintDao.getComplaintById(complaintId)
            if (comp != null) {
                FirebaseFirestore.getInstance().collection("complaints")
                    .document(complaintId)
                    .set(comp)
            }
        } catch (_: Exception) {}
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
            try {
                val comp = complaintDao.getComplaintById(meeting.complaintId)
                if (comp != null) {
                    FirebaseFirestore.getInstance().collection("complaints")
                        .document(meeting.complaintId)
                        .set(comp)
                }
            } catch (_: Exception) {}
        }
        try {
            FirebaseFirestore.getInstance().collection("mediation_meetings")
                .document(meeting.meetingId)
                .set(meeting)
        } catch (_: Exception) {}
    }

    suspend fun updateMeetingStatus(meetingId: String, status: String, notes: String?) {
        mediationMeetingDao.updateMeetingStatus(meetingId, status, notes)
        try {
            val meeting = mediationMeetingDao.getMeetingById(meetingId)
            if (meeting != null) {
                FirebaseFirestore.getInstance().collection("mediation_meetings")
                    .document(meetingId)
                    .set(meeting)
            }
        } catch (_: Exception) {}
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

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("noc_certificates")
                .document(noc.nocId)
                .set(noc)

            val vaultItem = collateralVaultDao.getVaultItemByLoanId(loanId)
            if (vaultItem != null) {
                firestore.collection("collateral_vault")
                    .document(vaultItem.vaultItemId)
                    .set(vaultItem)
            }
        } catch (_: Exception) {}

        sendUserNotification(
            userId = borrowerId,
            title = "📜 Legal No-Objection Certificate Issued!",
            message = "Loan clearance certificate #$nocId issued. Collateral has been released from vault custody.",
            actionRoute = "loans"
        )

        return noc
    }

    // --- Agent Roster Actions ---

    suspend fun suspendAgent(userId: String, reason: String) {
        val user = userDao.getUserById(userId) ?: return
        val updated = user.copy(agentStatus = "SUSPENDED", isOnDuty = false)
        userDao.updateUser(updated)
        try {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(updated)
        } catch (_: Exception) {}

        sendUserNotification(
            userId = userId,
            title = "Agent Account Suspended",
            message = "Your field officer privileges have been temporarily suspended: $reason",
            actionRoute = "profile"
        )
    }

    suspend fun reactivateAgent(userId: String) {
        val user = userDao.getUserById(userId) ?: return
        val updated = user.copy(agentStatus = "APPROVED", isOnDuty = true)
        userDao.updateUser(updated)
        try {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(updated)
        } catch (_: Exception) {}

        sendUserNotification(
            userId = userId,
            title = "Agent Account Re-activated",
            message = "Your field officer credentials are now active. You may resume field inspections.",
            actionRoute = "agent_main"
        )
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

    // --- Admin Access Request Engine & Instant Telegram Routing ---

    suspend fun submitAdminRequest(
        userId: String,
        userName: String,
        userPhone: String,
        userEmail: String,
        currentRole: String = "MEMBER",
        justification: String = "",
        requestedRole: String = "ADMIN"
    ) {
        val req = AdminRequestEntity(
            requestId = "admin_req_" + UUID.randomUUID().toString().take(8),
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            userEmail = userEmail,
            reason = justification,
            currentRole = currentRole,
            requestedRole = requestedRole
        )
        submitAdminRequest(req)
    }

    suspend fun submitAdminRequest(request: AdminRequestEntity) {
        adminRequestDao.insertRequest(request)

        // 1. Push to Firestore collection 'admin_requests'
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("admin_requests")
                .document(request.requestId)
                .set(request)
        } catch (_: Exception) {}

        // 2. Insert In-App Notification for Admin in Room and Firestore
        val notifId = "notif_admin_req_${request.requestId}"
        val notif = NotificationEntity(
            notificationId = notifId,
            userId = "ADMIN",
            title = "👑 New Platform Admin Request",
            message = "${request.userName} requested ${request.requestedRole} access: \"${request.reason}\"",
            type = "ADMIN_REQUEST",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = "app_owner_hub?tab=0"
        )
        try {
            notificationDao.insertNotification(notif)
            FirebaseFirestore.getInstance().collection("notifications")
                .document(notifId)
                .set(notif)
        } catch (_: Exception) {}

        // 3. Send Telegram Alert to Master Admin (@satyam_081)
        try {
            telegramManager.sendAdminAlert(
                """
                <b>👑 New Platform Admin / Staff Access Request</b>

                <b>Applicant:</b> ${TelegramManager.escapeHtml(request.userName)}
                <b>User ID:</b> <code>${TelegramManager.escapeHtml(request.userId)}</code>
                <b>Phone:</b> ${TelegramManager.escapeHtml(request.userPhone)}
                <b>Email:</b> ${TelegramManager.escapeHtml(request.userEmail)}
                <b>Requested Role:</b> ${TelegramManager.escapeHtml(request.requestedRole)}
                <b>Justification:</b> <i>"${TelegramManager.escapeHtml(request.reason)}"</i>

                <i>Action: Review in Master Admin Command Center -> Users & KYC Tab</i>
                """.trimIndent()
            )
        } catch (_: Exception) {}
    }

    suspend fun approveAdminRequest(requestId: String, adminNotes: String? = "Approved by Master Admin") {
        val req = adminRequestDao.getRequestById(requestId) ?: return
        adminRequestDao.updateRequestStatus(
            requestId = requestId,
            status = "APPROVED",
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = "satyam_081",
            adminNotes = adminNotes
        )

        try {
            val updatedReq = adminRequestDao.getRequestById(requestId)
            if (updatedReq != null) {
                FirebaseFirestore.getInstance().collection("admin_requests")
                    .document(requestId)
                    .set(updatedReq)
            }
        } catch (_: Exception) {}

        // Elevate user role to ADMIN in Firestore directly (guarantees remote applicant gets updated role)
        try {
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(req.userId)
            userDocRef.update("role", req.requestedRole).addOnFailureListener {
                userDocRef.set(
                    mapOf("userId" to req.userId, "role" to req.requestedRole),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
        } catch (_: Exception) {}

        val user = userDao.getUserById(req.userId)
        if (user != null) {
            val updatedUser = user.copy(role = req.requestedRole)
            userDao.updateUser(updatedUser)
        }

        // Notify user via local Room and Firestore
        sendUserNotification(
            userId = req.userId,
            title = "👑 Platform Role Elevated to ${req.requestedRole}!",
            message = "Your request has been approved by the Master Admin. You now have privileged administrative access to the platform console.",
            actionRoute = "app_owner_hub"
        )
    }

    suspend fun rejectAdminRequest(requestId: String, reason: String) {
        val req = adminRequestDao.getRequestById(requestId) ?: return
        adminRequestDao.updateRequestStatus(
            requestId = requestId,
            status = "REJECTED",
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = "satyam_081",
            adminNotes = reason
        )

        try {
            val updatedReq = adminRequestDao.getRequestById(requestId)
            if (updatedReq != null) {
                FirebaseFirestore.getInstance().collection("admin_requests")
                    .document(requestId)
                    .set(updatedReq)
            }
        } catch (_: Exception) {}

        sendUserNotification(
            userId = req.userId,
            title = "Admin Access Request Update",
            message = "Your request for administrative privileges was not approved at this time: $reason",
            actionRoute = "profile"
        )
    }

    // --- Direct Cloud User Notification Delivery ---

    suspend fun sendUserNotification(
        userId: String,
        title: String,
        message: String,
        actionRoute: String? = null
    ) {
        val notifId = "notif_adm_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            notificationId = notifId,
            userId = userId,
            title = title,
            message = message,
            type = "ADMIN_ANNOUNCEMENT",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = actionRoute
        )

        // 1. Insert into local Room
        try {
            notificationDao.insertNotification(notif)
        } catch (_: Exception) {}

        // 2. Push to Firestore for Cloud Delivery to user device
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("notifications")
                .document(notifId)
                .set(notif)
        } catch (_: Exception) {}
    }

    suspend fun dispatchCrossVerificationPair(
        loanId: String,
        loanTitle: String,
        borrowerName: String,
        borrowerPhone: String,
        borrowerAddress: String,
        lenderName: String,
        lenderPhone: String,
        lenderAddress: String,
        agent1Id: String,
        agent2Id: String,
        collateralItemName: String?,
        collateralEstimatedValue: Double?,
        payoutAmount: Double,
        originalVisitId: String? = null
    ): String {
        val pairId = "cross_pair_" + UUID.randomUUID().toString().take(8)
        val visit1Id = "visit_cv_brw_" + UUID.randomUUID().toString().take(8)
        val visit2Id = "visit_cv_lnd_" + UUID.randomUUID().toString().take(8)

        val pin1 = String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)
        val pin2 = String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)

        val user1 = if (agent1Id.isNotBlank()) userDao.getUserById(agent1Id) else null
        val user2 = if (agent2Id.isNotBlank()) userDao.getUserById(agent2Id) else null
        val name1 = user1?.name?.ifBlank { null } ?: user1?.username?.ifBlank { null } ?: "Officer 1"
        val phone1 = user1?.phone?.ifBlank { null } ?: "+919876543210"
        val name2 = user2?.name?.ifBlank { null } ?: user2?.username?.ifBlank { null } ?: "Officer 2"
        val phone2 = user2?.phone?.ifBlank { null } ?: "+919811223344"

        val visit1 = AgentVisitEntity(
            visitId = visit1Id,
            agentId = agent1Id,
            assignedAgentName = name1,
            agentPhone = phone1,
            loanId = loanId,
            visitType = "BORROWER_VERIFICATION",
            title = "Dual Audit: $loanTitle (Borrower Verification)",
            borrowerName = borrowerName,
            borrowerPhone = borrowerPhone,
            borrowerAddress = borrowerAddress,
            lenderName = lenderName,
            lenderPhone = lenderPhone,
            lenderAddress = lenderAddress,
            targetAddress = borrowerAddress,
            scheduledDate = "Today",
            scheduledTimeSlot = "11:00 AM - 12:30 PM",
            payoutAmount = payoutAmount,
            collateralItemName = collateralItemName,
            collateralEstimatedValue = collateralEstimatedValue,
            collateralPledgedValue = collateralEstimatedValue,
            status = "SCHEDULED",
            crossVerificationPairId = pairId,
            isCrossVerification = true,
            counterpartVisitId = visit2Id,
            verificationStage = "STAGE_1_PRIMARY",
            isCounterpartAnonymous = true,
            handshakePin = pin1,
            visitStageStatus = "DISPATCHED",
            distanceKm = 3.6
        )

        val visit2 = AgentVisitEntity(
            visitId = visit2Id,
            agentId = agent2Id,
            assignedAgentName = name2,
            agentPhone = phone2,
            loanId = loanId,
            visitType = "LENDER_VERIFICATION",
            title = "Dual Audit: $loanTitle (Lender & Escrow Verification)",
            borrowerName = borrowerName,
            borrowerPhone = borrowerPhone,
            borrowerAddress = borrowerAddress,
            lenderName = lenderName,
            lenderPhone = lenderPhone,
            lenderAddress = lenderAddress,
            targetAddress = lenderAddress,
            scheduledDate = "Today",
            scheduledTimeSlot = "11:30 AM - 01:00 PM",
            payoutAmount = payoutAmount,
            collateralItemName = collateralItemName,
            collateralEstimatedValue = collateralEstimatedValue,
            collateralPledgedValue = collateralEstimatedValue,
            status = "SCHEDULED",
            crossVerificationPairId = pairId,
            isCrossVerification = true,
            counterpartVisitId = visit1Id,
            verificationStage = "STAGE_1_PRIMARY",
            isCounterpartAnonymous = true,
            handshakePin = pin2,
            visitStageStatus = "DISPATCHED",
            distanceKm = 6.2
        )

        agentDao.insertVisits(listOf(visit1, visit2))

        // Cleanly clear original unassigned backlog visit if dispatched as dual pair
        if (!originalVisitId.isNullOrBlank()) {
            val originalVisit = agentDao.getVisitById(originalVisitId)
            if (originalVisit != null) {
                val updatedOriginal = originalVisit.copy(
                    status = "DISPATCHED",
                    agentId = "DUAL_DISPATCHED",
                    crossVerificationPairId = pairId,
                    visitStageStatus = "DISPATCHED_AS_DUAL_PAIR"
                )
                agentDao.updateVisit(updatedOriginal)
                try {
                    FirebaseFirestore.getInstance().collection("agent_visits")
                        .document(originalVisitId)
                        .set(updatedOriginal)
                } catch (_: Exception) {}
            }
        }

        // Push dispatched visits to Firestore so assigned agents receive them in real-time
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("agent_visits").document(visit1Id).set(visit1)
            firestore.collection("agent_visits").document(visit2Id).set(visit2)
        } catch (_: Exception) {}

        // Notify both assigned field verification officers
        if (agent1Id.isNotBlank()) {
            sendUserNotification(
                userId = agent1Id,
                title = "New Field Verification Task Dispatched 📍",
                message = "You have been assigned to verify $borrowerName for '$loanTitle'. Tap to view dispatch itinerary.",
                actionRoute = "agent_main"
            )
        }
        if (agent2Id.isNotBlank()) {
            sendUserNotification(
                userId = agent2Id,
                title = "New Field Verification Task Dispatched 📍",
                message = "You have been assigned to verify $lenderName for '$loanTitle'. Tap to view dispatch itinerary.",
                actionRoute = "agent_main"
            )
        }

        // Notify borrower of dual-audit dispatch
        if (loanId.isNotBlank()) {
            val loan = loanDao.getLoanById(loanId)
            val borrowerId = loan?.borrowerId
            if (!borrowerId.isNullOrBlank()) {
                sendUserNotification(
                    userId = borrowerId,
                    title = "Dual Verification Protocol Dispatched 🛡️",
                    message = "Two independent officers have been dispatched to verify '$loanTitle'. Handshake PIN is ready in your loan details.",
                    actionRoute = "loan_detail/$loanId"
                )
            }
        }

        return pairId
    }

    /**
     * Posts an Android system status-bar notification for Master Admin alerts.
     */
    fun postAdminSystemNotification(title: String, body: String, actionRoute: String? = null) {
        try {
            com.loanzo.app.util.NotificationChannelHelper.setupNotificationChannels(context)
            val channelId = com.loanzo.app.util.NotificationChannelHelper.CHANNEL_ADMIN
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(context, com.loanzo.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!actionRoute.isNullOrBlank()) {
                    putExtra("navigate_to", actionRoute)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.loanzo.app.R.mipmap.ic_launcher)
                .setContentTitle(title.replace(Regex("[⏰⚡🔔⚠️📜🚨👑⚖️]"), "").trim())
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Failed to post admin system notification", e)
        }
    }

    /**
     * Real-time listener: Continuously syncs admin requests, agent applications,
     * users, and visits from Firestore into Room.
     * Reconciles any existing pending applications on startup and triggers real-time
     * in-app notifications and status bar alerts for Master Admin.
     */
    fun startRealtimeAdminCloudSync(scope: kotlinx.coroutines.CoroutineScope) {
        val firestore = FirebaseFirestore.getInstance()

        // 0. Startup Reconciliation: Pull directly from Cloud Firestore & backfill notifications
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Directly pull pending agent applications from Firestore
                try {
                    val cloudApps = firestore.collection("agent_applications").get().await()
                    for (doc in cloudApps.documents) {
                        val app = doc.toAgentApplication()
                        if (app != null && app.applicationId.isNotBlank()) {
                            agentDao.insertApplication(app)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud apps pull error: ${e.message}")
                }

                // Directly pull pending admin requests from Firestore
                try {
                    val cloudReqs = firestore.collection("admin_requests").get().await()
                    for (doc in cloudReqs.documents) {
                        val req = doc.toAdminRequest()
                        if (req != null && req.requestId.isNotBlank()) {
                            adminRequestDao.insertRequest(req)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud admin reqs pull error: ${e.message}")
                }

                // Directly pull complaints from Firestore
                try {
                    val cloudComps = firestore.collection("complaints").get().await()
                    for (doc in cloudComps.documents) {
                        val comp = doc.toComplaint()
                        if (comp != null && comp.complaintId.isNotBlank()) {
                            complaintDao.insertComplaint(comp)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud complaints pull error: ${e.message}")
                }

                // Directly pull users from Firestore to populate admin's local Room
                try {
                    val cloudUsers = firestore.collection("users").get().await()
                    for (doc in cloudUsers.documents) {
                        val u = doc.toSafeUserEntity()
                        if (u != null && u.userId.isNotBlank()) {
                            val existing = userDao.getUserById(u.userId)
                            if (existing == null) {
                                userDao.insertUser(u)
                            } else {
                                val merged = u.copy(
                                    role = if (existing.role == "ADMIN") "ADMIN" else u.role
                                )
                                userDao.updateUser(merged)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud users pull error: ${e.message}")
                }

                // Reconcile pending agent applications (e.g. Ishita's AGENT-APP-3477EDDF)
                val pendingApps = agentDao.getPendingApplicationsSync()
                for (app in pendingApps) {
                    val notifId = "notif_agent_app_${app.applicationId}"
                    if (!notificationDao.existsNotificationById(notifId)) {
                        val notif = NotificationEntity(
                            notificationId = notifId,
                            userId = "ADMIN",
                            title = "🚨 New Field Agent Application",
                            message = "${app.applicantName} submitted an agent empanelment dossier (${app.priorDomain}, ${app.operatingCity}). Tap to review & approve.",
                            type = "AGENT_APPLICATION",
                            timestamp = app.submittedAt,
                            isRead = false,
                            actionRoute = "app_owner_hub?tab=1"
                        )
                        notificationDao.insertNotification(notif)
                        postAdminSystemNotification(notif.title, notif.message, notif.actionRoute)
                    }
                }

                // Reconcile pending admin requests
                val pendingReqs = adminRequestDao.getPendingRequestsSync()
                for (req in pendingReqs) {
                    val notifId = "notif_admin_req_${req.requestId}"
                    if (!notificationDao.existsNotificationById(notifId)) {
                        val notif = NotificationEntity(
                            notificationId = notifId,
                            userId = "ADMIN",
                            title = "👑 New Platform Admin Request",
                            message = "${req.userName} requested ${req.requestedRole} access: \"${req.reason}\"",
                            type = "ADMIN_REQUEST",
                            timestamp = req.requestedAt,
                            isRead = false,
                            actionRoute = "app_owner_hub?tab=0"
                        )
                        notificationDao.insertNotification(notif)
                        postAdminSystemNotification(notif.title, notif.message, notif.actionRoute)
                    }
                }

                // Reconcile open grievances
                val openComplaints = complaintDao.getOpenComplaintsSync()
                for (comp in openComplaints) {
                    val notifId = "notif_complaint_${comp.complaintId}"
                    if (!notificationDao.existsNotificationById(notifId)) {
                        val notif = NotificationEntity(
                            notificationId = notifId,
                            userId = "ADMIN",
                            title = "⚖️ New Grievance / Dispute Filed",
                            message = "${comp.complainantName} reported ${comp.targetPartyName}: ${comp.subject}",
                            type = "COMPLAINT",
                            timestamp = comp.createdAt,
                            isRead = false,
                            actionRoute = "app_owner_hub?tab=5"
                        )
                        notificationDao.insertNotification(notif)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("AdminRepo", "Startup reconciliation error: ${e.message}")
            }
        }

        // 1. Listen to Admin Requests
        try {
            firestore.collection("admin_requests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val req = doc.toAdminRequest()
                            if (req != null && req.requestId.isNotBlank()) {
                                adminRequestDao.insertRequest(req)
                                if (req.status == "PENDING") {
                                    val notifId = "notif_admin_req_${req.requestId}"
                                    if (!notificationDao.existsNotificationById(notifId)) {
                                        val notif = NotificationEntity(
                                            notificationId = notifId,
                                            userId = "ADMIN",
                                            title = "👑 New Platform Admin Request",
                                            message = "${req.userName} requested ${req.requestedRole} access: \"${req.reason}\"",
                                            type = "ADMIN_REQUEST",
                                            timestamp = req.requestedAt,
                                            isRead = false,
                                            actionRoute = "app_owner_hub?tab=0"
                                        )
                                        notificationDao.insertNotification(notif)
                                        postAdminSystemNotification(notif.title, notif.message, notif.actionRoute)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (_: Exception) {}

        // 2. Listen to Agent Empanelment Applications
        try {
            firestore.collection("agent_applications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val app = doc.toAgentApplication()
                            if (app != null && app.applicationId.isNotBlank()) {
                                agentDao.insertApplication(app)
                                if (app.status == "PENDING") {
                                    val notifId = "notif_agent_app_${app.applicationId}"
                                    if (!notificationDao.existsNotificationById(notifId)) {
                                        val notif = NotificationEntity(
                                            notificationId = notifId,
                                            userId = "ADMIN",
                                            title = "🚨 New Field Agent Application",
                                            message = "${app.applicantName} submitted an agent empanelment dossier (${app.priorDomain}, ${app.operatingCity}). Tap to review & approve.",
                                            type = "AGENT_APPLICATION",
                                            timestamp = app.submittedAt,
                                            isRead = false,
                                            actionRoute = "app_owner_hub?tab=1"
                                        )
                                        notificationDao.insertNotification(notif)
                                        postAdminSystemNotification(notif.title, notif.message, notif.actionRoute)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (_: Exception) {}

        // 3. Listen to Real Registered Users
        try {
            firestore.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val user = doc.toSafeUserEntity()
                                if (user != null && user.userId.isNotBlank()) {
                                    val existing = userDao.getUserById(user.userId)
                                    if (existing == null) {
                                        userDao.insertUser(user)
                                    } else {
                                        // Merge without overwriting local admin roles
                                        val merged = user.copy(
                                            role = if (existing.role == "ADMIN") "ADMIN" else user.role
                                        )
                                        userDao.updateUser(merged)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
        } catch (_: Exception) {}

        // 4. Listen to Agent Visits
        try {
            firestore.collection("agent_visits")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val visit = doc.toObject(AgentVisitEntity::class.java)
                            if (visit != null && visit.visitId.isNotBlank()) {
                                agentDao.insertVisit(visit)
                            }
                        }
                    }
                }
        } catch (_: Exception) {}

        // 5. Listen to Real User Complaints & Reports
        try {
            firestore.collection("complaints")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val comp = doc.toComplaint()
                            if (comp != null && comp.complaintId.isNotBlank()) {
                                complaintDao.insertComplaint(comp)
                                if (comp.status == "OPEN" || comp.status == "INVESTIGATING") {
                                    val notifId = "notif_complaint_${comp.complaintId}"
                                    if (!notificationDao.existsNotificationById(notifId)) {
                                        val notif = NotificationEntity(
                                            notificationId = notifId,
                                            userId = "ADMIN",
                                            title = "⚖️ New Grievance / Dispute Filed",
                                            message = "${comp.complainantName} reported ${comp.targetPartyName}: ${comp.subject}",
                                            type = "COMPLAINT",
                                            timestamp = comp.createdAt,
                                            isRead = false,
                                            actionRoute = "app_owner_hub?tab=5"
                                        )
                                        notificationDao.insertNotification(notif)
                                        postAdminSystemNotification(notif.title, notif.message, notif.actionRoute)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toAgentApplication(): AgentApplicationEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(AgentApplicationEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            AgentApplicationEntity(
                applicationId = (d["applicationId"] as? String) ?: this.id,
                userId = (d["userId"] as? String) ?: "",
                applicantName = (d["applicantName"] as? String) ?: "",
                applicantPhone = (d["applicantPhone"] as? String) ?: "",
                applicantEmail = (d["applicantEmail"] as? String) ?: "",
                experienceYears = (d["experienceYears"] as? String) ?: "1-2 Years",
                priorDomain = (d["priorDomain"] as? String) ?: "Banking / NBFC",
                policeVerificationNumber = (d["policeVerificationNumber"] as? String) ?: "",
                policeStation = (d["policeStation"] as? String) ?: "",
                policeVerificationDate = (d["policeVerificationDate"] as? String) ?: "",
                policeDocUri = (d["policeDocUri"] as? String) ?: "",
                permanentAddress = (d["permanentAddress"] as? String) ?: "",
                operatingCity = (d["operatingCity"] as? String) ?: "",
                operatingPincode = (d["operatingPincode"] as? String) ?: "",
                serviceRadiusKm = ((d["serviceRadiusKm"] as? Number)?.toInt()) ?: 10,
                vehicleType = (d["vehicleType"] as? String) ?: "Two-Wheeler",
                drivingLicenseNumber = (d["drivingLicenseNumber"] as? String) ?: "",
                status = (d["status"] as? String) ?: "PENDING",
                submittedAt = ((d["submittedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                reviewedAt = (d["reviewedAt"] as? Number)?.toLong(),
                adminRemarks = d["adminRemarks"] as? String
            )
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toAdminRequest(): AdminRequestEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(AdminRequestEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            AdminRequestEntity(
                requestId = (d["requestId"] as? String) ?: this.id,
                userId = (d["userId"] as? String) ?: "",
                userName = (d["userName"] as? String) ?: "",
                userPhone = (d["userPhone"] as? String) ?: "",
                userEmail = (d["userEmail"] as? String) ?: "",
                currentRole = (d["currentRole"] as? String) ?: "MEMBER",
                requestedRole = (d["requestedRole"] as? String) ?: "ADMIN",
                reason = (d["reason"] as? String) ?: "",
                status = (d["status"] as? String) ?: "PENDING",
                requestedAt = ((d["requestedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                reviewedAt = (d["reviewedAt"] as? Number)?.toLong(),
                reviewedBy = d["reviewedBy"] as? String,
                adminNotes = (d["adminNotes"] ?: d["reviewNotes"]) as? String
            )
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toComplaint(): ComplaintEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(ComplaintEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            ComplaintEntity(
                complaintId = (d["complaintId"] as? String) ?: this.id,
                complainantId = (d["complainantId"] as? String) ?: "",
                complainantName = (d["complainantName"] as? String) ?: "",
                complainantRole = (d["complainantRole"] as? String) ?: "BORROWER",
                complainantPhone = (d["complainantPhone"] as? String) ?: "",
                targetPartyId = (d["targetPartyId"] as? String) ?: "",
                targetPartyName = (d["targetPartyName"] as? String) ?: "",
                targetPartyRole = (d["targetPartyRole"] as? String) ?: "LENDER",
                loanId = (d["loanId"] as? String) ?: "",
                category = (d["category"] as? String) ?: "OTHER",
                priority = (d["priority"] as? String) ?: "MEDIUM",
                subject = (d["subject"] as? String) ?: "",
                description = (d["description"] as? String) ?: "",
                status = (d["status"] as? String) ?: "OPEN",
                createdAt = ((d["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                resolutionNotes = (d["resolutionNotes"] ?: d["resolution"]) as? String,
                resolvedAt = (d["resolvedAt"] as? Number)?.toLong()
            )
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toSafeUserEntity(): UserEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(UserEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            try {
                UserEntity(
                    userId = (d["userId"] as? String) ?: this.id,
                    name = (d["name"] as? String) ?: "",
                    email = (d["email"] as? String) ?: "",
                    phone = (d["phone"] as? String) ?: "",
                    username = (d["username"] as? String) ?: "",
                    password = (d["password"] as? String) ?: "",
                    role = (d["role"] as? String) ?: "BORROWER",
                    kycStatus = (d["kycStatus"] as? String) ?: "PENDING",
                    panNumber = (d["panNumber"] as? String) ?: "",
                    aadhaarNumber = (d["aadhaarNumber"] as? String) ?: "",
                    aadhaarVerified = (d["aadhaarVerified"] as? Boolean) ?: false,
                    selfieVerified = (d["selfieVerified"] as? Boolean) ?: false,
                    upiId = (d["upiId"] as? String) ?: "",
                    upiVerified = (d["upiVerified"] as? Boolean) ?: false,
                    bankAccountNumber = (d["bankAccountNumber"] as? String) ?: "",
                    bankIfsc = (d["bankIfsc"] as? String) ?: "",
                    bankVerified = (d["bankVerified"] as? Boolean) ?: false,
                    profilePhotoUri = (d["profilePhotoUri"] as? String) ?: "",
                    panImageUrl = (d["panImageUrl"] as? String) ?: "",
                    aadhaarImageUrl = (d["aadhaarImageUrl"] as? String) ?: "",
                    emailVerified = (d["emailVerified"] as? Boolean) ?: false,
                    phoneVerified = (d["phoneVerified"] as? Boolean) ?: false,
                    panVerified = (d["panVerified"] as? Boolean) ?: false,
                    dateOfBirth = (d["dateOfBirth"] as? String) ?: "",
                    address = (d["address"] as? String) ?: "",
                    fcmToken = (d["fcmToken"] as? String) ?: "",
                    agentStatus = (d["agentStatus"] as? String) ?: "NOT_APPLIED",
                    isOnDuty = (d["isOnDuty"] as? Boolean) ?: true,
                    totalAgentEarnings = ((d["totalAgentEarnings"] as? Number)?.toDouble()) ?: 0.0,
                    registeredDeviceId = (d["registeredDeviceId"] as? String) ?: "",
                    registeredDeviceModel = (d["registeredDeviceModel"] as? String) ?: "",
                    createdAt = ((d["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
