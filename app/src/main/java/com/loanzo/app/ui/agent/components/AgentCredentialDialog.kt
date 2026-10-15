package com.loanzo.app.ui.agent.components

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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.VerificationManager

/**
 * Official Field Officer Credentials, Digital Identity Card & Partner Settings.
 * Displayed as a modal dialog when tapping the Officer Avatar in the Top Bar.
 */
@Composable
fun AgentCredentialDialog(
    user: UserEntity?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onSwitchToConsumer: (() -> Unit)? = null,
    onSwitchToAdmin: (() -> Unit)? = null
) {
    val isAppOwner = VerificationManager.isEligibleAppOwner(user)
    var showSwitchConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = CanvasPorcelain,
            border = BorderStroke(1.dp, BrandIceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandIceBlue,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = BrandRoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Officer Credentials",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextNavyDark
                            )
                            Text(
                                text = "Loanzo Field Operations Portal",
                                fontSize = 11.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, BrandIceBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSlateMedium,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ── 1. DIGITAL OFFICER ID BADGE (Holographic Card) ──
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BrandRoyalBlue,
                    border = BorderStroke(1.dp, GoldCoinAmber.copy(alpha = 0.6f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        BrandRoyalBlue,
                                        BrandCobalt,
                                        Color(0xFF1E3A8A)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            // Badge Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GoldCoinAmber
                                    ) {
                                        Text(
                                            text = "OFFICIAL IDENTITY CARD",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Emerald400)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTIVE CREDENTIAL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldBorder
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Officer Photo & Core Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(GoldCoinRich, GoldCoinBright)
                                                )
                                            )
                                    )
                                    LoanzoAvatar(
                                        user = user,
                                        size = 54.dp,
                                        showVerifiedBadge = true,
                                        borderColor = Color.White,
                                        borderWidth = 2.dp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = user?.name?.ifBlank { "Loanzo Field Officer" } ?: "Loanzo Field Officer",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Lead Field Assayer & Compliance",
                                        fontSize = 12.sp,
                                        color = BrandIceBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "ID: LZ-AGENT-${user?.userId?.takeLast(4)?.uppercase() ?: "4821"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = GoldCoinBright,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom Card Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "STATION JURISDICTION",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandIceBlue.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "Delhi-NCR Sector Unit 04",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "VALID THROUGH",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandIceBlue.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "31-DEC-2027 • SEC 65B SEAL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldCoinBright
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 2. PERFORMANCE & TRUST AUDIT ──
                Text(
                    text = "Performance & Quality Metrics",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavyDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = GoldCoinCream,
                        border = BorderStroke(1.dp, GoldCoinBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GoldCoinRich, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Rating", fontSize = 11.sp, color = TextSlateMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "4.95 / 5.0", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextNavyDark)
                            Text(text = "Top 1% Officer", fontSize = 9.sp, color = GoldCoinAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldLight,
                        border = BorderStroke(1.dp, EmeraldBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Emerald500, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "On-Time", fontSize = 11.sp, color = TextSlateMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "98.4%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextNavyDark)
                            Text(text = "ETA Accuracy", fontSize = 9.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(1.dp, BrandIceBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = BrandCobalt, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Fraud Block", fontSize = 11.sp, color = TextSlateMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "100%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextNavyDark)
                            Text(text = "Zero False Passes", fontSize = 9.sp, color = BrandCobalt, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── 3. OPERATIONAL DISPATCH PREFERENCES ──
                Text(
                    text = "Operational Preferences",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavyDark
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandCobalt, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Assigned Operating Pincodes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextNavyDark)
                            }
                            Text(text = "201301, 201309", fontSize = 11.sp, color = TextSlateMuted, fontWeight = FontWeight.Medium)
                        }

                        HorizontalDivider(color = BrandIceBorder, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = BrandCobalt, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Preferred Shift Hours", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextNavyDark)
                            }
                            Text(text = "09:00 AM - 07:00 PM", fontSize = 11.sp, color = TextSlateMuted, fontWeight = FontWeight.Medium)
                        }

                        HorizontalDivider(color = BrandIceBorder, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Offline Inspection Cache", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextNavyDark)
                            }
                            Text(text = "Synced Live", fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── SECURE AGENT SIGN OUT ──
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedLight,
                        contentColor = Red500
                    ),
                    border = BorderStroke(1.dp, RedBorder)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = Red500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out of Field Portal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Red500
                    )
                }
            }
        }
    }
}
