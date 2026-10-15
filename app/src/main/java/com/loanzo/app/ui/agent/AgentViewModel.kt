package com.loanzo.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.repository.AgentRepository
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable UI state for the Loanzo Field Operations cockpit.
 * All UI reads from this single source of truth via StateFlow.
 */
data class AgentUiState(
    val isLoading: Boolean = true,
    val user: UserEntity? = null,
    val isOnDuty: Boolean = true,
    val visits: List<AgentVisitEntity> = emptyList(),

    // Derived counters (pre-computed to avoid recomposition overhead)
    val completedVisitsCount: Int = 0,
    val pendingVisitsCount: Int = 0,
    val todayVisitsCount: Int = 0,
    val totalEarnings: Double = 0.0,

    // Sync & connectivity status
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0,
    val lastSyncTimestamp: Long? = null,

    // Active inspection state
    val activeInspectionVisit: AgentVisitEntity? = null,
    val showPayoutSuccessDialog: Double? = null,

    // Selected tab index for bottom navigation
    val selectedTab: Int = 0,

    // Error state
    val errorMessage: String? = null
)

/**
 * Sealed interface for user-driven intents dispatched to the ViewModel.
 * Follows the MVI unidirectional data flow pattern.
 */
sealed interface AgentIntent {
    data class ToggleDutyStatus(val isOnDuty: Boolean) : AgentIntent
    data class UpdateVisitStage(val visitId: String, val stage: String) : AgentIntent
    data class StartInspection(val visit: AgentVisitEntity) : AgentIntent
    data object DismissInspection : AgentIntent
    data class CompleteVisit(
        val visitId: String,
        val remarks: String,
        val isCollateralAuthentic: Boolean,
        val isBorrowerVerified: Boolean,
        val isLenderVerified: Boolean,
        val photoProof: String,
        val appraisedValue: Double?,
        val officerRecommendation: String?
    ) : AgentIntent
    data class CompleteVisitWithGps(
        val visitId: String,
        val remarks: String,
        val isCollateralAuthentic: Boolean,
        val isBorrowerVerified: Boolean,
        val isLenderVerified: Boolean,
        val photoProof: String,
        val appraisedValue: Double?,
        val officerRecommendation: String?,
        val latitude: Double?,
        val longitude: Double?
    ) : AgentIntent
    data class DismissPayoutDialog(val amount: Double?) : AgentIntent
    data class SelectTab(val index: Int) : AgentIntent
    data object DismissError : AgentIntent
}

/**
 * AgentViewModel — Central state orchestrator for the Field Operations cockpit.
 *
 * Architecture: Strict MVI/MVVM hybrid.
 * - Single immutable [AgentUiState] exposed as [StateFlow].
 * - All mutations dispatched through [AgentIntent] sealed interface.
 * - Data layer interactions delegated to [AgentRepository] and [UserRepository].
 * - Coroutine-scoped to [viewModelScope] with IO dispatcher for database operations.
 *
 * Benchmark references: Rupeek Valuer App (mission state machine), Bajaj Finserv
 * Direct FI (field investigation), Salesforce Field Service Mobile (deterministic
 * stage progression).
 */
