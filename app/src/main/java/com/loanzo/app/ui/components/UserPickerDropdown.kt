package com.loanzo.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.*

/**
 * Standard candidate users fallback in case offline repository is syncing.
 */
val DEFAULT_DEMO_CANDIDATE_USERS = listOf(
    UserEntity(
        userId = "demo_lender_priya",
        name = "Priya Patel",
        email = "priya.patel@loanzo.app",
        phone = "+91 91234 56789",
        username = "priya_invest",
        role = "LENDER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_vikram_malhotra",
        name = "Vikram Malhotra",
        email = "vikram.investor@loanzo.app",
        phone = "+91 98333 44556",
        username = "vikram_angel",
        role = "LENDER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_rajesh_gupta",
        name = "Rajesh Gupta",
        email = "rajesh.gupta@loanzo.app",
        phone = "+91 98999 88776",
        username = "rajesh_capital",
        role = "LENDER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_borrower_rahul",
        name = "Rahul Sharma",
        email = "rahul.sharma@loanzo.app",
        phone = "+91 98765 43210",
        username = "rahul_sharma",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_sneha_roy",
        name = "Sneha Roy",
        email = "sneha.clinic@loanzo.app",
        phone = "+91 98111 22334",
        username = "sneha_roy",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_amit_verma",
        name = "Amit Verma",
        email = "amit.agri@loanzo.app",
        phone = "+91 98444 55667",
        username = "amit_verma",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_coborrower_rohan",
        name = "Dr. Rohan Patil",
        email = "rohan.patil@demo.loanzo.app",
        phone = "+91 98765 88990",
        username = "dr_rohan_patil",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_guarantor_nirmala",
        name = "Nirmala Devi",
        email = "nirmala.devi@demo.loanzo.app",
        phone = "+91 99100 33445",
        username = "nirmala_devi",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_meera_sen",
        name = "Meera Sen",
        email = "meera.sen@demo.loanzo.app",
        phone = "+91 98301 44552",
        username = "meera_sen",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_kunal_rawat",
        name = "Kunal Rawat",
        email = "kunal.rawat@demo.loanzo.app",
        phone = "+91 98441 77221",
        username = "kunal_rawat",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_alok_trivedi",
        name = "Alok Trivedi",
        email = "alok.trivedi@demo.loanzo.app",
        phone = "+91 98122 33445",
        username = "alok_trivedi",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_user_arjun",
        name = "Arjun Mehta",
        email = "arjun.mehta@demo.loanzo.app",
        phone = "+91 98765 12340",
        username = "arjun_mehta",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_agent_abhisi",
        name = "Abhisi (Field Agent)",
        email = "abhisi@loanzo.app",
        phone = "+91 98100 12345",
        username = "abhisi",
        role = "AGENT",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_agent_sunil",
        name = "Sunil Patil (Field Officer 2)",
        email = "sunil.agent@loanzo.app",
        phone = "+91 98200 54321",
        username = "agent_sunil",
        role = "AGENT",
        kycStatus = "VERIFIED"
    ),
    UserEntity(
        userId = "demo_admin_satyam",
        name = "Satyam Kumar (Admin)",
        email = "satyam@loanzo.app",
        phone = "+91 70615 59039",
        username = "satyam0810",
        role = "ADMIN",
        kycStatus = "VERIFIED"
    )
)

/**
 * Reusable, searchable dropdown / scroll-down user picker.
 * Solves manual typing of opaque IDs across Loan creation, Chat Hub, and Reporting.
 */
