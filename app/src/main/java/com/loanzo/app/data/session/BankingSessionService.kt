package com.loanzo.app.data.session

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background Session Service to detect when user removes the app from Android Recents.
 *
 * The Android OS invokes onTaskRemoved when the user swipes away the task card from Overview / Recents.
 */
@AndroidEntryPoint
class BankingSessionService : Service() {

    @Inject
    lateinit var sessionManager: BankingSessionManager

    @Inject
    lateinit var userRepository: UserRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // User explicitly swiped app away from Recents
        sessionManager.onAppRemovedFromRecents()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userRepository.clearSession()
            } catch (_: Exception) {}
        }
        stopSelf()
    }
}