@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init {
        initializeAgent()
    }

    /**
     * Bootstrap the agent session: resolve user identity, begin observing
     * user entity and assigned visits reactively.
     */
    private fun initializeAgent() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = userRepository.getCurrentUserIdSync()
            if (uid.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // Seed cached user instantly for fast first-frame render
            val cachedUser = userRepository.getUserById(uid)
            if (cachedUser != null) {
                _uiState.update {
                    it.copy(
                        user = cachedUser,
                        isOnDuty = cachedUser.isOnDuty,
                        totalEarnings = cachedUser.totalAgentEarnings
                    )
                }
            }

            // Trigger cloud sync for freshest data
            userRepository.syncUserById(uid, forceCloud = true)

            // Observe user entity reactively
            launch {
                userRepository.observeUser(uid).collect { user ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            isOnDuty = user?.isOnDuty ?: it.isOnDuty,
                            totalEarnings = user?.totalAgentEarnings ?: it.totalEarnings
                        )
                    }
                }
            }

            // Observe assigned visits reactively
            launch {
                agentRepository.getVisitsForAgent(uid).collect { visits ->
                    val completed = visits.count { v -> v.status == "COMPLETED" }
                    val pending = visits.count { v -> v.status != "COMPLETED" }
                    val today = visits.count { v ->
                        v.scheduledDate.equals("Today", ignoreCase = true)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            visits = visits,
                            completedVisitsCount = completed,
                            pendingVisitsCount = pending,
                            todayVisitsCount = today
                        )
                    }
                }
            }
        }
    }

    /**
     * Primary intent dispatcher. All UI interactions route through here.
     */
    fun dispatch(intent: AgentIntent) {
        when (intent) {
            is AgentIntent.ToggleDutyStatus -> handleToggleDuty(intent.isOnDuty)
            is AgentIntent.UpdateVisitStage -> handleUpdateVisitStage(intent.visitId, intent.stage)
            is AgentIntent.StartInspection -> handleStartInspection(intent.visit)
            is AgentIntent.DismissInspection -> handleDismissInspection()
            is AgentIntent.CompleteVisit -> handleCompleteVisit(intent)
            is AgentIntent.CompleteVisitWithGps -> handleCompleteVisitWithGps(intent)
            is AgentIntent.DismissPayoutDialog -> handleDismissPayoutDialog()
            is AgentIntent.SelectTab -> handleSelectTab(intent.index)
            is AgentIntent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    // ── Intent Handlers ───────────────────────────────────────────────

    private fun handleToggleDuty(isOnDuty: Boolean) {
        val userId = _uiState.value.user?.userId ?: return
        _uiState.update { it.copy(isOnDuty = isOnDuty) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                agentRepository.setDutyStatus(userId, isOnDuty)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update duty status: ${e.message}") }
            }
        }
    }

    private fun handleUpdateVisitStage(visitId: String, stage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                agentRepository.updateVisitStage(visitId, stage)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update visit stage: ${e.message}") }
            }
        }
    }

    private fun handleStartInspection(visit: AgentVisitEntity) {
        _uiState.update { it.copy(activeInspectionVisit = visit) }
    }

    private fun handleDismissInspection() {
        _uiState.update { it.copy(activeInspectionVisit = null) }
    }

    private fun handleCompleteVisit(intent: AgentIntent.CompleteVisit) {
        val payout = _uiState.value.activeInspectionVisit?.payoutAmount ?: 0.0
        _uiState.update {
            it.copy(
                activeInspectionVisit = null,
                showPayoutSuccessDialog = payout
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                agentRepository.completeVisit(
                    visitId = intent.visitId,
                    agentRemarks = intent.remarks,
                    isCollateralAuthentic = intent.isCollateralAuthentic,
                    isBorrowerIdentityVerified = intent.isBorrowerVerified,
                    isLenderIdentityVerified = intent.isLenderVerified,
                    proofPhotoUris = intent.photoProof,
                    appraisedValue = intent.appraisedValue,
                    officerRecommendation = intent.officerRecommendation
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to submit attestation: ${e.message}") }
            }
        }
    }

    private fun handleCompleteVisitWithGps(intent: AgentIntent.CompleteVisitWithGps) {
        val payout = _uiState.value.activeInspectionVisit?.payoutAmount ?: 0.0
        _uiState.update {
            it.copy(
                activeInspectionVisit = null,
                showPayoutSuccessDialog = payout
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                agentRepository.completeVisit(
                    visitId = intent.visitId,
                    agentRemarks = intent.remarks,
                    isCollateralAuthentic = intent.isCollateralAuthentic,
                    isBorrowerIdentityVerified = intent.isBorrowerVerified,
                    isLenderIdentityVerified = intent.isLenderVerified,
                    proofPhotoUris = intent.photoProof,
                    appraisedValue = intent.appraisedValue,
                    officerRecommendation = intent.officerRecommendation,
                    agentLatitude = intent.latitude,
                    agentLongitude = intent.longitude
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to submit attestation: ${e.message}") }
            }
        }
    }

    private fun handleDismissPayoutDialog() {
        _uiState.update { it.copy(showPayoutSuccessDialog = null) }
    }

    private fun handleSelectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}
