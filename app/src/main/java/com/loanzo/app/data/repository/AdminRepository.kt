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
    private val telegramManager: TelegramManager,
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager
) {

    private val firestore: FirebaseFirestore
        get() = com.loanzo.app.data.firebase.FirestoreProvider.get()

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
            firestore.collection("agent_visits")
                .document(visitId)
                .set(agentVisitToMap(updatedVisit), com.google.firebase.firestore.SetOptions.merge())
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
                firestore.collection("collateral_vault")
                    .document(vaultItemId)
                    .set(collateralVaultToMap(item), com.google.firebase.firestore.SetOptions.merge())
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
                firestore.collection("collateral_vault")
                    .document(item.vaultItemId)
                    .set(collateralVaultToMap(item), com.google.firebase.firestore.SetOptions.merge())
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
                firestore.collection("complaints")
                    .document(complaintId)
                    .set(complaintToMap(comp), com.google.firebase.firestore.SetOptions.merge())

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
                firestore.collection("complaints")
                    .document(complaintId)
                    .set(complaintToMap(comp), com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (_: Exception) {}
    }

    suspend fun submitComplaint(complaint: ComplaintEntity) {
        complaintDao.insertComplaint(complaint)
        try {
            firestore
                .collection("complaints")
                .document(complaint.complaintId)
                .set(complaintToMap(complaint), com.google.firebase.firestore.SetOptions.merge())
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
            firestore.collection("notifications")
                .document(notifId)
                .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
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
                firestore.collection("users")
                    .document(user.userId)
                    .set(mapOf(
                        "registeredDeviceId" to newDeviceId,
                        "registeredDeviceModel" to newDeviceModel
                    ), com.google.firebase.firestore.SetOptions.merge())
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
                firestore.collection("complaints")
                    .document(complaintId)
                    .set(complaintToMap(comp), com.google.firebase.firestore.SetOptions.merge())
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
                    firestore.collection("complaints")
                        .document(meeting.complaintId)
                        .set(complaintToMap(comp), com.google.firebase.firestore.SetOptions.merge())
                }
            } catch (_: Exception) {}
        }
        try {
            firestore.collection("mediation_meetings")
                .document(meeting.meetingId)
                .set(mediationMeetingToMap(meeting), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}
    }

    suspend fun updateMeetingStatus(meetingId: String, status: String, notes: String?) {
        mediationMeetingDao.updateMeetingStatus(meetingId, status, notes)
        try {
            val meeting = mediationMeetingDao.getMeetingById(meetingId)
            if (meeting != null) {
                firestore.collection("mediation_meetings")
                    .document(meetingId)
                    .set(mediationMeetingToMap(meeting), com.google.firebase.firestore.SetOptions.merge())
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
            val firestore = firestore
            firestore.collection("noc_certificates")
                .document(noc.nocId)
                .set(nocToMap(noc), com.google.firebase.firestore.SetOptions.merge())

            val vaultItem = collateralVaultDao.getVaultItemByLoanId(loanId)
            if (vaultItem != null) {
                firestore.collection("collateral_vault")
                    .document(vaultItem.vaultItemId)
                    .set(collateralVaultToMap(vaultItem), com.google.firebase.firestore.SetOptions.merge())
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
            firestore.collection("users")
                .document(userId)
                .set(mapOf("agentStatus" to "SUSPENDED", "isOnDuty" to false), com.google.firebase.firestore.SetOptions.merge())
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
            firestore.collection("users")
                .document(userId)
                .set(mapOf("agentStatus" to "APPROVED", "isOnDuty" to true), com.google.firebase.firestore.SetOptions.merge())
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
        // No-op: Demo data seeding disabled completely. Only real admin data is shown.
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
            val firestore = firestore
            firestore.collection("admin_requests")
                .document(request.requestId)
                .set(adminRequestToMap(request), com.google.firebase.firestore.SetOptions.merge())
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
            firestore.collection("notifications")
                .document(notifId)
                .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
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
                firestore.collection("admin_requests")
                    .document(requestId)
                    .set(adminRequestToMap(updatedReq), com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (_: Exception) {}

        // Elevate user role to ADMIN in Firestore directly (guarantees remote applicant gets updated role)
        try {
            val userDocRef = firestore.collection("users").document(req.userId)
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
                firestore.collection("admin_requests")
                    .document(requestId)
                    .set(adminRequestToMap(updatedReq), com.google.firebase.firestore.SetOptions.merge())
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
            val firestore = firestore
            firestore.collection("notifications")
                .document(notifId)
                .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
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
                    firestore.collection("agent_visits")
                        .document(originalVisitId)
                        .set(agentVisitToMap(updatedOriginal), com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}
            }
        }

        // Push dispatched visits to Firestore so assigned agents receive them in real-time
        try {
            val firestore = firestore
            firestore.collection("agent_visits").document(visit1Id).set(agentVisitToMap(visit1), com.google.firebase.firestore.SetOptions.merge())
            firestore.collection("agent_visits").document(visit2Id).set(agentVisitToMap(visit2), com.google.firebase.firestore.SetOptions.merge())
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
        val firestore = firestore

        // 0. Startup Reconciliation: Pull directly from Cloud Firestore & backfill notifications
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                firebaseManager.ensureFirebaseAuthSession()
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

                // Directly pull agent visits from Firestore
                try {
                    val cloudVisits = firestore.collection("agent_visits").get().await()
                    for (doc in cloudVisits.documents) {
                        val visit = doc.toAgentVisit()
                        if (visit != null && visit.visitId.isNotBlank()) {
                            agentDao.insertVisit(visit)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud visits pull error: ${e.message}")
                }

                // Directly pull collateral vault from Firestore
                try {
                    val cloudVault = firestore.collection("collateral_vault").get().await()
                    for (doc in cloudVault.documents) {
                        val item = doc.toCollateralVaultEntity()
                        if (item != null && item.vaultItemId.isNotBlank()) {
                            collateralVaultDao.insertVaultItem(item)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud vault pull error: ${e.message}")
                }

                // Directly pull mediation meetings from Firestore
                try {
                    val cloudMeetings = firestore.collection("mediation_meetings").get().await()
                    for (doc in cloudMeetings.documents) {
                        val meeting = doc.toMediationMeetingEntity()
                        if (meeting != null && meeting.meetingId.isNotBlank()) {
                            mediationMeetingDao.insertMeeting(meeting)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud meetings pull error: ${e.message}")
                }

                // Directly pull noc certificates from Firestore
                try {
                    val cloudNocs = firestore.collection("noc_certificates").get().await()
                    for (doc in cloudNocs.documents) {
                        val noc = doc.toNocCertificateEntity()
                        if (noc != null && noc.nocId.isNotBlank()) {
                            nocCertificateDao.insertNoc(noc)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepo", "Direct cloud nocs pull error: ${e.message}")
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
                            actionRoute = "app_owner_hub?tab=11"
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

        // 1. Listen to Admin Requests & Agent Empanelment
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            firebaseManager.ensureFirebaseAuthSession()
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
                                            actionRoute = "app_owner_hub?tab=11"
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
                            val visit = doc.toAgentVisit()
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

        // 6. Listen to Collateral Vault
        try {
            firestore.collection("collateral_vault")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val item = doc.toCollateralVaultEntity()
                            if (item != null && item.vaultItemId.isNotBlank()) {
                                collateralVaultDao.insertVaultItem(item)
                            }
                        }
                    }
                }
        } catch (_: Exception) {}

        // 7. Listen to Mediation Meetings
        try {
            firestore.collection("mediation_meetings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val meeting = doc.toMediationMeetingEntity()
                            if (meeting != null && meeting.meetingId.isNotBlank()) {
                                mediationMeetingDao.insertMeeting(meeting)
                            }
                        }
                    }
                }
        } catch (_: Exception) {}

        // 8. Listen to NOC Certificates
        try {
            firestore.collection("noc_certificates")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val noc = doc.toNocCertificateEntity()
                            if (noc != null && noc.nocId.isNotBlank()) {
                                nocCertificateDao.insertNoc(noc)
                            }
                        }
                    }
                }
        } catch (_: Exception) {}
        }
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

    suspend fun refreshAgentApplicationsFromCloud(): List<AgentApplicationEntity> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val list = mutableListOf<AgentApplicationEntity>()
        try {
            val snapshot = firestore.collection("agent_applications").get().await()
            for (doc in snapshot.documents) {
                val app = doc.toAgentApplication()
                if (app != null && app.applicationId.isNotBlank()) {
                    agentDao.insertApplication(app)
                    list.add(app)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AdminRepository", "refreshAgentApplicationsFromCloud error: ${e.message}")
        }
        list
    }

    suspend fun refreshUsersFromCloud(): List<UserEntity> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val list = mutableListOf<UserEntity>()
        try {
            val snapshot = firestore.collection("users").get().await()
            for (doc in snapshot.documents) {
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
                    list.add(u)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AdminRepository", "refreshUsersFromCloud error: ${e.message}")
        }
        list
    }

    fun agentVisitToMap(v: AgentVisitEntity): Map<String, Any?> {
        return hashMapOf(
            "visitId" to v.visitId,
            "agentId" to v.agentId,
            "loanId" to v.loanId,
            "visitType" to v.visitType,
            "title" to v.title,
            "borrowerName" to v.borrowerName,
            "borrowerPhone" to v.borrowerPhone,
            "borrowerAddress" to v.borrowerAddress,
            "lenderName" to v.lenderName,
            "lenderPhone" to v.lenderPhone,
            "lenderAddress" to v.lenderAddress,
            "targetAddress" to v.targetAddress,
            "targetLatitude" to v.targetLatitude,
            "targetLongitude" to v.targetLongitude,
            "scheduledDate" to v.scheduledDate,
            "scheduledTimeSlot" to v.scheduledTimeSlot,
            "payoutAmount" to v.payoutAmount,
            "collateralItemName" to v.collateralItemName,
            "collateralEstimatedValue" to v.collateralEstimatedValue,
            "collateralPledgedValue" to v.collateralPledgedValue,
            "status" to v.status,
            "agentRemarks" to v.agentRemarks,
            "isCollateralAuthentic" to v.isCollateralAuthentic,
            "isBorrowerIdentityVerified" to v.isBorrowerIdentityVerified,
            "isLenderIdentityVerified" to v.isLenderIdentityVerified,
            "proofPhotoUris" to v.proofPhotoUris,
            "completedAt" to v.completedAt,
            "createdAt" to v.createdAt,
            "crossVerificationPairId" to v.crossVerificationPairId,
            "isCrossVerification" to v.isCrossVerification,
            "counterpartVisitId" to v.counterpartVisitId,
            "verificationStage" to v.verificationStage,
            "appraisedValue" to v.appraisedValue,
            "counterpartAppraisedValue" to v.counterpartAppraisedValue,
            "valuationDiscrepancyPercent" to v.valuationDiscrepancyPercent,
            "officerRecommendation" to v.officerRecommendation,
            "isCounterpartAnonymous" to v.isCounterpartAnonymous,
            "assignedAgentName" to v.assignedAgentName,
            "agentPhone" to v.agentPhone,
            "handshakePin" to v.handshakePin,
            "isHandshakePinVerified" to v.isHandshakePinVerified,
            "visitStageStatus" to v.visitStageStatus,
            "agentLatitude" to v.agentLatitude,
            "agentLongitude" to v.agentLongitude,
            "loanType" to v.loanType,
            "distanceKm" to v.distanceKm
        )
    }

    fun collateralVaultToMap(item: CollateralVaultEntity): Map<String, Any?> {
        return hashMapOf(
            "vaultItemId" to item.vaultItemId,
            "loanId" to item.loanId,
            "borrowerId" to item.borrowerId,
            "borrowerName" to item.borrowerName,
            "borrowerPhone" to item.borrowerPhone,
            "assetDescription" to item.assetDescription,
            "assetType" to item.assetType,
            "estimatedValue" to item.estimatedValue,
            "appraisedPurityOrCondition" to item.appraisedPurityOrCondition,
            "vaultFacilityName" to item.vaultFacilityName,
            "lockerNumber" to item.lockerNumber,
            "barcodeTag" to item.barcodeTag,
            "tamperSealNumber" to item.tamperSealNumber,
            "custodyStatus" to item.custodyStatus,
            "photoUri" to item.photoUri,
            "intakeAgentId" to item.intakeAgentId,
            "intakeDate" to item.intakeDate,
            "releaseDate" to item.releaseDate,
            "createdAt" to item.createdAt
        )
    }

    fun complaintToMap(c: ComplaintEntity): Map<String, Any?> {
        return hashMapOf(
            "complaintId" to c.complaintId,
            "complainantId" to c.complainantId,
            "complainantName" to c.complainantName,
            "complainantRole" to c.complainantRole,
            "complainantPhone" to c.complainantPhone,
            "targetPartyId" to c.targetPartyId,
            "targetPartyName" to c.targetPartyName,
            "targetPartyRole" to c.targetPartyRole,
            "loanId" to c.loanId,
            "category" to c.category,
            "priority" to c.priority,
            "subject" to c.subject,
            "description" to c.description,
            "evidenceUris" to c.evidenceUris,
            "status" to c.status,
            "resolutionNotes" to c.resolutionNotes,
            "resolvedAt" to c.resolvedAt,
            "createdAt" to c.createdAt
        )
    }

    fun mediationMeetingToMap(m: MediationMeetingEntity): Map<String, Any?> {
        return hashMapOf(
            "meetingId" to m.meetingId,
            "title" to m.title,
            "agenda" to m.agenda,
            "loanId" to m.loanId,
            "complaintId" to m.complaintId,
            "borrowerId" to m.borrowerId,
            "borrowerName" to m.borrowerName,
            "borrowerPhone" to m.borrowerPhone,
            "lenderId" to m.lenderId,
            "lenderName" to m.lenderName,
            "lenderPhone" to m.lenderPhone,
            "agentId" to m.agentId,
            "agentName" to m.agentName,
            "meetingType" to m.meetingType,
            "meetingLinkOrLocation" to m.meetingLinkOrLocation,
            "scheduledDateTime" to m.scheduledDateTime,
            "scheduledTimeSlotStr" to m.scheduledTimeSlotStr,
            "status" to m.status,
            "adminNotes" to m.adminNotes,
            "createdAt" to m.createdAt
        )
    }

    fun nocToMap(noc: NocCertificateEntity): Map<String, Any?> {
        return hashMapOf(
            "nocId" to noc.nocId,
            "loanId" to noc.loanId,
            "borrowerId" to noc.borrowerId,
            "borrowerName" to noc.borrowerName,
            "borrowerPan" to noc.borrowerPan,
            "lenderId" to noc.lenderId,
            "lenderName" to noc.lenderName,
            "principalAmount" to noc.principalAmount,
            "totalRepaidAmount" to noc.totalRepaidAmount,
            "collateralReleasedDesc" to noc.collateralReleasedDesc,
            "digitalSignatureHash" to noc.digitalSignatureHash,
            "issuedAt" to noc.issuedAt,
            "issuedByAdminId" to noc.issuedByAdminId,
            "status" to noc.status
        )
    }

    fun adminRequestToMap(r: AdminRequestEntity): Map<String, Any?> {
        return hashMapOf(
            "requestId" to r.requestId,
            "userId" to r.userId,
            "userName" to r.userName,
            "userPhone" to r.userPhone,
            "userEmail" to r.userEmail,
            "currentRole" to r.currentRole,
            "requestedRole" to r.requestedRole,
            "reason" to r.reason,
            "status" to r.status,
            "requestedAt" to r.requestedAt,
            "reviewedAt" to r.reviewedAt,
            "reviewedBy" to r.reviewedBy,
            "adminNotes" to r.adminNotes
        )
    }

    fun notificationToMap(n: NotificationEntity): Map<String, Any?> {
        return hashMapOf(
            "notificationId" to n.notificationId,
            "userId" to n.userId,
            "title" to n.title,
            "message" to n.message,
            "type" to n.type,
            "relatedLoanId" to n.relatedLoanId,
            "actionRoute" to n.actionRoute,
            "dayKey" to n.dayKey,
            "timestamp" to n.timestamp,
            "isRead" to n.isRead
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toAgentVisit(): AgentVisitEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(AgentVisitEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            AgentVisitEntity(
                visitId = (d["visitId"] as? String) ?: this.id,
                agentId = (d["agentId"] as? String) ?: "",
                loanId = (d["loanId"] as? String) ?: "",
                visitType = (d["visitType"] as? String) ?: "BORROWER_VERIFICATION",
                title = (d["title"] as? String) ?: "",
                borrowerName = (d["borrowerName"] as? String) ?: "",
                borrowerPhone = (d["borrowerPhone"] as? String) ?: "",
                borrowerAddress = (d["borrowerAddress"] as? String) ?: "",
                lenderName = (d["lenderName"] as? String) ?: "",
                lenderPhone = (d["lenderPhone"] as? String) ?: "",
                lenderAddress = (d["lenderAddress"] as? String) ?: "",
                targetAddress = (d["targetAddress"] as? String) ?: "",
                targetLatitude = (d["targetLatitude"] as? Number)?.toDouble() ?: 0.0,
                targetLongitude = (d["targetLongitude"] as? Number)?.toDouble() ?: 0.0,
                scheduledDate = (d["scheduledDate"] as? String) ?: "Today",
                scheduledTimeSlot = (d["scheduledTimeSlot"] as? String) ?: "",
                payoutAmount = (d["payoutAmount"] as? Number)?.toDouble() ?: 0.0,
                collateralItemName = d["collateralItemName"] as? String,
                collateralEstimatedValue = (d["collateralEstimatedValue"] as? Number)?.toDouble(),
                collateralPledgedValue = (d["collateralPledgedValue"] as? Number)?.toDouble(),
                status = (d["status"] as? String) ?: "SCHEDULED",
                agentRemarks = (d["agentRemarks"] as? String) ?: "",
                isCollateralAuthentic = (d["isCollateralAuthentic"] as? Boolean) ?: false,
                isBorrowerIdentityVerified = (d["isBorrowerIdentityVerified"] as? Boolean) ?: false,
                isLenderIdentityVerified = (d["isLenderIdentityVerified"] as? Boolean) ?: false,
                proofPhotoUris = (d["proofPhotoUris"] as? String) ?: "",
                completedAt = (d["completedAt"] as? Number)?.toLong(),
                createdAt = ((d["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                crossVerificationPairId = d["crossVerificationPairId"] as? String,
                isCrossVerification = (d["isCrossVerification"] as? Boolean) ?: false,
                counterpartVisitId = d["counterpartVisitId"] as? String,
                verificationStage = (d["verificationStage"] as? String) ?: "STAGE_1_PRIMARY",
                appraisedValue = (d["appraisedValue"] as? Number)?.toDouble(),
                counterpartAppraisedValue = (d["counterpartAppraisedValue"] as? Number)?.toDouble(),
                valuationDiscrepancyPercent = (d["valuationDiscrepancyPercent"] as? Number)?.toDouble(),
                officerRecommendation = d["officerRecommendation"] as? String,
                isCounterpartAnonymous = (d["isCounterpartAnonymous"] as? Boolean) ?: true,
                assignedAgentName = d["assignedAgentName"] as? String,
                agentPhone = (d["agentPhone"] as? String) ?: "",
                handshakePin = (d["handshakePin"] as? String) ?: "",
                isHandshakePinVerified = (d["isHandshakePinVerified"] as? Boolean) ?: false,
                visitStageStatus = (d["visitStageStatus"] as? String) ?: "SCHEDULED",
                agentLatitude = (d["agentLatitude"] as? Number)?.toDouble(),
                agentLongitude = (d["agentLongitude"] as? Number)?.toDouble(),
                loanType = (d["loanType"] as? String) ?: "PERSONAL",
                distanceKm = (d["distanceKm"] as? Number)?.toDouble()
            )
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCollateralVaultEntity(): CollateralVaultEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(CollateralVaultEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            CollateralVaultEntity(
                vaultItemId = (d["vaultItemId"] as? String) ?: this.id,
                loanId = (d["loanId"] as? String) ?: "",
                borrowerId = (d["borrowerId"] as? String) ?: "",
                borrowerName = (d["borrowerName"] as? String) ?: "",
                borrowerPhone = (d["borrowerPhone"] as? String) ?: "",
                assetDescription = (d["assetDescription"] as? String) ?: "",
                assetType = (d["assetType"] as? String) ?: "OTHER",
                estimatedValue = (d["estimatedValue"] as? Number)?.toDouble() ?: 0.0,
                appraisedPurityOrCondition = (d["appraisedPurityOrCondition"] as? String) ?: "",
                vaultFacilityName = (d["vaultFacilityName"] as? String) ?: "Loanzo Central Vault - Delhi NCR",
                lockerNumber = (d["lockerNumber"] as? String) ?: "PENDING_ALLOCATION",
                barcodeTag = (d["barcodeTag"] as? String) ?: "",
                tamperSealNumber = (d["tamperSealNumber"] as? String) ?: "",
                custodyStatus = (d["custodyStatus"] as? String) ?: "PENDING_INTAKE",
                photoUri = (d["photoUri"] as? String) ?: "",
                intakeAgentId = d["intakeAgentId"] as? String,
                intakeDate = (d["intakeDate"] as? Number)?.toLong(),
                releaseDate = (d["releaseDate"] as? Number)?.toLong(),
                createdAt = ((d["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
            )
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toMediationMeetingEntity(): MediationMeetingEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(MediationMeetingEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            MediationMeetingEntity(
                meetingId = (d["meetingId"] as? String) ?: this.id,
                title = (d["title"] as? String) ?: "",
                agenda = (d["agenda"] as? String) ?: "",
                loanId = d["loanId"] as? String,
                complaintId = d["complaintId"] as? String,
                borrowerId = d["borrowerId"] as? String,
                borrowerName = d["borrowerName"] as? String,
                borrowerPhone = d["borrowerPhone"] as? String,
                lenderId = d["lenderId"] as? String,
                lenderName = d["lenderName"] as? String,
                lenderPhone = d["lenderPhone"] as? String,
                agentId = d["agentId"] as? String,
                agentName = d["agentName"] as? String,
                meetingType = (d["meetingType"] as? String) ?: "GOOGLE_MEET",
                meetingLinkOrLocation = (d["meetingLinkOrLocation"] as? String) ?: "",
                scheduledDateTime = (d["scheduledDateTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                scheduledTimeSlotStr = (d["scheduledTimeSlotStr"] as? String) ?: "",
                status = (d["status"] as? String) ?: "SCHEDULED",
                adminNotes = d["adminNotes"] as? String,
                createdAt = ((d["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
            )
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toNocCertificateEntity(): NocCertificateEntity? {
        val d = this.data ?: return null
        return try {
            this.toObject(NocCertificateEntity::class.java)
        } catch (_: Exception) {
            null
        } ?: run {
            NocCertificateEntity(
                nocId = (d["nocId"] as? String) ?: this.id,
                loanId = (d["loanId"] as? String) ?: "",
                borrowerId = (d["borrowerId"] as? String) ?: "",
                borrowerName = (d["borrowerName"] as? String) ?: "",
                borrowerPan = (d["borrowerPan"] as? String) ?: "",
                lenderId = (d["lenderId"] as? String) ?: "",
                lenderName = (d["lenderName"] as? String) ?: "",
                principalAmount = (d["principalAmount"] as? Number)?.toDouble() ?: 0.0,
                totalRepaidAmount = (d["totalRepaidAmount"] as? Number)?.toDouble() ?: 0.0,
                collateralReleasedDesc = (d["collateralReleasedDesc"] as? String) ?: "",
                digitalSignatureHash = (d["digitalSignatureHash"] as? String) ?: "",
                issuedAt = (d["issuedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                issuedByAdminId = (d["issuedByAdminId"] as? String) ?: "ADMIN-SATYAM-0810",
                status = (d["status"] as? String) ?: "ACTIVE_CLEARANCE"
            )
        }
    }
}
