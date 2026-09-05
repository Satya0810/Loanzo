package com.loanzo.app.ui.loan

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ESignWebViewScreen(
    signUrl: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.document_esign), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            // Leegality usually redirects to a preconfigured callback URL on success/failure
                            // For this demo, we can just intercept anything containing "success" or a specific scheme
                            if (url.contains("leegality.com/success") || url.contains("loanzo://esign/success")) {
                                onSuccess()
                                return true
                            } else if (url.contains("loanzo://esign/cancel") || url.contains("leegality.com/cancel")) {
                                onCancel()
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(signUrl)
                }
            }
        )
    }
}
