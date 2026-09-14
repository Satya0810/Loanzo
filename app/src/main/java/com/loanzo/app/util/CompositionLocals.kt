package com.loanzo.app.util

import androidx.compose.runtime.compositionLocalOf
import com.loanzo.app.data.repository.AdminRepository
import com.loanzo.app.data.repository.AgentRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.data.session.BankingSessionManager

/**
 * CompositionLocal to provide core repositories and session managers
 * to composables without requiring Hilt injection at every level.
 */
val LocalUserRepository = compositionLocalOf<UserRepository> {
    error("No UserRepository provided")
}

val LocalAgentRepository = compositionLocalOf<AgentRepository> {
    error("No AgentRepository provided")
}

val LocalAdminRepository = compositionLocalOf<AdminRepository> {
    error("No AdminRepository provided")
}

val LocalBankingSessionManager = compositionLocalOf<BankingSessionManager> {
    error("No BankingSessionManager provided")
}

val LocalSplashWarmupCoordinator = compositionLocalOf<SplashWarmupCoordinator> {
    error("No SplashWarmupCoordinator provided")
}

val LocalSupportTicketRepository = compositionLocalOf<com.loanzo.app.data.repository.SupportTicketRepository> {
    error("No SupportTicketRepository provided")
}

val LocalNotificationRepository = compositionLocalOf<com.loanzo.app.data.repository.NotificationRepository> {
    error("No NotificationRepository provided")
}

val LocalTranslationHelper = compositionLocalOf<TranslationHelper?> {
    null
}

val LocalAppLanguage = compositionLocalOf {
    "en"
}

/**
 * Convenient Composable string translation extension.
 * When language is non-English, dynamically translates text via the internal on-device engine
 * or returns instantaneous domain glossary translation.
 */
@androidx.compose.runtime.Composable
fun String.t(): String {
    val language = LocalAppLanguage.current
    if (language.equals("en", ignoreCase = true) || this.isBlank()) return this
    val helper = LocalTranslationHelper.current ?: TranslationHelper.instance
    return helper?.rememberTranslated(this, language) ?: AppGlossary.getTranslation(this, language) ?: this
}


@androidx.compose.runtime.Composable
fun androidx.compose.ui.text.AnnotatedString.t(): androidx.compose.ui.text.AnnotatedString {
    val language = LocalAppLanguage.current
    if (language.equals("en", ignoreCase = true) || this.text.isBlank()) return this
    val helper = LocalTranslationHelper.current ?: TranslationHelper.instance
    val translated = helper?.rememberTranslated(this.text, language) ?: AppGlossary.getTranslation(this.text, language) ?: this.text
    return androidx.compose.ui.text.AnnotatedString(
        text = translated,
        spanStyles = this.spanStyles,
        paragraphStyles = this.paragraphStyles
    )
}

