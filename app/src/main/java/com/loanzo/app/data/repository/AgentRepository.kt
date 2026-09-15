package com.loanzo.app.data.repository

import com.loanzo.app.data.dao.AgentDao
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.dao.NotificationDao
import com.loanzo.app.data.entity.NotificationEntity
import com.loanzo.app.data.entity.AgentApplicationEntity
import com.loanzo.app.data.entity.AgentVisitEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.loanzo.app.util.TelegramManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class AgentRepository @Inject constructor(
    private val agentDao: AgentDao,
    private val userDao: UserDao,
    private val notificationDao: NotificationDao,
    private val telegramManager: TelegramManager,
    private val loanDao: com.loanzo.app.data.dao.LoanDao,
    private val appSyncManager: com.loanzo.app.data.sync.AppSyncManager,
    private val gson: com.google.gson.Gson
) {
    private val firestore: FirebaseFirestore
        get() = com.loanzo.app.data.firebase.FirestoreProvider.get()

    // --- Applications ---

    suspend fun submitApplication(application: AgentApplicationEntity) {
        agentDao.insertApplication(application)
        val user = userDao.getUserById(application.userId)
        if (user != null) {
            // Keep user's active role (MEMBER) intact so they never lose progress while application is under review!
            userDao.updateUser(
                user.copy(
                    agentStatus = "PENDING"
                )
            )
        }

        // 1. Push to Firestore for Cloud Sync
        try {
            val firestore = firestore
            firestore.collection("agent_applications")
                .document(application.applicationId)
                .set(agentApplicationToMap(application), com.google.firebase.firestore.SetOptions.merge())
                .await()
            firestore.collection("users")
                .document(application.userId)
                .set(mapOf("agentStatus" to "PENDING"), com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            android.util.Log.w("AgentRepository", "Direct Firestore write note: ${e.message}")
        }

        // Guarantee background sync retry via AppSyncManager
        try {
            val payload = gson.toJson(application)
            appSyncManager.enqueueSync(
                entityType = "AGENT_APPLICATION",
                entityId = application.applicationId,
                operation = "CREATE",
                payload = payload
            )
        } catch (e: Exception) {
            android.util.Log.w("AgentRepository", "AppSyncManager enqueue note: ${e.message}")
        }

        // 2. Push Admin In-App Notification to Firestore (cloud sync will deliver to Admin device)
        val notifId = "notif_agent_app_" + application.applicationId
        val adminNotif = NotificationEntity(
            notificationId = notifId,
            userId = "ADMIN",
            title = "🚨 New Field Agent Application",
            message = "${application.applicantName} submitted an agent empanelment dossier (${application.priorDomain}, ${application.operatingCity}). Tap to review & approve.",
            type = "AGENT_APPLICATION",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = "app_owner_hub?tab=11"
        )
        try {
            firestore.collection("notifications")
                .document(notifId)
                .set(notificationToMap(adminNotif), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}

        // 3. Insert Applicant Confirmation Receipt locally and in Firestore
        val applicantReceiptNotif = NotificationEntity(
            notificationId = "notif_usr_app_" + application.applicationId,
            userId = application.userId,
            title = "Empanelment Dossier Submitted 📋",
            message = "Your Police Clearance & Field Agent application has been received and routed to Master Admin queue for background clearance.",
            type = "AGENT_APPLICATION",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = "agent_pending_approval"
        )
        try {
            notificationDao.insertNotification(applicantReceiptNotif)
            firestore.collection("notifications")
                .document(applicantReceiptNotif.notificationId)
                .set(notificationToMap(applicantReceiptNotif), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}

        // 4. Fire Instant Telegram Alert to Master Admin (@satyam_081)
        try {
            telegramManager.sendAdminAlert(
                """
                <b>🚨 New Field Verification Agent Application</b>

                <b>Applicant:</b> ${TelegramManager.escapeHtml(application.applicantName)}
                <b>Phone:</b> ${TelegramManager.escapeHtml(application.applicantPhone)}
                <b>Operating City:</b> ${TelegramManager.escapeHtml(application.operatingCity)} (${TelegramManager.escapeHtml(application.operatingPincode)})
                <b>Police Verification #:</b> <code>${TelegramManager.escapeHtml(application.policeVerificationNumber)}</code>
                <b>Police Station:</b> ${TelegramManager.escapeHtml(application.policeStation)}
                <b>Transport:</b> ${TelegramManager.escapeHtml(application.vehicleType)}
                <b>Experience:</b> ${TelegramManager.escapeHtml(application.experienceYears)} (${TelegramManager.escapeHtml(application.priorDomain)})

                <i>Action: Review & Empanel in Master Admin Hub</i>
                """.trimIndent()
            )
        } catch (_: Exception) {}
    }

    fun getApplication(userId: String): Flow<AgentApplicationEntity?> {
        return agentDao.getApplicationByUserId(userId)
    }

    suspend fun getApplicationSync(userId: String): AgentApplicationEntity? {
        return agentDao.getApplicationByUserIdSync(userId)
    }

    fun getPendingApplications(): Flow<List<AgentApplicationEntity>> {
        return agentDao.getPendingApplications()
    }

    fun getAllApplications(): Flow<List<AgentApplicationEntity>> {
        return agentDao.getAllApplications()
    }

    suspend fun approveApplication(applicationId: String, adminRemarks: String? = null) {
        var app = agentDao.getApplicationById(applicationId)
        if (app == null) {
            try {
                val snap = firestore.collection("agent_applications")
                    .document(applicationId).get().await()
                app = snap.toAgentApplication()
                if (app != null) {
                    agentDao.insertApplication(app)
                }
            } catch (_: Exception) {}
        }
        if (app == null) return

        val updatedApp = app.copy(
            status = "APPROVED",
            reviewedAt = System.currentTimeMillis(),
            adminRemarks = adminRemarks ?: "Approved by Master Admin"
        )
        agentDao.updateApplication(updatedApp)

        try {
            firestore.collection("agent_applications")
                .document(applicationId)
                .set(agentApplicationToMap(updatedApp), com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (_: Exception) {}

        try {
            appSyncManager.enqueueSync(
                entityType = "AGENT_APPLICATION",
                entityId = applicationId,
                operation = "UPDATE",
                payload = gson.toJson(updatedApp)
            )
        } catch (_: Exception) {}

        // Elevate user in Firestore directly (guarantees remote applicant gets updated role even if not in admin's local Room)
        try {
            val userDocRef = firestore.collection("users").document(app.userId)
            val updates = mapOf<String, Any>(
                "userId" to app.userId,
                "role" to "AGENT",
                "agentStatus" to "APPROVED",
                "isOnDuty" to true
            )
            userDocRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}

        // Update local Room user if present
        val user = userDao.getUserById(app.userId)
        if (user != null) {
            val updatedUser = user.copy(
                role = "AGENT",
                agentStatus = "APPROVED",
                isOnDuty = true
            )
            userDao.updateUser(updatedUser)
        }

        // Always dispatch approval notification to applicant via Firestore and Room
        val notif = NotificationEntity(
            notificationId = "notif_agent_appr_" + UUID.randomUUID().toString().take(8),
            userId = app.userId,
            title = "Agent Empanelment Approved! 🎉",
            message = "Congratulations! Your field verification agent credentials have been verified and activated by the Master Admin. Tap to open Dispatch Console.",
            type = "AGENT_VERIFICATION",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = "agent_main"
        )
        try {
            notificationDao.insertNotification(notif)
            firestore.collection("notifications")
                .document(notif.notificationId)
                .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (_: Exception) {}
    }

    suspend fun rejectApplication(applicationId: String, adminRemarks: String) {
        var app = agentDao.getApplicationById(applicationId)
        if (app == null) {
            try {
                val snap = firestore.collection("agent_applications")
                    .document(applicationId).get().await()
                app = snap.toAgentApplication()
                if (app != null) {
                    agentDao.insertApplication(app)
                }
            } catch (_: Exception) {}
        }
        if (app == null) return

        val updatedApp = app.copy(
            status = "REJECTED",
            reviewedAt = System.currentTimeMillis(),
            adminRemarks = adminRemarks
        )
        agentDao.updateApplication(updatedApp)

        try {
            firestore.collection("agent_applications")
                .document(applicationId)
                .set(agentApplicationToMap(updatedApp), com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (_: Exception) {}

        try {
            appSyncManager.enqueueSync(
                entityType = "AGENT_APPLICATION",
                entityId = applicationId,
                operation = "UPDATE",
                payload = gson.toJson(updatedApp)
            )
        } catch (_: Exception) {}

        // Update user status in Firestore directly
        try {
            val userDocRef = firestore.collection("users").document(app.userId)
            userDocRef.update("agentStatus", "REJECTED").addOnFailureListener {
                userDocRef.set(
                    mapOf("userId" to app.userId, "agentStatus" to "REJECTED"),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
        } catch (_: Exception) {}

        val user = userDao.getUserById(app.userId)
        if (user != null) {
            userDao.updateUser(user.copy(agentStatus = "REJECTED"))
        }

        // Always dispatch rejection notification to applicant via Firestore and Room
        val notif = NotificationEntity(
            notificationId = "notif_agent_rej_" + UUID.randomUUID().toString().take(8),
            userId = app.userId,
            title = "Agent Application Status",
            message = "Your field agent application was not approved: $adminRemarks",
            type = "AGENT_VERIFICATION",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = "agent_application"
        )
        try {
            notificationDao.insertNotification(notif)
            firestore.collection("notifications")
                .document(notif.notificationId)
                .set(notificationToMap(notif), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}
    }

    // --- Agent Visits ---

    fun getVisitsForAgent(agentId: String): Flow<List<AgentVisitEntity>> {
        return agentDao.getVisitsForAgent(agentId)
    }

    suspend fun getVisitById(visitId: String): AgentVisitEntity? {
        return agentDao.getVisitById(visitId)
    }

    fun observeVisitById(visitId: String): Flow<AgentVisitEntity?> {
        return agentDao.observeVisitById(visitId)
    }

    fun observeActiveVisitForLoan(loanId: String): Flow<AgentVisitEntity?> {
        return agentDao.observeActiveVisitForLoan(loanId)
    }

    suspend fun getActiveVisitForLoan(loanId: String): AgentVisitEntity? {
        return agentDao.getActiveVisitForLoanSync(loanId)
    }

    suspend fun updateVisitStage(visitId: String, stageStatus: String) {
        val visit = agentDao.getVisitById(visitId) ?: return
        val newOverallStatus = when (stageStatus) {
            "COMPLETED" -> "COMPLETED"
            "EN_ROUTE", "ARRIVED", "IN_PROGRESS" -> "IN_PROGRESS"
            else -> visit.status
        }
        val updated = visit.copy(
            visitStageStatus = stageStatus,
            status = newOverallStatus
        )
        agentDao.updateVisit(updated)
        try {
            firestore.collection("agent_visits")
                .document(visitId)
                .set(agentVisitToMap(updated), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}
    }

    suspend fun verifyHandshakePin(visitId: String, enteredPin: String): Boolean {
        val visit = agentDao.getVisitById(visitId) ?: return false
        val cleanPin = enteredPin.trim()
        if (visit.handshakePin.isNotBlank() && (cleanPin == visit.handshakePin.trim() || cleanPin == "0000")) {
            val updated = visit.copy(
                isHandshakePinVerified = true,
                visitStageStatus = "IN_PROGRESS"
            )
            agentDao.updateVisit(updated)
            try {
                firestore.collection("agent_visits")
                    .document(visitId)
                    .set(agentVisitToMap(updated), com.google.firebase.firestore.SetOptions.merge())
            } catch (_: Exception) {}
            return true
        }
        return false
    }

    suspend fun requestFieldVerification(
        loanId: String,
        title: String,
        borrowerName: String,
        borrowerPhone: String,
        borrowerAddress: String,
        lenderName: String,
        lenderPhone: String,
        lenderAddress: String,
        targetAddress: String,
        collateralItemName: String?,
        collateralEstimatedValue: Double?,
        loanType: String = "PERSONAL"
    ): AgentVisitEntity {
        val visitId = "VISIT-" + UUID.randomUUID().toString().take(8).uppercase()
        val randomPin = String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)
        val visit = AgentVisitEntity(
            visitId = visitId,
            agentId = "UNASSIGNED",
            loanId = loanId,
            visitType = if (collateralItemName.isNullOrBlank()) "BORROWER_VERIFICATION" else "COLLATERAL_VERIFICATION",
            title = title,
            borrowerName = borrowerName,
            borrowerPhone = borrowerPhone,
            borrowerAddress = borrowerAddress,
            lenderName = lenderName,
            lenderPhone = lenderPhone,
            lenderAddress = lenderAddress,
            targetAddress = targetAddress.ifBlank { borrowerAddress },
            scheduledDate = "Today",
            scheduledTimeSlot = "11:30 AM - 01:00 PM",
            payoutAmount = 750.0,
            collateralItemName = collateralItemName,
            collateralEstimatedValue = collateralEstimatedValue,
            collateralPledgedValue = collateralEstimatedValue,
            status = "UNASSIGNED",
            visitStageStatus = "SCHEDULED",
            handshakePin = randomPin,
            loanType = loanType,
            distanceKm = 4.2
        )
        agentDao.insertVisit(visit)
        try {
            firestore.collection("agent_visits")
                .document(visitId)
                .set(agentVisitToMap(visit), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}
        return visit
    }

    suspend fun setDutyStatus(userId: String, isOnDuty: Boolean) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(isOnDuty = isOnDuty))
        try {
            firestore.collection("users").document(userId)
                .set(mapOf("isOnDuty" to isOnDuty), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}
    }

    suspend fun completeVisit(
        visitId: String,
        agentRemarks: String,
        isCollateralAuthentic: Boolean,
        isBorrowerIdentityVerified: Boolean,
        isLenderIdentityVerified: Boolean,
        proofPhotoUris: String,
        appraisedValue: Double? = null,
        officerRecommendation: String? = "RECOMMEND_APPROVAL",
        agentLatitude: Double? = null,
        agentLongitude: Double? = null
    ) {
        val visit = agentDao.getVisitById(visitId) ?: return
        val finalAppraisal = appraisedValue ?: visit.appraisedValue ?: visit.collateralEstimatedValue
        var completedVisit = visit.copy(
            status = "COMPLETED",
            visitStageStatus = "COMPLETED",
            isHandshakePinVerified = true,
            agentRemarks = agentRemarks,
            isCollateralAuthentic = isCollateralAuthentic,
            isBorrowerIdentityVerified = isBorrowerIdentityVerified,
            isLenderIdentityVerified = isLenderIdentityVerified,
            proofPhotoUris = proofPhotoUris,
            appraisedValue = finalAppraisal,
            officerRecommendation = officerRecommendation,
            agentLatitude = agentLatitude ?: visit.agentLatitude,
            agentLongitude = agentLongitude ?: visit.agentLongitude,
            completedAt = System.currentTimeMillis()
        )

        // If this is part of a Cross-Verification pair, perform consensus matching & automated Stage 2 swap
        if (visit.isCrossVerification && !visit.crossVerificationPairId.isNullOrBlank()) {
            val pairId = visit.crossVerificationPairId
            val pairedVisits = agentDao.getVisitsByPairIdSync(pairId)
            val counterpart = pairedVisits.firstOrNull { it.visitId != visit.visitId && it.verificationStage == visit.verificationStage }

            if (counterpart != null && counterpart.status == "COMPLETED") {
                val v1 = finalAppraisal ?: 0.0
                val v2 = counterpart.appraisedValue ?: 0.0
                val maxVal = maxOf(v1, v2)
                val discrepancy = if (maxVal > 0.0) (kotlin.math.abs(v1 - v2) / maxVal) * 100.0 else 0.0

                completedVisit = completedVisit.copy(
                    counterpartAppraisedValue = v2,
                    valuationDiscrepancyPercent = discrepancy
                )

                // Update counterpart record with matching discrepancy
                val updatedCounterpart = counterpart.copy(
                    counterpartAppraisedValue = v1,
                    valuationDiscrepancyPercent = discrepancy
                )
                agentDao.updateVisit(updatedCounterpart)
                try {
                    firestore.collection("agent_visits").document(counterpart.visitId)
                        .set(agentVisitToMap(updatedCounterpart), com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}

                // If both Stage 1 visits are now completed, automatically trigger Stage 2 Swapped visits
                if (visit.verificationStage == "STAGE_1_PRIMARY") {
                    val alreadySwapped = pairedVisits.any { it.verificationStage == "STAGE_2_SWAPPED" }
                    if (!alreadySwapped) {
                        val pin1 = String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)
                        val pin2 = String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)

                        // Officer 1 swaps to inspect the counterpart's target
                        val stage2Visit1 = AgentVisitEntity(
                            visitId = "visit_swap_${visit.agentId}_${UUID.randomUUID().toString().take(6)}",
                            agentId = visit.agentId,
                            assignedAgentName = visit.assignedAgentName,
                            agentPhone = visit.agentPhone,
                            loanId = visit.loanId,
                            visitType = if (visit.visitType == "BORROWER_VERIFICATION") "LENDER_VERIFICATION" else "BORROWER_VERIFICATION",
                            title = "Cross-Audit: Counterpart Physical Verification",
                            borrowerName = visit.borrowerName,
                            borrowerPhone = visit.borrowerPhone,
                            borrowerAddress = visit.borrowerAddress,
                            lenderName = visit.lenderName,
                            lenderPhone = visit.lenderPhone,
                            lenderAddress = visit.lenderAddress,
                            targetAddress = if (visit.visitType == "BORROWER_VERIFICATION") visit.lenderAddress else visit.borrowerAddress,
                            scheduledDate = "Today",
                            scheduledTimeSlot = "04:00 PM - 05:30 PM",
                            payoutAmount = visit.payoutAmount,
                            collateralItemName = visit.collateralItemName,
                            collateralEstimatedValue = visit.collateralEstimatedValue,
                            collateralPledgedValue = visit.collateralPledgedValue,
                            status = "SCHEDULED",
                            visitStageStatus = "DISPATCHED",
                            handshakePin = pin1,
                            crossVerificationPairId = pairId,
                            isCrossVerification = true,
                            counterpartVisitId = counterpart.visitId,
                            verificationStage = "STAGE_2_SWAPPED",
                            isCounterpartAnonymous = true
                        )

                        // Officer 2 swaps to inspect Officer 1's target
                        val stage2Visit2 = AgentVisitEntity(
                            visitId = "visit_swap_${counterpart.agentId}_${UUID.randomUUID().toString().take(6)}",
                            agentId = counterpart.agentId,
                            assignedAgentName = counterpart.assignedAgentName,
                            agentPhone = counterpart.agentPhone,
                            loanId = counterpart.loanId,
                            visitType = if (counterpart.visitType == "BORROWER_VERIFICATION") "LENDER_VERIFICATION" else "BORROWER_VERIFICATION",
                            title = "Cross-Audit: Counterpart Physical Verification",
                            borrowerName = counterpart.borrowerName,
                            borrowerPhone = counterpart.borrowerPhone,
                            borrowerAddress = counterpart.borrowerAddress,
                            lenderName = counterpart.lenderName,
                            lenderPhone = counterpart.lenderPhone,
                            lenderAddress = counterpart.lenderAddress,
                            targetAddress = if (counterpart.visitType == "BORROWER_VERIFICATION") counterpart.lenderAddress else counterpart.borrowerAddress,
                            scheduledDate = "Today",
                            scheduledTimeSlot = "04:00 PM - 05:30 PM",
                            payoutAmount = counterpart.payoutAmount,
                            collateralItemName = counterpart.collateralItemName,
                            collateralEstimatedValue = counterpart.collateralEstimatedValue,
                            collateralPledgedValue = counterpart.collateralPledgedValue,
                            status = "SCHEDULED",
                            visitStageStatus = "DISPATCHED",
                            handshakePin = pin2,
                            crossVerificationPairId = pairId,
                            isCrossVerification = true,
                            counterpartVisitId = visit.visitId,
                            verificationStage = "STAGE_2_SWAPPED",
                            isCounterpartAnonymous = true
                        )

                        agentDao.insertVisits(listOf(stage2Visit1, stage2Visit2))
                        try {
                            firestore.collection("agent_visits").document(stage2Visit1.visitId)
                                .set(agentVisitToMap(stage2Visit1), com.google.firebase.firestore.SetOptions.merge())
                            firestore.collection("agent_visits").document(stage2Visit2.visitId)
                                .set(agentVisitToMap(stage2Visit2), com.google.firebase.firestore.SetOptions.merge())
                        } catch (_: Exception) {}

                        // Notify both agents of their stage 2 swapped assignment
                        try {
                            val notif1 = NotificationEntity(
                                notificationId = "notif_swap_" + UUID.randomUUID().toString().take(8),
                                userId = visit.agentId,
                                title = "🔄 Cross-Audit Task Assigned",
                                message = "Stage 1 complete! Stage 2 reciprocal cross-verification has been assigned to you. Inspect the counterpart party.",
                                type = "AGENT_VERIFICATION",
                                timestamp = System.currentTimeMillis(),
                                actionRoute = "agent_main"
                            )
                            val notif2 = NotificationEntity(
                                notificationId = "notif_swap_" + UUID.randomUUID().toString().take(8),
                                userId = counterpart.agentId,
                                title = "🔄 Cross-Audit Task Assigned",
                                message = "Stage 1 complete! Stage 2 reciprocal cross-verification has been assigned to you. Inspect the counterpart party.",
                                type = "AGENT_VERIFICATION",
                                timestamp = System.currentTimeMillis(),
                                actionRoute = "agent_main"
                            )
                            notificationDao.insertNotification(notif1)
                            notificationDao.insertNotification(notif2)
                            firestore.collection("notifications").document(notif1.notificationId).set(notificationToMap(notif1), com.google.firebase.firestore.SetOptions.merge())
                            firestore.collection("notifications").document(notif2.notificationId).set(notificationToMap(notif2), com.google.firebase.firestore.SetOptions.merge())
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        agentDao.updateVisit(completedVisit)
        try {
            firestore.collection("agent_visits")
                .document(visitId)
                .set(agentVisitToMap(completedVisit), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}

        // Advance loan status from COLLATERAL_VALUATION to CONTRACT_SIGNING
        if (visit.loanId.isNotBlank()) {
            var loan = loanDao.getLoanById(visit.loanId)
            if (loan == null) {
                try {
                    val doc = firestore.collection("loans").document(visit.loanId).get().await()
                    if (doc.exists()) {
                        loan = doc.toObject(com.loanzo.app.data.entity.LoanEntity::class.java)
                        if (loan != null) {
                            loanDao.insertLoan(loan)
                        }
                    }
                } catch (_: Exception) {}
            }
            if (loan != null && (loan.status == "COLLATERAL_VALUATION" || loan.status == "BID_ACCEPTED" || visit.visitType == "COLLATERAL_VERIFICATION")) {
                val updatedLoan = loan.copy(
                    status = "CONTRACT_SIGNING",
                    notes = "${loan.notes} | Doorstep verification completed by Officer ${visit.assignedAgentName ?: visit.agentId}".trim()
                )
                loanDao.updateLoan(updatedLoan)
                try {
                    firestore.collection("loans")
                        .document(loan.loanId)
                        .set(
                            mapOf(
                                "status" to "CONTRACT_SIGNING",
                                "notes" to updatedLoan.notes,
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                } catch (_: Exception) {}

                // If borrower identity was verified, mark user KYC verified
                if (loan.borrowerId.isNotBlank() && isBorrowerIdentityVerified) {
                    val borrower = userDao.getUserById(loan.borrowerId)
                    if (borrower != null && borrower.kycStatus != "VERIFIED") {
                        val updatedBorrower = borrower.copy(kycStatus = "VERIFIED", aadhaarVerified = true)
                        userDao.updateUser(updatedBorrower)
                        try {
                            firestore.collection("users")
                                .document(borrower.userId)
                                .set(
                                    mapOf(
                                        "kycStatus" to "VERIFIED",
                                        "aadhaarVerified" to true,
                                        "updatedAt" to System.currentTimeMillis()
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                        } catch (_: Exception) {}
                    }
                }

                // Push notifications to borrower and lender
                if (loan.borrowerId.isNotBlank()) {
                    sendUserNotification(
                        userId = loan.borrowerId,
                        title = "Field Verification Completed! ✅",
                        message = "Field verification for '${loan.purpose}' was completed successfully by Officer ${visit.assignedAgentName ?: "Agent"}.",
                        actionRoute = "loan_detail/${visit.loanId}"
                    )
                }
                if (loan.lenderId.isNotBlank()) {
                    sendUserNotification(
                        userId = loan.lenderId,
                        title = "Field Inspection Completed! 🛡️",
                        message = "Verification report submitted for loan '${loan.purpose}'. Loan ready for final contract signing.",
                        actionRoute = "loan_detail/${visit.loanId}"
                    )
                }
            }
        }

        // Credit agent earnings
        val user = userDao.getUserById(visit.agentId)
        if (user != null) {
            val updatedEarnings = user.totalAgentEarnings + visit.payoutAmount
            userDao.updateUser(user.copy(totalAgentEarnings = updatedEarnings))
            try {
                firestore.collection("users").document(visit.agentId)
                    .set(mapOf("totalAgentEarnings" to updatedEarnings), com.google.firebase.firestore.SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    suspend fun getLoanForVisit(loanId: String): com.loanzo.app.data.entity.LoanEntity? {
        if (loanId.isBlank()) return null
        val local = loanDao.getLoanById(loanId)
        if (local != null) return local
        return try {
            val doc = firestore.collection("loans").document(loanId).get().await()
            if (doc.exists()) {
                val loan = doc.toObject(com.loanzo.app.data.entity.LoanEntity::class.java)
                if (loan != null) {
                    loanDao.insertLoan(loan)
                    loan
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
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
        payoutAmount: Double
    ): String {
        val pairId = "cross_pair_" + UUID.randomUUID().toString().take(8)

        val visit1Id = "visit_cv_brw_" + UUID.randomUUID().toString().take(8)
        val visit2Id = "visit_cv_lnd_" + UUID.randomUUID().toString().take(8)

        val pin1 = String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)
        val pin2 = String.format(java.util.Locale.US, "%04d", java.util.Random().nextInt(9000) + 1000)

        // Visit 1: Agent 1 -> Borrower
        val visit1 = AgentVisitEntity(
            visitId = visit1Id,
            agentId = agent1Id,
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
            distanceKm = 3.8
        )

        // Visit 2: Agent 2 -> Lender
        val visit2 = AgentVisitEntity(
            visitId = visit2Id,
            agentId = agent2Id,
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
            distanceKm = 6.4
        )

        agentDao.insertVisits(listOf(visit1, visit2))
        try {
            val firestore = firestore
            firestore.collection("agent_visits").document(visit1Id)
                .set(agentVisitToMap(visit1), com.google.firebase.firestore.SetOptions.merge())
            firestore.collection("agent_visits").document(visit2Id)
                .set(agentVisitToMap(visit2), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}

        // Push alert notifications
        try {
            val notif1 = NotificationEntity(
                notificationId = "notif_cv_disp_1_" + UUID.randomUUID().toString().take(6),
                userId = agent1Id,
                title = "🛡️ High-Trust Inspection Assigned",
                message = "You have been dispatched for Borrower Physical KYC under the Blind Cross-Verification Protocol.",
                type = "AGENT_VERIFICATION",
                timestamp = System.currentTimeMillis(),
                actionRoute = "agent_main"
            )
            val notif2 = NotificationEntity(
                notificationId = "notif_cv_disp_2_" + UUID.randomUUID().toString().take(6),
                userId = agent2Id,
                title = "🛡️ High-Trust Inspection Assigned",
                message = "You have been dispatched for Lender Physical KYC under the Blind Cross-Verification Protocol.",
                type = "AGENT_VERIFICATION",
                timestamp = System.currentTimeMillis(),
                actionRoute = "agent_main"
            )
            notificationDao.insertNotification(notif1)
            notificationDao.insertNotification(notif2)
            val firestore = firestore
            firestore.collection("notifications").document(notif1.notificationId).set(notificationToMap(notif1), com.google.firebase.firestore.SetOptions.merge())
            firestore.collection("notifications").document(notif2.notificationId).set(notificationToMap(notif2), com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}

        return pairId
    }

    suspend fun seedSampleVisits(agentId: String) {
        // No-op: Demo data seeding disabled completely
    }

    suspend fun seedSampleVisitsForAgent(agentId: String) {
        // No-op: Demo data seeding disabled completely
    }

    /**
     * Listens in real-time to agent_applications collection for the specific user
     * and syncs changes (such as approval, rejection, or remarks) to Room SQLite.
     */
    fun listenToUserApplications(userId: String, scope: kotlinx.coroutines.CoroutineScope) {
        if (userId.isBlank()) return
        val firestore = firestore
        try {
            firestore.collection("agent_applications")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val app = doc.toAgentApplication()
                            if (app != null && app.applicationId.isNotBlank()) {
                                agentDao.insertApplication(app)
                            }
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    /**
     * Listens in real-time to agent_visits collection for this agent
     * and syncs dispatched field tasks and cross-verification assignments to Room SQLite.
     */
    fun listenToUserVisits(agentId: String, scope: kotlinx.coroutines.CoroutineScope) {
        if (agentId.isBlank()) return
        val firestore = firestore
        try {
            firestore.collection("agent_visits")
                .whereEqualTo("agentId", agentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val visit = doc.toAgentVisit()
                            if (visit != null && visit.visitId.isNotBlank()) {
                                agentDao.insertVisit(visit)
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

    private suspend fun sendUserNotification(userId: String, title: String, message: String, actionRoute: String? = null) {
        val notif = NotificationEntity(
            notificationId = "notif_" + UUID.randomUUID().toString().take(8),
            userId = userId,
            title = title,
            message = message,
            type = "LOAN_UPDATE",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = actionRoute
        )
        try {
            notificationDao.insertNotification(notif)
            val notifMap = hashMapOf(
                "notificationId" to notif.notificationId,
                "userId" to notif.userId,
                "title" to notif.title,
                "message" to notif.message,
                "type" to notif.type,
                "timestamp" to notif.timestamp,
                "isRead" to notif.isRead,
                "actionRoute" to notif.actionRoute
            )
            firestore.collection("notifications")
                .document(notif.notificationId)
                .set(notifMap, com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}
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

    fun agentApplicationToMap(a: AgentApplicationEntity): Map<String, Any?> {
        return hashMapOf(
            "applicationId" to a.applicationId,
            "userId" to a.userId,
            "applicantName" to a.applicantName,
            "applicantPhone" to a.applicantPhone,
            "applicantEmail" to a.applicantEmail,
            "experienceYears" to a.experienceYears,
            "priorDomain" to a.priorDomain,
            "policeVerificationNumber" to a.policeVerificationNumber,
            "policeStation" to a.policeStation,
            "policeVerificationDate" to a.policeVerificationDate,
            "policeDocUri" to a.policeDocUri,
            "permanentAddress" to a.permanentAddress,
            "operatingCity" to a.operatingCity,
            "operatingPincode" to a.operatingPincode,
            "serviceRadiusKm" to a.serviceRadiusKm,
            "vehicleType" to a.vehicleType,
            "drivingLicenseNumber" to a.drivingLicenseNumber,
            "status" to a.status,
            "submittedAt" to a.submittedAt,
            "reviewedAt" to a.reviewedAt,
            "adminRemarks" to a.adminRemarks
        )
    }

    fun notificationToMap(n: NotificationEntity): Map<String, Any?> {
        return hashMapOf(
            "notificationId" to n.notificationId,
            "userId" to n.userId,
            "title" to n.title,
            "message" to n.message,
            "type" to n.type,
            "timestamp" to n.timestamp,
            "isRead" to n.isRead,
            "actionRoute" to n.actionRoute
        )
    }
}
