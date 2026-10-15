package com.loanzo.app.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AdminRequestEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.LocalUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminKycDeskScreen(
    initialFilter: Int = 0,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminRepository = LocalAdminRepository.current
    val userRepository = LocalUserRepository.current

    val allUsers by userRepository.getAllUsers().collectAsState(initial = emptyList())
    val allAdminRequests by adminRepository.allAdminRequests.collectAsState(initial = emptyList())
    val pendingAdminRequests = remember(allAdminRequests) { allAdminRequests.filter { it.status == "PENDING" } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(initialFilter) }
    var inspectingDoc by remember { mutableStateOf<DocumentInspectionData?>(null) }
    var userForRejectAction by remember { mutableStateOf<UserEntity?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var isSyncingCloud by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminRepository.startRealtimeAdminCloudSync(scope)
        scope.launch(Dispatchers.IO) {
            adminRepository.refreshUsersFromCloud()
        }
    }

    val pendingKycCount = remember(allUsers) {
        allUsers.count {
            it.kycStatus.equals("PENDING", ignoreCase = true) ||
            it.kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
            it.kycStatus.equals("IN_REVIEW", ignoreCase = true) ||
            it.kycStatus.equals("UNDER_REVIEW", ignoreCase = true) ||
            it.kycStatus.equals("RE_KYC_REQUESTED", ignoreCase = true)
        }
    }
    val verifiedCount = remember(allUsers) { allUsers.count { it.kycStatus.equals("VERIFIED", ignoreCase = true) } }
    val elevationCount = pendingAdminRequests.size

    val filteredUsers = remember(allUsers, searchQuery, selectedFilter, pendingAdminRequests) {
        allUsers.filter { u ->
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
                3 -> pendingAdminRequests.any { it.userId == u.userId }
                4 -> u.role.equals("AGENT", ignoreCase = true) || u.agentStatus.equals("APPROVED", ignoreCase = true)
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KYC & Identity Desk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Institutional User Attestation & Role Elevation",
                            fontSize = 11.sp,
                            color = TextSlateMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextNavyDark)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isSyncingCloud) {
                                isSyncingCloud = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        adminRepository.refreshUsersFromCloud()
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Cloud sync complete", Toast.LENGTH_SHORT).show()
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
        },
        containerColor = CanvasPorcelain
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("TOTAL USERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                        Text("${allUsers.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextNavyDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (pendingKycCount > 0) Gold500.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("PENDING KYC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (pendingKycCount > 0) Color(0xFFB45309) else TextSlateMuted)
                        Text("$pendingKycCount", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (pendingKycCount > 0) Color(0xFFB45309) else TextNavyDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("VERIFIED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        Text("$verifiedCount", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (elevationCount > 0) Red500.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ELEVATIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (elevationCount > 0) Red500 else TextSlateMuted)
                        Text("$elevationCount", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (elevationCount > 0) Red500 else TextNavyDark)
                    }
                }
            }

            // Search Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isBlank()) {
                                Text("Search by name, phone, PAN, Aadhaar...", fontSize = 13.sp, color = TextSlateMuted)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Filter Chips
            val filterOptions = listOf(
                "All (${allUsers.size})",
                "Pending ($pendingKycCount)",
                "Verified ($verifiedCount)",
                "Elevations ($elevationCount)",
                "Agents (${allUsers.count { it.role.equals("AGENT", ignoreCase = true) }})"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEachIndexed { index, title ->
                    val isSelected = selectedFilter == index
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BrandRoyalBlue else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { selectedFilter = index }
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else TextNavyDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // User Cards List
            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No users found matching query", fontWeight = FontWeight.Bold, color = TextNavyDark)
                        Text("Adjust your search or filter criteria", fontSize = 12.sp, color = TextSlateMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it.userId }) { user ->
                        val userElevationReq = pendingAdminRequests.firstOrNull { it.userId == user.userId }
                        AdminKycUserCard(
                            user = user,
                            elevationRequest = userElevationReq,
                            onApproveElevation = { reqId ->
                                scope.launch(Dispatchers.IO) {
                                    adminRepository.approveAdminRequest(reqId)
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "User elevated to Admin role successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onRejectElevation = { reqId, reason ->
                                scope.launch(Dispatchers.IO) {
                                    adminRepository.rejectAdminRequest(reqId, reason)
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Role elevation rejected", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onInspectDocument = { docData -> inspectingDoc = docData },
                            onApproveKyc = {
                                scope.launch(Dispatchers.IO) {
                                    adminRepository.verifyFullUserKyc(user.userId, true, "Verified by Admin")
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "KYC verified for ${user.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onRejectKyc = {
                                userForRejectAction = user
                                rejectionReason = ""
                            },
                            onCall = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }

    // Document Inspection Dialog
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
            },
            onReject = { reason ->
                doc.onReject(reason)
                inspectingDoc = null
            },
            onDismiss = { inspectingDoc = null }
        )
    }

    // Reject KYC Dialog
    if (userForRejectAction != null) {
        AlertDialog(
            onDismissRequest = { userForRejectAction = null },
            title = {
                Text("Reject KYC Submission", fontWeight = FontWeight.Bold, color = TextNavyDark)
            },
            text = {
                Column {
                    Text(
                        "Please provide deficiency reasons for ${userForRejectAction?.name}. The user will be notified to re-submit clear documents.",
                        fontSize = 13.sp,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Deficiency Reason") },
                        placeholder = { Text("e.g., Unclear document picture, PAN name mismatch") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = userForRejectAction ?: return@Button
                        val reason = rejectionReason.ifBlank { "Documents unreadable or incomplete" }
                        scope.launch(Dispatchers.IO) {
                            adminRepository.verifyFullUserKyc(user.userId, false, reason)
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                Toast.makeText(context, "KYC rejected with notice sent", Toast.LENGTH_SHORT).show()
                            }
                        }
                        userForRejectAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Issue Rejection", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userForRejectAction = null }) {
                    Text("Cancel", color = TextSlateMuted)
                }
            }
        )
    }
}

@Composable
private fun AdminKycUserCard(
    user: UserEntity,
    elevationRequest: AdminRequestEntity?,
    onApproveElevation: (String) -> Unit,
    onRejectElevation: (String, String) -> Unit,
    onInspectDocument: (DocumentInspectionData) -> Unit,
    onApproveKyc: () -> Unit,
    onRejectKyc: () -> Unit,
    onCall: () -> Unit
) {
    val isKycVerified = user.kycStatus.equals("VERIFIED", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (elevationRequest != null) Gold500.copy(alpha = 0.7f) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Elevation Request Banner if pending
            if (elevationRequest != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldCoinCream,
                    border = BorderStroke(1.dp, GoldCoinBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ADMIN ROLE ELEVATION REQUEST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "Reason: ${elevationRequest.reason.ifBlank { "Requested by user" }}",
                                fontSize = 11.sp,
                                color = TextNavyDark
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onRejectElevation(elevationRequest.requestId, "Rejected by admin") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Reject", fontSize = 11.sp, color = Red500)
                            }
                            Button(
                                onClick = { onApproveElevation(elevationRequest.requestId) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Approve", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // User Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoanzoAvatar(
                    user = user,
                    size = 44.dp,
                    showVerifiedBadge = isKycVerified,
                    borderWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name.ifBlank { "Unnamed User" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isKycVerified) Emerald500.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isKycVerified) "VERIFIED" else "PENDING",
                                color = if (isKycVerified) Emerald500 else Color(0xFFB45309),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${user.username} • ${user.phone.ifBlank { "No Phone" }}",
                        fontSize = 11.sp,
                        color = TextSlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    val userTrustScore = if (isKycVerified) 85 else 50
                    Text(
                        text = "Trust: $userTrustScore",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Document Attestation Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocStatusBadge(
                    label = "Aadhaar",
                    isVerified = user.aadhaarVerified,
                    docNumber = user.aadhaarNumber.takeLast(4).let { if (it.isNotBlank()) "...$it" else "" },
                    onClick = {
                        onInspectDocument(
                            DocumentInspectionData(
                                title = "Aadhaar Card",
                                category = "USER_KYC",
                                subjectName = user.name,
                                subjectPhone = user.phone,
                                documentNumber = user.aadhaarNumber,
                                issuingAuthority = "UIDAI National Identity",
                                photoUri = user.aadhaarImageUrl,
                                onApprove = { onApproveKyc() },
                                onReject = { onRejectKyc() }
                            )
                        )
                    }
                )
                DocStatusBadge(
                    label = "PAN",
                    isVerified = user.panVerified,
                    docNumber = user.panNumber.let { if (it.isNotBlank()) it.take(3) + "..." else "" },
                    onClick = {
                        onInspectDocument(
                            DocumentInspectionData(
                                title = "Permanent Account Number (PAN)",
                                category = "USER_KYC",
                                subjectName = user.name,
                                subjectPhone = user.phone,
                                documentNumber = user.panNumber,
                                issuingAuthority = "Income Tax Dept",
                                photoUri = user.panImageUrl,
                                onApprove = { onApproveKyc() },
                                onReject = { onRejectKyc() }
                            )
                        )
                    }
                )
                DocStatusBadge(
                    label = "Bank",
                    isVerified = user.bankVerified,
                    docNumber = user.bankAccountNumber.takeLast(4).let { if (it.isNotBlank()) "...$it" else "" },
                    onClick = null
                )
                DocStatusBadge(
                    label = "Phone",
                    isVerified = user.phoneVerified,
                    docNumber = "",
                    onClick = null
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCall,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.8f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextNavyDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp, color = TextNavyDark)
                }

                OutlinedButton(
                    onClick = {
                        onInspectDocument(
                            DocumentInspectionData(
                                title = "Identity Dossier",
                                category = "USER_KYC",
                                subjectName = user.name,
                                subjectPhone = user.phone,
                                documentNumber = user.panNumber.ifBlank { user.aadhaarNumber },
                                issuingAuthority = "Govt of India",
                                photoUri = if (user.panImageUrl.isNotBlank()) user.panImageUrl else user.aadhaarImageUrl,
                                onApprove = { onApproveKyc() },
                                onReject = { onRejectKyc() }
                            )
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextNavyDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect", fontSize = 11.sp, color = TextNavyDark)
                }

                if (!isKycVerified) {
                    OutlinedButton(
                        onClick = onRejectKyc,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.9f),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        border = BorderStroke(1.dp, Red500.copy(alpha = 0.5f))
                    ) {
                        Text("Reject", fontSize = 11.sp, color = Red500, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApproveKyc,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text("Verify KYC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocStatusBadge(
    label: String,
    isVerified: Boolean,
    docNumber: String,
    onClick: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isVerified) Emerald500.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
        border = BorderStroke(0.5.dp, if (isVerified) Emerald500.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Pending,
                contentDescription = null,
                tint = if (isVerified) Emerald500 else TextSlateMuted,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isVerified) Emerald500 else TextNavyDark
                )
                if (docNumber.isNotBlank()) {
                    Text(
                        text = docNumber,
                        fontSize = 8.sp,
                        color = TextSlateMuted
                    )
                }
            }
        }
    }
}
