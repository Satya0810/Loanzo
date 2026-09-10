package com.loanzo.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.loanzo.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RoleSelectionScreen(
    userName: String,
    userId: String = "",
    userPhone: String = "",
    userEmail: String = "",
    userRole: String = "MEMBER",
    onSelectNormalMember: () -> Unit,
    onSelectAgent: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminRepository = com.loanzo.app.util.LocalAdminRepository.current
    var showAdminRequestDialog by remember { mutableStateOf(false) }
    var adminJustification by remember { mutableStateOf("") }
    var isSubmittingRequest by remember { mutableStateOf(false) }

    var selectedRole by remember { mutableStateOf<String?>("USER") } // "USER" or "AGENT"

    val goldAccent = GoldCoinBright
    val emeraldAccent = Emerald400
    val royalBlue = BrandRoyalBlue

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandRoyalBlue, Color(0xFF3B82F6)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HowToReg,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Emerald400.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KYC VERIFICATION COMPLETED",
                        color = Emerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome, ${userName.ifBlank { "Member" }}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose your operating participation mode in the Loanzo decentralized financial ecosystem.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Option 1: Normal Member (Crisp White Card on Porcelain Canvas)
            MemberRoleCard(
                title = "Normal Member",
                badgeText = "P2P BORROWER & LENDER",
                badgeColor = BrandRoyalBlue,
                icon = Icons.Default.AccountBalanceWallet,
                iconGradient = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6)),
                description = "Borrow capital at competitive rates, fund peer requests, or trade pledged collateral assets.",
                benefits = listOf(
                    "Create or fund peer-to-peer loan contracts",
                    "Automated Digital Agreements & legal stamp generation",
                    "Direct UPI & Net-Banking settlement to bank",
                    "Zero field travel or inspection duties"
                ),
                isSelected = selectedRole == "USER",
                onClick = { selectedRole = "USER" }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Option 2: Loanzo Certified Field Agent (Signature Obsidian Black VIP Card)
            ObsidianAgentRoleCard(
                title = "Loanzo Certified Agent",
                badgeText = "EARN ₹500 - ₹1,500 / VISIT",
                badgeColor = GoldCoinBright,
                icon = Icons.Default.Security,
                iconGradient = listOf(GoldCoinAmber, GoldCoinBright),
                description = "Empanel as an official on-ground field inspection officer & physical collateral appraiser.",
                benefits = listOf(
                    "Inspect Gold, Vehicles & Real Estate collateral",
                    "Physical Borrower & Lender in-person verification",
                    "Turn-by-turn navigation & instant camera proof",
                    "Daily payouts credited per completed inspection",
                    "Requires Police Clearance Certificate (PCC)"
                ),
                isSelected = selectedRole == "AGENT",
                onClick = { selectedRole = "AGENT" }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Action Button
            Button(
                onClick = {
                    if (selectedRole == "USER") {
                        onSelectNormalMember()
                    } else if (selectedRole == "AGENT") {
                        onSelectAgent()
                    }
                },
                enabled = selectedRole != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRole == "AGENT") GoldCoinRich else BrandRoyalBlue,
                    contentColor = if (selectedRole == "AGENT") Navy900 else Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (selectedRole) {
                            "AGENT" -> "Proceed to Certified Officer Empanelment"
                            "USER" -> "Continue to Member Dashboard"
                            else -> "Select a Role to Continue"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedRole != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdminRequestDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Are you platform staff? Request Admin Access",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold500
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Admin Request Dialog
        if (showAdminRequestDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSubmittingRequest) showAdminRequestDialog = false },
                icon = {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Gold500, modifier = Modifier.size(32.dp))
                },
                title = {
                    Text("Request Platform Staff Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column {
                        Text(
                            text = "Platform Admin clearance grants access to collateral vault oversight, agent dispatching, KYC attestations, and ombudsman mediation.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = adminJustification,
                            onValueChange = { adminJustification = it },
                            placeholder = { Text("Enter your reason / department / employee ID...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (adminJustification.isNotBlank()) {
                                scope.launch(Dispatchers.IO) {
                                    isSubmittingRequest = true
                                    adminRepository.submitAdminRequest(
                                        userId = userId.ifBlank { "USR-PENDING" },
                                        userName = userName.ifBlank { "User" },
                                        userPhone = userPhone,
                                        userEmail = userEmail,
                                        currentRole = userRole,
                                        justification = adminJustification.trim()
                                    )
                                    isSubmittingRequest = false
                                    showAdminRequestDialog = false
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Admin clearance request submitted to platform owner!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = adminJustification.isNotBlank() && !isSubmittingRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900)
                    ) {
                        if (isSubmittingRequest) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Navy900, strokeWidth = 2.dp)
                        } else {
                            Text("Submit Request", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAdminRequestDialog = false },
                        enabled = !isSubmittingRequest
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Normal Member Card: Crisp White GlassCard on Porcelain Canvas
 */
@Composable
private fun MemberRoleCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconGradient: List<Color>,
    description: String,
    benefits: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 5.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x0A0F172A),
                spotColor = Color(0x181D4ED8)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) BrandRoyalBlue else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(iconGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = BrandRoyalBlue,
                        unselectedColor = Color(0xFF94A3B8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandRoyalBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = benefit,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * Signature Obsidian Black VIP Card (Field Agent Opportunity Card)
 */
@Composable
private fun ObsidianAgentRoleCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconGradient: List<Color>,
    description: String,
    benefits: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x33000000),
                spotColor = Color(0x400B0F19)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) GoldCoinBright else Color(0xFF334155)
        )
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Navy700, Navy900)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(iconGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Navy900,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldCoinRich.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, GoldCoinBright.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = badgeText,
                            color = GoldCoinBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = GoldCoinBright,
                        unselectedColor = Color(0xFF64748B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = Gray300,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = benefit,
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}
