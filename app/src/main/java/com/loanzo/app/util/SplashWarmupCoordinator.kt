package com.loanzo.app.util

import android.content.Context
import com.loanzo.app.data.dao.LoanDao
import com.loanzo.app.data.dao.NotificationDao
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.data.session.BankingSessionManager
import com.loanzo.app.ui.navigation.Routes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class WarmupResult(
    val targetRoute: String,
    val user: UserEntity? = null,
    val isSessionLocked: Boolean = false,
    val isUntrustedDevice: Boolean = false
)

/**
 * Parallel Startup Warmup & Cold-to-Hot Database Priming Engine.
 *
 * Runs concurrently with the 3.5-second cinematic logo animation.
 * Performs deep security clearance, hardware device binding validation,
 * session health auditing, and Room cache prefetching on Dispatchers.IO.
 *
 * Guarantees that when the splash screen transitions to the destination,
 * the UI renders instantaneously with complete data — ZERO layout jumps,
 * ZERO loading spinners, and ZERO mid-flight route redirection.
 */
@Singleton
class SplashWarmupCoordinator @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: BankingSessionManager,
    private val userDao: UserDao,
    private val loanDao: LoanDao,
    private val notificationDao: NotificationDao,
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager,
    private val presentationAccountSeeder: com.loanzo.app.data.PresentationAccountSeeder,
    @ApplicationContext private val context: Context
) {
    suspend fun executeParallelWarmup(): WarmupResult = withContext(Dispatchers.IO) {
        try {
            // Stage 0: Non-blocking asynchronous Firebase Auth priming (prevents offline/slow network splash hanging)
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    firebaseManager.ensureFirebaseAuthSession()
                } catch (e: Exception) {
                    android.util.Log.w("SplashWarmupCoordinator", "Background Firebase session warmup note: ${e.message}")
                }
            }

            // Automatic startup purge of all residual demo entities & stale demo biometrics
            try {
                userRepository.purgeDemoDataAndBiometrics()
            } catch (_: Exception) {}

            // Prime presentation accounts & multi-role ecosystem (Devesh, Romesh, Shivam, Suman)
            try {
                presentationAccountSeeder.seedPresentationAccounts()
            } catch (e: Exception) {
                android.util.Log.e("SplashWarmupCoordinator", "Error seeding presentation accounts: ${e.message}")
            }

            // Stage 1: Fast Session & Identity Inspection
            val isLoggedIn = userRepository.isLoggedIn().first()
            val userId = userRepository.getCurrentUserIdSync()

            // If app was removed from recents or not in recents, or full login is required:
            val isFullLoginRequired = sessionManager.isFullLoginRequired() || !sessionManager.isAppInRecents()
            if (isFullLoginRequired) {
                userRepository.clearSession()
                sessionManager.clearSession()
                return@withContext WarmupResult(
                    targetRoute = Routes.LOGIN,
                    user = null,
                    isSessionLocked = false
                )
            }

            // If active session belongs to a demo/fake account, kill it immediately and force login
            if (isLoggedIn && (VerificationManager.isDemoAccount(userId) || userId.isNullOrBlank())) {
                userRepository.clearSession()
                sessionManager.clearSession()
                return@withContext WarmupResult(
                    targetRoute = Routes.LOGIN,
                    user = null,
                    isSessionLocked = false
                )
            }

            if (!isLoggedIn || userId.isNullOrBlank()) {
                return@withContext WarmupResult(
                    targetRoute = Routes.LOGIN,
                    user = null,
                    isSessionLocked = false
                )
            }

            // Stage 2: Hardware Device Cryptographic Binding Check
            val user = userDao.getUserById(userId)
            if (user == null) {
                userRepository.clearSession()
                sessionManager.clearSession()
                return@withContext WarmupResult(
                    targetRoute = Routes.LOGIN,
                    user = null
                )
            }

            if (user.registeredDeviceId.isNotBlank()) {
                val isDeviceMatched = sessionManager.validateDeviceBinding(user)
                if (!isDeviceMatched) {
                    // Hardware signature mismatch -> Route to security grievance immediately
                    return@withContext WarmupResult(
                        targetRoute = Routes.FORGOT_PASSWORD,
                        user = user,
                        isUntrustedDevice = true
                    )
                }
            }

            // Stage 3: Bank-Grade Inactivity & Hard Expiration Audit
            val lastActive = sessionManager.getLastActiveTime()
            val now = System.currentTimeMillis()

            // Rule: Inactivity > 2 days (48 hours) -> Hard Session Expiration
            if (lastActive > 0L && (now - lastActive) > BankingSessionManager.HARD_EXPIRY_TIMEOUT_MS) {
                userRepository.clearSession()
                sessionManager.clearSession()
                return@withContext WarmupResult(
                    targetRoute = Routes.LOGIN,
                    user = null
                )
            }

            // Check if session is locked due to background inactivity (> 3 minutes) while in recents
            val isLocked = sessionManager.isSessionLocked() || (lastActive > 0L && (now - lastActive) >= BankingSessionManager.INACTIVITY_LOCK_TIMEOUT_MS)
            if (isLocked) {
                sessionManager.lockSession()
                return@withContext WarmupResult(
                    targetRoute = Routes.SESSION_LOCK,
                    user = user,
                    isSessionLocked = true
                )
            }

            // Stage 4: Database Cache Priming & Prefetching (Cold-to-Hot Warmup)
            // Pre-warm active loans and notifications into Room memory
            try {
                loanDao.getAllLoansForUser(userId).firstOrNull()
                notificationDao.getUnreadCount(userId).firstOrNull()
            } catch (_: Exception) {}

            // Touch active session timestamp
            sessionManager.recordActivityTimestamp(now)

            // Stage 5: Deterministic Single-Shot Destination Resolution
            WarmupResult(
                targetRoute = Routes.MAIN,
                user = user,
                isSessionLocked = false,
                isUntrustedDevice = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful fallback to LOGIN on unexpected error
            WarmupResult(
                targetRoute = Routes.LOGIN,
                user = null
            )
        }
    }
}
