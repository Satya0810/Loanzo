package com.loanzo.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.sync.AppSyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loanzo_prefs")

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val appSyncManager: AppSyncManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
        private val DIGILOCKER_SESSION_ID = stringPreferencesKey("digilocker_session_id")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val BIOMETRIC_USER_ID = stringPreferencesKey("biometric_user_id")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")

        // ─── Onboarding & Guided Tour keys ─────────────────────────────────────
        val ONBOARDING_WELCOME_COMPLETED = booleanPreferencesKey("onboarding_welcome_completed")
        val GUIDE_DASHBOARD_SEEN         = booleanPreferencesKey("guide_dashboard_seen")
        val GUIDE_LOANS_SEEN             = booleanPreferencesKey("guide_loans_seen")
        val GUIDE_PROFILE_SEEN           = booleanPreferencesKey("guide_profile_seen")
        val GUIDE_MARKETPLACE_SEEN       = booleanPreferencesKey("guide_marketplace_seen")
        val GUIDE_POST_BUTTON_SEEN       = booleanPreferencesKey("guide_post_button_seen")
        val NAV_TOOLTIPS_SEEN            = booleanPreferencesKey("nav_tooltips_seen")
        val ACTIVE_TOUR_ID               = stringPreferencesKey("active_tour_id")
        val ACTIVE_TOUR_STEP             = intPreferencesKey("active_tour_step")
    }

    suspend fun saveBiometricEnrollment(userId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_USER_ID] = userId
            prefs[BIOMETRIC_ENABLED] = enabled
        }
    }

    fun isBiometricEnabled(): Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    fun getBiometricUserId(): Flow<String?> = context.dataStore.data.map { it[BIOMETRIC_USER_ID] }
    suspend fun getBiometricUserIdSync(): String? = context.dataStore.data.first()[BIOMETRIC_USER_ID]
    suspend fun isBiometricEnabledSync(): Boolean = context.dataStore.data.first()[BIOMETRIC_ENABLED] ?: false

    suspend fun saveDigiLockerSessionId(sessionId: String) {
        context.dataStore.edit { prefs -> prefs[DIGILOCKER_SESSION_ID] = sessionId }
    }

    suspend fun getDigiLockerSessionIdSync(): String? =
        context.dataStore.data.first()[DIGILOCKER_SESSION_ID]

    suspend fun clearDigiLockerSessionId() {
        context.dataStore.edit { prefs -> prefs.remove(DIGILOCKER_SESSION_ID) }
    }

    // Session management
    suspend fun saveSession(userId: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_USER_ID] = userId
            prefs[IS_LOGGED_IN] = true
            prefs[USER_ROLE] = role
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(CURRENT_USER_ID)
            prefs[IS_LOGGED_IN] = false
            prefs.remove(USER_ROLE)
        }
    }

    fun getCurrentUserId(): Flow<String?> = context.dataStore.data.map { it[CURRENT_USER_ID] }
    fun isLoggedIn(): Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    fun getCurrentRole(): Flow<String?> = context.dataStore.data.map { it[USER_ROLE] }

    suspend fun getCurrentUserIdSync(): String? =
        context.dataStore.data.first()[CURRENT_USER_ID]

    // User CRUD — offline-first with automatic background sync
    suspend fun createUser(user: UserEntity) {
        userDao.insertUser(user)
        try { appSyncManager.enqueueUserSync(user, "CREATE") } catch (e: Exception) {
            Log.w("UserRepository", "Sync enqueue failed (will retry): ${e.message}")
        }
    }
    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
        try { appSyncManager.enqueueUserSync(user, "UPDATE") } catch (e: Exception) {
            Log.w("UserRepository", "Sync enqueue failed (will retry): ${e.message}")
        }
    }
    suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId)
    fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUser(userId)
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)
    suspend fun getUserByUsername(username: String): UserEntity? = userDao.getUserByUsername(username)
    fun getUsersByRole(role: String): Flow<List<UserEntity>> = userDao.getUsersByRole(role)
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    fun searchUsers(query: String): Flow<List<UserEntity>> = userDao.searchUsers(query)
    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    // Theme preference — Light Theme is the signature system default
    fun getThemeMode(): Flow<String> = context.dataStore.data.map {
        val mode = it[THEME_MODE]
        if (mode == null || mode == "SYSTEM") "LIGHT" else mode
    }
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    // App Language preference
    fun getAppLanguage(): Flow<String> = context.dataStore.data.map { it[APP_LANGUAGE] ?: "en" }
    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { prefs -> prefs[APP_LANGUAGE] = languageCode }
    }

    // ─── Onboarding & Guided Tour state ────────────────────────────────────────

    /** Welcome carousel — shown once on first login */
    fun isWelcomeOnboardingCompleted(): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_WELCOME_COMPLETED] ?: false }
    suspend fun setWelcomeOnboardingCompleted() {
        context.dataStore.edit { prefs -> prefs[ONBOARDING_WELCOME_COMPLETED] = true }
    }

    /** Nav tooltip blinking — clears once all nav items tapped */
    fun areNavTooltipsSeen(): Flow<Boolean> =
        context.dataStore.data.map { it[NAV_TOOLTIPS_SEEN] ?: false }
    suspend fun setNavTooltipsSeen() {
        context.dataStore.edit { prefs -> prefs[NAV_TOOLTIPS_SEEN] = true }
    }

    /** Per-screen contextual guide cards */
    fun isGuideSeen(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>): Flow<Boolean> =
        context.dataStore.data.map { it[key] ?: false }
    suspend fun markGuideSeen(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>) {
        context.dataStore.edit { prefs -> prefs[key] = true }
    }

    /** Active guided tour session — persisted so tour survives app restart */
    fun getActiveTourId(): Flow<String?> =
        context.dataStore.data.map { it[ACTIVE_TOUR_ID] }
    fun getActiveTourStep(): Flow<Int> =
        context.dataStore.data.map { it[ACTIVE_TOUR_STEP] ?: 0 }
    suspend fun setActiveTour(tourId: String, step: Int = 0) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_TOUR_ID] = tourId
            prefs[ACTIVE_TOUR_STEP] = step
        }
    }
    suspend fun advanceTourStep(step: Int) {
        context.dataStore.edit { prefs -> prefs[ACTIVE_TOUR_STEP] = step }
    }
    suspend fun clearActiveTour() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACTIVE_TOUR_ID)
            prefs.remove(ACTIVE_TOUR_STEP)
        }
    }
}
