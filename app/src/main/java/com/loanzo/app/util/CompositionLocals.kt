package com.loanzo.app.util

import androidx.compose.runtime.compositionLocalOf
import com.loanzo.app.data.repository.AgentRepository
import com.loanzo.app.data.repository.UserRepository

/**
 * CompositionLocal to provide UserRepository and AgentRepository to composables
 * without requiring Hilt injection at every level.
 */
val LocalUserRepository = compositionLocalOf<UserRepository> {
    error("No UserRepository provided")
}

val LocalAgentRepository = compositionLocalOf<AgentRepository> {
    error("No AgentRepository provided")
}
