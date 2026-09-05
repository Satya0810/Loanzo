package com.loanzo.app.ui.loan

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
            _uiState.update { it.copy(isLoading = false, translatedText = result ?: "Translation failed.") }
        }
    }
}

data class TranslationState(
    val isLoading: Boolean = false,
    val translatedText: String = ""
)
