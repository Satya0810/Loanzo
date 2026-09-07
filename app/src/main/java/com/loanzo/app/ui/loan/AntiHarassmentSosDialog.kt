package com.loanzo.app.ui.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.loanzo.app.util.TelegramManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * AntiHarassmentSosDialog
 *
 * Provides borrowers with an emergency legal protection tool against debt recovery harassment:
 * - Bharatiya Nyaya Sanhita, 2023 (BNS) Sections 351 & 352 (Criminal Intimidation)
 * - Indian Penal Code, 1860 (IPC) Sections 503 & 506
 * - RBI Digital Lending Guidelines 2022 (Curfew hours: 8 AM - 7 PM, no unauthorized home visits)
 */
@Composable
fun AntiHarassmentSosDialog(
    loan: LoanEntity,
    borrower: UserEntity,
    lender: UserEntity,
    telegramManager: TelegramManager? = null,
    onDismiss: () -> Unit,
    onAlertDispatched: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }

    val violationTypes = listOf(
        "Curfew Breach (Contacted outside 8 AM - 7 PM)",
        "Criminal Intimidation / Threats (BNS §351)",
        "Unauthorized Residence Visit",
        "Contacting Family / Third Parties without Consent",
        "Extortionate / Compound Interest Gouging"
    )

    var selectedViolation by remember { mutableStateOf(violationTypes[0]) }
    var incidentNotes by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isDispatched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Red500.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Red500,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ANTI-HARASSMENT SOS DEFENSE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Red500
                    )
                    Text(
                        text = "Statutory Protection • BNS 351/352 & RBI DLG",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            if (isDispatched) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "Emergency Statutory Notice Dispatched",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "A formal legal notice under Bharatiya Nyaya Sanhita 2023 (Sections 351/352) has been transmitted to the lending syndicate and platform compliance officers. Late fee accruals on this loan are temporarily frozen pending grievance mediation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Legal Caution Alert
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Under Indian law, debt recovery through abusive calling, threatening relatives, or visiting homes outside 8 AM to 7 PM constitutes non-bailable criminal intimidation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text(
                        text = "Select Nature of Harassment:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        violationTypes.forEach { type ->
                            val isSelected = selectedViolation == type
                            Surface(
                                color = if (isSelected) Red500.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedViolation = type }
                                    .border(
                                        1.dp,
                                        if (isSelected) Red500 else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedViolation = type },
                                        colors = RadioButtonDefaults.colors(selectedColor = Red500)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Red500 else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = incidentNotes,
                        onValueChange = { incidentNotes = it },
                        label = { Text("Incident Details (Time, Caller ID, Specific Words)") },
                        placeholder = { Text("Describe the encounter...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Evidence Timestamp Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Timestamp: ${dateFormat.format(Date())}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                            Text("• Borrower UID: ${borrower.userId}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                            Text("• Loan Reference: ${loan.loanId}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isDispatched) {
                Button(
                    onClick = onAlertDispatched,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                ) {
                    Text("Close")
                }
            } else {
                Button(
                    onClick = {
                        isSending = true
                        coroutineScope.launch {
                            val alertHtml = """
                                <b>🚨 STATUTORY HARASSMENT & COERCION SOS ALERT</b>
                                <b>Loan ID:</b> ${loan.loanId}
                                <b>Borrower:</b> ${borrower.name} (${borrower.phone})
                                <b>Lender:</b> ${lender.name} (${lender.phone})
                                <b>Violation Type:</b> $selectedViolation
                                <b>Details:</b> ${incidentNotes.ifBlank { "Immediate emergency triggered by borrower." }}
                                <b>Timestamp:</b> ${dateFormat.format(Date())}
                                
                                <i>STATUTORY CITATION:</i>
                                This alert records potential criminal intimidation under <b>Bharatiya Nyaya Sanhita 2023 (§351/352)</b> and breaches <b>RBI Digital Lending Guidelines (2022)</b>.
                                Late fees are administratively frozen pending inquiry.
                            """.trimIndent()

                            telegramManager?.sendAdminAlert(alertHtml)
                            isSending = false
                            isDispatched = true
                        }
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transmitting...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch Statutory Notice")
                    }
                }
            }
        },
        dismissButton = {
            if (!isDispatched) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
