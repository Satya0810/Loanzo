package com.loanzo.app.ui.loan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.util.TranslationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val translationHelper: TranslationHelper
) : ViewModel() {
    private val _uiState = MutableStateFlow(TranslationState())
    val uiState: StateFlow<TranslationState> = _uiState.asStateFlow()

    fun translate(text: String, targetLang: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = translationHelper.translateText(text, targetLang)
            _uiState.update { it.copy(isLoading = false, translatedText = result ?: "Translation failed (API limit reached or network error).") }
        }
    }
}

data class TranslationState(
    val isLoading: Boolean = false,
    val translatedText: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationDemoScreen(
    onBack: () -> Unit,
    viewModel: TranslationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var targetLang by remember { mutableStateOf("hi") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Translation Demo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back") 
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Enter chat message or document text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = targetLang == "hi",
                    onClick = { targetLang = "hi" },
                    label = { Text("Hindi (hi)") }
                )
                FilterChip(
                    selected = targetLang == "mr",
                    onClick = { targetLang = "mr" },
                    label = { Text("Marathi (mr)") }
                )
            }
            
            Button(
                onClick = { viewModel.translate(inputText, targetLang) },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputText.isNotBlank() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Translate via LibreTranslate API")
                }
            }
            
            if (state.translatedText.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Result:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.translatedText)
                    }
                }
            }
        }
    }
}
