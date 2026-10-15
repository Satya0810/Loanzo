package com.loanzo.app.ui.agent.tabs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.ui.theme.*

/**
 * Tab 3: Field Communications & Safety Center.
 * Dedicated channel center with assigned borrowers, Central Dispatch desk,
 * High-Priority Emergency SOS Safety Beacon, and Regulatory Risk Bulletins.
 * Built strictly without decorative emojis.
 */
@Composable
fun AgentCommsTab(
    visits: List<AgentVisitEntity>,
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit,
    onTriggerSos: () -> Unit
) {
    val context = LocalContext.current
    var showSosDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. HIGH-PRIORITY EMERGENCY SOS SAFETY CARD ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RedLight),
                border = BorderStroke(1.dp, RedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Red500,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Emergency Safety Beacon",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Red500
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Transmit live coordinates to security desk",
                                fontSize = 11.sp,
                                color = TextSlateMedium
                            )
                        }
                    }

                    Button(
                        onClick = { showSosDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Red500),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TRIGGER SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // ── 2. CENTRAL DISPATCH & UNDERWRITING HOTLINE ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandIceBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Central Underwriting & Dispatch Desk",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavyDark
                                )
                                Text(
                                    text = "Hours: 08:00 AM - 10:00 PM IST",
                                    fontSize = 10.sp,
                                    color = TextSlateMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldLight
                        ) {
                            Text(
                                text = "ONLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1800562696"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandIceBlue, contentColor = BrandRoyalBlue),
                            border = BorderStroke(1.dp, BrandIceBorder)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onNavigateToChat("dispatch_desk", null, "ADMIN")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chat Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 3. ASSIGNED VISIT DIRECT CHAT CHANNELS ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Visit Communications",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavyDark
                )
                Text(
                    text = "${visits.size} Assigned",
                    fontSize = 12.sp,
                    color = TextSlateMuted
                )
            }
        }

        if (visits.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Active Channels", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                        Text("Channels are generated automatically upon visit dispatch.", fontSize = 11.sp, color = TextSlateMuted)
                    }
                }
            }
        } else {
            items(visits) { visit ->
                val partyName = visit.borrowerName.ifBlank { visit.lenderName }.ifBlank { "Registered Party" }
                val channelId = "chat_${visit.visitId}"

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToChat(channelId, visit.loanId, null) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BrandIceBlue,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (visit.visitType == "COLLATERAL_VERIFICATION") Icons.Default.Scale else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = BrandCobalt,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = partyName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextNavyDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = BrandIceBlue
                                    ) {
                                        Text(
                                            text = if (visit.visitType == "COLLATERAL_VERIFICATION") "COLLATERAL" else "KYC",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandRoyalBlue,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to coordinate arrival, location pin, or reschedule",
                                    fontSize = 11.sp,
                                    color = TextSlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Chat",
                            tint = TextSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── 4. OPERATIONAL & RISK BULLETINS ──
        item {
            Text(
                text = "Operational & Risk Bulletins",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextNavyDark
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Orange500, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Counterfeit Hallmark Alert (Indiranagar / Sector 62)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Risk operations identified forged 916 HUID stamps. Field officers must execute nitric acid streak & neodymium magnet tests before sealing pouch.",
                            fontSize = 11.sp,
                            color = TextSlateMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Issued by Chief Risk Officer • 3h ago",
                            fontSize = 9.sp,
                            color = BrandCobalt,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandIceBlue,
                border = BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Regulatory Mandate: In-Person Handshake PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All disbursements require mutual in-person 4-digit PIN verification. Never request PIN over telephone.",
                            fontSize = 11.sp,
                            color = TextSlateMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Loanzo Compliance Desk • Yesterday",
                            fontSize = 9.sp,
                            color = BrandCobalt,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // SOS Emergency Confirmation Dialog
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Red500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Field SOS Alert?", fontWeight = FontWeight.ExtraBold, color = Red500)
                }
            },
            text = {
                Column {
                    Text(
                        text = "This will immediately transmit your high-accuracy GPS coordinates to Loanzo Emergency Security Desk and alert on-duty supervisors.",
                        fontSize = 13.sp,
                        color = TextNavyDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RedLight,
                        border = BorderStroke(1.dp, RedBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Direct Police Emergency: 112", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red500)
                            Text("Loanzo Security Helpline: 1800-562-696", fontSize = 11.sp, color = TextSlateMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        onTriggerSos()
                        Toast.makeText(context, "SOS Emergency Beacon Transmitted to Dispatch", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("CONFIRM SOS NOW", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
