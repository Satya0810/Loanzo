package com.loanzo.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUserDirectoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val adminRepository = LocalAdminRepository.current

    val allUsers by adminRepository.allUsers.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        adminRepository.startRealtimeAdminCloudSync(scope)
    }

    val unverifiedUsersCount = remember(allUsers) {
        allUsers.count { it.kycStatus in listOf("PENDING", "IN_PROGRESS", "SUBMITTED", "RE_KYC_REQUESTED") || (!it.aadhaarVerified || !it.panVerified || !it.bankVerified || !it.selfieVerified) }
    }
    val appealsCount = remember(allUsers) {
        allUsers.count { it.hasPendingBanAppeal && it.banAppealStatus == "PENDING" }
    }
    val bannedCount = remember(allUsers) {
        allUsers.count { it.isBanned }
    }
    val verifiedCount = remember(allUsers) {
        allUsers.count { it.kycStatus == "VERIFIED" }
    }

    val filteredUsers = remember(allUsers, searchQuery, selectedFilter) {
        allUsers.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.phone.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.userId.contains(searchQuery, ignoreCase = true)

            val isUnverified = user.kycStatus in listOf("PENDING", "IN_PROGRESS", "SUBMITTED", "RE_KYC_REQUESTED") ||
                (!user.aadhaarVerified || !user.panVerified || !user.bankVerified || !user.selfieVerified)

            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "UNVERIFIED" -> isUnverified
                "APPEALS" -> user.hasPendingBanAppeal && user.banAppealStatus == "PENDING"
                "VERIFIED" -> user.kycStatus == "VERIFIED"
                "BANNED" -> user.isBanned
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header (Zero Icons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onNavigateBack,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "< Back",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "${allUsers.size} Total Accounts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Users Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Review identities, pending verifications, appeals, and account sanctions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Search Bar (Zero Icons, Unboxed)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by name, phone, email, or @username...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filters Tabs (Horizontal Scrollable for Small Devices)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterTabs = listOf(
                    "ALL" to "All (${allUsers.size})",
                    "UNVERIFIED" to "Unverified ($unverifiedUsersCount)",
                    "APPEALS" to "Ban Appeals ($appealsCount)",
                    "VERIFIED" to "Verified ($verifiedCount)",
                    "BANNED" to "Banned ($bannedCount)"
                )

                filterTabs.forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    Surface(
                        onClick = { selectedFilter = key },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Profiles List
            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No user profiles matching this filter or search query.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it.userId }) { user ->
                        UserProfileManagementCard(
                            user = user,
                            onClick = { onNavigateToUserProfile(user.userId) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean user profile card with profile picture/initial, details,
 * and dynamic wrapping missing-verification tags.
 * Strictly zero icons, zero emojis, zero single-tap approvals.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserProfileManagementCard(
    user: UserEntity,
    onClick: () -> Unit
) {
    val isVerified = user.kycStatus == "VERIFIED"
    val pendingVerifications = remember(user) {
        val list = mutableListOf<String>()
        if (!user.aadhaarVerified) list.add("Aadhaar Pending")
        if (!user.panVerified) list.add("PAN Pending")
        if (!user.bankVerified) list.add("Bank Pending")
        if (!user.selfieVerified) list.add("Selfie Pending")
        list
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Avatar, Name, Role Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar / Initial (clean circle)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user.name.ifBlank { user.username.ifBlank { "U" } }).take(1).uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = user.name.ifBlank { "Unnamed User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Role Tag
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = user.role,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "@${user.username.ifBlank { user.userId }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact & Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (user.phone.isNotBlank()) user.phone else "Phone: N/A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Status: ${user.kycStatus}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isVerified) Emerald600 else Color(0xFFB45309)
                )
            }

            // Sanction Alert Banner (if banned or appeal pending)
            if (user.isBanned || (user.hasPendingBanAppeal && user.banAppealStatus == "PENDING")) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user.isBanned) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Red400.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (user.banType == "TEMPORARY") "TEMPORARILY SUSPENDED" else "PERMANENTLY BANNED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Red400,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (user.hasPendingBanAppeal && user.banAppealStatus == "PENDING") {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "APPEAL PENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Pending Verification FlowRow (Wraps naturally on any screen width)
            if (pendingVerifications.isNotEmpty() && !isVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pending Document Checks:",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pendingVerifications.forEach { pendingLabel ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = pendingLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer prompt to tap and inspect
            Text(
                text = "Tap to review documents & moderate >",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
