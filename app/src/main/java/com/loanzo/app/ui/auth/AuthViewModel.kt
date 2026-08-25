package com.loanzo.app.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import android.content.Context
import com.loanzo.app.data.didit.DiditVerificationService
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    val currentRole: String? = null,
    val error: String? = null,
    val registrationSuccess: Boolean = false,
    // Phone Auth state
    val verificationId: String? = null,
    val isOtpSent: Boolean = false,
    // KYC state
    val kycStep: Int = 0, // 0=not started, 1=PAN, 2=Aadhaar, 3=Selfie, 4=Bank/UPI, 5=Complete
    val kycStatus: String = "PENDING",
    // Didit KYC state
    val isDiditLoading: Boolean = false,
    val diditSessionId: String? = null,
    val diditStatus: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val diditService: DiditVerificationService
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.isLoggedIn().collect { loggedIn ->
                if (loggedIn) {
                    val userId = userRepository.getCurrentUserIdSync()
                    val role = userRepository.getCurrentRole().first()
                    _uiState.update {
                        it.copy(isLoggedIn = true, currentUserId = userId, currentRole = role)
                    }
                }
            }
        }
    }

    // 1. Initiate Phone Authentication
    fun sendOtp(phoneNumber: String, activity: Activity) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval or instant verification
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOtpSent = true,
                        verificationId = verificationId
                    )
                }
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(activity)             // Activity (for callback binding)
            .setCallbacks(callbacks)           // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // 2. Verify OTP code entered by user
    fun verifyOtp(code: String) {
        val verificationId = _uiState.value.verificationId
        if (verificationId == null) {
            _uiState.update { it.copy(error = "Verification ID is missing.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    // 3. Complete sign-in with Firebase
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val phoneNumber = firebaseUser?.phoneNumber ?: ""
                    
                    // Check local DB if user exists by phone
                    viewModelScope.launch {
                        val existingUser = userRepository.getUserByPhone(phoneNumber)
                        if (existingUser != null) {
                            // User exists, log them in
                            userRepository.saveSession(existingUser.userId, existingUser.role)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    currentUserId = existingUser.userId,
                                    currentRole = existingUser.role
                                )
                            }
                        } else {
                            // New user, wait for registration details
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    // Not marking loggedIn yet, they need to provide Name/Role
                                    error = "New user detected. Please register.",
                                    isOtpSent = false // reset
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = task.exception?.localizedMessage)
                    }
                }
            }
    }

    // Register after OTP if new user
    fun register(
        name: String,
        email: String,
        phone: String,
        role: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = auth.currentUser?.uid ?: UUID.randomUUID().toString()
                val user = UserEntity(
                    userId = userId,
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    kycStatus = "PENDING"
                )
                userRepository.createUser(user)
                userRepository.saveSession(userId, role)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUserId = userId,
                        currentRole = role,
                        registrationSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun completeKycStep(step: Int, userId: String, updates: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            val user = userRepository.getUserById(userId) ?: return@launch
            val updatedUser = when (step) {
                1 -> user.copy(panNumber = updates["pan"] ?: "", kycStatus = "IN_PROGRESS")
                2 -> user.copy(aadhaarVerified = true, kycStatus = "IN_PROGRESS")
                3 -> user.copy(selfieVerified = true, kycStatus = "IN_PROGRESS")
                4 -> user.copy(
                    upiId = updates["upiId"] ?: "",
                    bankAccountNumber = updates["bankAccount"] ?: "",
                    kycStatus = "VERIFIED"
                )
                else -> user
            }
            userRepository.updateUser(updatedUser)
            _uiState.update {
                it.copy(
                    kycStep = step,
                    kycStatus = updatedUser.kycStatus
                )
            }
        }
    }

    /**
     * Initiates the automated Didit KYC verification flow.
     */
    fun startDiditVerification(context: Context) {
        val userId = _uiState.value.currentUserId ?: auth.currentUser?.uid ?: "user_${System.currentTimeMillis()}"
        _uiState.update { it.copy(isDiditLoading = true, error = null) }

        viewModelScope.launch {
            val result = diditService.createVerificationSession(userId)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isDiditLoading = false,
                        diditSessionId = session.sessionId
                    )
                }
                diditService.launchVerificationFlow(context, session.url)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDiditLoading = false,
                        error = "Failed to launch Didit KYC: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Handles callback returned from Didit verification deep link or status check.
     */
    fun handleDiditCallback(sessionId: String, status: String) {
        val isApproved = status.equals("Approved", ignoreCase = true) || status.equals("success", ignoreCase = true)
        val kycStatus = if (isApproved) "VERIFIED" else if (status.equals("Declined", ignoreCase = true)) "REJECTED" else "IN_PROGRESS"

        _uiState.update {
            it.copy(
                diditSessionId = sessionId,
                diditStatus = status,
                kycStatus = kycStatus,
                kycStep = if (isApproved) 5 else it.kycStep
            )
        }

        val currentId = _uiState.value.currentUserId ?: return
        viewModelScope.launch {
            val user = userRepository.getUserById(currentId) ?: return@launch
            val updatedUser = user.copy(
                kycStatus = kycStatus,
                aadhaarVerified = isApproved,
                selfieVerified = isApproved
            )
            userRepository.updateUser(updatedUser)
        }
    }

    fun logout() {
        auth.signOut()
        viewModelScope.launch {
            userRepository.clearSession()
            _uiState.update { AuthUiState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
