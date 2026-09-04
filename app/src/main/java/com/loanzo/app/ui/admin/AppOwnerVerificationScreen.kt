package com.loanzo.app.ui.admin

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
    verifications: List<VerificationEntity>,
    agentApplications: List<AgentApplicationEntity> = emptyList(),
    onApproveVerification: (token: String, phone: String) -> Unit,
    onManualVerify: (String) -> Unit,
    onApproveAgentApplication: (String) -> Unit = {},
    onRejectAgentApplication: (String, String) -> Unit = { _, _ -> },
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
    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var manualTokenInput by remember { mutableStateOf("") }

    // Dialog & Sheet States
    var inspectingDoc by remember { mutableStateOf<DocumentInspectionData?>(null) }
    var dispatchingVisit by remember { mutableStateOf<AgentVisitEntity?>(null) }
    var schedulingMediation by remember { mutableStateOf<ComplaintEntity?>(null) }
    var assigningLockerItem by remember { mutableStateOf<CollateralVaultEntity?>(null) }

    // Counts for Badges
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
                                    color = MaterialTheme.colorScheme.onSurface,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Institutional Top KPI Ribbon
            AdminKpiRibbon(
                activeAgents = agentApplications.count { it.status == "APPROVED" },
                pendingDocs = pendingAgentsCount,
                unassignedVisits = unassignedCount,
                vaultValue = vaultTotalValue,
                openComplaints = openComplaintsCount,
                totalNocs = nocs.size
            )

            // Horizontally Scrollable Operational Tabs
            val tabs = listOf(
                "👥 Agents (${agentApplications.size})",
                "📑 KYC & Docs ($pendingAgentsCount)",
                "🗺️ Dispatch ($unassignedCount)",
                "💎 Vault (${vaultItems.size})",
                "⚖️ Complaints ($openComplaintsCount)",
                "📜 Legal NOC (${nocs.size})",
                "📅 Hearings (${meetings.size})",
                "🔑 SMS Tokens ($pendingTokensCount)"
            )

            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Gold500,
                edgePadding = 12.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (activeTab == index) Gold500 else Gray400,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Tab Content Router
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                when (activeTab) {
                    0 -> AgentsTab(
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
                    1 -> DocumentKycTab(
                        agentApplications = agentApplications,
                        onInspect = { data -> inspectingDoc = data }
                    )
                    2 -> DispatchTab(
                        unassignedVisits = unassignedVisits,
                        onOpenDispatch = { visit -> dispatchingVisit = visit }
                    )
                    3 -> VaultTab(
                        vaultItems = vaultItems,
                        onAssignLocker = { item -> assigningLockerItem = item },
                        onRelease = { loanId -> scope.launch(Dispatchers.IO) { adminRepository.releaseCollateral(loanId) } }
                    )
                    4 -> ComplaintsTab(
                        complaints = complaints,
                        onScheduleHearing = { cmp -> schedulingMediation = cmp },
                        onResolve = { id -> scope.launch(Dispatchers.IO) { adminRepository.resolveComplaint(id, "Resolved by Master Admin") } },
                        onDismiss = { id -> scope.launch(Dispatchers.IO) { adminRepository.dismissComplaint(id, "Dismissed post verification") } }
                    )
                    5 -> NocTab(
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
                    6 -> HearingsTab(
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
                    7 -> SmsInterceptorTab(
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
@Composable
private fun AdminKpiRibbon(
    activeAgents: Int,
    pendingDocs: Int,
    unassignedVisits: Int,
    vaultValue: Double,
    openComplaints: Int,
    totalNocs: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KpiChip("🛡️ Agents", "$activeAgents Active", Emerald400)
        KpiChip("📑 KYC Queue", "$pendingDocs Pending", if (pendingDocs > 0) Gold500 else Emerald400)
        KpiChip("🗺️ Dispatch", "$unassignedVisits Needed", if (unassignedVisits > 0) Color(0xFFF97316) else Emerald400)
        KpiChip("💎 Vault Assets", "₹${(vaultValue / 100000).formatDecimal(1)}L", Gold500)
        KpiChip("⚖️ Grievances", "$openComplaints Open", if (openComplaints > 0) Color(0xFFEF4444) else Emerald400)
        KpiChip("📜 NOCs Issued", "$totalNocs Clear", Emerald400)
    }
}

@Composable
private fun KpiChip(label: String, value: String, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Gray400, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = value, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            val matchesSearch = agent.permanentAddress.contains(searchQuery, ignoreCase = true) ||
                    agent.operatingCity.contains(searchQuery, ignoreCase = true) ||
                    agent.policeVerificationNumber.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> agent.status == "PENDING"
                "APPROVED" -> agent.status == "APPROVED"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by City, Address, PCC #...", color = Gray500, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Gray400) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold500,
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipItem("All Agents (${agents.size})", selectedFilter == "ALL") { selectedFilter = "ALL" }
            FilterChipItem("Pending (${agents.count { it.status == "PENDING" }})", selectedFilter == "PENDING") { selectedFilter = "PENDING" }
            FilterChipItem("Empaneled (${agents.count { it.status == "APPROVED" }})", selectedFilter == "APPROVED") { selectedFilter = "APPROVED" }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredAgents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No agent records found", color = Gray400, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(filteredAgents) { agent ->
                    AgentRosterCard(
                        agent = agent,
                        onApprove = { onApprove(agent.applicationId) },
                        onReject = { reason -> onReject(agent.applicationId, reason) },
                        onInspectDoc = { onInspectDoc(agent) },
                        onCall = {
                            val cleanPhone = agent.applicantPhone.ifBlank { "+917061559039" }
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                            context.startActivity(intent)
                        },
                        onWhatsApp = {
                            val cleanPhone = agent.applicantPhone.filter { it.isDigit() }.ifBlank { "917061559039" }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone"))
                            context.startActivity(intent)
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
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val isPending = agent.status == "PENDING"
    val isApproved = agent.status == "APPROVED"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, if (isApproved) Emerald400.copy(alpha = 0.4f) else Color(0xFF1E293B)),
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
                        color = if (isApproved) Emerald400.copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Badge, null, tint = if (isApproved) Emerald400 else Gold500, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = agent.applicantName.ifBlank { agent.permanentAddress.take(24) },
                            color = Color.White,
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
                    color = if (isApproved) Emerald400.copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = agent.status,
                        color = if (isApproved) Emerald400 else Gold500,
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
                    .background(Color(0xFF141D2E))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("Police Clearance: ${agent.policeVerificationNumber} (${agent.policeStation})", color = Gray300, fontSize = 11.sp)
                Text("Transport: ${agent.vehicleType} ${if (agent.drivingLicenseNumber.isNotBlank()) "• DL: ${agent.drivingLicenseNumber}" else ""}", color = Gray300, fontSize = 11.sp)
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
            }
        }
    }
}

// --- MODULE 2: KYC & DOCUMENTS TAB ---
@Composable
private fun DocumentKycTab(
    agentApplications: List<AgentApplicationEntity>,
    onInspect: (DocumentInspectionData) -> Unit
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PCC Clearance & Driving License", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                    onApprove = {},
                                    onReject = {}
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
                Text("All field visits currently mapped to agents!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Zero backlog in inspection queue", color = Gray400, fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(unassignedVisits) { visit ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
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
                        Text(visit.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Address: ${visit.targetAddress}", color = Gray300, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, if (isSecured) Gold500.copy(alpha = 0.4f) else Color(0xFF1E293B)),
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
                    Text("Borrower: ${item.borrowerName} • Facility: ${item.vaultFacilityName}", color = Gray400, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141D2E))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Locker: ${item.lockerNumber}", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Seal: ${if (item.tamperSealNumber.isNotBlank()) item.tamperSealNumber else "Pending"}", color = Gray300, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAssignLocker(item) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSecured) Color(0xFF1E293B) else Gold500, contentColor = if (isSecured) Color.White else Navy900)
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
    onDismiss: (String) -> Unit
) {
    val context = LocalContext.current
    if (complaints.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Zero open complaints on record", color = Gray400, fontSize = 13.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(complaints) { cmp ->
                val isOpen = cmp.status == "OPEN" || cmp.status == "INVESTIGATING"
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, if (cmp.priority == "CRITICAL_LEGAL") Color(0xFFEF4444) else Color(0xFF1E293B)),
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
                                color = if (cmp.priority == "CRITICAL_LEGAL") Color(0xFFEF4444).copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${cmp.priority} • ${cmp.complainantRole}",
                                    color = if (cmp.priority == "CRITICAL_LEGAL") Color(0xFFEF4444) else Gold500,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Text(cmp.status, color = Gray400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cmp.subject, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(cmp.description, color = Gray300, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("From: ${cmp.complainantName} (${cmp.complainantPhone})", color = Gold500, fontSize = 11.sp)

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2E)),
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
                    Text("Clearance for ${eligibleLoan.borrower} (PAN: ${eligibleLoan.pan})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Lender: ${eligibleLoan.lender} • Total Repaid: ₹${eligibleLoan.repaid.toInt()}", color = Gray300, fontSize = 11.sp)
                    Text("Pledged Collateral: ${eligibleLoan.collateral}", color = Gray400, fontSize = 11.sp)

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
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
                    Text("Collateral Released: ${noc.collateralReleasedDesc}", color = Gray300, fontSize = 11.sp)
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
                Text("No hearings scheduled", color = Gray400, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(meetings) { m ->
                    val isScheduled = m.status == "SCHEDULED"
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, if (isScheduled) Emerald400.copy(alpha = 0.4f) else Color(0xFF1E293B)),
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
                            Text(m.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Parties: ${m.borrowerName} & ${m.lenderName}", color = Gray300, fontSize = 11.sp)
                            Text("Agenda: ${m.agenda}", color = Gray400, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

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
                                            Text("Launch Google Meet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Button(
                                        onClick = { onMarkCompleted(m.meetingId) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
                                    ) {
                                        Text("Mark Concluded", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Manual Verification Token Overrule", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualTokenInput,
                        onValueChange = onManualTokenChange,
                        placeholder = { Text("Token or Phone #...", color = Gray500, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold500,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                        Text("Verify", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (verifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No verification tokens received yet", color = Gray400, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(verifications) { item ->
                    val isVerified = item.status == "VERIFIED"
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
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
                            Column(modifier = Modifier.weight(1f)) {
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
                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        color = if (selected) Gold500 else Color(0xFF1E293B),
        border = BorderStroke(1.dp, if (selected) Gold500 else Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (selected) Navy900 else Gray300,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun Double.formatDecimal(decimals: Int): String {
    return String.format(Locale.getDefault(), "%.${decimals}f", this)
}
