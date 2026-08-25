package com.loanzo.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.entity.UserEntity
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
    @ApplicationContext private val context: Context
) {
    companion object {
        private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
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
            prefs.clear()
        }
    }

    fun getCurrentUserId(): Flow<String?> = context.dataStore.data.map { it[CURRENT_USER_ID] }
    fun isLoggedIn(): Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    fun getCurrentRole(): Flow<String?> = context.dataStore.data.map { it[USER_ROLE] }

    suspend fun getCurrentUserIdSync(): String? =
        context.dataStore.data.first()[CURRENT_USER_ID]

    // User CRUD
    suspend fun createUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId)
    fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUser(userId)
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)
    fun getUsersByRole(role: String): Flow<List<UserEntity>> = userDao.getUsersByRole(role)
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    // Theme preference
    fun getThemeMode(): Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "SYSTEM" }
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }
}
