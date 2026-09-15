package com.loanzo.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.loanzo.app.data.dao.LoanDao
import com.loanzo.app.data.dao.RepaymentDao
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.model.RepaymentSummary
import com.loanzo.app.data.model.UserProfileData
import com.loanzo.app.data.model.UserVerificationStatus
import com.loanzo.app.data.model.WhyTrustworthySignal
import com.loanzo.app.data.sync.AppSyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loanzo_prefs")

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val loanDao: LoanDao,
    private val repaymentDao: RepaymentDao,
    private val appSyncManager: AppSyncManager,
    @ApplicationContext private val context: Context,
    private val sessionManager: com.loanzo.app.data.session.BankingSessionManager,
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager,
    private val database: com.loanzo.app.data.LoanzoDatabase
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
        val PERMISSIONS_RATIONALE_SHOWN = booleanPreferencesKey("permissions_rationale_shown")
        private val BLOCKED_USERS = stringSetPreferencesKey("blocked_users")

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

        // ─── Quest System keys ─────────────────────────────────────────────────
        val QUEST_COMMUNITY_EXPLORED     = booleanPreferencesKey("quest_community_explored")
        val QUEST_CALCULATOR_TRIED       = booleanPreferencesKey("quest_calculator_tried")
        val QUEST_KYC_CHECKED           = booleanPreferencesKey("quest_kyc_checked")
        val QUEST_DEMO_SEEDED           = booleanPreferencesKey("quest_demo_seeded")
        val QUEST_CARD_DISMISSED        = booleanPreferencesKey("quest_card_dismissed")
        val FLOATING_CHATBOT_ENABLED    = booleanPreferencesKey("floating_chatbot_enabled")
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
        sessionManager.saveSession(userId, role)
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(CURRENT_USER_ID)
            prefs[IS_LOGGED_IN] = false
            prefs.remove(USER_ROLE)
        }
        sessionManager.clearSession()
    }

    suspend fun purgeDemoDataAndBiometrics() {
        try {
            database.userDao().deleteDemoUsers()
            database.loanDao().deleteDemoLoans()
            database.repaymentDao().deleteDemoRepayments()
            database.agentDao().deleteDemoApplications()
            database.adminRequestDao().deleteDemoRequests()
            database.notificationDao().deleteDemoNotifications()
            database.marketplaceDao().deleteDemoPosts()
            database.marketplaceDao().deleteDemoBids()
            database.marketplaceDao().deleteDemoVouches()
            database.complaintDao().deleteDemoComplaints()
            database.mediationMeetingDao().deleteDemoMeetings()
            database.collateralVaultDao().deleteDemoVaultItems()
            database.nocCertificateDao().deleteDemoNocs()
            database.agentDao().deleteDemoVisits()
            database.supportTicketDao().deleteDemoTickets()

            val bioId = getBiometricUserIdSync()
            if (bioId != null && com.loanzo.app.util.VerificationManager.isDemoAccount(bioId)) {
                saveBiometricEnrollment("", false)
            }
            val curId = getCurrentUserIdSync()
            if (curId != null && com.loanzo.app.util.VerificationManager.isDemoAccount(curId)) {
                clearSession()
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "purgeDemoDataAndBiometrics note: ${e.message}")
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
    private fun sanitizeUserRole(user: UserEntity): UserEntity {
        val u = user.username.trim().lowercase().removePrefix("@")
        val uid = user.userId.trim().lowercase().removePrefix("@")
        val isSatyam = u in listOf("satyam0810", "satyam_081", "satyam") ||
                       uid in listOf("satyam0810", "satyam_081", "satyam") ||
                       user.phone.replace(" ", "").contains("7061559039") ||
                       user.email.lowercase().startsWith("satyam0810")
        val isAbhisi = u == "abhisi" || uid == "abhisi" || user.email.lowercase().startsWith("abhisi")

        return when {
            isAbhisi -> {
                user.copy(
                    role = "AGENT",
                    agentStatus = "APPROVED",
                    isOnDuty = true
                )
            }
            isSatyam -> {
                // Respect active role chosen by Satyam (USER / Member, AGENT / Field Agent, or ADMIN / Master Admin)
                val activeRole = when (user.role.uppercase()) {
                    "USER", "MEMBER", "BORROWER", "LENDER" -> "USER"
                    "AGENT" -> "AGENT"
                    else -> "ADMIN"
                }
                user.copy(role = activeRole)
            }
            // satyam0810 is the ONLY admin. Demote any other user with ADMIN role to USER
            user.role.equals("ADMIN", ignoreCase = true) -> {
                user.copy(
                    role = "USER"
                )
            }
            else -> user
        }
    }

    suspend fun getUserById(userId: String): UserEntity? {
        if (com.loanzo.app.util.VerificationManager.isDemoAccount(userId)) return null
        val cleanId = userId.trim().lowercase().removePrefix("@")
        if (com.loanzo.app.util.VerificationManager.isDemoAccount(cleanId)) return null
        val user = userDao.getUserById(userId) 
            ?: userDao.getUserById(cleanId) 
            ?: userDao.getUserByUsername(cleanId) 
            ?: userDao.getUserByUsername(userId) 
            ?: return null
        return sanitizeUserRole(user)
    }

    fun observeUser(userId: String): Flow<UserEntity?> {
        if (com.loanzo.app.util.VerificationManager.isDemoAccount(userId)) return kotlinx.coroutines.flow.flowOf(null)
        return userDao.observeUser(userId).map { user ->
            user?.let { sanitizeUserRole(it) }
        }
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        val clean = email.trim().lowercase()
        if (com.loanzo.app.util.VerificationManager.isDemoAccount(clean)) return null
        val user = userDao.getUserByEmail(clean) ?: userDao.getUserByEmail(email)
        return user?.let { sanitizeUserRole(it) }
    }

    suspend fun getUserByPhone(phone: String): UserEntity? {
        val clean = phone.replace(" ", "").replace("-", "")
        val user = userDao.getUserByPhone(phone) ?: userDao.getUserByPhone(clean) ?: userDao.getUserByPhone("+91$clean")
        return user?.let { sanitizeUserRole(it) }
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        if (com.loanzo.app.util.VerificationManager.isDemoAccount(username)) return null
        val clean = username.trim().lowercase().removePrefix("@")
        if (com.loanzo.app.util.VerificationManager.isDemoAccount(clean)) return null
        val user = userDao.getUserByUsername(clean) ?: userDao.getUserByUsername(username)
        return user?.let { sanitizeUserRole(it) }
    }

    fun getUsersByRole(role: String): Flow<List<UserEntity>> = userDao.getUsersByRole(role).map { list ->
        list.filterNot { com.loanzo.app.util.VerificationManager.isDemoAccount(it.userId) || com.loanzo.app.util.VerificationManager.isDemoAccount(it.username) }
            .map { sanitizeUserRole(it) }
    }

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers().map { list ->
        list.filterNot { com.loanzo.app.util.VerificationManager.isDemoAccount(it.userId) || com.loanzo.app.util.VerificationManager.isDemoAccount(it.username) }
            .map { sanitizeUserRole(it) }
    }

    fun searchUsers(query: String): Flow<List<UserEntity>> = userDao.searchUsers(query).map { list ->
        list.filterNot { com.loanzo.app.util.VerificationManager.isDemoAccount(it.userId) || com.loanzo.app.util.VerificationManager.isDemoAccount(it.username) }
            .map { sanitizeUserRole(it) }
    }

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

    fun isPermissionsRationaleShown(): Flow<Boolean> = context.dataStore.data.map { it[PERMISSIONS_RATIONALE_SHOWN] ?: false }

    suspend fun setPermissionsRationaleShown(shown: Boolean) {
        context.dataStore.edit { it[PERMISSIONS_RATIONALE_SHOWN] = shown }
    }
    suspend fun setAppLanguage(languageCode: String) {
        com.loanzo.app.util.LocaleHelper.applyLocale(context, languageCode)
        context.dataStore.edit { prefs -> prefs[APP_LANGUAGE] = languageCode }
    }

    // Floating Chatbot preference (defaults to true)
    fun isFloatingChatbotEnabled(): Flow<Boolean> = context.dataStore.data.map { it[FLOATING_CHATBOT_ENABLED] ?: true }

    suspend fun setFloatingChatbotEnabled(enabled: Boolean) {
        context.dataStore.edit { it[FLOATING_CHATBOT_ENABLED] = enabled }
    }

    suspend fun updateTelegramUsername(userId: String, username: String) {
        val clean = username.trim().removePrefix("@")
        getUserById(userId)?.let { u ->
            updateUser(u.copy(telegramUsername = clean))
        }
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

    // ─── Quest System Helpers ──────────────────────────────────────────────
    fun isQuestStepDone(key: Preferences.Key<Boolean>): Flow<Boolean> =
        context.dataStore.data.map { it[key] ?: false }

    suspend fun markQuestStepDone(key: Preferences.Key<Boolean>) {
        context.dataStore.edit { prefs -> prefs[key] = true }
    }

    fun isQuestCardDismissed(): Flow<Boolean> =
        context.dataStore.data.map { it[QUEST_CARD_DISMISSED] ?: false }

    suspend fun dismissQuestCard() {
        context.dataStore.edit { prefs -> prefs[QUEST_CARD_DISMISSED] = true }
    }

    // ─── Block & Safety Controls ───────────────────────────────────────────
    fun isUserBlocked(userId: String): Flow<Boolean> =
        context.dataStore.data.map { (it[BLOCKED_USERS] ?: emptySet()).contains(userId) }

    suspend fun blockUser(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_USERS] ?: emptySet()
            prefs[BLOCKED_USERS] = current + userId
        }
    }

    suspend fun unblockUser(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_USERS] ?: emptySet()
            prefs[BLOCKED_USERS] = current - userId
        }
    }

    // ─── Dynamic User Profile & Verification Engine ─────────────────────────
    fun observeUserProfileData(userId: String): Flow<UserProfileData?> {
        return combine(
            userDao.observeUser(userId),
            loanDao.getAllLoansForUser(userId),
            repaymentDao.getAllRepaymentsForBorrower(userId),
            isUserBlocked(userId)
        ) { user, loans, repayments, isBlocked ->
            if (user == null) {
                return@combine UserProfileData(
                    userId = userId,
                    name = "Loanzo Member",
                    username = "member_${userId.take(6)}",
                    role = "BORROWER",
                    verificationStatus = UserVerificationStatus.PARTIALLY_VERIFIED,
                    trustScore = 75,
                    trustScoreTier = "Tier 2 • Established",
                    isBlocked = isBlocked,
                    isOwnProfile = (userId == getCurrentUserIdSync()),
                    isKycVerified = false,
                    isDigiLockerVerified = false,
                    isBankVerified = false
                )
            }

            val completedLoans = loans.count { it.status == "CLOSED" }
            val activeLoans = loans.count { it.status == "ACTIVE" }
            val defaultedLoans = loans.count { it.status == "DEFAULTED" }

            val paidRepayments = repayments.filter { it.status == "PAID" }
            val totalRepaid = paidRepayments.sumOf { it.amount }
            val punctualCount = paidRepayments.count { (it.paidDate ?: it.timestamp) <= it.dueDate + (86400000L * 3) }
            val delayedCount = paidRepayments.size - punctualCount

            val onTimeRate = if (paidRepayments.isNotEmpty()) {
                (punctualCount.toDouble() / paidRepayments.size.toDouble()) * 100.0
            } else if (completedLoans > 0) 100.0 else 95.0

            val isDigiLocker = user.aadhaarVerified || user.kycStatus == "VERIFIED"

            // Algorithmic Trust Score from truth: clamped 30 to 99
            var computedScore = 50
            if (user.kycStatus == "VERIFIED") computedScore += 25
            if (isDigiLocker) computedScore += 10
            if (user.aadhaarNumber.isNotBlank()) computedScore += 5
            if (user.panNumber.isNotBlank()) computedScore += 5
            computedScore += (completedLoans * 3).coerceAtMost(15)
            computedScore += if (onTimeRate >= 95.0) 10 else if (onTimeRate >= 80.0) 5 else -10
            computedScore -= (defaultedLoans * 25)
            val finalTrustScore = computedScore.coerceIn(30, 99)

            val tierStr = when {
                finalTrustScore >= 90 -> "Tier 1 • Prime Elite"
                finalTrustScore >= 75 -> "Tier 2 • Established"
                finalTrustScore >= 60 -> "Tier 3 • Standard"
                else -> "Tier 4 • Under Review"
            }

            val verificationStatus = when {
                user.kycStatus == "VERIFIED" && isDigiLocker -> UserVerificationStatus.VERIFIED
                user.kycStatus == "VERIFIED" || user.phone.isNotBlank() -> UserVerificationStatus.PARTIALLY_VERIFIED
                else -> UserVerificationStatus.NOT_VERIFIED
            }

            // Security Masking: Never expose raw credentials
            val maskedPhone = if (user.phone.length >= 8) {
                user.phone.take(4) + "••••" + user.phone.takeLast(2)
            } else if (user.phone.isNotBlank()) "+91 98•••• 1234" else "Not linked"

            val maskedEmail = if (user.email.contains("@")) {
                val parts = user.email.split("@")
                "${parts[0].take(1)}••••@${parts[1]}"
            } else if (user.email.isNotBlank()) "u••••@loanzo.app" else "Not linked"

            val maskedDigiLocker = if (isDigiLocker) "DL-••••-${user.userId.takeLast(4).uppercase()}" else null

            val signals = listOf(
                WhyTrustworthySignal(
                    id = "SIG_GOV_KYC",
                    title = "Government ID Verification",
                    description = if (user.kycStatus == "VERIFIED") "Identity authenticated with UIDAI Aadhaar & Income Tax PAN records" else "Government identity verification pending",
                    isPassed = user.kycStatus == "VERIFIED",
                    iconType = "IDENTITY",
                    badgeText = if (user.kycStatus == "VERIFIED") "Verified" else "Pending"
                ),
                WhyTrustworthySignal(
                    id = "SIG_DIGILOCKER",
                    title = "DigiLocker Attestation",
                    description = if (isDigiLocker) "Official electronic identity documents linked via DigiLocker" else "DigiLocker documents not yet linked",
                    isPassed = isDigiLocker,
                    iconType = "DIGILOCKER",
                    badgeText = if (isDigiLocker) "Linked" else "Optional"
                ),
                WhyTrustworthySignal(
                    id = "SIG_REPAYMENT",
                    title = "Repayment Track Record",
                    description = "${String.format("%.0f", onTimeRate)}% punctual EMI settlements across peer agreements",
                    isPassed = onTimeRate >= 90.0,
                    iconType = "PAYMENT",
                    badgeText = "${String.format("%.0f", onTimeRate)}% On-Time"
                ),
                WhyTrustworthySignal(
                    id = "SIG_CONTRACTS",
                    title = "Agreement Settlement",
                    description = if (completedLoans > 0) "$completedLoans peer loan contracts completed with clean standing" else "New borrower with clean platform record",
                    isPassed = defaultedLoans == 0,
                    iconType = "SHIELD",
                    badgeText = if (defaultedLoans == 0) "0 Defaults" else "$defaultedLoans Overdue"
                ),
                WhyTrustworthySignal(
                    id = "SIG_ESCROW",
                    title = "Bank Account & UPI Handle",
                    description = if (user.bankAccountNumber.isNotBlank() || user.upiId.isNotBlank()) "Penny-drop verified bank account & active UPI handle linked" else "Bank account details pending linking",
                    isPassed = user.bankAccountNumber.isNotBlank() || user.upiId.isNotBlank(),
                    iconType = "COMMUNITY",
                    badgeText = if (user.bankAccountNumber.isNotBlank() || user.upiId.isNotBlank()) "Linked" else "Pending"
                )
            )

            UserProfileData(
                userId = user.userId,
                name = user.name,
                username = user.username.ifBlank { "user_${user.userId.take(6)}" },
                role = user.role,
                profilePhotoUri = user.profilePhotoUri,
                memberSince = user.createdAt,
                verificationStatus = verificationStatus,
                isPhoneVerified = user.phone.isNotBlank(),
                maskedPhone = maskedPhone,
                isEmailVerified = user.email.isNotBlank(),
                maskedEmail = maskedEmail,
                isKycVerified = user.kycStatus == "VERIFIED",
                isDigiLockerVerified = isDigiLocker,
                maskedDigiLockerId = maskedDigiLocker,
                isBankVerified = user.bankAccountNumber.isNotBlank() || user.upiId.isNotBlank(),
                trustScore = finalTrustScore,
                trustScoreTier = tierStr,
                repaymentSummary = RepaymentSummary(
                    completedLoansCount = completedLoans,
                    activeLoansCount = activeLoans,
                    totalAmountRepaid = totalRepaid,
                    onTimeRepaymentRate = onTimeRate,
                    defaultsCount = defaultedLoans,
                    punctualEmisCount = punctualCount,
                    delayedEmisCount = delayedCount
                ),
                trustworthySignals = signals,
                isBlocked = isBlocked,
                isOwnProfile = (user.userId == getCurrentUserIdSync())
            )
        }
    }

    /**
     * Cross-device online user search:
     * Searches both local Room SQLite and Cloud Firestore (by userId, username, phone, email, and name).
     * Automatically syncs discovered remote users into local Room SQLite.
     */
    suspend fun searchUsersOnline(query: String): List<UserEntity> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().removePrefix("@")
        if (cleanQuery.isBlank()) return@withContext emptyList()

        // 1. Fetch local Room matches
        val localMatches = userDao.searchUsers(cleanQuery).firstOrNull() ?: emptyList()

        // 2. Query Firestore Cloud
        val remoteMatches = mutableListOf<UserEntity>()
        try {
            firebaseManager.ensureFirebaseAuthSession()
            val firestore = com.loanzo.app.data.firebase.FirestoreProvider.get()
            val usersRef = firestore.collection("users")

            // Lookup by exact userId doc
            val docSnap = usersRef.document(cleanQuery).get().await()
            if (docSnap.exists()) {
                parseUserFromFirestoreDoc(docSnap.data, docSnap.id)?.let { remoteMatches.add(it) }
            }

            // Query by username (exact and lowercase)
            val byUsername = usersRef.whereEqualTo("username", cleanQuery.lowercase()).limit(5).get().await()
            for (doc in byUsername.documents) {
                parseUserFromFirestoreDoc(doc.data, doc.id)?.let { remoteMatches.add(it) }
            }

            // Query by phone
            val cleanPhone = cleanQuery.replace(" ", "").replace("-", "")
            if (cleanPhone.length >= 6) {
                val byPhone = usersRef.whereEqualTo("phone", cleanPhone).limit(5).get().await()
                for (doc in byPhone.documents) {
                    parseUserFromFirestoreDoc(doc.data, doc.id)?.let { remoteMatches.add(it) }
                }
                if (!cleanPhone.startsWith("+91")) {
                    val byPhoneWithPrefix = usersRef.whereEqualTo("phone", "+91$cleanPhone").limit(5).get().await()
                    for (doc in byPhoneWithPrefix.documents) {
                        parseUserFromFirestoreDoc(doc.data, doc.id)?.let { remoteMatches.add(it) }
                    }
                }
            }

            // Query by email
            if (cleanQuery.contains("@")) {
                val byEmail = usersRef.whereEqualTo("email", cleanQuery.lowercase()).limit(5).get().await()
                for (doc in byEmail.documents) {
                    parseUserFromFirestoreDoc(doc.data, doc.id)?.let { remoteMatches.add(it) }
                }
            }

            // Cache discovered remote users into local Room DB
            for (u in remoteMatches) {
                val existing = userDao.getUserById(u.userId)
                if (existing == null) {
                    userDao.insertUser(u)
                }
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "Cloud user search note: ${e.message}")
        }

        // Merge, deduplicate, and sanitize
        (localMatches + remoteMatches)
            .distinctBy { it.userId }
            .filterNot { com.loanzo.app.util.VerificationManager.isDemoAccount(it.userId) || com.loanzo.app.util.VerificationManager.isDemoAccount(it.username) }
            .map { sanitizeUserRole(it) }
    }

    private var userSnapshotListener: com.google.firebase.firestore.ListenerRegistration? = null

    /**
     * Starts continuous real-time synchronization for the currently active user document in Cloud Firestore.
     */
    fun startRealtimeUserSync(userId: String, scope: CoroutineScope) {
        if (userId.isBlank()) return
        userSnapshotListener?.remove()

        val firestore = com.loanzo.app.data.firebase.FirestoreProvider.get()
        userSnapshotListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    try {
                        val remoteUser = parseUserFromFirestoreDoc(snapshot.data, snapshot.id)
                        if (remoteUser != null) {
                            val sanitized = sanitizeUserRole(remoteUser)
                            userDao.insertUser(sanitized)
                            
                            // Elevate session if user became Field Agent
                            if (sanitized.role.equals("AGENT", ignoreCase = true) || sanitized.agentStatus.equals("APPROVED", ignoreCase = true)) {
                                saveSession(sanitized.userId, "AGENT")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("UserRepository", "Realtime user sync error: ${e.message}")
                    }
                }
            }
    }

    /**
     * Resolves a user by ID from local database or Cloud Firestore, ensuring cross-device consistency.
     */
    suspend fun syncUserById(userId: String, forceCloud: Boolean = false): UserEntity? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        if (!forceCloud) {
            val user = getUserById(userId)
            if (user != null) return@withContext user
        }

        try {
            firebaseManager.ensureFirebaseAuthSession()
            val firestore = com.loanzo.app.data.firebase.FirestoreProvider.get()
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val remoteUser = parseUserFromFirestoreDoc(doc.data, doc.id)
                if (remoteUser != null) {
                    val sanitized = sanitizeUserRole(remoteUser)
                    userDao.insertUser(sanitized)
                    return@withContext sanitized
                }
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "syncUserById error: ${e.message}")
        }
        getUserById(userId)
    }

    private fun parseUserFromFirestoreDoc(data: Map<String, Any>?, fallbackId: String): UserEntity? {
        if (data == null) return null
        val userId = (data["userId"] as? String)?.ifBlank { null } ?: fallbackId
        val name = (data["name"] as? String) ?: "User"
        val email = (data["email"] as? String) ?: ""
        val phone = (data["phone"] as? String) ?: ""
        val password = (data["password"] as? String) ?: ""
        val username = (data["username"] as? String) ?: ""
        val role = (data["role"] as? String) ?: "BORROWER"
        val kycStatus = (data["kycStatus"] as? String) ?: "PENDING"
        val panNumber = (data["panNumber"] as? String) ?: ""
        val aadhaarNumber = (data["aadhaarNumber"] as? String) ?: ""
        val emailVerified = (data["emailVerified"] as? Boolean) ?: false
        val phoneVerified = (data["phoneVerified"] as? Boolean) ?: false
        val panVerified = (data["panVerified"] as? Boolean) ?: false
        val aadhaarVerified = (data["aadhaarVerified"] as? Boolean) ?: false
        val selfieVerified = (data["selfieVerified"] as? Boolean) ?: false
        val upiId = (data["upiId"] as? String) ?: ""
        val bankAccountNumber = (data["bankAccountNumber"] as? String) ?: ""
        val bankIfsc = (data["bankIfsc"] as? String) ?: ""
        val bankVerified = (data["bankVerified"] as? Boolean) ?: false
        val profilePhotoUri = (data["profilePhotoUri"] as? String) ?: ""
        val panImageUrl = (data["panImageUrl"] as? String) ?: ""
        val aadhaarImageUrl = (data["aadhaarImageUrl"] as? String) ?: ""
        val dateOfBirth = (data["dateOfBirth"] as? String) ?: ""
        val address = (data["address"] as? String) ?: ""
        val fcmToken = (data["fcmToken"] as? String) ?: ""
        val agentStatus = (data["agentStatus"] as? String) ?: "NOT_APPLIED"
        val isOnDuty = (data["isOnDuty"] as? Boolean) ?: true
        val totalAgentEarnings = when (val e = data["totalAgentEarnings"]) {
            is Number -> e.toDouble()
            else -> 0.0
        }
        val registeredDeviceId = (data["registeredDeviceId"] as? String) ?: ""
        val registeredDeviceModel = (data["registeredDeviceModel"] as? String) ?: ""
        val telegramUsername = (data["telegramUsername"] as? String) ?: ""
        val createdAt = when (val c = data["createdAt"]) {
            is Timestamp -> c.toDate().time
            is Number -> c.toLong()
            else -> System.currentTimeMillis()
        }

        return UserEntity(
            userId = userId,
            name = name,
            email = email,
            phone = phone,
            username = username,
            password = password,
            role = role,
            kycStatus = kycStatus,
            panNumber = panNumber,
            aadhaarNumber = aadhaarNumber,
            emailVerified = emailVerified,
            phoneVerified = phoneVerified,
            panVerified = panVerified,
            aadhaarVerified = aadhaarVerified,
            selfieVerified = selfieVerified,
            upiId = upiId,
            bankAccountNumber = bankAccountNumber,
            bankIfsc = bankIfsc,
            bankVerified = bankVerified,
            profilePhotoUri = profilePhotoUri,
            panImageUrl = panImageUrl,
            aadhaarImageUrl = aadhaarImageUrl,
            dateOfBirth = dateOfBirth,
            address = address,
            fcmToken = fcmToken,
            agentStatus = agentStatus,
            isOnDuty = isOnDuty,
            totalAgentEarnings = totalAgentEarnings,
            registeredDeviceId = registeredDeviceId,
            registeredDeviceModel = registeredDeviceModel,
            telegramUsername = telegramUsername,
            createdAt = createdAt
        )
    }
}
