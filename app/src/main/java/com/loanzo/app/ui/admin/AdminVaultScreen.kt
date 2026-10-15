package com.loanzo.app.ui.admin

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
import com.loanzo.app.data.entity.CollateralVaultEntity
import com.loanzo.app.data.entity.NocCertificateEntity
import com.loanzo.app.ui.components.SegmentedCapsuleTab
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.toInrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVaultScreen(
    initialTab: Int = 0,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminRepository = LocalAdminRepository.current

    val allVaultItems by adminRepository.allVaultItems.collectAsState(initial = emptyList())
    val allNocs by adminRepository.allNocs.collectAsState(initial = emptyList())

    val totalVaultValue = remember(allVaultItems) { allVaultItems.sumOf { it.estimatedValue } }
    val pendingAllocationCount = remember(allVaultItems) { allVaultItems.count { it.lockerNumber == "PENDING_ALLOCATION" } }
    val storedLockersCount = remember(allVaultItems) { allVaultItems.count { it.lockerNumber != "PENDING_ALLOCATION" } }

    var selectedSubTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var assigningLockerItem by remember { mutableStateOf<CollateralVaultEntity?>(null) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminRepository.startRealtimeAdminCloudSync(scope)
    }

    val filteredVaultItems = remember(allVaultItems, searchQuery) {
        if (searchQuery.isBlank()) allVaultItems else {
            allVaultItems.filter {
                it.loanId.contains(searchQuery, ignoreCase = true) ||
                it.borrowerName.contains(searchQuery, ignoreCase = true) ||
                it.assetDescription.contains(searchQuery, ignoreCase = true) ||
                it.lockerNumber.contains(searchQuery, ignoreCase = true) ||
                it.barcodeTag.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredNocs = remember(allNocs, searchQuery) {
        if (searchQuery.isBlank()) allNocs else {
            allNocs.filter {
                it.loanId.contains(searchQuery, ignoreCase = true) ||
                it.borrowerName.contains(searchQuery, ignoreCase = true) ||
                it.lenderName.contains(searchQuery, ignoreCase = true) ||
                it.nocId.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Collateral Vault & Legal NOC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Institutional Custody & Digital Clearance Register",
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
                                        adminRepository.startRealtimeAdminCloudSync(scope)
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Vault custody synced", Toast.LENGTH_SHORT).show()
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
                                contentDescription = "Sync",
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
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("TOTAL CUSTODY VALUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                        Text(totalVaultValue.toInrString(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("LOCKERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                        Text("$storedLockersCount", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextNavyDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (pendingAllocationCount > 0) Color(0xFFF97316).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("PENDING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (pendingAllocationCount > 0) Color(0xFFC2410C) else TextSlateMuted)
                        Text("$pendingAllocationCount", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (pendingAllocationCount > 0) Color(0xFFC2410C) else TextNavyDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("LEGAL NOCS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        Text("${allNocs.size}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                    }
                }
            }

            // Sub-Tab Switcher
            SegmentedCapsuleTab(
                tabs = listOf(
                    "Collateral Lockers (${allVaultItems.size})",
                    "Legal NOC Registry (${allNocs.size})"
                ),
                selectedIndex = selectedSubTab,
                onTabSelected = { selectedSubTab = it },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                activeColor = BrandRoyalBlue,
                activeTextColor = Color.White,
                inactiveTextColor = TextSlateMuted,
                containerColor = Color.White
            )

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
                                Text(
                                    if (selectedSubTab == 0) "Search lockers by borrower, barcode, facility..." else "Search NOCs by borrower, lender, ID...",
                                    fontSize = 13.sp,
                                    color = TextSlateMuted
                                )
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

            // Sub-Tab Content
            if (selectedSubTab == 0) {
                // Collateral Lockers List
                if (filteredVaultItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No collateral lockers found", fontWeight = FontWeight.Bold, color = TextNavyDark)
                            Text("Active pledge items allocated to bank lockers appear here", fontSize = 12.sp, color = TextSlateMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredVaultItems, key = { it.vaultItemId }) { item ->
                            AdminVaultItemCard(
                                item = item,
                                onAssignLocker = { assigningLockerItem = item },
                                onReleaseCollateral = {
                                    scope.launch(Dispatchers.IO) {
                                        adminRepository.releaseCollateral(item.loanId)
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Collateral released for Loan ${item.loanId}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Legal NOC Registry List
                if (filteredNocs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Legal NOC certificates found", fontWeight = FontWeight.Bold, color = TextNavyDark)
                            Text("Fully repaid loan discharge certificates will be logged here", fontSize = 12.sp, color = TextSlateMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNocs, key = { it.nocId }) { noc ->
                            AdminNocCard(noc = noc)
                        }
                    }
                }
            }
        }
    }

    // Assign Locker Dialog
    assigningLockerItem?.let { item ->
        AssignVaultLockerDialog(
            item = item,
            onAssign = { lockerNumber, barcodeTag, sealNumber ->
                scope.launch(Dispatchers.IO) {
                    adminRepository.assignVaultLocker(
                        vaultItemId = item.vaultItemId,
                        lockerNumber = lockerNumber,
                        barcodeTag = barcodeTag,
                        sealNumber = sealNumber
                    )
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Locker allocated: $lockerNumber", Toast.LENGTH_SHORT).show()
                    }
                }
                assigningLockerItem = null
            },
            onDismiss = { assigningLockerItem = null }
        )
    }
}

@Composable
private fun AdminVaultItemCard(
    item: CollateralVaultEntity,
    onAssignLocker: () -> Unit,
    onReleaseCollateral: () -> Unit
) {
    val isPendingAllocation = item.lockerNumber == "PENDING_ALLOCATION"
    val isReleased = item.custodyStatus == "RELEASED"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isPendingAllocation) Color(0xFFF97316).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Collateral Category + Estimated Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(0.5.dp, BrandRoyalBlue.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = item.assetType.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRoyalBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Valuation: ${item.estimatedValue.toInrString()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Borrower & Description
            Text(
                text = item.assetDescription,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextNavyDark
            )
            Text(
                text = "Borrower: ${item.borrowerName} • Loan ID: ${item.loanId}",
                fontSize = 12.sp,
                color = TextSlateMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Facility & Locker Info Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("FACILITY LOCATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                            Text(item.vaultFacilityName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LOCKER / SAFE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                            Text(
                                text = if (isPendingAllocation) "Pending Allocation" else item.lockerNumber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPendingAllocation) Color(0xFFC2410C) else Emerald500
                            )
                        }
                    }

                    if (item.barcodeTag.isNotBlank() || item.tamperSealNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (item.barcodeTag.isNotBlank()) {
                                Text("Tag: ${item.barcodeTag}", fontSize = 10.sp, color = TextSlateMuted)
                            }
                            if (item.tamperSealNumber.isNotBlank()) {
                                Text("Seal: ${item.tamperSealNumber}", fontSize = 10.sp, color = TextSlateMuted)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAssignLocker,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPendingAllocation) Color(0xFFF97316) else BrandRoyalBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPendingAllocation) "Assign Locker" else "Re-assign Safe",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (!isReleased) {
                    OutlinedButton(
                        onClick = onReleaseCollateral,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextNavyDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Release", fontSize = 11.sp, color = TextNavyDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminNocCard(
    noc: NocCertificateEntity
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Certificate ID + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOC #${noc.nocId.take(12)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavyDark
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Emerald500.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "LEGAL DISCHARGE ISSUED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Borrower: ${noc.borrowerName} (${noc.borrowerPan})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextNavyDark
            )
            Text(
                text = "Lender: ${noc.lenderName} • Loan ID: ${noc.loanId}",
                fontSize = 12.sp,
                color = TextSlateMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("SANCTIONED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                        Text(noc.principalAmount.toInrString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOTAL REPAID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                        Text(noc.totalRepaidAmount.toInrString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                    }
                }
            }

            if (noc.digitalSignatureHash.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Hash: ${noc.digitalSignatureHash.take(24)}...",
                    fontSize = 9.sp,
                    color = TextSlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
