package com.loanzo.app.util

import androidx.compose.runtime.compositionLocalOf
import com.loanzo.app.data.repository.UserRepository

/**
 * CompositionLocal to provide UserRepository to composables
 * without requiring Hilt injection at every level.
 */
val LocalUserRepository = compositionLocalOf<UserRepository> {
    error("No UserRepository provided")
}
