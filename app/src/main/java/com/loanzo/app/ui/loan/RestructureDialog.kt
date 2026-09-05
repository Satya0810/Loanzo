package com.loanzo.app.ui.loan

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.calculateEMI
import com.loanzo.app.util.toInrString

@Composable
fun RestructureLoanDialog(
    loan: LoanEntity,
    onDismiss: () -> Unit,
    onRestructure: (newTenureMonths: Int, moratoriumMonths: Int) -> Unit
) {
    var newTenure by remember { mutableStateOf(loan.tenureMonths.toString()) }
    var moratoriumMonths by remember { mutableStateOf(loan.moratoriumMonths.toString()) }

    val currentTenure = loan.tenureMonths
    val parsedNewTenure = newTenure.toIntOrNull() ?: currentTenure
    val parsedMoratorium = moratoriumMonths.toIntOrNull() ?: 0

    val oldEmi = calculateEMI(loan.sanctionedAmount, loan.interestRate, currentTenure)
    val effectiveRemainingTenure = (parsedNewTenure - parsedMoratorium).coerceAtLeast(1)
    val newEmi = calculateEMI(loan.sanctionedAmount, loan.interestRate, effectiveRemainingTenure)
    val moratoriumMonthlyInterest = loan.sanctionedAmount * (loan.interestRate / (12 * 100))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Gold500)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restructure Loan", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Adjust tenure or provide EMI relief for ${loan.purpose}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // New Tenure input
                OutlinedTextField(
                    value = newTenure,
                    onValueChange = { newTenure = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Tenure (Months)") },
                    supportingText = { Text("Currently ${loan.tenureMonths} months") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Moratorium Months input
                OutlinedTextField(
                    value = moratoriumMonths,
                    onValueChange = { moratoriumMonths = it.filter { c -> c.isDigit() } },
                    label = { Text("EMI Holiday / Moratorium (Months)") },
                    supportingText = { Text("Borrower pays interest only for this period") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Comparison Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Preview Changes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Gold500)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Original EMI:", style = MaterialTheme.typography.bodySmall)
                            Text(oldEmi.toInrString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("New Regular EMI:", style = MaterialTheme.typography.bodySmall)
                            Text(newEmi.toInrString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald400)
                        }

                        if (parsedMoratorium > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Moratorium Payment (Interest only):", style = MaterialTheme.typography.bodySmall)
                                Text(moratoriumMonthlyInterest.toInrString(), style = MaterialTheme.typography.bodySmall, color = Orange400, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRestructure(parsedNewTenure, parsedMoratorium)
                    onDismiss()
                },
                enabled = parsedNewTenure > parsedMoratorium && parsedNewTenure > 0,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900)
            ) {
                Text("Apply Restructuring", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
