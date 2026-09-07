package com.loanzo.app.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.util.DeviceSecurityHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.bankingSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "loanzo_banking_session_prefs")

enum class SessionState {
    UNAUTHENTICATED,
    ACTIVE,
    LOCKED,
    EXPIRED,
    UNTRUSTED_DEVICE
}

/**
 * Bank-Grade Institutional Session Lifecycle Manager.
 *
 * Implements standard fintech & banking security policies:
 * 1. Background Inactivity Auto-Lock: If the app is minimized for > 3 minutes,
 *    the session is non-destructively locked. Drafts and local state are preserved.
 * 2. Hard Session Expiration: If the app is inactive for > 2 days (48 hours),
 *    the session completely expires, requiring full login.
 * 3. Quick Unlock Re-Authentication: Non-destructive biometric / PIN unlock.
 * 4. Hardware Device Cryptographic Binding Validation.
 * 5. Foreground Touch Idle Tracking (5 minutes).
 */
@Singleton
class BankingSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_LAST_ACTIVE_TIME = longPreferencesKey("banking_session_last_active_time")
        val KEY_IS_SESSION_LOCKED = booleanPreferencesKey("banking_session_is_locked")
        val KEY_SESSION_USER_ID = stringPreferencesKey("banking_session_user_id")
        val KEY_SESSION_ROLE = stringPreferencesKey("banking_session_role")

        const val INACTIVITY_LOCK_TIMEOUT_MS = 3 * 60 * 1000L // 3 minutes in background -> Quick Lock
        const val HARD_EXPIRY_TIMEOUT_MS = 2 * 24 * 60 * 60 * 1000L // 2 days (48 hours) -> Hard Logout
        const val FOREGROUND_IDLE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes foreground idle -> Quick Lock
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessionState = MutableStateFlow(SessionState.UNAUTHENTICATED)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var backgroundTimestamp: Long = 0L
    private var lastUserTouchTimestamp: Long = System.currentTimeMillis()

    init {
        scope.launch {
            val prefs = context.bankingSessionDataStore.data.first()
            val isLocked = prefs[KEY_IS_SESSION_LOCKED] ?: false
            val lastActive = prefs[KEY_LAST_ACTIVE_TIME] ?: 0L
            val now = System.currentTimeMillis()

            if (lastActive > 0L && (now - lastActive) > HARD_EXPIRY_TIMEOUT_MS) {
                _sessionState.value = SessionState.EXPIRED
            } else if (isLocked) {
                _sessionState.value = SessionState.LOCKED
            }
        }
    }

    /**
     * Records app transition to background (e.g. user minimized app, switched apps).
     */
    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
        scope.launch {
            recordActivityTimestamp(backgroundTimestamp)
        }
    }

    /**
     * Evaluates session security when app returns to foreground.
     * Checks if background duration exceeded the 3-minute quick-lock threshold
     * or the 2-day hard-expiry threshold.
     */
    fun onAppForegrounded(isLoggedIn: Boolean) {
        if (!isLoggedIn) {
            _sessionState.value = SessionState.UNAUTHENTICATED
            return
        }

        val now = System.currentTimeMillis()
        val elapsedBackgroundTime = if (backgroundTimestamp > 0L) now - backgroundTimestamp else 0L

        if (elapsedBackgroundTime > HARD_EXPIRY_TIMEOUT_MS) {
            // Inactive > 2 days -> Hard Logout
            _sessionState.value = SessionState.EXPIRED
        } else if (elapsedBackgroundTime >= INACTIVITY_LOCK_TIMEOUT_MS) {
            // Inactive > 3 minutes -> Non-destructive Quick Lock
            lockSession()
        } else {
            // Quick app-switch (e.g. checking SMS OTP or phone call) -> Remain ACTIVE
            if (_sessionState.value != SessionState.LOCKED) {
                _sessionState.value = SessionState.ACTIVE
            }
        }
        backgroundTimestamp = 0L
        lastUserTouchTimestamp = now
    }

    /**
     * Resets foreground touch inactivity timer on user interaction.
     */
    fun onUserInteracted() {
        lastUserTouchTimestamp = System.currentTimeMillis()
    }

    /**
     * Checks if foreground idle duration exceeded 5 minutes.
     */
    fun checkForegroundIdleTimeout(isLoggedIn: Boolean): Boolean {
        if (!isLoggedIn) return false
        val now = System.currentTimeMillis()
        val idleElapsed = now - lastUserTouchTimestamp
        if (idleElapsed >= FOREGROUND_IDLE_TIMEOUT_MS && _sessionState.value == SessionState.ACTIVE) {
            lockSession()
            return true
        }
        return false
    }

    /**
     * Non-destructively locks the session (requires Biometric / PIN to resume).
     */
    fun lockSession() {
        _sessionState.value = SessionState.LOCKED
        scope.launch {
            context.bankingSessionDataStore.edit { prefs ->
                prefs[KEY_IS_SESSION_LOCKED] = true
            }
        }
    }

    /**
     * Unlocks the session upon successful Biometric or PIN authentication.
     */
    fun unlockSession() {
        val now = System.currentTimeMillis()
        lastUserTouchTimestamp = now
        _sessionState.value = SessionState.ACTIVE
        scope.launch {
            context.bankingSessionDataStore.edit { prefs ->
                prefs[KEY_IS_SESSION_LOCKED] = false
                prefs[KEY_LAST_ACTIVE_TIME] = now
            }
        }
    }

    /**
     * Validates that the device hardware cryptographic signature matches the registered device.
     */
    fun validateDeviceBinding(user: UserEntity): Boolean {
        val currentDevId = DeviceSecurityHelper.getHardwareDeviceId(context)
        val isMatched = DeviceSecurityHelper.isDeviceMatched(user.registeredDeviceId, currentDevId)
        if (!isMatched) {
            _sessionState.value = SessionState.UNTRUSTED_DEVICE
        }
        return isMatched
    }

    /**
     * Saves session credentials and resets active timestamps.
     */
    suspend fun saveSession(userId: String, role: String) {
        val now = System.currentTimeMillis()
        lastUserTouchTimestamp = now
        _sessionState.value = SessionState.ACTIVE
        context.bankingSessionDataStore.edit { prefs ->
            prefs[KEY_SESSION_USER_ID] = userId
            prefs[KEY_SESSION_ROLE] = role
            prefs[KEY_LAST_ACTIVE_TIME] = now
            prefs[KEY_IS_SESSION_LOCKED] = false
        }
    }

    /**
     * Hard logout clearing all session tokens and active states.
     */
    suspend fun clearSession() {
        _sessionState.value = SessionState.UNAUTHENTICATED
        backgroundTimestamp = 0L
        context.bankingSessionDataStore.edit { prefs ->
            prefs.remove(KEY_SESSION_USER_ID)
            prefs.remove(KEY_SESSION_ROLE)
            prefs.remove(KEY_LAST_ACTIVE_TIME)
            prefs[KEY_IS_SESSION_LOCKED] = false
        }
    }

    suspend fun recordActivityTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.bankingSessionDataStore.edit { prefs ->
            prefs[KEY_LAST_ACTIVE_TIME] = timestamp
        }
    }

    suspend fun getLastActiveTime(): Long {
        return context.bankingSessionDataStore.data.first()[KEY_LAST_ACTIVE_TIME] ?: 0L
    }

    suspend fun isSessionLocked(): Boolean {
        return context.bankingSessionDataStore.data.first()[KEY_IS_SESSION_LOCKED] ?: false
    }
}
