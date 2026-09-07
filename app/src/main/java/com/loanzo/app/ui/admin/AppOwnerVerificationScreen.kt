package com.loanzo.app.ui.admin

import com.loanzo.app.ui.components.ExecutiveHeroCard
import com.loanzo.app.ui.theme.Navy700
import com.loanzo.app.ui.theme.Navy800
import com.loanzo.app.ui.theme.Navy900
import com.loanzo.app.ui.theme.SurfaceDarkElevated
import com.loanzo.app.ui.theme.GoldCoinBright
import com.loanzo.app.ui.theme.GoldCoinRich
import com.loanzo.app.ui.theme.GoldCoinAmber
import com.loanzo.app.ui.theme.Gray300
import com.loanzo.app.ui.theme.Gray400
import com.loanzo.app.ui.theme.Emerald400
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.Emerald500


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
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
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

    // Observe Admin Data Streams
    val complaints by adminRepository.allComplaints.collectAsState(initial = emptyList())
    val meetings by adminRepository.allMeetings.collectAsState(initial = emptyList())
    val vaultItems by adminRepository.allVaultItems.collectAsState(initial = emptyList())
    val nocs by adminRepository.allNocs.collectAsState(initial = emptyList())
    val allVisits by adminRepository.allVisits.collectAsState(initial = emptyList())
    val unassignedVisits = remember(allVisits) { allVisits.filter { it.agentId == "UNASSIGNED" } }

    // Seed sample data on first entry
    LaunchedEffect(Unit) {
        adminRepository.seedSampleAdminDataIfEmpty()
    }

    // Active Tab state:
    // 0: 👥 Agents, 1: 📑 Documents & KYC, 2: 🗺️ Dispatch, 3: 💎 Vault, 4: ⚖️ Complaints, 5: 📜 NOCs, 6: 📅 Hearings, 7: 🔑 SMS Tokens
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var manualTokenInput by remember { mutableStateOf("") }

    // Dialog & Sheet States
    var inspectingDoc by remember { mutableStateOf<DocumentInspectionData?>(null) }
    var dispatchingVisit by remember { mutableStateOf<AgentVisitEntity?>(null) }
    var schedulingMediation by remember { mutableStateOf<ComplaintEntity?>(null) }
    var assigningLockerItem by remember { mutableStateOf<CollateralVaultEntity?>(null) }

    // Counts for Badges
    val pendingUsersCount = remember(allUsers) { allUsers.count { it.kycStatus == "PENDING" || it.kycStatus == "IN_PROGRESS" } }
    val pendingAgentsCount = remember(agentApplications) { agentApplications.count { it.status == "PENDING" } }
    val openComplaintsCount = remember(complaints) { complaints.count { it.status == "OPEN" || it.status == "INVESTIGATING" } }
    val unassignedCount = remember(unassignedVisits) { unassignedVisits.size }
    val vaultTotalValue = remember(vaultItems) { vaultItems.sumOf { it.estimatedValue } }
    val pendingTokensCount = remember(verifications) { verifications.count { it.status == "PENDING" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Loanzo Logo",
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Master Admin Command Center",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "👑 App Owner (@satyam0810 • +91 7061559039)",
                                fontSize = 11.sp,
                                color = Gold500,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A1627),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF070E1A)
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

            // Smart Horizontal Operational Desks Bar (9 Desks with Badges & Auto-scroll)
            val desks = listOf(
                AdminDeskItem(0, "Users & KYC", Icons.Default.People, allUsers.size, pendingUsersCount, if (pendingUsersCount > 0) Gold500 else Emerald400),
                AdminDeskItem(1, "Agents", Icons.Default.Groups, agentApplications.size, pendingAgentsCount, Gold500),
                AdminDeskItem(2, "Agent KYC", Icons.Default.AssignmentInd, pendingAgentsCount, pendingAgentsCount, if (pendingAgentsCount > 0) Gold500 else Emerald400),
                AdminDeskItem(3, "Dispatch", Icons.Default.NearMe, unassignedCount, unassignedCount, if (unassignedCount > 0) Color(0xFFF97316) else Emerald400),
                AdminDeskItem(4, "Vault", Icons.Default.Diamond, vaultItems.size),
                AdminDeskItem(5, "Grievances", Icons.Default.Gavel, complaints.size, openComplaintsCount, if (openComplaintsCount > 0) Color(0xFFEF4444) else Emerald400),
                AdminDeskItem(6, "Legal NOC", Icons.Default.Description, nocs.size),
                AdminDeskItem(7, "Hearings", Icons.Default.Event, meetings.size),
                AdminDeskItem(8, "Tokens", Icons.Default.Key, verifications.size, pendingTokensCount, if (pendingTokensCount > 0) Gold500 else Emerald400)
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
                    0 -> UsersKycTab(
                        users = allUsers,
                        onInspectDoc = { data -> inspectingDoc = data },
                        onVerifyUser = onVerifyUserKyc,
                        onCallUser = { phone ->
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    1 -> AgentsTab(
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
                        onReactivate = { agentId -> scope.launch(Dispatchers.IO) { adminRepository.reactivateAgent(agentId) } }
                    )
                    2 -> DocumentKycTab(
                        agentApplications = agentApplications,
                        onInspect = { data -> inspectingDoc = data },
                        onApproveAgent = onApproveAgentApplication,
                        onRejectAgent = onRejectAgentApplication
                    )
                    3 -> DispatchTab(
                        unassignedVisits = unassignedVisits,
                        onOpenDispatch = { visit -> dispatchingVisit = visit }
                    )
                    4 -> VaultTab(
                        vaultItems = vaultItems,
                        onAssignLocker = { item -> assigningLockerItem = item },
                        onRelease = { loanId -> scope.launch(Dispatchers.IO) { adminRepository.releaseCollateral(loanId) } }
                    )
                    5 -> ComplaintsTab(
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
                    6 -> NocTab(
                        nocs = nocs,
                        vaultItems = vaultItems,
                        onGenerateNoc = { loanId, borrower, pan, lender, amount, repaid, desc ->
                            scope.launch(Dispatchers.IO) {
                                adminRepository.generateNoc(
                                    loanId = loanId,
                                    borrowerId = "USR-BRW",
                                    borrowerName = borrower,
                                    borrowerPan = pan,
                                    lenderId = "USR-LND",
                                    lenderName = lender,
                                    principalAmount = amount,
                                    totalRepaidAmount = repaid,
                                    collateralDesc = desc
                                )
                            }
                        }
                    )
                    7 -> HearingsTab(
                        meetings = meetings,
                        onScheduleNew = { schedulingMediation = ComplaintEntity(
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
                        ) },
                        onMarkCompleted = { id -> scope.launch(Dispatchers.IO) { adminRepository.updateMeetingStatus(id, "COMPLETED", "Concluded successfully") } }
                    )
                    8 -> SmsInterceptorTab(
                        verifications = verifications,
                        manualTokenInput = manualTokenInput,
                        onManualTokenChange = { manualTokenInput = it },
                        onApprove = onApproveVerification,
                        onManualVerify = onManualVerify
                    )
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
            DispatchAgentSheet(
                visit = visit,
                availableAgents = agentApplications.filter { it.status == "APPROVED" },
                onDispatch = { agentId, payout ->
                    scope.launch(Dispatchers.IO) {
                        adminRepository.assignAgentToVisit(visit.visitId, agentId, payout)
                    }
                    dispatchingVisit = null
                    Toast.makeText(context, "Visit mapped to field agent successfully!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { dispatchingVisit = null }
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
    val alertColor: Color = Color(0xFFEF4444)
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
        color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF0F1E36),
        border = BorderStroke(
            1.2.dp,
            if (isSelected) Gold500 else Color(0xFF1E3250)
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
                tint = if (isSelected) Gold500 else Gray400,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = desk.title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Gray300,
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
                        color = if (desk.alertColor == Gold500) Navy900 else Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            } else if (desk.count > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "${desk.count}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray400,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
            accentColor = if (pendingKycUsers > 0) Gold500 else Emerald400
        )

        // Vault Escrow Chip
        ExecutiveMetricChip(
            label = "VAULT ESCROW",
            value = "₹${(vaultValue / 100000).formatDecimal(1)}L",
            icon = Icons.Default.Diamond,
            accentColor = GoldCoinBright
        )

        // Agents Chip
        ExecutiveMetricChip(
            label = "AGENTS",
            value = "$activeAgents On-Duty",
            icon = Icons.Default.Shield,
            accentColor = Emerald400
        )

        // Dispatch Chip
        ExecutiveMetricChip(
            label = "DISPATCH",
            value = if (unassignedVisits > 0) "$unassignedVisits Pending" else "All Assigned",
            icon = Icons.Default.NearMe,
            accentColor = if (unassignedVisits > 0) Color(0xFFF97316) else Emerald400
        )

        // Ombudsman Chip
        ExecutiveMetricChip(
            label = "OMBUDSMAN",
            value = if (openComplaints > 0) "$openComplaints Open" else "Zero Open",
            icon = Icons.Default.Gavel,
            accentColor = if (openComplaints > 0) Color(0xFFEF4444) else Emerald400
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
        color = Color(0xFF0F1E36),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
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
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
            }
            Column {
                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Gray400, letterSpacing = 0.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
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
    onInspectDoc: (DocumentInspectionData) -> Unit,
    onVerifyUser: (user: UserEntity, approve: Boolean, remarks: String) -> Unit,
    onCallUser: (phone: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) }
    var userForAction by remember { mutableStateOf<UserEntity?>(null) }
    var isRejectDialogVisible by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

    val pendingCount = remember(users) { users.count { it.kycStatus == "PENDING" || it.kycStatus == "IN_PROGRESS" } }
    val verifiedCount = remember(users) { users.count { it.kycStatus == "VERIFIED" } }
    val agentCount = remember(users) { users.count { it.role == "AGENT" || it.agentStatus == "APPROVED" || it.agentStatus == "PENDING" } }
    val lenderCount = remember(users) { users.count { it.role == "LENDER" } }
    val borrowerCount = remember(users) { users.count { it.role == "BORROWER" || it.role == "MEMBER" } }

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
                1 -> u.kycStatus == "PENDING" || u.kycStatus == "IN_PROGRESS"
                2 -> u.kycStatus == "VERIFIED"
                3 -> u.role == "AGENT" || u.agentStatus == "APPROVED" || u.agentStatus == "PENDING"
                4 -> u.role == "LENDER"
                5 -> u.role == "BORROWER" || u.role == "MEMBER"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, phone, PAN, Aadhaar...", color = Gray400, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gold500, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Gray400, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0D1B2E),
                unfocusedContainerColor = Color(0xFF0D1B2E),
                focusedBorderColor = Gold500,
                unfocusedBorderColor = Color(0xFF1E3250),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
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
            FilterChipItem("⚠️ Pending (${pendingCount})", selectedFilter == 1) { selectedFilter = 1 }
            FilterChipItem("✅ Verified (${verifiedCount})", selectedFilter == 2) { selectedFilter = 2 }
            FilterChipItem("🛡️ Agents (${agentCount})", selectedFilter == 3) { selectedFilter = 3 }
            FilterChipItem("💰 Lenders (${lenderCount})", selectedFilter == 4) { selectedFilter = 4 }
            FilterChipItem("📋 Borrowers (${borrowerCount})", selectedFilter == 5) { selectedFilter = 5 }
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
                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Gray400, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No users matched your filter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Try a different search keyword or filter tab", color = Gray400, fontSize = 12.sp)
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
                        onCall = { onCallUser(user.phone) }
                    )
                }
            }
        }
    }

    // Rejection Dialog
    if (isRejectDialogVisible && userForAction != null) {
        AlertDialog(
            onDismissRequest = { isRejectDialogVisible = false },
            title = { Text("KYC Deficiency Notice: ${userForAction?.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Specify the reason to notify the member for re-submission:", color = Gray300, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userForAction?.let { onVerifyUser(it, false, rejectionReason) }
                        isRejectDialogVisible = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Issue Notice", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRejectDialogVisible = false }) {
                    Text("Cancel", color = Gray400)
                }
            },
            containerColor = Color(0xFF0F1E36)
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
    onCall: () -> Unit
) {
    val isVerified = user.kycStatus == "VERIFIED"
    val isPending = user.kycStatus == "PENDING" || user.kycStatus == "IN_PROGRESS"
    val isRejected = user.kycStatus == "REJECTED"

    val creditTier = when {
        isVerified -> Pair("785 • Prime AAA", Emerald400)
        user.kycStatus == "IN_PROGRESS" -> Pair("710 • Standard", Color(0xFF38BDF8))
        isRejected -> Pair("580 • High Risk", Color(0xFFEF4444))
        else -> Pair("660 • Unrated", Gold500)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F1E36),
        border = BorderStroke(
            1.2.dp,
            when {
                isVerified -> Emerald400.copy(alpha = 0.4f)
                isPending -> Gold500.copy(alpha = 0.5f)
                isRejected -> Color(0xFFEF4444).copy(alpha = 0.4f)
                else -> Color(0xFF1E3250)
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isVerified) Emerald400.copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f))
                            .border(1.5.dp, if (isVerified) Emerald400 else Gold500, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase().ifBlank { "U" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isVerified) Emerald400 else Gold500
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name.ifBlank { "Unnamed Member" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = user.role.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${user.phone} • ${user.email.ifBlank { "No email" }}",
                            fontSize = 11.sp,
                            color = Gray400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isVerified -> Emerald400.copy(alpha = 0.15f)
                        isPending -> Gold500.copy(alpha = 0.15f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isVerified -> Emerald400.copy(alpha = 0.4f)
                            isPending -> Gold500.copy(alpha = 0.4f)
                            else -> Color(0xFFEF4444).copy(alpha = 0.4f)
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
                                isVerified -> Emerald400
                                isPending -> Gold500
                                else -> Color(0xFFEF4444)
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
                                isVerified -> Emerald400
                                isPending -> Gold500
                                else -> Color(0xFFEF4444)
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
                    .background(Color(0xFF0A1627), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF1E3250), RoundedCornerShape(10.dp))
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
                        Text("CIBIL Rating:", fontSize = 11.sp, color = Gray400)
                        Text(creditTier.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = creditTier.second)
                    }
                    if (user.agentStatus != "NOT_APPLIED") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "AGENT: ${user.agentStatus}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.agentStatus == "APPROVED") Emerald400 else Gold500,
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
                // Call Member Button
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Emerald400, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!isVerified) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onReject,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onApprove,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verify KYC", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald400.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "✓ Certified Platform Member",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald400,
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
                tint = if (isVerified) Emerald400 else Gray400,
                modifier = Modifier.size(13.dp)
            )
            Text(label, fontSize = 11.sp, color = Gray400)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }

        if (hasDoc) {
            Text(
                text = "Inspect ➔",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Gold500,
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
    onReactivate: (String) -> Unit
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
            placeholder = { Text("Search by Name, City, PCC #, Phone...", color = Gray500, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Gray400) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold500,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipItem("All Agents (${agents.size})", selectedFilter == "ALL") { selectedFilter = "ALL" }
            FilterChipItem("Pending (${agents.count { it.status == "PENDING" }})", selectedFilter == "PENDING") { selectedFilter = "PENDING" }
            FilterChipItem("Empaneled (${agents.count { it.status == "APPROVED" }})", selectedFilter == "APPROVED") { selectedFilter = "APPROVED" }
            FilterChipItem("Suspended (${agents.count { it.status == "SUSPENDED" }})", selectedFilter == "SUSPENDED") { selectedFilter = "SUSPENDED" }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredAgents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No agent records found", color = Color(0xFF94A3B8), fontSize = 13.sp)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
        border = BorderStroke(1.dp, if (isApproved) Emerald400.copy(alpha = 0.4f) else if (isSuspended) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFF1E3250)),
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
                        color = if (isApproved) Emerald400.copy(alpha = 0.2f) else if (isSuspended) Color(0xFFEF4444).copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Badge, null, tint = if (isApproved) Emerald400 else if (isSuspended) Color(0xFFEF4444) else Gold500, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = agent.applicantName.ifBlank { agent.permanentAddress.take(24) },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${agent.operatingCity} • ${agent.serviceRadiusKm} km Radius",
                            color = Gray400,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isApproved) Emerald400.copy(alpha = 0.2f) else if (isSuspended) Color(0xFFEF4444).copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = agent.status,
                        color = if (isApproved) Emerald400 else if (isSuspended) Color(0xFFEF4444) else Gold500,
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
                    .background(Color(0xFF162544))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("Police Clearance: ${agent.policeVerificationNumber} (${agent.policeStation})", color = Color.White, fontSize = 11.sp)
                Text("Transport: ${agent.vehicleType} ${if (agent.drivingLicenseNumber.isNotBlank()) "• DL: ${agent.drivingLicenseNumber}" else ""}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
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
                    border = BorderStroke(1.dp, Gold500),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.FindInPage, null, tint = Gold500, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect PCC", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Emerald500.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Phone, null, tint = Emerald400, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Emerald500.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, null, tint = Emerald400, modifier = Modifier.size(16.dp))
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.6f),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF97316)),
                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.6f))
                ) {
                    Text("Suspend Field Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            } else if (isSuspended) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onReactivate,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reactivate Field Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

// --- MODULE 2: KYC & DOCUMENTS TAB ---
@Composable
private fun DocumentKycTab(
    agentApplications: List<AgentApplicationEntity>,
    onInspect: (DocumentInspectionData) -> Unit,
    onApproveAgent: (String) -> Unit,
    onRejectAgent: (String, String) -> Unit
) {
    val sampleUserKyc = remember {
        listOf(
            DocumentInspectionData(
                title = "Aadhaar Card (UIDAI Verified)",
                category = "USER_KYC",
                subjectName = "Rahul Verma",
                subjectPhone = "+919876543210",
                documentNumber = "XXXX-XXXX-8921",
                issuingAuthority = "UIDAI Govt of India",
                photoUri = null,
                onApprove = {},
                onReject = {}
            ),
            DocumentInspectionData(
                title = "Income Tax Return (ITR-V)",
                category = "USER_KYC",
                subjectName = "Deepak Chawla",
                subjectPhone = "+919650112233",
                documentNumber = "ITR-AY25-26-88129",
                issuingAuthority = "Income Tax Dept",
                photoUri = null,
                onApprove = {},
                onReject = {}
            )
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text("User Borrowers & Lenders KYC Queue", color = Gold500, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        items(sampleUserKyc) { doc ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = BorderStroke(1.dp, Color(0xFF1E3250)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${doc.subjectName} • ${doc.subjectPhone}", color = Gray400, fontSize = 11.sp)
                        Text("Doc #: ${doc.documentNumber}", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onInspect(doc) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Inspect & Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Agent Police Clearance & DL Queue", color = Gold500, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        items(agentApplications) { agent ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = BorderStroke(1.dp, Color(0xFF1E3250)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PCC Clearance & Driving License", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Station: ${agent.policeStation} • City: ${agent.operatingCity}", color = Gray400, fontSize = 11.sp)
                        Text("PCC #: ${agent.policeVerificationNumber}", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onInspect(
                                DocumentInspectionData(
                                    title = "Police Clearance Certificate (PCC)",
                                    category = "AGENT_EMPANELMENT",
                                    subjectName = agent.applicantName,
                                    subjectPhone = agent.applicantPhone,
                                    documentNumber = agent.policeVerificationNumber,
                                    issuingAuthority = agent.policeStation,
                                    photoUri = agent.policeDocUri,
                                    onApprove = { onApproveAgent(agent.applicationId) },
                                    onReject = { reason -> onRejectAgent(agent.applicationId, reason) }
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Audit PCC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- MODULE 3: DISPATCH ENGINE TAB ---
@Composable
private fun DispatchTab(
    unassignedVisits: List<AgentVisitEntity>,
    onOpenDispatch: (AgentVisitEntity) -> Unit
) {
    if (unassignedVisits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DoneAll, null, tint = Emerald400, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("All field visits currently mapped to agents!", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Zero backlog in inspection queue", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(unassignedVisits) { visit ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.5f)),
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
                                color = Color(0xFFF97316).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "🚨 UNASSIGNED VISIT",
                                    color = Color(0xFFF97316),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "Bounty: ₹${visit.payoutAmount.toInt()}",
                                color = Gold500,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(visit.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Address: ${visit.targetAddress}", color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Party: ${visit.borrowerName} (${visit.borrowerPhone})", color = Gray400, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onOpenDispatch(visit) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
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
}

// --- MODULE 4: VAULT TAB ---
@Composable
private fun VaultTab(
    vaultItems: List<CollateralVaultEntity>,
    onAssignLocker: (CollateralVaultEntity) -> Unit,
    onRelease: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(vaultItems) { item ->
            val isSecured = item.custodyStatus == "SECURED_IN_VAULT"
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = BorderStroke(1.dp, if (isSecured) Gold500.copy(alpha = 0.4f) else Color(0xFF1E3250)),
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
                            color = Gold500.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = item.assetType,
                                color = Gold500,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Est: ₹${item.estimatedValue.toInt()}",
                            color = Emerald400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.assetDescription, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Borrower: ${item.borrowerName} • Facility: ${item.vaultFacilityName}", color = Color(0xFFCBD5E1), fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF162544))
                            .border(BorderStroke(0.8.dp, Color(0xFF1E3250)), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Locker: ${item.lockerNumber}", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Seal: ${if (item.tamperSealNumber.isNotBlank()) item.tamperSealNumber else "Pending"}", color = Color.White, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAssignLocker(item) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSecured) Color(0xFF162544) else Gold500, contentColor = if (isSecured) Color.White else Navy900)
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
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
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
    if (complaints.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Zero open complaints on record", color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
    } else {
        val devIdRegex = remember { Regex("ID: ([^\\)\\s]+)") }
        val devModelRegex = remember { Regex("New Device: ([^\\(]+)") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(complaints) { cmp ->
                val isOpen = cmp.status == "OPEN" || cmp.status == "INVESTIGATING"
                val isRecovery = cmp.category == "UNREGISTERED_DEVICE_RECOVERY"
                val extractedDevId = if (cmp.evidenceUris.contains("||")) cmp.evidenceUris.split("||").firstOrNull() ?: "" else devIdRegex.find(cmp.description)?.groupValues?.getOrNull(1) ?: ""
                val extractedDevModel = if (cmp.evidenceUris.contains("||")) cmp.evidenceUris.split("||").getOrNull(1) ?: "" else devModelRegex.find(cmp.description)?.groupValues?.getOrNull(1)?.trim() ?: "Authorized New Phone"

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    border = BorderStroke(1.dp, if (isRecovery) Color(0xFF8B5CF6) else if (cmp.priority == "CRITICAL_LEGAL") Color(0xFFEF4444) else Color(0xFF1E3250)),
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
                                color = if (isRecovery) Color(0xFF7C3AED).copy(alpha = 0.2f) else if (cmp.priority == "CRITICAL_LEGAL") Color(0xFFEF4444).copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isRecovery) "📱 DEVICE RECOVERY GRIEVANCE" else "${cmp.priority} • ${cmp.complainantRole}",
                                    color = if (isRecovery) Color(0xFF8B5CF6) else if (cmp.priority == "CRITICAL_LEGAL") Color(0xFFEF4444) else Gold500,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Text(cmp.status, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cmp.subject, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(cmp.description, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("From: ${cmp.complainantName} (${cmp.complainantPhone})", color = Gold500, fontSize = 11.sp)

                        if (isRecovery) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E1B4B),
                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Hardware Device Transfer Request", fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE), fontSize = 12.sp)
                                    Text("New Device Model: $extractedDevModel", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Hardware UID: $extractedDevId", fontSize = 10.sp, color = Color(0xFFA5B4FC))
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
                                    border = BorderStroke(1.dp, Emerald400)
                                ) {
                                    Icon(Icons.Default.Phone, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                                }

                                if (isRecovery) {
                                    Button(
                                        onClick = {
                                            onAuthorizeDeviceTransfer(cmp.complaintId, cmp.complainantId, extractedDevId, extractedDevModel)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.4f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
                                    ) {
                                        Icon(Icons.Default.SecurityUpdateGood, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Authorize Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onDismiss(cmp.complaintId) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.0f)
                                    ) {
                                        Text("Dismiss", fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { onScheduleHearing(cmp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900)
                                    ) {
                                        Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mediate Hearing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onResolve(cmp.complaintId) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
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
    val eligibleLoan = remember {
        object {
            val loanId = "LOAN-84920"
            val borrower = "Rahul Verma"
            val pan = "ABCDE1234F"
            val lender = "Kapil Dev Sharma"
            val amount = 100000.0
            val repaid = 106000.0
            val collateral = "22K Hallmark Gold Coins (20g)"
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = BorderStroke(1.dp, Gold500),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Emerald500.copy(alpha = 0.2f)
                        ) {
                            Text("100% PAID • ZERO DUES", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text(eligibleLoan.loanId, color = Gold500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Clearance for ${eligibleLoan.borrower} (PAN: ${eligibleLoan.pan})", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Lender: ${eligibleLoan.lender} • Total Repaid: ₹${eligibleLoan.repaid.toInt()}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Text("Pledged Collateral: ${eligibleLoan.collateral}", color = Color(0xFFCBD5E1), fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onGenerateNoc(
                                eligibleLoan.loanId,
                                eligibleLoan.borrower,
                                eligibleLoan.pan,
                                eligibleLoan.lender,
                                eligibleLoan.amount,
                                eligibleLoan.repaid,
                                eligibleLoan.collateral
                            )
                            Toast.makeText(context, "Cryptographic NOC issued & collateral released!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Attest & Issue Official Legal NOC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Digitally Signed NOC Certificates Registry", color = Gold500, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        items(nocs) { noc ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(noc.nocId, color = Emerald400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(4.dp), color = Emerald500.copy(alpha = 0.2f)) {
                            Text("LEGAL CLEARANCE", color = Emerald400, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    Text("Borrower: ${noc.borrowerName} • Loan: ${noc.loanId}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Collateral Released: ${noc.collateralReleasedDesc}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Text("Digital Signature: ${noc.digitalSignatureHash}", color = Gold500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onScheduleNew,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Schedule New Arbitration / Hearing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (meetings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hearings scheduled", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(meetings) { m ->
                    val isScheduled = m.status == "SCHEDULED"
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                        border = BorderStroke(1.dp, if (isScheduled) Emerald400.copy(alpha = 0.4f) else Color(0xFF1E3250)),
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
                                    color = if (m.meetingType == "GOOGLE_MEET") Emerald500.copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (m.meetingType == "GOOGLE_MEET") "📹 GOOGLE MEET" else "🏢 IN-PERSON VAULT",
                                        color = if (m.meetingType == "GOOGLE_MEET") Emerald400 else Gold500,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Text(m.scheduledTimeSlotStr, color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(m.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Parties: ${m.borrowerName} & ${m.lenderName}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            Text("Agenda: ${m.agenda}", color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

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
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
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
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162544), contentColor = Color.White)
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

// --- MODULE 8: SMS & AUTH INTERCEPTOR TAB ---
@Composable
private fun SmsInterceptorTab(
    verifications: List<VerificationEntity>,
    manualTokenInput: String,
    onManualTokenChange: (String) -> Unit,
    onApprove: (String, String) -> Unit,
    onManualVerify: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
            border = BorderStroke(1.dp, Color(0xFF1E3250)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Manual Verification Token Overrule", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualTokenInput,
                        onValueChange = onManualTokenChange,
                        placeholder = { Text("Token or Phone #...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold500,
                            unfocusedBorderColor = Color(0xFF1E3250),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF162544),
                            unfocusedContainerColor = Color(0xFF162544)
                        )
                    )
                    Button(
                        onClick = {
                            if (manualTokenInput.isNotBlank()) {
                                onManualVerify(manualTokenInput)
                                onManualTokenChange("")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
                    ) {
                        Text("Verify", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (verifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No verification tokens received yet", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(verifications) { item ->
                    val isVerified = item.status == "VERIFIED"
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                        border = BorderStroke(1.dp, if (isVerified) Emerald400.copy(alpha = 0.3f) else Gold500.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isVerified) Emerald400.copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (item.channel.contains("WHATSAPP", ignoreCase = true)) Icons.AutoMirrored.Filled.Message else Icons.Default.Sms,
                                        null,
                                        tint = if (isVerified) Emerald400 else Gold500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.phone, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(item.status, color = if (isVerified) Emerald400 else Gold500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Token: ${item.token}", color = Gold500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (!isVerified) {
                                Button(
                                    onClick = { onApprove(item.token, item.phone) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
                                ) {
                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
        color = if (selected) Gold500 else Color(0xFF0F1E36),
        border = BorderStroke(1.dp, if (selected) Gold500 else Color(0xFF1E3250)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (selected) Navy900 else Color(0xFFCBD5E1),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun Double.formatDecimal(decimals: Int): String {
    return String.format(Locale.getDefault(), "%.${decimals}f", this)
}