@Composable
fun UserPickerDropdown(
    selectedUserId: String,
    onUserSelected: (UserEntity) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select User",
    placeholder: String = "Search or scroll down to pick member...",
    preferredRole: String? = null,
    candidateUsers: List<UserEntity> = emptyList(),
    onSearchOnline: (suspend (String) -> List<UserEntity>)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf(preferredRole ?: "ALL") }
    var onlineUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim().removePrefix("@")
        if (q.length >= 2 && onSearchOnline != null) {
            try {
                onlineUsers = onSearchOnline(q)
            } catch (_: Exception) {}
        }
    }

    val allUsers = remember(candidateUsers, onlineUsers) {
        val merged = (candidateUsers + onlineUsers + DEFAULT_DEMO_CANDIDATE_USERS).distinctBy { it.userId }
        merged
    }

    val selectedUser = remember(selectedUserId, allUsers) {
        allUsers.find {
            it.userId.equals(selectedUserId, ignoreCase = true) ||
            it.username.equals(selectedUserId.removePrefix("@"), ignoreCase = true) ||
            it.name.equals(selectedUserId, ignoreCase = true) ||
            it.phone.equals(selectedUserId, ignoreCase = true)
        }
    }

    val roleOptions = listOf("ALL", "LENDER", "BORROWER", "AGENT", "ADMIN")

    val filteredUsers = remember(allUsers, searchQuery, selectedRoleFilter) {
        allUsers.filter { user ->
            val matchesRole = when (selectedRoleFilter) {
                "ALL" -> true
                "LENDER" -> user.role.equals("LENDER", ignoreCase = true)
                "BORROWER" -> user.role.equals("BORROWER", ignoreCase = true)
                "AGENT" -> user.role.equals("AGENT", ignoreCase = true)
                "ADMIN" -> user.role.equals("ADMIN", ignoreCase = true)
                else -> true
            }

            val cleanQuery = searchQuery.trim().removePrefix("@").lowercase()
            val matchesQuery = if (cleanQuery.isBlank()) true else {
                user.name.lowercase().contains(cleanQuery) ||
                user.username.lowercase().contains(cleanQuery) ||
                user.phone.contains(cleanQuery) ||
                user.userId.lowercase().contains(cleanQuery)
            }

            matchesRole && matchesQuery
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Dropdown Trigger Container
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon / Avatar
                if (selectedUser != null) {
                    val avatarTint = when (selectedUser.role.uppercase()) {
                        "LENDER" -> Gold500
                        "BORROWER" -> Emerald500
                        "AGENT" -> Blue400
                        "ADMIN" -> Color(0xFF6366F1)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        shape = CircleShape,
                        color = avatarTint.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, avatarTint.copy(alpha = 0.4f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = selectedUser.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = avatarTint,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    if (selectedUser != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedUser.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "@${selectedUser.username.ifBlank { selectedUser.userId.take(8) }}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    } else if (selectedUserId.isNotBlank()) {
                        Text(
                            text = selectedUserId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Trailing Action Controls
                if (selectedUserId.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onUserSelected(UserEntity(userId = "", name = ""))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Dropdown Panel with Search, Role Filters, and Scrollable Member List
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, @username, or phone...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(roleOptions) { role ->
                            val isSelected = selectedRoleFilter == role
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable { selectedRoleFilter = role }
                            ) {
                                Text(
                                    text = when (role) {
                                        "ALL" -> "All Members"
                                        "LENDER" -> "💼 Lenders"
                                        "BORROWER" -> "🤝 Borrowers"
                                        "AGENT" -> "🕵️ Agents"
                                        "ADMIN" -> "👑 Admins"
                                        else -> role
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SCROLL DOWN TO SELECT MEMBER (${filteredUsers.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scrollable User List
                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users found matching '$searchQuery'",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            items(filteredUsers, key = { it.userId }) { user ->
                                val isCurrentlySelected = selectedUser?.userId == user.userId
                                val roleColor = when (user.role.uppercase()) {
                                    "LENDER" -> Gold500
                                    "BORROWER" -> Emerald500
                                    "AGENT" -> Blue400
                                    "ADMIN" -> Color(0xFF6366F1)
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrentlySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUserSelected(user)
                                            isExpanded = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Avatar
                                        Surface(
                                            shape = CircleShape,
                                            color = roleColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, roleColor.copy(alpha = 0.4f)),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = user.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = roleColor,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (user.kycStatus == "VERIFIED") {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Verified",
                                                        tint = Emerald500,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "@${user.username.ifBlank { user.userId.take(8) }}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (user.phone.isNotBlank()) {
                                                    Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = user.phone,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Role Pill
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = roleColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = user.role.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = roleColor,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (isCurrentlySelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
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
    }
}
