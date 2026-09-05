package com.loanzo.app.ui.auth

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.*

@Composable
fun RoleSelectionScreen(
    userName: String,
    onSelectNormalMember: () -> Unit,
    onSelectAgent: () -> Unit
) {
    var selectedRole by remember { mutableStateOf<String?>(null) } // "USER" or "AGENT"

    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val cardBorder = Color(0xFF30363D)
    val goldAccent = Color(0xFFFFB800)
    val emeraldAccent = Color(0xFF10B981)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))),
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
                color = emeraldAccent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = emeraldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KYC VERIFICATION COMPLETED",
                        color = emeraldAccent,
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
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose how you want to participate in the Loanzo decentralized ecosystem.",
                fontSize = 13.sp,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Option 1: Normal Member
            RoleSelectionCard(
                title = "Normal Member",
                badgeText = "P2P BORROWER & LENDER",
                badgeColor = Color(0xFF38BDF8),
                icon = Icons.Default.AccountBalanceWallet,
                iconGradient = listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
                description = "Borrow funds, fund peer requests, or trade in the collateral marketplace.",
                benefits = listOf(
                    "Create or Fund P2P loan contracts",
                    "Automated Digital Agreements & NOC generation",
                    "Direct UPI & Net-Banking settlement",
                    "Zero field travel or inspection duties"
                ),
                isSelected = selectedRole == "USER",
                onClick = { selectedRole = "USER" }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Option 2: Loanzo Certified Field Agent
            RoleSelectionCard(
                title = "Loanzo Certified Agent",
                badgeText = "EARN ₹500 - ₹1,500 / VISIT",
                badgeColor = goldAccent,
                icon = Icons.Default.Security,
                iconGradient = listOf(Color(0xFFD97706), goldAccent),
                description = "Empanel as an official on-ground field inspection & collateral appraisal officer.",
                benefits = listOf(
                    "Inspect Gold, Vehicles & Real Estate collateral",
                    "Physical Borrower & Lender in-person verification",
                    "Direct phone, WhatsApp & Google Maps navigation",
                    "Daily payouts credited per completed inspection",
                    "Requires Police Verification (PCC) clearance"
                ),
                isSelected = selectedRole == "AGENT",
                onClick = { selectedRole = "AGENT" }
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                    containerColor = if (selectedRole == "AGENT") goldAccent else Color(0xFF3B82F6),
                    disabledContainerColor = Color(0xFF21262D)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (selectedRole) {
                            "AGENT" -> "Proceed to Bank-Grade Empanelment Form"
                            "USER" -> "Continue to Normal Dashboard"
                            else -> "Select a Role to Continue"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedRole == "AGENT") Color.Black else Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedRole != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (selectedRole == "AGENT") Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RoleSelectionCard(
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
    val cardBg = if (isSelected) Color(0xFF1F2937) else Color(0xFF161B22)
    val borderColor = if (isSelected) badgeColor else Color(0xFF30363D)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        color = badgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
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
                        selectedColor = badgeColor,
                        unselectedColor = Color(0xFF4B5563)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = benefit,
                        fontSize = 12.sp,
                        color = Color(0xFFE5E7EB)
                    )
                }
            }
        }
    }
}
