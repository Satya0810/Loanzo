package com.loanzo.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import com.loanzo.app.util.LocalUserRepository
import com.loanzo.app.util.LocalAgentRepository
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.LocalBankingSessionManager
import com.loanzo.app.util.LocalSplashWarmupCoordinator
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.data.session.BankingSessionManager
import com.loanzo.app.util.SplashWarmupCoordinator
import com.loanzo.app.ui.auth.AuthViewModel
import com.loanzo.app.ui.navigation.LoanzoNavGraph
import com.loanzo.app.ui.theme.LoanzoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var agentRepository: com.loanzo.app.data.repository.AgentRepository
    @Inject lateinit var adminRepository: com.loanzo.app.data.repository.AdminRepository
    @Inject lateinit var sessionManager: BankingSessionManager
    @Inject lateinit var warmupCoordinator: SplashWarmupCoordinator
    @Inject lateinit var supportTicketRepository: com.loanzo.app.data.repository.SupportTicketRepository
    @Inject lateinit var notificationRepository: com.loanzo.app.data.repository.NotificationRepository
    @Inject lateinit var translationHelper: com.loanzo.app.util.TranslationHelper

    private var currentActivityLanguage: String = ""

    val pendingNavigationRoute = androidx.compose.runtime.mutableStateOf<String?>(null)

    fun consumePendingNavigation(): String? {
        val route = pendingNavigationRoute.value
        pendingNavigationRoute.value = null
        return route
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = com.loanzo.app.util.LocaleHelper.getPersistedLanguage(newBase)
        currentActivityLanguage = lang
        val localized = com.loanzo.app.util.LocaleHelper.createLocalizedContext(newBase, lang)
        super.attachBaseContext(localized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.loanzo.app.util.NotificationChannelHelper.setupNotificationChannels(this)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)
        intent?.getStringExtra("navigate_to")?.let {
            pendingNavigationRoute.value = it
        }

        // Proactively request notification permission on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Ensure offline translation model starts downloading if user chose Hindi or other language
        val initialLang = com.loanzo.app.util.LocaleHelper.getPersistedLanguage(this)
        com.loanzo.app.util.LocaleHelper.applyLocale(this, initialLang)
        if (initialLang != "en") {
            translationHelper.downloadModelIfNeeded(initialLang)
        }

        setContent {
            val themeMode by userRepository.getThemeMode().collectAsState(initial = "LIGHT")
            val currentLanguageCode = com.loanzo.app.util.LocaleHelper.getPersistedLanguage(this@MainActivity)
            val appLanguage by userRepository.getAppLanguage().collectAsState(initial = currentLanguageCode)

            val isDark = when (themeMode) {
                "DARK" -> true
                else -> false // Signature Brand Light Theme as primary default & system theme
            }

            androidx.compose.runtime.LaunchedEffect(appLanguage) {
                // If language is changed, download on-device word/model files internally
                if (appLanguage != "en") {
                    translationHelper.downloadModelIfNeeded(appLanguage)
                }

                if (currentActivityLanguage.isNotBlank() && appLanguage != currentActivityLanguage) {
                    com.loanzo.app.util.LocaleHelper.applyLocale(this@MainActivity, appLanguage)
                    currentActivityLanguage = appLanguage
                    this@MainActivity.recreate()
                    return@LaunchedEffect
                }
            }

            CompositionLocalProvider(
                com.loanzo.app.util.LocalTranslationHelper provides translationHelper,
                com.loanzo.app.util.LocalAppLanguage provides appLanguage,
                LocalUserRepository provides userRepository,
                LocalAgentRepository provides agentRepository,
                LocalAdminRepository provides adminRepository,
                LocalBankingSessionManager provides sessionManager,
                LocalSplashWarmupCoordinator provides warmupCoordinator,
                com.loanzo.app.util.LocalSupportTicketRepository provides supportTicketRepository,
                com.loanzo.app.util.LocalNotificationRepository provides notificationRepository
            ) {
                LoanzoTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LoanzoNavGraph(authViewModel = authViewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val authState = authViewModel.uiState.value
        sessionManager.onAppForegrounded(authState.isLoggedIn)
    }

    override fun onStop() {
        super.onStop()
        sessionManager.onAppBackgrounded()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionManager.onUserInteracted()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
        intent.getStringExtra("navigate_to")?.let {
            pendingNavigationRoute.value = it
        }
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            val isDigiLockerScheme = uri.scheme == "loanzo" && uri.host == "digilocker-callback"

            if (isDigiLockerScheme) {
                android.util.Log.d("MainActivity", "DigiLocker callback URI: $uri")
                val error = uri.getQueryParameter("error")
                val code = uri.getQueryParameter("code")
                    ?: uri.getQueryParameter("session_id")
                    ?: uri.getQueryParameter("sessionId")
                    ?: uri.getQueryParameter("status")
                    ?: "SUCCESS"
                val returnedSessionId = uri.getQueryParameter("session_id") ?: uri.getQueryParameter("sessionId")

                if (error.isNullOrBlank() || error.equals("null", ignoreCase = true)) {
                    authViewModel.handleDigiLockerCallback(code, returnedSessionId)
                    Toast.makeText(this, "DigiLocker Authorization Received. Verifying documents...", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "DigiLocker Verification Cancelled: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
