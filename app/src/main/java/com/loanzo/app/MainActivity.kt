package com.loanzo.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)
        setContent {
            val themeMode by userRepository.getThemeMode().collectAsState(initial = "SYSTEM")
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            CompositionLocalProvider(LocalUserRepository provides userRepository) {
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
            val isCustomScheme = uri.scheme == "loanzo" && uri.host == "kyc-callback"
            val isHttpsScheme = uri.scheme == "https" && uri.host == "loanzo.app" && (uri.path?.contains("kyc-callback") == true)

            if (isCustomScheme || isHttpsScheme) {
                val sessionId = uri.getQueryParameter("session_id") ?: ""
                val status = uri.getQueryParameter("status") ?: "Approved"
                authViewModel.handleDiditCallback(sessionId, status)
                Toast.makeText(this, "Didit KYC status: $status", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
