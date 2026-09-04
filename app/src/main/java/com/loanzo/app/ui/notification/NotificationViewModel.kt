package com.loanzo.app.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.NotificationEntity
import com.loanzo.app.data.repository.NotificationRepository
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotificationFilter { ALL, DEADLINES, OVERDUE, UNREAD }
enum class DateRangeFilter { ALL_TIME, TODAY, THIS_WEEK, THIS_MONTH }

data class NotificationUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val rawNotifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val selectedCategoryTag: String? = null,
    val selectedDateFilter: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val searchQuery: String = "",
    val isLoading: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (searchQuery.isNotBlank()) count++
            if (selectedFilter != NotificationFilter.ALL) count++
            if (selectedCategoryTag != null) count++
            if (selectedDateFilter != DateRangeFilter.ALL_TIME) count++
            return count
        }
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = userRepository.getCurrentUserIdSync()
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, notifications = emptyList(), rawNotifications = emptyList()) }
                return@launch
            }

            // Scan for new deadline notifications
            notificationRepository.scanAndGenerateDeadlineNotifications(userId)

            // Observe notifications
            notificationRepository.getNotifications(userId).collect { notifs ->
                _uiState.update { state ->
                    val filtered = applyFilter(
                        notifs,
                        state.selectedFilter,
                        state.selectedCategoryTag,
                        state.selectedDateFilter,
                        state.searchQuery
                    )
                    state.copy(
                        rawNotifications = notifs,
                        notifications = filtered,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            notificationRepository.getUnreadCount(userId).collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun filter(filter: NotificationFilter) {
        _uiState.update { state ->
            val updated = state.copy(selectedFilter = filter)
            updated.copy(
                notifications = applyFilter(
                    state.rawNotifications,
                    filter,
                    updated.selectedCategoryTag,
                    updated.selectedDateFilter,
                    updated.searchQuery
                )
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = query)
            updated.copy(
                notifications = applyFilter(
                    state.rawNotifications,
                    updated.selectedFilter,
                    updated.selectedCategoryTag,
                    updated.selectedDateFilter,
                    query
                )
            )
        }
    }

    fun setDateFilter(dateFilter: DateRangeFilter) {
        _uiState.update { state ->
            val updated = state.copy(selectedDateFilter = dateFilter)
            updated.copy(
                notifications = applyFilter(
                    state.rawNotifications,
                    updated.selectedFilter,
                    updated.selectedCategoryTag,
                    dateFilter,
                    updated.searchQuery
                )
            )
        }
    }

    fun setCategoryTag(tag: String?) {
        _uiState.update { state ->
            val newTag = if (state.selectedCategoryTag == tag) null else tag
            val updated = state.copy(selectedCategoryTag = newTag)
            updated.copy(
                notifications = applyFilter(
                    state.rawNotifications,
                    updated.selectedFilter,
                    newTag,
                    updated.selectedDateFilter,
                    updated.searchQuery
                )
            )
        }
    }

    fun clearAllFilters() {
        _uiState.update { state ->
            state.copy(
                searchQuery = "",
                selectedFilter = NotificationFilter.ALL,
                selectedCategoryTag = null,
                selectedDateFilter = DateRangeFilter.ALL_TIME,
                notifications = state.rawNotifications
            )
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            notificationRepository.markAllAsRead(userId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            notificationRepository.clearAll(userId)
        }
    }

    fun refreshDeadlines() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            notificationRepository.scanAndGenerateDeadlineNotifications(userId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun applyFilter(
        notifications: List<NotificationEntity>,
        filter: NotificationFilter,
        categoryTag: String?,
        dateFilter: DateRangeFilter,
        query: String
    ): List<NotificationEntity> {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val sevenDaysMs = 7 * oneDayMs
        val thirtyDaysMs = 30 * oneDayMs

        return notifications.filter { item ->
            // 1. Primary Filter
            val matchesFilter = when (filter) {
                NotificationFilter.ALL -> true
                NotificationFilter.DEADLINES -> item.type == "DEADLINE"
                NotificationFilter.OVERDUE -> item.type == "OVERDUE"
                NotificationFilter.UNREAD -> !item.isRead
            }

            // 2. Category Tag Filter
            val matchesCategory = if (categoryTag.isNullOrBlank()) true else {
                when (categoryTag.uppercase()) {
                    "ACTIONS" -> item.type in listOf("OVERDUE", "DISBURSEMENT_PENDING", "AGREEMENT_READY", "ACTION_REQUIRED")
                    "PAYMENTS" -> item.type in listOf("REPAYMENT_RECEIVED", "REPAYMENT_SUCCESS", "DISBURSED", "PAYMENT")
                    "DEADLINES" -> item.type in listOf("DEADLINE", "DUE_SOON", "OVERDUE")
                    "AGREEMENTS" -> item.type in listOf("AGREEMENT_READY", "AGREEMENT_SIGNED", "KYC_VERIFIED", "LEGAL")
                    else -> item.type.contains(categoryTag, ignoreCase = true) || item.title.contains(categoryTag, ignoreCase = true)
                }
            }

            // 3. Date Range Filter
            val matchesDate = when (dateFilter) {
                DateRangeFilter.ALL_TIME -> true
                DateRangeFilter.TODAY -> (now - item.timestamp) <= oneDayMs
                DateRangeFilter.THIS_WEEK -> (now - item.timestamp) <= sevenDaysMs
                DateRangeFilter.THIS_MONTH -> (now - item.timestamp) <= thirtyDaysMs
            }

            // 4. Search Query Filter
            val matchesQuery = if (query.isBlank()) true else {
                val q = query.trim().lowercase()
                item.title.lowercase().contains(q) ||
                item.message.lowercase().contains(q) ||
                (item.relatedLoanId?.lowercase()?.contains(q) == true) ||
                item.type.lowercase().contains(q)
            }

            matchesFilter && matchesCategory && matchesDate && matchesQuery
        }
    }
}
