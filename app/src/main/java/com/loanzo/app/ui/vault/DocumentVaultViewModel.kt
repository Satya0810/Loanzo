package com.loanzo.app.ui.vault

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.VaultDocumentEntity
import com.loanzo.app.data.repository.DocumentVaultRepository
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentVaultUiState(
    val documents: List<VaultDocumentEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class DocumentVaultViewModel @Inject constructor(
    private val vaultRepository: DocumentVaultRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentVaultUiState())
    val uiState: StateFlow<DocumentVaultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUserId().filterNotNull().collectLatest { uid ->
                vaultRepository.getDocumentsForUser(uid).collectLatest { docs ->
                    _uiState.update { it.copy(documents = docs) }
                }
            }
        }
    }

    fun generateMasterDossier(userId: String) {
        if (_uiState.value.isGenerating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = "Generating Master Dossier with official Loanzo branding...") }
            val doc = vaultRepository.generateAndVaultMasterDossier(userId)
            if (doc != null) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        message = "Master Financial & Legal Dossier generated and secured in your vault!",
                        isSuccess = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        message = "Failed to generate document dossier. Please try again.",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun openDocument(context: Context, document: VaultDocumentEntity) {
        vaultRepository.openDocument(context, document)
    }

    fun shareDocument(context: Context, document: VaultDocumentEntity) {
        vaultRepository.shareDocument(context, document)
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            vaultRepository.deleteDocument(documentId)
            _uiState.update { it.copy(message = "Document removed from vault.", isSuccess = true) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
