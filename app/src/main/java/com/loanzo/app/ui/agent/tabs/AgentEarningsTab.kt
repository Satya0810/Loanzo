package com.loanzo.app.ui.agent.tabs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString

/**
 * Tab 4: Officer Wallet, Bounties & Settlement.
 * Bank-grade institutional earnings dashboard with 1-tap IMPS withdrawal,
 * travel milestone allowances, itemized transaction ledger, and linked bank accounts.
 * Built strictly without decorative emojis.
 */
@Composable
fun AgentEarningsTab(
    user: UserEntity?,
    totalEarnings: Double,
    completedVisitsCount: Int,
    onInstantWithdrawal: (Double) -> Unit = {}
) {
    val context = LocalContext.current
    val balance = if (totalEarnings > 0) totalEarnings else 4500.0
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawalSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. OFFICER WALLET & CASHOUT CARD ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandIceBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OFFICER ESCROW BALANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandRoyalBlue,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = balance.toInrString(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextNavyDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ready to Payout",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSlateMuted,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { showWithdrawDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Withdraw", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BrandIceBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Automated Settlement: Daily 08:00 PM IST",
                                fontSize = 11.sp,
                                color = TextSlateMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandIceBlue
                        ) {
                            Text(
                                text = if (user?.bankAccountNumber?.isNotBlank() == true) "Bank A/C ending ${user.bankAccountNumber.takeLast(4)}" else "UPI Linked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandRoyalBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── 2. INCENTIVES & PERFORMANCE MILESTONES ──
        item {
            Text(
                text = "Performance & Travel Incentives",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextNavyDark
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(1.dp, BrandIceBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Travel Allowance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("INR 300 / Day", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = BrandRoyalBlue)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Threshold: 5 completed stops", fontSize = 9.sp, color = TextSlateMuted)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldLight,
                    border = BorderStroke(1.dp, EmeraldBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Emerald500, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consensus Bounty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("+INR 500 / Month", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Zero valuation discrepancy", fontSize = 9.sp, color = TextSlateMuted)
                    }
                }
            }
        }

        // ── 3. DETAILED PAYOUT TRANSACTION LEDGER ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Settlement Statements",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavyDark
                )
                Text(
                    text = "IMPS Certified",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald500
                )
            }
        }

        val transactions = listOf(
            Triple("+INR 350.00", "Gold Collateral Appraisal • MSME #100K", "Today 11:45 AM • Verified"),
            Triple("+INR 250.00", "Physical KYC Verification • Romesh Singh", "Today 09:15 AM • Verified"),
            Triple("+INR 300.00", "Daily Travel Allowance Milestone", "Yesterday 07:30 PM • Credited"),
            Triple("+INR 350.00", "Collateral & Premises Audit • Kumar Manish", "14-Sep-2026 • Verified"),
            Triple("-INR 1,250.00", "IMPS Settlement to Bank A/C ending 9482", "12-Sep-2026 • Ref #90218401")
        )

        items(transactions.size) { index ->
            val (amount, title, subtitle) = transactions[index]
            val isCredit = amount.startsWith("+")

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
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
                            color = if (isCredit) EmeraldLight else BrandIceBlue,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isCredit) Emerald500 else BrandRoyalBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    Text(
                        text = amount,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCredit) Emerald500 else TextNavyDark
                    )
                }
            }
        }

        // ── 4. LINKED SETTLEMENT ACCOUNTS ──
        item {
            Text(
                text = "Linked Settlement Channels",
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BrandCobalt, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "State Bank of India (A/C ending 9482)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavyDark
                                )
                                Text(text = "IFSC: SBIN0005423 • IMPS Direct", fontSize = 10.sp, color = TextSlateMuted)
                            }
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = EmeraldLight) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "PRIMARY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BrandIceBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "UPI: abhisi.field@okhdfc",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavyDark
                                )
                                Text(text = "Instant 1-Click Payout Active", fontSize = 10.sp, color = TextSlateMuted)
                            }
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = EmeraldLight) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "VERIFIED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Instant Withdrawal Confirmation Dialog
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Emerald500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant Settlement Withdrawal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Withdraw ${balance.toInrString()} immediately to your primary State Bank of India account via direct IMPS settlement?",
                        fontSize = 13.sp,
                        color = TextNavyDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(1.dp, BrandIceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Settlement Rail: Direct Escrow IMPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandCobalt)
                            Text("Zero transaction fee for Verified Officers", fontSize = 10.sp, color = TextSlateMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWithdrawDialog = false
                        withdrawalSuccess = true
                        onInstantWithdrawal(balance)
                        Toast.makeText(context, "Withdrawal of ${balance.toInrString()} initiated via IMPS (Ref #90218401)", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Confirm Transfer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
