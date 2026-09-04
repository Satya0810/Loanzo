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
import com.loanzo.app.data.repository.UserRepository
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)

        // Truecaller OAuth removed per security audit

        setContent {
            val themeMode by userRepository.getThemeMode().collectAsState(initial = "LIGHT")
            val appLanguage by userRepository.getAppLanguage().collectAsState(initial = "en")

            val isDark = when (themeMode) {
                "DARK" -> true
                else -> false // Signature Brand Light Theme as primary default & system theme
            }

            val currentContext = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(appLanguage) {
                try {
                    val locale = java.util.Locale(appLanguage)
                    java.util.Locale.setDefault(locale)
                    val resources = currentContext.resources
                    val config = android.content.res.Configuration(resources.configuration)
                    config.setLocale(locale)
                    @Suppress("DEPRECATION")
                    resources.updateConfiguration(config, resources.displayMetrics)
                } catch (_: Exception) {}
            }

            CompositionLocalProvider(
                LocalUserRepository provides userRepository,
                com.loanzo.app.util.LocalAgentRepository provides agentRepository,
                com.loanzo.app.util.LocalAdminRepository provides adminRepository
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
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
