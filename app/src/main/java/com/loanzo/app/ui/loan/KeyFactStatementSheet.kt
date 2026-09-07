package com.loanzo.app.ui.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString

/**
 * KeyFactStatementDialog
 *
 * Implements the mandatory Key Fact Statement (KFS) required under:
 * - RBI Guidelines on Digital Lending (DLG), August 2022 (Annexure I)
 * - RBI Master Direction on Fair Lending Practices & Penal Charges (RBI/2023-24/53)
 * - Digital Personal Data Protection Act, 2023
 */
@Composable
fun KeyFactStatementDialog(
    loan: LoanEntity,
    lender: UserEntity,
    borrower: UserEntity,
    onDismiss: () -> Unit,
    onAcknowledgeKfs: () -> Unit
) {
    var hasAcknowledged by remember { mutableStateOf(false) }

    val totalInterest = loan.sanctionedAmount * (loan.interestRate / 100.0) * (loan.tenureMonths / 12.0)
    val totalRepayable = loan.sanctionedAmount + totalInterest

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = BrandRoyalBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = BrandRoyalBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "KEY FACT STATEMENT (KFS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B1E3B)
                    )
                    Text(
                        text = "Statutory Disclosure • RBI DLG 2022",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandRoyalBlue
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Statutory Banner
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Under the RBI Digital Lending Guidelines, this statement provides transparent disclosure of all credit terms before digital contract execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Metric Table Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KfsRow("Sanctioned Loan Amount", loan.sanctionedAmount.toInrString(), isBold = true)
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        KfsRow("Annual Percentage Rate (APR)", "${loan.interestRate}% p.a. (Simple)")
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        KfsRow("Total Repayment Amount", totalRepayable.toInrString(), isBold = true)
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        KfsRow("Tenure / Maturity", "${loan.tenureMonths} Months")
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        KfsRow("Disbursement Channel", "Direct UPI A2A (Non-Custodial)")
                    }
                }

                // Regulatory Clauses Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "STATUTORY BORROWER SAFEGUARDS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandRoyalBlue
                        )

                        KfsClauseItem(
                            title = "Penal Charges Cap (RBI/2023-24/53)",
                            detail = "Penal interest is strictly simple (non-compounded) and capped at 2% p.a. with a mandatory 3-day grace period."
                        )

                        KfsClauseItem(
                            title = "3-Day Look-up / Cooling-off Period",
                            detail = "Borrower has a statutory right to exit the loan within 3 days without any prepayment penalty upon returning the principal."
                        )

                        KfsClauseItem(
                            title = "Privacy-by-Design (DPDP Act 2023)",
                            detail = "Zero access to phone contacts, media gallery, or call logs. Data resides on-device with AES-256 encryption."
                        )

                        KfsClauseItem(
                            title = "Grievance Officer Contact",
                            detail = "Loanzo Legal Redressal Desk: compliance@loanzo.app | TAT: 48 Hours"
                        )
                    }
                }

                // Mandatory Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasAcknowledged) Color(0xFFECFDF5) else Color.Transparent)
                        .padding(4.dp)
                ) {
                    Checkbox(
                        checked = hasAcknowledged,
                        onCheckedChange = { hasAcknowledged = it },
                        colors = CheckboxDefaults.colors(checkedColor = Emerald500)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "I have read and statutorily acknowledged this Key Fact Statement under RBI Digital Lending Guidelines 2022.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (hasAcknowledged) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAcknowledgeKfs,
                enabled = hasAcknowledged,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Acknowledge & Sign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun KfsRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun KfsClauseItem(title: String, detail: String) {
    Column {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
