package com.loanzo.app.ui.loan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.Navy900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    loanId: String,
    onBack: () -> Unit,
    viewModel: TranslationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    val originalText = """
        LOAN AGREEMENT TERMS AND CONDITIONS
        
        1. REPAYMENT: The Borrower agrees to repay the Principal Amount along with applicable interest in the schedule provided.
        2. DEFAULT: If the Borrower fails to make any payment on time, the Lender reserves the right to declare the entire outstanding balance immediately due.
        3. PLEDGES: The Borrower has provided the listed assets as collateral. In the event of default, the Lender may liquidate these assets.
        4. JURISDICTION: This agreement is subject to the local lending laws and RBI guidelines.
    """.trimIndent()
    
    var isTranslated by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Agreement") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("< Back") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isTranslated && !state.isLoading) {
                        viewModel.translate(originalText, "hi")
                    } else if (isTranslated) {
                        isTranslated = false
                    }
                },
                containerColor = Gold500,
                contentColor = Navy900,
                icon = { 
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Navy900)
                    else Icon(Icons.Default.Translate, contentDescription = null)
                },
                text = { Text(if (isTranslated) "Show Original" else "Translate to Hindi") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTranslated && state.translatedText.isNotBlank()) "Translated Document (Hindi)" else "Original Document (English)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isTranslated && state.translatedText.isNotBlank()) state.translatedText else originalText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
    
    LaunchedEffect(state.translatedText) {
        if (state.translatedText.isNotBlank() && !state.isLoading) {
            isTranslated = true
        }
    }
}
