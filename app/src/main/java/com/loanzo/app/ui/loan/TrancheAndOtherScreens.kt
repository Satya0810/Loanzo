package com.loanzo.app.ui.loan

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.domain.RuleEvaluation
import com.loanzo.app.domain.RuleSeverity
import com.loanzo.app.domain.model.PurposeCategory
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toRelativeTime
import com.loanzo.app.util.toDateString
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.loanzo.app.util.UpiHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrancheRequestScreen(
    loanId: String,
    ruleEvaluation: RuleEvaluation?,
    onSubmit: (amount: Double, payeeName: String, payeeUpiId: String,
               purpose: String, purposeCategory: String, note: String) -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var payeeName by remember { mutableStateOf("") }
    var payeeUpiId by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PurposeCategory.OTHER) }
    var note by remember { mutableStateOf("") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.request_tranche), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.disbursement_request),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.specify_the_payee_amount_and_purpose_for_this_tranche),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.amount)) },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = payeeName,
                onValueChange = { payeeName = it },
                label = { Text(stringResource(R.string.payee_name)) },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = payeeUpiId,
                onValueChange = { payeeUpiId = it },
                label = { Text(stringResource(R.string.payee_upi_id)) },
                placeholder = { Text(stringResource(R.string.merchant_upi)) },
                leadingIcon = { Icon(Icons.Default.Payment, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text(stringResource(R.string.purpose_description)) },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Purpose category dropdown
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.purpose_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(14.dp)
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    PurposeCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                selectedCategory = category
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(14.dp)
            )

            // Rule engine result display
            if (ruleEvaluation != null) {
                GlassCard {
                    Text(stringResource(R.string.rule_engine_result), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusBadge(
                        text = ruleEvaluation.result.displayName,
                        color = when (ruleEvaluation.result.name) {
                            "CONSISTENT", "AUTO_APPROVED" -> Emerald400
                            "MISMATCH", "REVIEW" -> Orange400
                            "BLOCKED" -> Red400
                            else -> Gray400
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ruleEvaluation.checks.forEach { check ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (check.passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                null,
                                tint = when (check.severity) {
                                    RuleSeverity.INFO -> Emerald400
                                    RuleSeverity.WARNING -> Orange400
                                    RuleSeverity.HARD_BLOCK -> Red400
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(check.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text(check.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSubmit(
                        amount.toDoubleOrNull() ?: 0.0,
                        payeeName,
                        payeeUpiId,
                        purpose,
                        selectedCategory.name,
                        note
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                enabled = amount.isNotBlank() && payeeName.isNotBlank() && purpose.isNotBlank()
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.submit_tranche_request), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MakeRepaymentScreen(
    loanId: String,
    outstandingAmount: Double,
    onSubmit: (amount: Double, transactionRef: String) -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var transactionRef by remember { mutableStateOf("") }
    var showQrDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showQrDialog) {
        val amountToPay = amount.toDoubleOrNull() ?: outstandingAmount
        UpiQrCodeDialog(
            payeeName = "Loanzo Repayments",
            payeeUpiId = "merchant@upi",
            amount = amountToPay,
            loanPurpose = "Loan $loanId Repayment",
            onDismiss = { showQrDialog = false }
        )
    }

    val upiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
            val data = result.data?.getStringExtra("response") ?: result.data?.getStringExtra("Status")
            if (data != null) {
                // Typical response: txnId=...&responseCode=00&Status=SUCCESS&txnRef=...
                val responseParams = data.split("&").associate { 
                    val parts = it.split("=")
                    if(parts.size > 1) parts[0].lowercase() to parts[1] else it to ""
                }
                
                val status = responseParams["status"]?.lowercase() ?: ""
                val txnRef = responseParams["txnref"] ?: responseParams["txnid"] ?: ""
                
                if (status == "success" || status == "submitted") {
                    transactionRef = txnRef
                    Toast.makeText(context, "Payment processing. Verify with reference ID.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.make_repayment), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientCard(gradientColors = listOf(Navy600, Navy700)) {
                Text(stringResource(R.string.outstanding_balance), style = MaterialTheme.typography.labelMedium, color = Gray300)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "₹${String.format(java.util.Locale.getDefault(), "%,.2f", outstandingAmount)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (outstandingAmount > 0) Gold400 else Emerald400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.repayment_amount)) },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Quick amount chips
            Text(stringResource(R.string.quick_select), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Full Amount" to outstandingAmount,
                    "50%" to outstandingAmount * 0.5,
                    "25%" to outstandingAmount * 0.25
                ).forEach { (label, value) ->
                    AssistChip(
                        onClick = { amount = String.format(java.util.Locale.getDefault(), "%.0f", value) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = transactionRef,
                onValueChange = { transactionRef = it },
                label = { Text(stringResource(R.string.upi_transaction_reference)) },
                placeholder = { Text(stringResource(R.string.enter_upi_transaction_id)) },
                leadingIcon = { Icon(Icons.Default.Receipt, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { 
                        val amountToPay = amount.toDoubleOrNull() ?: 0.0
                        if (amountToPay > 0 && UpiHelper.isUpiAvailable(context)) {
                            val intent = UpiHelper.createPaymentIntent(
                                payeeUpiId = "merchant@upi",
                                payeeName = "Loanzo Repayments",
                                amount = amountToPay,
                                transactionNote = "Repayment for Loan $loanId"
                            )
                            upiLauncher.launch(intent)
                        } else if (amountToPay <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No UPI app found on this device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Payment, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.pay_directly_with_upi_app), fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showQrDialog = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp), tint = Emerald400)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Show QR Code", fontSize = 12.sp, color = Color.White, maxLines = 1)
                }
            }

            Text(
                stringResource(R.string.tip_you_can_pay_directly_using_a_upi_app_installed_on_this_device_or_complete_it_externally_and_paste_the_transaction_reference),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slide to Repay (Cred / Jupiter inspired)
            SwipeToConfirmButton(
                text = "Slide to Record Repayment ➔",
                enabled = amount.isNotBlank() && transactionRef.isNotBlank(),
                thumbColor = Emerald400,
                trackColors = listOf(Navy700, Navy800),
                activeTrackColor = Emerald400.copy(alpha = 0.25f),
                onConfirm = { onSubmit(amount.toDoubleOrNull() ?: 0.0, transactionRef) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditTrailScreen(
    events: List<com.loanzo.app.data.entity.AuditEventEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audit_trail), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = "No audit events",
                    subtitle = "Activity will be logged here"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events.size) { index ->
                    val event = events[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Timeline dot and line
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(32.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (event.event) {
                                        "CREATED" -> Blue400
                                        "APPROVED" -> Emerald400
                                        "REJECTED" -> Red400
                                        "PAID" -> Emerald400
                                        "FLAGGED" -> Orange400
                                        else -> Gray400
                                    }.copy(alpha = 0.2f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        when (event.event) {
                                            "CREATED" -> Icons.Default.Add
                                            "APPROVED" -> Icons.Default.Check
                                            "REJECTED" -> Icons.Default.Close
                                            "PAID" -> Icons.Default.Payment
                                            "FLAGGED" -> Icons.Default.Flag
                                            "UPDATED" -> Icons.Default.Edit
                                            else -> Icons.Default.Info
                                        },
                                        null,
                                        modifier = Modifier.padding(6.dp),
                                        tint = when (event.event) {
                                            "CREATED" -> Blue400
                                            "APPROVED" -> Emerald400
                                            "REJECTED" -> Red400
                                            "PAID" -> Emerald400
                                            "FLAGGED" -> Orange400
                                            else -> Gray400
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        event.event,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        event.timestamp.toRelativeTime(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (event.description.isNotBlank()) {
                                    Text(
                                        event.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (event.oldState.isNotBlank() || event.newState.isNotBlank()) {
                                    Text(
                                        "${event.oldState} → ${event.newState}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PledgeScreen(
    loanId: String,
    onSubmit: (assetDescription: String, assetType: String, estimatedValue: Double, weight: Double) -> Unit,
    onBack: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("GOLD") }
    var value by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    val assetTypes = listOf("GOLD", "PROPERTY", "VEHICLE", "EQUIPMENT", "OTHER")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_pledge), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.asset_collateral_details), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.asset_description)) },
                leadingIcon = { Icon(Icons.Default.Security, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Text(stringResource(R.string.asset_type), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                assetTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.estimated_value)) },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (selectedType == "GOLD") {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.weight_grams)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSubmit(
                        description,
                        selectedType,
                        value.toDoubleOrNull() ?: 0.0,
                        weight.toDoubleOrNull() ?: 0.0
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                enabled = description.isNotBlank() && value.isNotBlank()
            ) {
                Icon(Icons.Default.Security, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_pledge), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
