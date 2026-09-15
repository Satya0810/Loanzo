package com.loanzo.app.ui.admin

import com.loanzo.app.ui.components.ExecutiveHeroCard
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.ui.components.SegmentedCapsuleTab
import com.loanzo.app.ui.theme.*


import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.R
import com.loanzo.app.data.entity.*
import com.loanzo.app.ui.support.AdminTicketConsole
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.LocalSupportTicketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOwnerVerificationScreen(
    allUsers: List<UserEntity> = emptyList(),
    verifications: List<VerificationEntity>,
    agentApplications: List<AgentApplicationEntity> = emptyList(),
    onApproveVerification: (token: String, phone: String) -> Unit,
    onManualVerify: (String) -> Unit,
    onApproveAgentApplication: (String) -> Unit = {},
    onRejectAgentApplication: (String, String) -> Unit = { _, _ -> },
    onVerifyUserKyc: (user: UserEntity, approve: Boolean, remarks: String) -> Unit = { _, _, _ -> },
    initialTab: Int = 0,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminRepository = LocalAdminRepository.current
    val supportTicketRepository = LocalSupportTicketRepository.current

    // Observe Admin Data Streams
    val complaints by adminRepository.allComplaints.collectAsState(initial = emptyList())
    val meetings by adminRepository.allMeetings.collectAsState(initial = emptyList())
    val vaultItems by adminRepository.allVaultItems.collectAsState(initial = emptyList())
    val nocs by adminRepository.allNocs.collectAsState(initial = emptyList())
    val allVisits by adminRepository.allVisits.collectAsState(initial = emptyList())
    val allTickets by supportTicketRepository.allTickets.collectAsState(initial = emptyList())
    val allAdminRequests by adminRepository.allAdminRequests.collectAsState(initial = emptyList())
    val pendingAdminRequests = remember(allAdminRequests) { allAdminRequests.filter { it.status == "PENDING" } }
    val unassignedVisits = remember(allVisits) { allVisits.filter { it.agentId == "UNASSIGNED" || it.agentId.isBlank() } }
    val crossVerificationVisits = remember(allVisits) { allVisits.filter { it.isCrossVerification } }

    // Seed sample data and start real-time cloud sync on first entry
    LaunchedEffect(Unit) {
        adminRepository.startRealtimeAdminCloudSync(scope)
        scope.launch(Dispatchers.IO) {
            adminRepository.refreshAgentApplicationsFromCloud()
            adminRepository.refreshUsersFromCloud()
        }
    }

    // Active Operational Module:
    // Module 0: 👥 Identity & Agents (Members, KYC, Empaneled Agents, PCC)
    // Module 1: 🗺️ Field Dispatch (Visits, Dual-Officer Consensus Matrix)
    // Module 2: 💎 Vault & Legal NOC (Collateral Lockers, Cryptographic NOCs)
    // Module 3: ⚖️ Resolution & Support (Grievances, Ombudsman Hearings, Support Tickets)
    val initialModule = remember(initialTab) {
        when (initialTab) {
            0, 10, 11 -> 0
            1, 20, 21 -> 1
            2, 4, 6, 30, 31 -> 2
            3, 5, 7, 9, 40, 41, 42 -> 3
            else -> 0
        }
    }
    var activeTab by remember(initialTab) { mutableIntStateOf(initialModule) }
    var identitySubTab by remember(initialTab) {
        mutableIntStateOf(if (initialTab == 11) 1 else 0)
    }
    var vaultSubTab by remember(initialTab) {
        mutableIntStateOf(if (initialTab == 6 || initialTab == 31) 1 else 0)
    }
    var supportSubTab by remember(initialTab) {
        mutableIntStateOf(
            when (initialTab) {
                7, 41 -> 1
                9, 42 -> 2
                else -> 0
            }
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog & Sheet States
    var inspectingDoc by remember { mutableStateOf<DocumentInspectionData?>(null) }
    var dispatchingVisit by remember { mutableStateOf<AgentVisitEntity?>(null) }
    var schedulingMediation by remember { mutableStateOf<ComplaintEntity?>(null) }
    var assigningLockerItem by remember { mutableStateOf<CollateralVaultEntity?>(null) }
    var sendingNotificationToUser by remember { mutableStateOf<UserEntity?>(null) }
    var directNotifTitle by remember { mutableStateOf("") }
    var directNotifMessage by remember { mutableStateOf("") }

    // Counts for Badges
    val pendingUsersCount = remember(allUsers, pendingAdminRequests) {
        allUsers.count { it.kycStatus == "PENDING" || it.kycStatus == "IN_PROGRESS" } + pendingAdminRequests.size
    }
    val pendingAgentsCount = remember(agentApplications) { agentApplications.count { it.status == "PENDING" } }
    val openComplaintsCount = remember(complaints) { complaints.count { it.status == "OPEN" || it.status == "INVESTIGATING" } }
    val unassignedCount = remember(unassignedVisits) { unassignedVisits.size }
    val vaultTotalValue = remember(vaultItems) { vaultItems.sumOf { it.estimatedValue } }
    val openTicketsCount = remember(allTickets) { allTickets.count { it.status == "OPEN" || it.status == "UNDER_REVIEW" || it.status == "CALLBACK_SCHEDULED" || it.status == "IN_PROGRESS" } }

    LoanzoTheme(darkTheme = false) {
        Scaffold(
            topBar = {
                Surface(
                    shadowElevation = 1.dp,
                    color = Color.White
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "Loanzo Logo",
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Command Center",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = TextNavyDark,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = GoldCoinCream,
                                            border = BorderStroke(1.dp, GoldCoinBorder)
                                        ) {
                                            Text(
                                                text = "MASTER ADMIN",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = GoldCoinAmber,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Institutional Governance & Operations",
                                        fontSize = 11.sp,
                                        color = TextSlateMuted,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextNavyDark)
                            }
                        },
                        actions = {
                            var isSyncingCloud by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    if (!isSyncingCloud) {
                                        isSyncingCloud = true
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                adminRepository.startRealtimeAdminCloudSync(scope)
                                                adminRepository.refreshAgentApplicationsFromCloud()
                                                adminRepository.refreshUsersFromCloud()
                                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Cloud sync complete: Applications & users up-to-date", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            } finally {
                                                isSyncingCloud = false
                                            }
                                        }
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(BrandIceBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync Cloud Data",
                                        tint = BrandRoyalBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = TextNavyDark,
                            navigationIconContentColor = TextNavyDark
                        )
                    )
                }
            },
            containerColor = CanvasPorcelain
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Compact Executive Operational Metrics Ribbon
            AdminExecutiveRibbon(
                totalUsers = allUsers.size,
                pendingKycUsers = pendingUsersCount,
                activeAgents = agentApplications.count { it.status == "APPROVED" },
                unassignedVisits = unassignedCount,
                vaultValue = vaultTotalValue,
                openComplaints = openComplaintsCount
            )

            // Smart Operational Modules Bar (4 Strategic Desks)
            val desks = listOf(
                AdminDeskItem(
                    id = 0,
                    title = "Identity & Agents",
                    icon = Icons.Default.Badge,
                    count = allUsers.size + agentApplications.size,
                    alertCount = pendingUsersCount + pendingAgentsCount,
                    alertColor = if ((pendingUsersCount + pendingAgentsCount) > 0) GoldCoinAmber else Emerald500
                ),
                AdminDeskItem(
                    id = 1,
                    title = "Field Dispatch",
                    icon = Icons.Default.NearMe,
                    count = allVisits.size,
                    alertCount = unassignedCount,
                    alertColor = if (unassignedCount > 0) BrandSolarOrange else Emerald500
                ),
                AdminDeskItem(
                    id = 2,
                    title = "Vault & Legal NOC",
                    icon = Icons.Default.Diamond,
                    count = vaultItems.size + nocs.size
                ),
                AdminDeskItem(
                    id = 3,
                    title = "Resolution & Support",
                    icon = Icons.Default.Gavel,
                    count = complaints.size + allTickets.size,
                    alertCount = openComplaintsCount + openTicketsCount,
                    alertColor = if ((openComplaintsCount + openTicketsCount) > 0) Red500 else Emerald500
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                desks.forEach { desk ->
                    AdminDeskPill(
                        desk = desk,
                        isSelected = activeTab == desk.id,
                        onClick = { activeTab = desk.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Tab Content Router
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                when (activeTab) {
                    0 -> Column(modifier = Modifier.fillMaxSize()) {
                        SegmentedCapsuleTab(
                            tabs = listOf(
                                "Members & KYC (${allUsers.size})",
                                "Field Agents (${agentApplications.size})"
                            ),
                            selectedIndex = identitySubTab,
                            onTabSelected = { identitySubTab = it },
                            modifier = Modifier.padding(bottom = 10.dp),
                            activeColor = BrandRoyalBlue,
                            activeTextColor = Color.White,
                            inactiveTextColor = TextSlateMuted,
                            containerColor = Color.White
                        )
                        if (identitySubTab == 0) {
                            UsersKycTab(
                                users = allUsers,
                                pendingAdminRequests = pendingAdminRequests,
                                onApproveAdminRequest = { reqId ->
                                    scope.launch(Dispatchers.IO) {
                                        adminRepository.approveAdminRequest(reqId)
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "User role elevated to Admin successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onRejectAdminRequest = { reqId, reason ->
                                    scope.launch(Dispatchers.IO) {
                                        adminRepository.rejectAdminRequest(reqId, reason)
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Admin request rejected", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onSendDirectNotif = { user ->
                                    sendingNotificationToUser = user
                                    directNotifTitle = "Platform Notification"
                                    directNotifMessage = ""
                                },
                                onInspectDoc = { data -> inspectingDoc = data },
                                onVerifyUser = onVerifyUserKyc,
                                onCallUser = { phone ->
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )
                        } else {
                            AgentsTab(
                                agents = agentApplications,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                onApprove = onApproveAgentApplication,
                                onReject = onRejectAgentApplication,
                                onInspectDoc = { agent ->
                                    inspectingDoc = DocumentInspectionData(
                                        title = "Police Clearance Certificate (PCC)",
                                        category = "AGENT_EMPANELMENT",
                                        subjectName = agent.applicantName,
                                        subjectPhone = agent.applicantPhone,
                                        documentNumber = agent.policeVerificationNumber,
                                        issuingAuthority = agent.policeStation,
                                        photoUri = agent.policeDocUri,
                                        onApprove = { onApproveAgentApplication(agent.applicationId) },
                                        onReject = { reason -> onRejectAgentApplication(agent.applicationId, reason) }
                                    )
                                },
                                onSuspend = { agentId -> scope.launch(Dispatchers.IO) { adminRepository.suspendAgent(agentId, "Admin suspension") } },
                                onReactivate = { agentId -> scope.launch(Dispatchers.IO) { adminRepository.reactivateAgent(agentId) } },
                                onRefreshCloud = {
                                    scope.launch(Dispatchers.IO) {
                                        adminRepository.refreshAgentApplicationsFromCloud()
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Agent applications refreshed from cloud", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                    1 -> DispatchTab(
                        unassignedVisits = unassignedVisits,
                        crossVerificationVisits = crossVerificationVisits,
                        allAgents = agentApplications.filter { it.status == "APPROVED" },
                        onOpenDispatch = { visit -> dispatchingVisit = visit },
                        onSanctionLoan = { loanId, title ->
                            scope.launch(Dispatchers.IO) {
                                val targetLoan = adminRepository.getLoanById(loanId)
                                val borrowerUserId = targetLoan?.borrowerId?.ifBlank { null } ?: "USR-BRW"
                                val lenderUserId = targetLoan?.lenderId?.ifBlank { null } ?: "USR-LND"

                                adminRepository.sendUserNotification(
                                    userId = borrowerUserId,
                                    title = "Dual-Agent Consensus Approved",
                                    message = "Your loan verification ($title) has been certified by dual field officers with 100% consensus. Escrow sanction approved!",
                                    actionRoute = "loans"
                                )
                                if (targetLoan != null && lenderUserId.isNotBlank()) {
                                    adminRepository.sendUserNotification(
                                        userId = lenderUserId,
                                        title = "Dual-Agent Consensus Approved",
                                        message = "Loan ($title) has passed 100% dual-agent consensus. Escrow sanction approved!",
                                        actionRoute = "loans"
                                    )
                                }
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Dual-Officer Consensus Sanctioned! Parties notified.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                    2 -> Column(modifier = Modifier.fillMaxSize()) {
                        SegmentedCapsuleTab(
                            tabs = listOf(
                                "Collateral Lockers (${vaultItems.size})",
                                "Legal NOC Registry (${nocs.size})"
                            ),
                            selectedIndex = vaultSubTab,
                            onTabSelected = { vaultSubTab = it },
                            modifier = Modifier.padding(bottom = 10.dp),
                            activeColor = BrandRoyalBlue,
                            activeTextColor = Color.White,
                            inactiveTextColor = TextSlateMuted,
                            containerColor = Color.White
                        )
                        if (vaultSubTab == 0) {
                            VaultTab(
                                vaultItems = vaultItems,
                                onAssignLocker = { item -> assigningLockerItem = item },
                                onRelease = { loanId -> scope.launch(Dispatchers.IO) { adminRepository.releaseCollateral(loanId) } }
                            )
                        } else {
                            NocTab(
                                nocs = nocs,
                                vaultItems = vaultItems,
                                onGenerateNoc = { loanId, borrower, pan, lender, amount, repaid, desc ->
                                    scope.launch(Dispatchers.IO) {
                                        val targetLoan = adminRepository.getLoanById(loanId)
                                        val realBorrowerId = targetLoan?.borrowerId?.ifBlank { null }
                                            ?: allUsers.firstOrNull { it.name.equals(borrower, ignoreCase = true) }?.userId
                                            ?: "USR-BRW"
                                        val realLenderId = targetLoan?.lenderId?.ifBlank { null }
                                            ?: allUsers.firstOrNull { it.name.equals(lender, ignoreCase = true) }?.userId
                                            ?: "USR-LND"

                                        adminRepository.generateNoc(
                                            loanId = loanId,
                                            borrowerId = realBorrowerId,
                                            borrowerName = borrower,
                                            borrowerPan = pan,
                                            lenderId = realLenderId,
                                            lenderName = lender,
                                            principalAmount = amount,
                                            totalRepaidAmount = repaid,
                                            collateralDesc = desc
                                        )
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Cryptographic NOC issued & collateral released!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                    3 -> Column(modifier = Modifier.fillMaxSize()) {
                        SegmentedCapsuleTab(
                            tabs = listOf(
                                "Grievances (${complaints.size})",
                                "Hearings (${meetings.size})",
                                "Support Tickets (${allTickets.size})"
                            ),
                            selectedIndex = supportSubTab,
                            onTabSelected = { supportSubTab = it },
                            modifier = Modifier.padding(bottom = 10.dp),
                            activeColor = BrandRoyalBlue,
                            activeTextColor = Color.White,
                            inactiveTextColor = TextSlateMuted,
                            containerColor = Color.White
                        )
                        when (supportSubTab) {
                            0 -> ComplaintsTab(
                                complaints = complaints,
                                onScheduleHearing = { cmp -> schedulingMediation = cmp },
                                onResolve = { id -> scope.launch(Dispatchers.IO) { adminRepository.resolveComplaint(id, "Resolved by Master Admin") } },
                                onDismiss = { id -> scope.launch(Dispatchers.IO) { adminRepository.dismissComplaint(id, "Dismissed post verification") } },
                                onAuthorizeDeviceTransfer = { compId, userId, newDevId, newDevModel ->
                                    scope.launch(Dispatchers.IO) {
                                        adminRepository.authorizeDeviceTransfer(compId, userId, newDevId, newDevModel)
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Device transfer authorized for $userId to $newDevModel", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                            1 -> HearingsTab(
                                meetings = meetings,
                                onScheduleNew = {
                                    schedulingMediation = ComplaintEntity(
                                        complaintId = "GENERAL",
                                        complainantId = "ADMIN",
                                        complainantName = "Master Admin",
                                        complainantRole = "ADMIN",
                                        complainantPhone = "+917061559039",
                                        category = "OTHER",
                                        priority = "MEDIUM",
                                        subject = "Loan Mediation Hearing",
                                        description = "Executive hearing session",
                                        status = "OPEN"
                                    )
                                },
                                onMarkCompleted = { id -> scope.launch(Dispatchers.IO) { adminRepository.updateMeetingStatus(id, "COMPLETED", "Concluded successfully") } }
                            )
                            2 -> AdminTicketConsole(
                                tickets = allTickets,
                                onScheduleCallback = { ticketId, callbackAt, notes ->
                                    scope.launch(Dispatchers.IO) {
                                        supportTicketRepository.scheduleCallback(ticketId, callbackAt, notes)
                                    }
                                },
                                onMarkUnderReview = { ticketId ->
                                    scope.launch(Dispatchers.IO) {
                                        supportTicketRepository.markUnderReview(ticketId)
                                    }
                                },
                                onMarkInProgress = { ticketId ->
                                    scope.launch(Dispatchers.IO) {
                                        supportTicketRepository.markInProgress(ticketId)
                                    }
                                },
                                onResolve = { ticketId, resolutionNotes ->
                                    scope.launch(Dispatchers.IO) {
                                        supportTicketRepository.resolveTicket(ticketId, resolutionNotes)
                                    }
                                },
                                onEscalate = { ticketId, notes ->
                                    scope.launch(Dispatchers.IO) {
                                        supportTicketRepository.escalateTicket(ticketId, notes)
                                    }
                                },
                                onReject = { ticketId, notes ->
                                    scope.launch(Dispatchers.IO) {
                                        supportTicketRepository.rejectTicket(ticketId, notes)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Active Sheets & Dialogs ---

        inspectingDoc?.let { doc ->
            DocumentInspectionDialog(
                docTitle = doc.title,
                docCategory = doc.category,
                subjectName = doc.subjectName,
                subjectPhone = doc.subjectPhone,
                documentNumber = doc.documentNumber,
                issuingAuthority = doc.issuingAuthority,
                photoUri = doc.photoUri,
                onApprove = {
                    doc.onApprove()
                    inspectingDoc = null
                    Toast.makeText(context, "Document verified & attested successfully", Toast.LENGTH_SHORT).show()
                },
                onReject = { reason ->
                    doc.onReject(reason)
                    inspectingDoc = null
                    Toast.makeText(context, "Deficiency notice issued: $reason", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { inspectingDoc = null }
            )
        }

        dispatchingVisit?.let { visit ->
            val workloadMap = remember(allVisits) {
                allVisits.filter { it.agentId.isNotBlank() && it.agentId != "UNASSIGNED" && it.status != "COMPLETED" && it.status != "CANCELLED" }
                    .groupingBy { it.agentId }
                    .eachCount()
            }
            DispatchAgentSheet(
                visit = visit,
                availableAgents = agentApplications.filter { it.status == "APPROVED" },
                agentWorkload = workloadMap,
                onDispatch = { agentId, payout ->
                    dispatchingVisit = null // Immediate dismissal prevents repetitive opening
                    scope.launch(Dispatchers.IO) {
                        adminRepository.assignAgentToVisit(visit.visitId, agentId, payout)
                    }
                    Toast.makeText(context, "Visit mapped to field agent successfully!", Toast.LENGTH_SHORT).show()
                },
                onDispatchDual = { agent1Id, agent2Id, payout ->
                    dispatchingVisit = null // Immediate dismissal prevents repetitive opening
                    scope.launch(Dispatchers.IO) {
                        adminRepository.dispatchCrossVerificationPair(
                            loanId = visit.loanId,
                            loanTitle = visit.title,
                            borrowerName = visit.borrowerName,
                            borrowerPhone = visit.borrowerPhone,
                            borrowerAddress = visit.borrowerAddress,
                            lenderName = visit.lenderName,
                            lenderPhone = visit.lenderPhone,
                            lenderAddress = visit.lenderAddress,
                            agent1Id = agent1Id,
                            agent2Id = agent2Id,
                            collateralItemName = visit.collateralItemName,
                            collateralEstimatedValue = visit.collateralEstimatedValue,
                            payoutAmount = payout,
                            originalVisitId = visit.visitId
                        )
                    }
                    Toast.makeText(context, "🛡️ Blind Dual-Agent Pair Dispatched Successfully!", Toast.LENGTH_LONG).show()
                },
                onDismiss = { dispatchingVisit = null }
            )
        }

        sendingNotificationToUser?.let { targetUser ->
            AlertDialog(
                onDismissRequest = { sendingNotificationToUser = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, null, tint = BrandRoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Direct Cloud Notification", color = TextNavyDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column {
                        Text("Recipient: ${targetUser.name} (${targetUser.phone})", color = TextSlateMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = directNotifTitle,
                            onValueChange = { directNotifTitle = it },
                            label = { Text("Notification Title") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextNavyDark,
                                unfocusedTextColor = TextNavyDark,
                                focusedBorderColor = BrandRoyalBlue,
                                unfocusedBorderColor = BrandIceBorder,
                                focusedLabelColor = BrandRoyalBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = directNotifMessage,
                            onValueChange = { directNotifMessage = it },
                            label = { Text("Message Body") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextNavyDark,
                                unfocusedTextColor = TextNavyDark,
                                focusedBorderColor = BrandRoyalBlue,
                                unfocusedBorderColor = BrandIceBorder,
                                focusedLabelColor = BrandRoyalBlue
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (directNotifTitle.isNotBlank() && directNotifMessage.isNotBlank()) {
                                scope.launch(Dispatchers.IO) {
                                    adminRepository.sendUserNotification(
                                        userId = targetUser.userId,
                                        title = directNotifTitle,
                                        message = directNotifMessage,
                                        actionRoute = "notifications"
                                    )
                                }
                                sendingNotificationToUser = null
                                Toast.makeText(context, "Notification delivered to ${targetUser.name}!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White),
                        enabled = directNotifTitle.isNotBlank() && directNotifMessage.isNotBlank()
                    ) {
                        Text("Send to Device", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sendingNotificationToUser = null }) {
                        Text("Cancel", color = TextSlateMuted)
                    }
                },
                containerColor = Color.White
            )
        }

        schedulingMediation?.let { cmp ->
            ScheduleMediationDialog(
                initialComplaintId = if (cmp.complaintId != "GENERAL") cmp.complaintId else null,
                initialLoanId = cmp.loanId,
                borrowerName = cmp.complainantName,
                borrowerPhone = cmp.complainantPhone,
                lenderName = cmp.targetPartyName,
                onSchedule = { newMeeting ->
                    scope.launch(Dispatchers.IO) {
                        adminRepository.scheduleMediationMeeting(newMeeting)
                    }
                    schedulingMediation = null
                    Toast.makeText(context, "Mediation hearing summons scheduled!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { schedulingMediation = null }
            )
        }

        assigningLockerItem?.let { item ->
            AssignVaultLockerDialog(
                item = item,
                onAssign = { locker, tag, seal ->
                    scope.launch(Dispatchers.IO) {
                        adminRepository.assignLockerAndSeal(item.vaultItemId, locker, tag, seal)
                    }
                    assigningLockerItem = null
                    Toast.makeText(context, "Asset sealed & secured in vault locker $locker!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { assigningLockerItem = null }
            )
        }
    }
    }
}

// Data holder for document inspector
data class DocumentInspectionData(
    val title: String,
    val category: String,
    val subjectName: String,
    val subjectPhone: String,
    val documentNumber: String,
    val issuingAuthority: String,
    val photoUri: String?,
    val onApprove: () -> Unit,
    val onReject: (String) -> Unit
)

// --- MODULE 0: KPI RIBBON ---
data class AdminDeskItem(
    val id: Int,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val count: Int,
    val alertCount: Int = 0,
    val alertColor: Color = Red500
)

@Composable
private fun AdminDeskPill(
    desk: AdminDeskItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) BrandRoyalBlue else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) BrandRoyalBlue else BrandIceBorder
        ),
        modifier = Modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = desk.icon,
                contentDescription = desk.title,
                tint = if (isSelected) Color.White else TextSlateMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = desk.title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextNavyDark,
                maxLines = 1,
                softWrap = false
            )
            if (desk.alertCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = desk.alertColor
                ) {
                    Text(
                        text = "${desk.alertCount}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            } else if (desk.count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) BrandCobalt else BrandIceBlue
                ) {
                    Text(
                        text = "${desk.count}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else BrandRoyalBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminExecutiveRibbon(
    totalUsers: Int,
    pendingKycUsers: Int,
    activeAgents: Int,
    unassignedVisits: Int,
    vaultValue: Double,
    openComplaints: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Members & KYC Chip
        ExecutiveMetricChip(
            label = "MEMBERS",
            value = "$totalUsers",
            subValue = if (pendingKycUsers > 0) "$pendingKycUsers Req" else "Verified",
            icon = Icons.Default.Person,
            accentColor = if (pendingKycUsers > 0) GoldCoinAmber else BrandRoyalBlue
        )

        // Vault Escrow Chip
        ExecutiveMetricChip(
            label = "VAULT ESCROW",
            value = "₹${(vaultValue / 100000).formatDecimal(1)}L",
            icon = Icons.Default.Diamond,
            accentColor = GoldCoinAmber
        )

        // Agents Chip
        ExecutiveMetricChip(
            label = "AGENTS",
            value = "$activeAgents On-Duty",
            icon = Icons.Default.Shield,
            accentColor = Emerald500
        )

        // Dispatch Chip
        ExecutiveMetricChip(
            label = "DISPATCH",
            value = if (unassignedVisits > 0) "$unassignedVisits Pending" else "All Assigned",
            icon = Icons.Default.NearMe,
            accentColor = if (unassignedVisits > 0) BrandSolarOrange else Emerald500
        )

        // Ombudsman Chip
        ExecutiveMetricChip(
            label = "OMBUDSMAN",
            value = if (openComplaints > 0) "$openComplaints Open" else "Zero Open",
            icon = Icons.Default.Gavel,
            accentColor = if (openComplaints > 0) Red500 else Emerald500
        )
    }
}

@Composable
private fun ExecutiveMetricChip(
    label: String,
    value: String,
    subValue: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandIceBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
            }
            Column {
                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted, letterSpacing = 0.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextNavyDark)
                    if (subValue != null) {
                        Text(subValue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                    }
                }
            }
        }
    }
}

// --- MODULE 0: USERS & KYC VERIFICATION DESK ---
@Composable
private fun UsersKycTab(
    users: List<UserEntity>,
    pendingAdminRequests: List<AdminRequestEntity> = emptyList(),
    onApproveAdminRequest: (String) -> Unit = {},
    onRejectAdminRequest: (String, String) -> Unit = { _, _ -> },
    onSendDirectNotif: (UserEntity) -> Unit = {},
    onInspectDoc: (DocumentInspectionData) -> Unit,
    onVerifyUser: (user: UserEntity, approve: Boolean, remarks: String) -> Unit,
    onCallUser: (phone: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) }
    var isRequestsExpanded by remember { mutableStateOf(false) }
    var userForAction by remember { mutableStateOf<UserEntity?>(null) }
    var isRejectDialogVisible by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

    val pendingCount = remember(users) {
        users.count {
            it.kycStatus.equals("PENDING", ignoreCase = true) ||
            it.kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
            it.kycStatus.equals("IN_REVIEW", ignoreCase = true) ||
            it.kycStatus.equals("UNDER_REVIEW", ignoreCase = true) ||
            it.kycStatus.equals("RE_KYC_REQUESTED", ignoreCase = true)
        }
    }
    val verifiedCount = remember(users) { users.count { it.kycStatus.equals("VERIFIED", ignoreCase = true) } }
    val agentCount = remember(users) { users.count { it.role.equals("AGENT", ignoreCase = true) || it.agentStatus.equals("APPROVED", ignoreCase = true) || it.agentStatus.equals("PENDING", ignoreCase = true) } }
    val lenderCount = remember(users) { users.count { it.role.equals("LENDER", ignoreCase = true) } }
    val borrowerCount = remember(users) { users.count { it.role.equals("BORROWER", ignoreCase = true) || it.role.equals("MEMBER", ignoreCase = true) } }

    val filteredUsers = remember(users, searchQuery, selectedFilter) {
        users.filter { u ->
            val matchesQuery = searchQuery.isBlank() ||
                    u.name.contains(searchQuery, ignoreCase = true) ||
                    u.phone.contains(searchQuery, ignoreCase = true) ||
                    u.email.contains(searchQuery, ignoreCase = true) ||
                    u.panNumber.contains(searchQuery, ignoreCase = true) ||
                    u.aadhaarNumber.contains(searchQuery, ignoreCase = true) ||
                    u.username.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                1 -> u.kycStatus.equals("PENDING", ignoreCase = true) ||
                     u.kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
                     u.kycStatus.equals("IN_REVIEW", ignoreCase = true) ||
                     u.kycStatus.equals("UNDER_REVIEW", ignoreCase = true) ||
                     u.kycStatus.equals("RE_KYC_REQUESTED", ignoreCase = true)
                2 -> u.kycStatus.equals("VERIFIED", ignoreCase = true)
                3 -> u.role.equals("AGENT", ignoreCase = true) || u.agentStatus.equals("APPROVED", ignoreCase = true) || u.agentStatus.equals("PENDING", ignoreCase = true)
                4 -> u.role.equals("LENDER", ignoreCase = true)
                5 -> u.role.equals("BORROWER", ignoreCase = true) || u.role.equals("MEMBER", ignoreCase = true)
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Collapsible Staff / Admin Requests Banner (Saves vertical space)
        if (pendingAdminRequests.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldCoinCream,
                border = BorderStroke(1.dp, GoldCoinBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRequestsExpanded = !isRequestsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(GoldCoinAmber.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldCoinAmber, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(
                                    text = "Staff / Admin Access Requests (${pendingAdminRequests.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavyDark
                                )
                                Text(
                                    text = if (isRequestsExpanded) "Tap to collapse" else "Tap to review & elevate requests",
                                    fontSize = 10.sp,
                                    color = TextSlateMuted
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = GoldCoinAmber
                            ) {
                                Text(
                                    text = "${pendingAdminRequests.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Icon(
                                imageVector = if (isRequestsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isRequestsExpanded) "Collapse" else "Expand",
                                tint = GoldCoinAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isRequestsExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))

                        pendingAdminRequests.forEach { req ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = req.userName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextNavyDark
                                            )
                                            Text(
                                                text = "${req.userPhone} • Current: ${req.currentRole}",
                                                fontSize = 11.sp,
                                                color = TextSlateMuted
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = GoldCoinCream,
                                            border = BorderStroke(1.dp, GoldCoinBorder)
                                        ) {
                                            Text(
                                                text = "REQ: ${req.requestedRole}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldCoinAmber,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (req.justification.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CanvasPorcelain,
                                            border = BorderStroke(1.dp, BrandIceBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "“${req.justification}”",
                                                fontSize = 11.sp,
                                                color = TextNavyDark,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { onRejectAdminRequest(req.requestId, "Admin clearance criteria not met") },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, RedBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                        ) {
                                            Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { onApproveAdminRequest(req.requestId) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Elevate to Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Daylight Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, phone, PAN, Aadhaar...", color = TextSlateMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = CanvasPorcelain,
                focusedBorderColor = BrandRoyalBlue,
                unfocusedBorderColor = BrandIceBorder,
                focusedTextColor = TextNavyDark,
                unfocusedTextColor = TextNavyDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipItem("All (${users.size})", selectedFilter == 0) { selectedFilter = 0 }
            FilterChipItem("Pending Review (${pendingCount})", selectedFilter == 1) { selectedFilter = 1 }
            FilterChipItem("Verified Members (${verifiedCount})", selectedFilter == 2) { selectedFilter = 2 }
            FilterChipItem("Field Agents (${agentCount})", selectedFilter == 3) { selectedFilter = 3 }
            FilterChipItem("Lenders (${lenderCount})", selectedFilter == 4) { selectedFilter = 4 }
            FilterChipItem("Borrowers (${borrowerCount})", selectedFilter == 5) { selectedFilter = 5 }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No users matched your filter", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Try a different search keyword or filter tab", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredUsers, key = { it.userId }) { user ->
                    AdminUserKycCard(
                        user = user,
                        onInspectAadhaar = {
                            onInspectDoc(
                                DocumentInspectionData(
                                    title = "Aadhaar Identity Dossier",
                                    category = "GOVT_IDENTITY",
                                    subjectName = user.name,
                                    subjectPhone = user.phone,
                                    documentNumber = user.aadhaarNumber.ifBlank { "Not Recorded" },
                                    issuingAuthority = "UIDAI / Unique Identification Authority of India",
                                    photoUri = user.aadhaarImageUrl.ifBlank { null },
                                    onApprove = { onVerifyUser(user, true, "Aadhaar verified & attested") },
                                    onReject = { reason -> onVerifyUser(user, false, reason) }
                                )
                            )
                        },
                        onInspectPan = {
                            onInspectDoc(
                                DocumentInspectionData(
                                    title = "Permanent Account Number (PAN)",
                                    category = "TAX_COMPLIANCE",
                                    subjectName = user.name,
                                    subjectPhone = user.phone,
                                    documentNumber = user.panNumber.ifBlank { "Not Recorded" },
                                    issuingAuthority = "Income Tax Department of India",
                                    photoUri = user.panImageUrl.ifBlank { null },
                                    onApprove = { onVerifyUser(user, true, "PAN record confirmed") },
                                    onReject = { reason -> onVerifyUser(user, false, reason) }
                                )
                            )
                        },
                        onInspectSelfie = {
                            onInspectDoc(
                                DocumentInspectionData(
                                    title = "Live Biometric Facial Selfie",
                                    category = "FACIAL_BIOMETRICS",
                                    subjectName = user.name,
                                    subjectPhone = user.phone,
                                    documentNumber = "BIO-${user.userId.takeLast(6).uppercase()}",
                                    issuingAuthority = "Loanzo AI Liveness Detection Engine",
                                    photoUri = user.profilePhotoUri.ifBlank { null },
                                    onApprove = { onVerifyUser(user, true, "Selfie liveness approved") },
                                    onReject = { reason -> onVerifyUser(user, false, reason) }
                                )
                            )
                        },
                        onApprove = { onVerifyUser(user, true, "Full KYC Verified by Master Admin") },
                        onReject = {
                            userForAction = user
                            rejectionReason = "Incomplete or blurry documentation"
                            isRejectDialogVisible = true
                        },
                        onCall = { onCallUser(user.phone) },
                        onSendNotif = { onSendDirectNotif(user) }
                    )
                }
            }
        }
    }

    // Rejection Dialog
    if (isRejectDialogVisible && userForAction != null) {
        AlertDialog(
            onDismissRequest = { isRejectDialogVisible = false },
            title = { Text("KYC Deficiency Notice: ${userForAction?.name}", color = TextNavyDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Specify the reason to notify the member for re-submission:", color = TextSlateMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextNavyDark,
                            unfocusedTextColor = TextNavyDark,
                            focusedBorderColor = Red500,
                            unfocusedBorderColor = BrandIceBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = rejectionReason.ifBlank { "Incomplete or unverified identity documents" }
                        userForAction?.let { onVerifyUser(it, false, finalReason) }
                        isRejectDialogVisible = false
                        rejectionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Issue Notice", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRejectDialogVisible = false }) {
                    Text("Cancel", color = TextSlateMuted)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun AdminUserKycCard(
    user: UserEntity,
    onInspectAadhaar: () -> Unit,
    onInspectPan: () -> Unit,
    onInspectSelfie: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onCall: () -> Unit,
    onSendNotif: () -> Unit = {}
) {
    val isVerified = user.kycStatus == "VERIFIED"
    val isPending = user.kycStatus == "PENDING" || user.kycStatus == "IN_PROGRESS"
    val isRejected = user.kycStatus == "REJECTED"

    val creditTier = when {
        isVerified -> Pair("785 • Prime AAA", Emerald500)
        user.kycStatus == "IN_PROGRESS" -> Pair("710 • Standard", BrandCobalt)
        isRejected -> Pair("580 • High Risk", Red500)
        else -> Pair("660 • Unrated", GoldCoinAmber)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            when {
                isVerified -> EmeraldBorder
                isPending -> GoldCoinBorder
                isRejected -> RedBorder
                else -> BrandIceBorder
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Avatar, Name, Role, and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    LoanzoAvatar(
                        user = user,
                        size = 40.dp,
                        showVerifiedBadge = isVerified,
                        borderColor = if (isVerified) Emerald500 else GoldCoinAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name.ifBlank { "Unnamed Member" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextNavyDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandIceBlue
                            ) {
                                Text(
                                    text = user.role.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandRoyalBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${user.phone} • ${user.email.ifBlank { "No email" }}",
                            fontSize = 11.sp,
                            color = TextSlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isVerified -> EmeraldLight
                        isPending -> GoldCoinCream
                        else -> RedLight
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isVerified -> EmeraldBorder
                            isPending -> GoldCoinBorder
                            else -> RedBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isVerified -> Icons.Default.Verified
                                isPending -> Icons.Default.HourglassTop
                                else -> Icons.Default.Cancel
                            },
                            contentDescription = null,
                            tint = when {
                                isVerified -> Emerald500
                                isPending -> GoldCoinAmber
                                else -> Red500
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                isVerified -> "VERIFIED"
                                isPending -> "PENDING"
                                else -> "REJECTED"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isVerified -> Emerald500
                                isPending -> GoldCoinAmber
                                else -> Red500
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Verification Items Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasPorcelain, RoundedCornerShape(10.dp))
                    .border(1.dp, BrandIceBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Aadhaar Row
                AdminVerificationRow(
                    label = "Aadhaar",
                    value = if (user.aadhaarNumber.isNotBlank()) "XXXX-XXXX-${user.aadhaarNumber.takeLast(4)}" else "Not submitted",
                    isVerified = user.aadhaarVerified,
                    hasDoc = user.aadhaarImageUrl.isNotBlank(),
                    onInspect = onInspectAadhaar
                )

                // PAN Row
                AdminVerificationRow(
                    label = "PAN Card",
                    value = user.panNumber.ifBlank { "Not submitted" },
                    isVerified = user.panVerified,
                    hasDoc = user.panImageUrl.isNotBlank(),
                    onInspect = onInspectPan
                )

                // Bank & UPI Row
                AdminVerificationRow(
                    label = "Bank A/C",
                    value = if (user.bankAccountNumber.isNotBlank()) "${user.bankAccountNumber.takeLast(4)} (${user.bankIfsc})" else "Not linked",
                    isVerified = user.bankVerified,
                    hasDoc = false,
                    onInspect = {}
                )

                // Selfie Row
                AdminVerificationRow(
                    label = "Liveness Selfie",
                    value = if (user.selfieVerified || user.profilePhotoUri.isNotBlank()) "Selfie Stored" else "Pending capture",
                    isVerified = user.selfieVerified,
                    hasDoc = user.profilePhotoUri.isNotBlank(),
                    onInspect = onInspectSelfie
                )

                // Credit Tier Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("CIBIL Rating:", fontSize = 11.sp, color = TextSlateMuted)
                        Text(creditTier.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = creditTier.second)
                    }
                    if (user.agentStatus != "NOT_APPLIED") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandIceBlue
                        ) {
                            Text(
                                text = "AGENT: ${user.agentStatus}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.agentStatus == "APPROVED") Emerald500 else GoldCoinAmber,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Call Member Button
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(36.dp)
                            .background(BrandIceBlue, CircleShape)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Emerald500, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Direct Notification Button
                    IconButton(
                        onClick = onSendNotif,
                        modifier = Modifier
                            .size(36.dp)
                            .background(BrandIceBlue, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Send Direct Message", tint = BrandRoyalBlue, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!isVerified) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Red500),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onApprove,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verify KYC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldLight,
                        border = BorderStroke(1.dp, EmeraldBorder)
                    ) {
                        Text(
                            text = "✓ Certified Platform Member",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald500,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminVerificationRow(
    label: String,
    value: String,
    isVerified: Boolean,
    hasDoc: Boolean,
    onInspect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isVerified) Emerald500 else TextSlateMuted,
                modifier = Modifier.size(13.dp)
            )
            Text(label, fontSize = 11.sp, color = TextSlateMuted)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextNavyDark)
        }

        if (hasDoc) {
            Text(
                text = "Inspect ➔",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BrandRoyalBlue,
                modifier = Modifier
                    .clickable { onInspect() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// --- MODULE 1: AGENTS TAB ---
@Composable
private fun AgentsTab(
    agents: List<AgentApplicationEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit,
    onInspectDoc: (AgentApplicationEntity) -> Unit,
    onSuspend: (String) -> Unit,
    onReactivate: (String) -> Unit,
    onRefreshCloud: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, APPROVED

    val filteredAgents = remember(agents, searchQuery, selectedFilter) {
        agents.filter { agent ->
            val matchesSearch = agent.applicantName.contains(searchQuery, ignoreCase = true) ||
                    agent.applicantPhone.contains(searchQuery, ignoreCase = true) ||
                    agent.permanentAddress.contains(searchQuery, ignoreCase = true) ||
                    agent.operatingCity.contains(searchQuery, ignoreCase = true) ||
                    agent.policeVerificationNumber.contains(searchQuery, ignoreCase = true) ||
                    agent.drivingLicenseNumber.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> agent.status == "PENDING"
                "APPROVED" -> agent.status == "APPROVED"
                "SUSPENDED" -> agent.status == "SUSPENDED"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by Name, City, PCC #, Phone...", color = TextSlateMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = BrandRoyalBlue) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = CanvasPorcelain,
                focusedBorderColor = BrandRoyalBlue,
                unfocusedBorderColor = BrandIceBorder,
                focusedTextColor = TextNavyDark,
                unfocusedTextColor = TextNavyDark
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipItem("All Agents (${agents.size})", selectedFilter == "ALL") { selectedFilter = "ALL" }
            FilterChipItem("Pending Approval (${agents.count { it.status == "PENDING" }})", selectedFilter == "PENDING") { selectedFilter = "PENDING" }
            FilterChipItem("Empaneled Active (${agents.count { it.status == "APPROVED" }})", selectedFilter == "APPROVED") { selectedFilter = "APPROVED" }
            FilterChipItem("Suspended (${agents.count { it.status == "SUSPENDED" }})", selectedFilter == "SUSPENDED") { selectedFilter = "SUSPENDED" }

            if (onRefreshCloud != null) {
                Button(
                    onClick = onRefreshCloud,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync from Cloud", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Cloud", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredAgents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No agent records found", color = TextSlateMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(filteredAgents) { agent ->
                    AgentRosterCard(
                        agent = agent,
                        onApprove = { onApprove(agent.applicationId) },
                        onReject = { reason -> onReject(agent.applicationId, reason) },
                        onInspectDoc = { onInspectDoc(agent) },
                        onSuspend = { onSuspend(agent.applicationId) },
                        onReactivate = { onReactivate(agent.applicationId) },
                        onCall = {
                            try {
                                val cleanPhone = agent.applicantPhone.ifBlank { "+917061559039" }
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Unable to launch phone dialer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsApp = {
                            try {
                                val cleanPhone = agent.applicantPhone.filter { it.isDigit() }.ifBlank { "917061559039" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentRosterCard(
    agent: AgentApplicationEntity,
    onApprove: () -> Unit,
    onReject: (String) -> Unit,
    onInspectDoc: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val isPending = agent.status == "PENDING"
    val isApproved = agent.status == "APPROVED"
    val isSuspended = agent.status == "SUSPENDED"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isApproved) EmeraldBorder else if (isSuspended) RedBorder else BrandIceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isApproved) EmeraldLight else if (isSuspended) RedLight else GoldCoinCream,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Badge, null, tint = if (isApproved) Emerald500 else if (isSuspended) Red500 else GoldCoinAmber, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = agent.applicantName.ifBlank { agent.permanentAddress.take(24) },
                            color = TextNavyDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${agent.operatingCity} • ${agent.serviceRadiusKm} km Radius",
                            color = TextSlateMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isApproved) EmeraldLight else if (isSuspended) RedLight else GoldCoinCream,
                    border = BorderStroke(1.dp, if (isApproved) EmeraldBorder else if (isSuspended) RedBorder else GoldCoinBorder)
                ) {
                    Text(
                        text = agent.status,
                        color = if (isApproved) Emerald500 else if (isSuspended) Red500 else GoldCoinAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CanvasPorcelain)
                    .border(1.dp, BrandIceBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("Police Clearance: ${agent.policeVerificationNumber} (${agent.policeStation})", color = TextNavyDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("Transport: ${agent.vehicleType} ${if (agent.drivingLicenseNumber.isNotBlank()) "• DL: ${agent.drivingLicenseNumber}" else ""}", color = TextSlateMuted, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onInspectDoc,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BrandIceBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.FindInPage, null, tint = BrandRoyalBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect PCC", color = BrandRoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandIceBlue)
                    ) {
                        Icon(Icons.Default.Phone, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandIceBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, null, tint = BrandRoyalBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onReject("PCC requirements or verification checklist failed.") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                        border = BorderStroke(1.dp, RedBorder)
                    ) {
                        Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.6f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Empanel Officer", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            } else if (isApproved) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSuspend,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandSolarOrange),
                    border = BorderStroke(1.dp, BrandSolarOrange)
                ) {
                    Text("Suspend Field Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            } else if (isSuspended) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onReactivate,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reactivate Field Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

// --- MODULE 1: DISPATCH ENGINE & DUAL-AGENT CONSENSUS MATRIX ---
@Composable
private fun DispatchTab(
    unassignedVisits: List<AgentVisitEntity>,
    crossVerificationVisits: List<AgentVisitEntity>,
    allAgents: List<AgentApplicationEntity>,
    onOpenDispatch: (AgentVisitEntity) -> Unit,
    onSanctionLoan: (loanId: String, loanTitle: String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(if (unassignedVisits.isEmpty() && crossVerificationVisits.isNotEmpty()) 1 else 0) }
    var searchQuery by remember { mutableStateOf("") }

    val crossPairs = remember(crossVerificationVisits) {
        crossVerificationVisits.filter { !it.crossVerificationPairId.isNullOrBlank() }
            .groupBy { it.crossVerificationPairId!! }
    }

    val filteredUnassigned = remember(unassignedVisits, searchQuery) {
        if (searchQuery.isBlank()) unassignedVisits
        else unassignedVisits.filter {
            it.borrowerName.contains(searchQuery, ignoreCase = true) ||
            it.borrowerAddress.contains(searchQuery, ignoreCase = true) ||
            it.loanId.contains(searchQuery, ignoreCase = true) ||
            (it.assignedAgentName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val filteredPairs = remember(crossPairs, searchQuery) {
        if (searchQuery.isBlank()) crossPairs
        else crossPairs.filter { (_, pair) ->
            pair.any {
                it.borrowerName.contains(searchQuery, ignoreCase = true) ||
                it.borrowerAddress.contains(searchQuery, ignoreCase = true) ||
                it.loanId.contains(searchQuery, ignoreCase = true) ||
                (it.assignedAgentName?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Daylight Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search visits by borrower, agent, address, loan ID...", color = TextSlateMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = CanvasPorcelain,
                focusedBorderColor = BrandRoyalBlue,
                unfocusedBorderColor = BrandIceBorder,
                focusedTextColor = TextNavyDark,
                unfocusedTextColor = TextNavyDark
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Segmented Sub-Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (subTab == 0) BrandSolarOrange.copy(alpha = 0.1f) else Color.White,
                border = BorderStroke(1.dp, if (subTab == 0) BrandSolarOrange else BrandIceBorder),
                modifier = Modifier.weight(1f).clickable { subTab = 0 }
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Backlog Queue (${filteredUnassigned.size})",
                        color = if (subTab == 0) BrandSolarOrange else TextSlateMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (subTab == 1) BrandRoyalBlue.copy(alpha = 0.1f) else Color.White,
                border = BorderStroke(1.dp, if (subTab == 1) BrandRoyalBlue else BrandIceBorder),
                modifier = Modifier.weight(1.3f).clickable { subTab = 1 }
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Dual-Officer Consensus (${filteredPairs.size})",
                        color = if (subTab == 1) BrandRoyalBlue else TextSlateMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (subTab == 0) {
            if (filteredUnassigned.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, null, tint = Emerald500, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All field visits currently mapped to agents!", color = TextNavyDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Zero backlog in inspection queue", color = TextSlateMuted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredUnassigned) { visit ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BrandIceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BrandSolarOrange.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "UNASSIGNED VISIT",
                                            color = BrandSolarOrange,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "Bounty: ₹${visit.payoutAmount.toInt()}",
                                        color = GoldCoinAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(visit.title, color = TextNavyDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Address: ${visit.targetAddress}", color = TextSlateMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Party: ${visit.borrowerName} (${visit.borrowerPhone})", color = TextSlateMuted, fontSize = 11.sp)

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onOpenDispatch(visit) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.NearMe, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Map & Dispatch Field Agent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Dual-Agent Consensus Matrix Tab
            if (filteredPairs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Security, null, tint = GoldCoinAmber, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Dual-Agent Inspections Dispatched", color = TextNavyDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Dispatch a loan with 2-Officer Blind Swapped Protocol to view consensus matrix.", color = TextSlateMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredPairs.entries.toList(), key = { it.key }) { entry ->
                        val pairId = entry.key
                        val visits = entry.value
                        val v1 = visits.firstOrNull { it.verificationStage == "STAGE_1_PRIMARY" && it.visitType == "BORROWER_VERIFICATION" } ?: visits.firstOrNull()
                        val v2 = visits.firstOrNull { it.verificationStage == "STAGE_1_PRIMARY" && it.visitType == "LENDER_VERIFICATION" } ?: visits.getOrNull(1)

                        val agent1App = allAgents.firstOrNull { it.userId == v1?.agentId }
                        val agent2App = allAgents.firstOrNull { it.userId == v2?.agentId }

                        val stage2Visits = visits.filter { it.verificationStage == "STAGE_2_SWAPPED" }
                        val isStage2Active = stage2Visits.isNotEmpty()
                        val isStage2Completed = isStage2Active && stage2Visits.all { it.status == "COMPLETED" }

                        // Valuation discrepancy calculation
                        val val1 = v1?.appraisedValue ?: v1?.collateralEstimatedValue ?: 0.0
                        val val2 = v2?.appraisedValue ?: v2?.collateralEstimatedValue ?: 0.0
                        val maxV = maxOf(val1, val2)
                        val discrepancyPct = if (maxV > 0.0) kotlin.math.abs(val1 - val2) / maxV * 100.0 else 0.0
                        val isTolerancePass = discrepancyPct <= 15.0
                        val isDualCompleted = (v1?.status == "COMPLETED" && v2?.status == "COMPLETED")

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (isDualCompleted && isTolerancePass) EmeraldBorder else BrandIceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header: Pair Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BrandIceBlue,
                                            border = BorderStroke(1.dp, BrandIceBorder)
                                        ) {
                                            Text(
                                                text = "BLIND RECIPROCAL AUDIT",
                                                color = BrandRoyalBlue,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(pairId, color = TextSlateMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Loan: ${v1?.loanId ?: ""}",
                                        color = TextNavyDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = v1?.title ?: "Cross-Verification Inspection Pair",
                                    color = TextNavyDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Asset: ${v1?.collateralItemName ?: "Registered Collateral & KYC"}",
                                    color = TextSlateMuted,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Side-by-Side Officer Reports
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Officer 1 Report Box
                                    OfficerReportCard(
                                        modifier = Modifier.weight(1f),
                                        officerRole = "Officer 1 (Borrower Initial)",
                                        officerName = agent1App?.applicantName ?: v1?.assignedAgentName ?: "Officer 1 (${v1?.agentId?.takeLast(4) ?: ""})",
                                        visit = v1,
                                        appraisalValue = val1
                                    )

                                    // Officer 2 Report Box
                                    OfficerReportCard(
                                        modifier = Modifier.weight(1f),
                                        officerRole = "Officer 2 (Lender Initial)",
                                        officerName = agent2App?.applicantName ?: v2?.assignedAgentName ?: "Officer 2 (${v2?.agentId?.takeLast(4) ?: ""})",
                                        visit = v2,
                                        appraisalValue = val2
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Stage 2 Swapped Progress Indicator
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CanvasPorcelain,
                                    border = BorderStroke(1.dp, BrandIceBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isStage2Completed) Icons.Default.CheckCircle else Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = if (isStage2Completed) Emerald500 else GoldCoinAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isStage2Completed) "Stage 2 Reciprocal Swap: Verified ✓" else if (isStage2Active) "Stage 2 Swapped Cross-Audit In-Progress..." else "Stage 2 Swap: Pending Stage 1 Completion",
                                                color = TextNavyDark,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = if (isStage2Completed) "2/2 Done" else if (isStage2Active) "1/2 Done" else "0/2",
                                            color = TextSlateMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Discrepancy & Consensus Analysis
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isTolerancePass && isDualCompleted) EmeraldLight else CanvasPorcelain,
                                    border = BorderStroke(1.dp, if (isTolerancePass && isDualCompleted) EmeraldBorder else BrandSolarOrange),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isTolerancePass && isDualCompleted) "DUAL-OFFICER CONSENSUS: CERTIFIED ✓" else "INDEPENDENT EVALUATION IN PROGRESS",
                                                color = if (isTolerancePass && isDualCompleted) Emerald500 else BrandSolarOrange,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Valuation Delta: ${String.format("%.1f", discrepancyPct)}% (Tolerance Limit: 15.0%)",
                                                color = TextNavyDark,
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (isTolerancePass && isDualCompleted) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Emerald500
                                            ) {
                                                Text(
                                                    text = "PASS ✓",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Sanction & Disburse Button
                                Button(
                                    onClick = { onSanctionLoan(v1?.loanId ?: "", v1?.title ?: "") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sanction Loan & Release Escrow post Dual-Audit",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficerReportCard(
    modifier: Modifier = Modifier,
    officerRole: String,
    officerName: String,
    visit: AgentVisitEntity?,
    appraisalValue: Double
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = CanvasPorcelain,
        border = BorderStroke(1.dp, BrandIceBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(officerRole, color = BrandRoyalBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(officerName, color = TextNavyDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(6.dp))

            val isDone = visit?.status == "COMPLETED"
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isDone) EmeraldLight else GoldCoinCream,
                border = BorderStroke(1.dp, if (isDone) EmeraldBorder else GoldCoinBorder)
            ) {
                Text(
                    text = if (isDone) "Report: Attested ✓" else "Status: ${visit?.status ?: "PENDING"}",
                    color = if (isDone) Emerald500 else GoldCoinAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Appraised: ₹${appraisalValue.toInt()}", color = TextNavyDark, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = "Rec: ${visit?.officerRecommendation?.replace("RECOMMEND_", "") ?: "PENDING"}",
                color = if (visit?.officerRecommendation == "RECOMMEND_APPROVAL") Emerald500 else TextSlateMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (visit?.agentRemarks?.isNotBlank() == true) {
                Text(
                    text = "\"${visit.agentRemarks}\"",
                    color = TextSlateMuted,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// --- MODULE 4: VAULT TAB ---
@Composable
private fun VaultTab(
    vaultItems: List<CollateralVaultEntity>,
    onAssignLocker: (CollateralVaultEntity) -> Unit,
    onRelease: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredVault = remember(vaultItems, searchQuery) {
        if (searchQuery.isBlank()) vaultItems
        else vaultItems.filter {
            it.borrowerName.contains(searchQuery, ignoreCase = true) ||
            it.assetDescription.contains(searchQuery, ignoreCase = true) ||
            it.assetType.contains(searchQuery, ignoreCase = true) ||
            it.lockerNumber.contains(searchQuery, ignoreCase = true) ||
            it.tamperSealNumber.contains(searchQuery, ignoreCase = true) ||
            it.vaultFacilityName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by owner, asset, locker #, seal #...", color = TextSlateMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = CanvasPorcelain,
                    focusedBorderColor = BrandRoyalBlue,
                    unfocusedBorderColor = BrandIceBorder,
                    focusedTextColor = TextNavyDark,
                    unfocusedTextColor = TextNavyDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filteredVault.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No collateral records found matching query", color = TextSlateMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredVault) { item ->
                val isSecured = item.custodyStatus == "SECURED_IN_VAULT"
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, if (isSecured) EmeraldBorder else BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldCoinCream,
                                border = BorderStroke(1.dp, GoldCoinBorder)
                            ) {
                                Text(
                                    text = item.assetType,
                                    color = GoldCoinAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "Est: ₹${item.estimatedValue.toInt()}",
                                color = Emerald500,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.assetDescription, color = TextNavyDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Borrower: ${item.borrowerName} • Facility: ${item.vaultFacilityName}", color = TextSlateMuted, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CanvasPorcelain)
                                .border(BorderStroke(1.dp, BrandIceBorder), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Locker: ${item.lockerNumber}", color = GoldCoinAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Seal: ${if (item.tamperSealNumber.isNotBlank()) item.tamperSealNumber else "Pending"}", color = TextNavyDark, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAssignLocker(item) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSecured) "Re-Assign Locker" else "Seal & Locker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (item.custodyStatus == "READY_FOR_RELEASE") {
                                Button(
                                    onClick = { onRelease(item.loanId) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White)
                                ) {
                                    Text("Release to Borrower", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- MODULE 5: COMPLAINTS TAB ---
@Composable
private fun ComplaintsTab(
    complaints: List<ComplaintEntity>,
    onScheduleHearing: (ComplaintEntity) -> Unit,
    onResolve: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onAuthorizeDeviceTransfer: (complaintId: String, usernameOrId: String, newDeviceId: String, newDeviceModel: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredComplaints = remember(complaints, searchQuery) {
        if (searchQuery.isBlank()) complaints
        else complaints.filter {
            it.complainantId.contains(searchQuery, ignoreCase = true) ||
            it.complainantName.contains(searchQuery, ignoreCase = true) ||
            (it.targetPartyId?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.targetPartyName?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.loanId?.contains(searchQuery, ignoreCase = true) == true) ||
            it.subject.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.status.contains(searchQuery, ignoreCase = true)
        }
    }

    val devIdRegex = remember { Regex("ID: ([^\\)\\s]+)") }
    val devModelRegex = remember { Regex("New Device: ([^\\(]+)") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search grievances by user, subject, issue...", color = TextSlateMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = CanvasPorcelain,
                    focusedBorderColor = BrandRoyalBlue,
                    unfocusedBorderColor = BrandIceBorder,
                    focusedTextColor = TextNavyDark,
                    unfocusedTextColor = TextNavyDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filteredComplaints.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Zero open complaints matching search query", color = TextSlateMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredComplaints) { cmp ->
                val isOpen = cmp.status == "OPEN" || cmp.status == "INVESTIGATING"
                val isRecovery = cmp.category == "UNREGISTERED_DEVICE_RECOVERY"
                val extractedDevId = if (cmp.evidenceUris.contains("||")) cmp.evidenceUris.split("||").firstOrNull() ?: "" else devIdRegex.find(cmp.description)?.groupValues?.getOrNull(1) ?: ""
                val extractedDevModel = if (cmp.evidenceUris.contains("||")) cmp.evidenceUris.split("||").getOrNull(1) ?: "" else devModelRegex.find(cmp.description)?.groupValues?.getOrNull(1)?.trim() ?: "Authorized New Phone"

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, if (isRecovery) BrandIceBorder else if (cmp.priority == "CRITICAL_LEGAL") RedBorder else BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isRecovery) BrandIceBlue else if (cmp.priority == "CRITICAL_LEGAL") RedLight else GoldCoinCream,
                                border = BorderStroke(1.dp, if (isRecovery) BrandIceBorder else if (cmp.priority == "CRITICAL_LEGAL") RedBorder else GoldCoinBorder)
                            ) {
                                Text(
                                    text = if (isRecovery) "DEVICE RECOVERY GRIEVANCE" else "${cmp.priority} • ${cmp.complainantRole}",
                                    color = if (isRecovery) BrandRoyalBlue else if (cmp.priority == "CRITICAL_LEGAL") Red500 else GoldCoinAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Text(cmp.status, color = TextSlateMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cmp.subject, color = TextNavyDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(cmp.description, color = TextSlateMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("From: ${cmp.complainantName} (${cmp.complainantPhone})", color = TextNavyDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)

                        if (isRecovery) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Hardware Device Transfer Request", fontWeight = FontWeight.Bold, color = BrandRoyalBlue, fontSize = 12.sp)
                                    Text("New Device Model: $extractedDevModel", fontSize = 11.sp, color = TextNavyDark, fontWeight = FontWeight.SemiBold)
                                    Text("Hardware UID: $extractedDevId", fontSize = 10.sp, color = TextSlateMedium)
                                }
                            }
                        }

                        if (isOpen) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cmp.complainantPhone}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BrandIceBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = BrandIceBlue)
                                ) {
                                    Icon(Icons.Default.Phone, null, tint = Emerald500, modifier = Modifier.size(14.dp))
                                }

                                if (isRecovery) {
                                    Button(
                                        onClick = {
                                            onAuthorizeDeviceTransfer(cmp.complaintId, cmp.complainantId, extractedDevId, extractedDevModel)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.4f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.SecurityUpdateGood, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Authorize Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onDismiss(cmp.complaintId) },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BrandIceBorder),
                                        modifier = Modifier.weight(1.0f)
                                    ) {
                                        Text("Dismiss", fontSize = 11.sp, color = TextSlateMuted)
                                    }
                                } else {
                                    Button(
                                        onClick = { onScheduleHearing(cmp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mediate Hearing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onResolve(cmp.complaintId) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White)
                                    ) {
                                        Text("Resolve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- MODULE 6: NOC CLEARANCE TAB ---
@Composable
private fun NocTab(
    nocs: List<NocCertificateEntity>,
    vaultItems: List<CollateralVaultEntity>,
    onGenerateNoc: (String, String, String, String, Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredNocs = remember(nocs, searchQuery) {
        if (searchQuery.isBlank()) nocs
        else nocs.filter {
            it.borrowerName.contains(searchQuery, ignoreCase = true) ||
            it.borrowerPan.contains(searchQuery, ignoreCase = true) ||
            it.loanId.contains(searchQuery, ignoreCase = true) ||
            it.nocId.contains(searchQuery, ignoreCase = true) ||
            it.collateralReleasedDesc.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search NOCs by borrower, PAN, loan ID...", color = TextSlateMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = CanvasPorcelain,
                    focusedBorderColor = BrandRoyalBlue,
                    unfocusedBorderColor = BrandIceBorder,
                    focusedTextColor = TextNavyDark,
                    unfocusedTextColor = TextNavyDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Digitally Signed NOC Certificates Registry (${filteredNocs.size})", color = TextNavyDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        items(filteredNocs) { noc ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(noc.nocId, color = Emerald500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldLight,
                            border = BorderStroke(1.dp, EmeraldBorder)
                        ) {
                            Text("LEGAL CLEARANCE", color = Emerald500, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("Borrower: ${noc.borrowerName} • Loan: ${noc.loanId}", color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Collateral Released: ${noc.collateralReleasedDesc}", color = TextSlateMuted, fontSize = 11.sp)
                    Text("Digital Signature: ${noc.digitalSignatureHash}", color = TextSlateMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// --- MODULE 7: HEARINGS TAB ---
@Composable
private fun HearingsTab(
    meetings: List<MediationMeetingEntity>,
    onScheduleNew: () -> Unit,
    onMarkCompleted: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredMeetings = remember(meetings, searchQuery) {
        if (searchQuery.isBlank()) meetings
        else meetings.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.agenda.contains(searchQuery, ignoreCase = true) ||
            it.meetingType.contains(searchQuery, ignoreCase = true) ||
            it.status.contains(searchQuery, ignoreCase = true) ||
            it.meetingLinkOrLocation.contains(searchQuery, ignoreCase = true) ||
            (it.borrowerName?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.lenderName?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.agentName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onScheduleNew,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Schedule New Arbitration / Hearing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search hearings by participant, case title...", color = TextSlateMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = CanvasPorcelain,
                focusedBorderColor = BrandRoyalBlue,
                unfocusedBorderColor = BrandIceBorder,
                focusedTextColor = TextNavyDark,
                unfocusedTextColor = TextNavyDark
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredMeetings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hearings found matching query", color = TextSlateMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(filteredMeetings) { m ->
                    val isScheduled = m.status == "SCHEDULED"
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isScheduled) EmeraldBorder else BrandIceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (m.meetingType == "GOOGLE_MEET") EmeraldLight else GoldCoinCream,
                                    border = BorderStroke(1.dp, if (m.meetingType == "GOOGLE_MEET") EmeraldBorder else GoldCoinBorder)
                                ) {
                                    Text(
                                        text = if (m.meetingType == "GOOGLE_MEET") "GOOGLE MEET" else "IN-PERSON VAULT",
                                        color = if (m.meetingType == "GOOGLE_MEET") Emerald500 else GoldCoinAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Text(m.scheduledTimeSlotStr, color = GoldCoinAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(m.title, color = TextNavyDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Parties: ${m.borrowerName} & ${m.lenderName}", color = TextSlateMuted, fontSize = 11.sp)
                            Text("Agenda: ${m.agenda}", color = TextSlateMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                            if (isScheduled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (m.meetingType == "GOOGLE_MEET") {
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(m.meetingLinkOrLocation))
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.3f),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                                        ) {
                                            Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Launch Google Meet", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                    Button(
                                        onClick = { onMarkCompleted(m.meetingId) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = CanvasPorcelain, contentColor = TextNavyDark),
                                        border = BorderStroke(1.dp, BrandIceBorder)
                                    ) {
                                        Text("Mark Concluded", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) BrandRoyalBlue else Color.White,
        border = BorderStroke(1.dp, if (selected) BrandRoyalBlue else BrandIceBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else TextSlateMedium,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun Double.formatDecimal(decimals: Int): String {
    return String.format(Locale.getDefault(), "%.${decimals}f", this)
}
