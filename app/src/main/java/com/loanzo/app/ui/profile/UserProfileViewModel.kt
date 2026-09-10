package com.loanzo.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.ComplaintEntity
import com.loanzo.app.data.model.UserProfileData
import com.loanzo.app.data.repository.AdminRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.util.TelegramManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class UserProfileUiState(
    val profileData: UserProfileData? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionMessage: String? = null,
    val showReportDialog: Boolean = false,
    val showBlockDialog: Boolean = false,
    val currentUserId: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private var observedUserId: String? = null

    init {
        viewModelScope.launch {
            userRepository.getCurrentUserId().collectLatest { uid ->
                _uiState.update { it.copy(currentUserId = uid) }
            }
        }
    }

    fun loadProfile(userId: String) {
        if (userId.isBlank()) return
        if (observedUserId == userId && _uiState.value.profileData != null) return
        observedUserId = userId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.observeUserProfileData(userId).collectLatest { data ->
                _uiState.update {
                    it.copy(
                        profileData = data,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setReportDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showReportDialog = visible) }
    }

    fun setBlockDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showBlockDialog = visible) }
    }

    fun toggleBlockUser() {
        val target = _uiState.value.profileData ?: return
        viewModelScope.launch {
            if (target.isBlocked) {
                userRepository.unblockUser(target.userId)
                _uiState.update { it.copy(actionMessage = "User unblocked successfully.", showBlockDialog = false) }
            } else {
                userRepository.blockUser(target.userId)
                _uiState.update { it.copy(actionMessage = "User has been blocked. You will not see their posts or offers.", showBlockDialog = false) }
            }
        }
    }

    fun submitReport(
        category: String,
        details: String,
        requestFreeze: Boolean = true
    ) {
        val target = _uiState.value.profileData ?: return
        val reporterId = _uiState.value.currentUserId ?: "ANONYMOUS"

        viewModelScope.launch {
            try {
                val reporter = if (reporterId != "ANONYMOUS") userRepository.getUserById(reporterId) else null
                val complaintId = "CMP-" + UUID.randomUUID().toString().take(8).uppercase()

                val complaint = ComplaintEntity(
                    complaintId = complaintId,
                    complainantId = reporterId,
                    complainantName = reporter?.name ?: "Verified Member",
                    complainantPhone = reporter?.phone ?: "+91 98000 00000",
                    complainantRole = reporter?.role ?: "BORROWER",
                    targetPartyId = target.userId,
                    targetPartyName = target.name,
                    category = category,
                    priority = "HIGH",
                    subject = if (requestFreeze) "Profile Restriction Requested" else "User Report: $category",
                    description = details.ifBlank { "User profile reported: $category" },
                    status = "OPEN",
                    loanId = null,
                    createdAt = System.currentTimeMillis()
                )

                adminRepository.submitComplaint(complaint)

                val alertMsg = """
                    🚨 <b>USER PROFILE REPORTED</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Case ID:</b> ${TelegramManager.escapeHtml(complaintId)}
                    <b>Target User:</b> ${TelegramManager.escapeHtml(target.name)} (@${TelegramManager.escapeHtml(target.username)})
                    <b>Target ID:</b> <code>${TelegramManager.escapeHtml(target.userId)}</code>
                    <b>Reporter:</b> ${TelegramManager.escapeHtml(reporter?.name ?: "Member")} (${TelegramManager.escapeHtml(reporterId)})
                    <b>Category:</b> ${TelegramManager.escapeHtml(category)}
                    <b>Remarks:</b> ${TelegramManager.escapeHtml(details.ifBlank { "N/A" })}
                    ━━━━━━━━━━━━━━━━━━━━
                    <i>Reported directly from User Profile Screen</i>
                """.trimIndent()

                try {
                    TelegramManager.instance.sendAdminAlert(alertMsg)
                } catch (_: Exception) {}

                _uiState.update {
                    it.copy(
                        actionMessage = "Report filed successfully (Case ID: $complaintId). Platform admins alerted.",
                        showReportDialog = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to submit report: ${e.localizedMessage}",
                        showReportDialog = false
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, actionMessage = null) }
    }
}
