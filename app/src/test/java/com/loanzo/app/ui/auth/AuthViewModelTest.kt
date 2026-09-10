package com.loanzo.app.ui.auth

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.loanzo.app.data.DemoDataSeeder
import com.loanzo.app.data.digilocker.DigiLockerVerificationService
import com.loanzo.app.data.drive.GoogleDriveManager
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.firebase.FirebaseManager
import com.loanzo.app.data.repository.AdminRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.fcm.LoanzoMessagingService
import com.loanzo.app.testutil.MainDispatcherExtension
import com.loanzo.app.util.DeviceSecurityHelper
import com.loanzo.app.util.TelegramManager
import com.loanzo.app.util.hashPassword
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.nio.file.Files

class AuthViewModelTest {

    @RegisterExtension
    @JvmField
    val mainDispatcher = MainDispatcherExtension()

    private val context = mockk<Context>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val firebaseManager = mockk<FirebaseManager>(relaxed = true)
    private val googleDriveManager = mockk<GoogleDriveManager>(relaxed = true)
    private val digiLockerService = mockk<DigiLockerVerificationService>(relaxed = true)
    private val telegramManager = mockk<TelegramManager>(relaxed = true)
    private val demoDataSeeder = mockk<DemoDataSeeder>(relaxed = true)
    private val adminRepository = mockk<AdminRepository>(relaxed = true)

    private lateinit var tempDir: File

    private val sampleUser = UserEntity(
        userId = "usr_vikram",
        username = "vikram99",
        name = "Vikram Sharma",
        email = "vikram@example.com",
        phone = "9876543210",
        password = hashPassword("Secret@123"),
        role = "USER",
        kycStatus = "VERIFIED",
        registeredDeviceId = "UID-TEST-DEV"
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        tempDir = Files.createTempDirectory("auth_test_files").toFile()
        every { context.filesDir } returns tempDir

        mockkStatic(com.google.firebase.auth.FirebaseAuth::class)
        val mockAuth = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
        every { com.google.firebase.auth.FirebaseAuth.getInstance() } returns mockAuth

        mockkObject(LoanzoMessagingService.Companion)
        every { LoanzoMessagingService.registerFcmToken(any(), any()) } just Runs

        mockkObject(DeviceSecurityHelper)
        every { DeviceSecurityHelper.getHardwareDeviceId(any()) } returns "UID-TEST-DEV"
        every { DeviceSecurityHelper.getDeviceModelName() } returns "Pixel 8 Pro"
        every { DeviceSecurityHelper.isDeviceMatched(any(), any()) } returns true

        every { userRepository.isLoggedIn() } returns flowOf(false)
        every { userRepository.getCurrentRole() } returns flowOf("USER")
        coEvery { userRepository.getCurrentUserIdSync() } returns null
        coEvery { userRepository.getUserByUsername(any()) } returns null
        coEvery { userRepository.getUserByEmail(any()) } returns null
        coEvery { userRepository.getUserByPhone(any()) } returns null
        coEvery { userRepository.getUserById(any()) } returns null
        coEvery { firebaseManager.fetchUserFromFirestore(any()) } returns null
        coEvery { firebaseManager.signInFirebaseAuthUser(any(), any()) } returns Result.failure(Exception("Bad credentials"))
        coEvery { demoDataSeeder.seedGlobalDemoData() } returns Result.success("Seeded")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(com.google.firebase.auth.FirebaseAuth::class)
        unmockkObject(LoanzoMessagingService.Companion)
        unmockkObject(DeviceSecurityHelper)
        tempDir.deleteRecursively()
    }

    private fun createViewModel(): AuthViewModel {
        return AuthViewModel(
            context = context,
            userRepository = userRepository,
            firebaseManager = firebaseManager,
            googleDriveManager = googleDriveManager,
            digiLockerService = digiLockerService,
            telegramManager = telegramManager,
            demoDataSeeder = demoDataSeeder,
            adminRepository = adminRepository
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Session Initialization
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session Initialization Tests")
    inner class SessionTests {

        @Test
        fun `when not logged in on launch, uiState indicates unauthenticated`() = runTest {
            every { userRepository.isLoggedIn() } returns flowOf(false)

            val viewModel = createViewModel()
            val state = viewModel.uiState.value

            assertThat(state.isLoggedIn).isFalse()
            assertThat(state.currentUserId).isNull()
            assertThat(state.isSessionChecking).isFalse()
        }

        @Test
        fun `when logged in on launch, uiState restores session and triggers FCM registration`() = runTest {
            every { userRepository.isLoggedIn() } returns flowOf(true)
            coEvery { userRepository.getCurrentUserIdSync() } returns "usr_vikram"
            every { userRepository.getCurrentRole() } returns flowOf("USER")
            coEvery { userRepository.getUserById("usr_vikram") } returns sampleUser

            val viewModel = createViewModel()
            val state = viewModel.uiState.value

            assertThat(state.isLoggedIn).isTrue()
            assertThat(state.currentUserId).isEqualTo("usr_vikram")
            assertThat(state.currentRole).isEqualTo("USER")
            assertThat(state.isSessionChecking).isFalse()
            verify { LoanzoMessagingService.registerFcmToken(context, "usr_vikram") }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Role Selection & State Reset
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Role Selection and Reset Tests")
    inner class RoleAndResetTests {

        @Test
        fun `setSelectedLoginRole updates role in state`() = runTest {
            val viewModel = createViewModel()
            viewModel.setSelectedLoginRole("Agent")

            assertThat(viewModel.uiState.value.selectedRole).isEqualTo("Agent")
        }

        @Test
        fun `resetAuthState clears error, success flags, and OTP data`() = runTest {
            val viewModel = createViewModel()
            viewModel.initTotpSetup("TestAccount")

            viewModel.resetAuthState()
            val state = viewModel.uiState.value

            assertThat(state.totpSecret).isNull()
            assertThat(state.error).isNull()
            assertThat(state.registrationSuccess).isFalse()
            assertThat(state.passwordResetSuccess).isFalse()
            assertThat(state.forgotPasswordStep).isEqualTo(1)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // User ID Verification
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User ID Verification Tests")
    inner class UserIdVerificationTests {

        @Test
        fun `verifyUserIdExists sets isUserIdVerified true when user found locally`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns sampleUser

            val viewModel = createViewModel()
            viewModel.verifyUserIdExists("vikram99")

            val state = viewModel.uiState.value
            assertThat(state.isUserIdVerified).isTrue()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNull()
        }

        @Test
        fun `verifyUserIdExists checks Firestore when not found locally`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns null
            coEvery { firebaseManager.fetchUserFromFirestore("vikram99") } returns sampleUser
            coEvery { userRepository.createUser(sampleUser) } returns Unit

            val viewModel = createViewModel()
            viewModel.verifyUserIdExists("vikram99")

            coVerify { userRepository.createUser(sampleUser) }
            val state = viewModel.uiState.value
            assertThat(state.isUserIdVerified).isTrue()
            assertThat(state.error).isNull()
        }

        @Test
        fun `verifyUserIdExists sets error when account is nowhere to be found`() = runTest {
            coEvery { userRepository.getUserByUsername("unknown") } returns null
            coEvery { firebaseManager.fetchUserFromFirestore("unknown") } returns null

            val viewModel = createViewModel()
            viewModel.verifyUserIdExists("unknown")

            val state = viewModel.uiState.value
            assertThat(state.isUserIdVerified).isFalse()
            assertThat(state.error).contains("Account not found")
        }

        @Test
        fun `resetUserIdVerification clears verification status`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns sampleUser

            val viewModel = createViewModel()
            viewModel.verifyUserIdExists("vikram99")
            assertThat(viewModel.uiState.value.isUserIdVerified).isTrue()

            viewModel.resetUserIdVerification()
            assertThat(viewModel.uiState.value.isUserIdVerified).isFalse()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Username Uniqueness
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Username Check Tests")
    inner class UsernameTests {

        @Test
        fun `checkUsernameUnique flags taken username if found in local database`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns sampleUser

            val viewModel = createViewModel()
            viewModel.checkUsernameUnique("vikram99")

            assertThat(viewModel.uiState.value.isUsernameUnique).isFalse()
        }

        @Test
        fun `checkUsernameUnique flags unique username if not found locally or in firestore`() = runTest {
            coEvery { userRepository.getUserByUsername("newuser123") } returns null
            coEvery { firebaseManager.fetchUserFromFirestore("newuser123") } returns null

            val viewModel = createViewModel()
            viewModel.checkUsernameUnique("newuser123")

            assertThat(viewModel.uiState.value.isUsernameUnique).isTrue()
        }

        @Test
        fun `checkUsernameUnique ignores input shorter than 3 characters`() = runTest {
            val viewModel = createViewModel()
            viewModel.checkUsernameUnique("ab")

            assertThat(viewModel.uiState.value.isUsernameUnique).isNull()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Login with Credentials
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Login with Credentials Tests")
    inner class LoginTests {

        @Test
        fun `login with blank password returns validation error`() = runTest {
            val viewModel = createViewModel()
            viewModel.loginWithCredentials("vikram99", "   ")

            val state = viewModel.uiState.value
            assertThat(state.isLoggedIn).isFalse()
            assertThat(state.error).contains("Password cannot be empty")
        }

        @Test
        fun `login with valid password establishes session successfully`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns sampleUser
            coEvery { userRepository.saveSession(sampleUser.userId, any()) } returns Unit
            coEvery { userRepository.getBiometricUserIdSync() } returns null

            val viewModel = createViewModel()
            viewModel.loginWithCredentials("vikram99", "Secret@123")

            val state = viewModel.uiState.value
            assertThat(state.isLoggedIn).isTrue()
            assertThat(state.currentUserId).isEqualTo(sampleUser.userId)
            assertThat(state.error).isNull()
            coVerify { userRepository.saveSession(sampleUser.userId, "USER") }
        }

        @Test
        fun `login with wrong password fails with error`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns sampleUser

            val viewModel = createViewModel()
            viewModel.loginWithCredentials("vikram99", "WrongPassword")

            val state = viewModel.uiState.value
            assertThat(state.isLoggedIn).isFalse()
            assertThat(state.error).contains("Incorrect password")
        }

        @Test
        fun `login with non-existent user returns account not found`() = runTest {
            coEvery { userRepository.getUserByUsername("ghost") } returns null
            coEvery { firebaseManager.fetchUserFromFirestore("ghost") } returns null

            val viewModel = createViewModel()
            viewModel.loginWithCredentials("ghost", "Secret@123")

            val state = viewModel.uiState.value
            assertThat(state.isLoggedIn).isFalse()
            assertThat(state.error).contains("Account not found")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Registration
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User Registration Tests")
    inner class RegistrationTests {

        @Test
        fun `register with invalid username fails with error`() = runTest {
            val viewModel = createViewModel()
            viewModel.register(
                name = "Test",
                email = "test@example.com",
                phone = "9876543210",
                pass = "Secret@123",
                role = "BORROWER",
                username = "a" // too short
            )

            val state = viewModel.uiState.value
            assertThat(state.registrationSuccess).isFalse()
            assertThat(state.error).contains("Username must be at least 3 characters")
        }

        @Test
        fun `register with duplicate email fails with error`() = runTest {
            coEvery { userRepository.getUserByUsername("validuser") } returns null
            coEvery { userRepository.getUserByEmail("vikram@example.com") } returns sampleUser

            val viewModel = createViewModel()
            viewModel.register(
                name = "Another User",
                email = "vikram@example.com",
                phone = "9999999999",
                pass = "Secret@123",
                role = "BORROWER",
                username = "validuser"
            )

            val state = viewModel.uiState.value
            assertThat(state.registrationSuccess).isFalse()
            assertThat(state.error).contains("Email is already registered")
        }

        @Test
        fun `register with valid details creates user and sets registrationSuccess`() = runTest {
            coEvery { userRepository.getUserByUsername("validuser") } returns null
            coEvery { userRepository.getUserByEmail("new@example.com") } returns null
            coEvery { userRepository.getUserByPhone("9123456780") } returns null
            coEvery { firebaseManager.fetchUserFromFirestore(any()) } returns null
            coEvery { userRepository.createUser(any()) } returns Unit
            coEvery { userRepository.clearSession() } returns Unit

            val viewModel = createViewModel()
            viewModel.register(
                name = "New Applicant",
                email = "new@example.com",
                phone = "9123456780",
                pass = "StrongPass@2026",
                role = "BORROWER",
                username = "validuser"
            )

            val state = viewModel.uiState.value
            assertThat(state.registrationSuccess).isTrue()
            assertThat(state.isLoggedIn).isFalse()
            coVerify { userRepository.createUser(match { it.username == "validuser" && it.name == "New Applicant" }) }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Forgot Password & 2FA
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Forgot Password & 2FA Tests")
    inner class ForgotPasswordTests {

        @Test
        fun `initiateForgotPassword transitions to step 2 on registered device`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns sampleUser

            val viewModel = createViewModel()
            viewModel.initiateForgotPassword("vikram99")

            val state = viewModel.uiState.value
            assertThat(state.forgotPasswordStep).isEqualTo(2)
            assertThat(state.resetLoginId).isEqualTo("vikram99")
            assertThat(state.resetUserEmail).isEqualTo(sampleUser.email)
        }

        @Test
        fun `add2FAFactor records factor and advances to step 5`() = runTest {
            val viewModel = createViewModel()
            viewModel.add2FAFactor("email")

            val state = viewModel.uiState.value
            assertThat(state.forgotPasswordStep).isEqualTo(5)
            assertThat(state.verified2FAFactors).contains("email")
        }

        @Test
        fun `resetPassword fails if password is too short`() = runTest {
            val viewModel = createViewModel()
            viewModel.resetPassword("short")

            assertThat(viewModel.uiState.value.error).contains("at least 8 characters")
        }

        @Test
        fun `resetPassword updates user password and marks passwordResetSuccess`() = runTest {
            coEvery { userRepository.getUserByUsername("vikram99") } returns sampleUser
            coEvery { userRepository.updateUser(any()) } returns Unit
            coEvery { userRepository.clearSession() } returns Unit

            val viewModel = createViewModel()
            viewModel.initiateForgotPassword("vikram99")
            testScheduler.advanceUntilIdle()

            viewModel.add2FAFactor("email")
            testScheduler.advanceUntilIdle()

            viewModel.resetPassword("NewSecretPassword@99")
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.passwordResetSuccess).isTrue()
            coVerify { userRepository.updateUser(match { it.password == hashPassword("NewSecretPassword@99") }) }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Biometric & KYC
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Biometrics & KYC Simulation Tests")
    inner class BiometricAndKycTests {

        @Test
        fun `checkBiometricEnrollment calls onEnrolled when user is registered and biometrics enabled`() = runTest {
            coEvery { userRepository.getBiometricUserIdSync() } returns "usr_vikram"
            coEvery { userRepository.isBiometricEnabledSync() } returns true
            coEvery { userRepository.getUserById("usr_vikram") } returns sampleUser

            var enrolledCalled = false
            val viewModel = createViewModel()
            viewModel.checkBiometricEnrollment(
                onEnrolled = { enrolledCalled = true },
                onNotEnrolled = {}
            )

            assertThat(enrolledCalled).isTrue()
        }

        @Test
        fun `checkBiometricEnrollment calls onNotEnrolled when biometrics disabled`() = runTest {
            coEvery { userRepository.isBiometricEnabledSync() } returns false

            var notEnrolledCalled = false
            val viewModel = createViewModel()
            viewModel.checkBiometricEnrollment(
                onEnrolled = {},
                onNotEnrolled = { notEnrolledCalled = true }
            )

            assertThat(notEnrolledCalled).isTrue()
        }

        @Test
        fun `completeKycStep advances step and persists updates`() = runTest {
            coEvery { userRepository.getCurrentUserIdSync() } returns "usr_vikram"
            coEvery { userRepository.getUserById("usr_vikram") } returns sampleUser
            coEvery { userRepository.updateUser(any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.completeKycStep(1, userId = "usr_vikram", updates = mapOf("pan" to "ABCDE1234F"))

            assertThat(viewModel.uiState.value.kycStep).isEqualTo(1)
            coVerify { userRepository.updateUser(match { it.panNumber == "ABCDE1234F" }) }
        }

        @Test
        fun `simulateQuickKyc updates status to VERIFIED and step to 5`() = runTest {
            coEvery { userRepository.getCurrentUserIdSync() } returns "usr_vikram"
            coEvery { userRepository.getUserById("usr_vikram") } returns sampleUser
            coEvery { userRepository.updateUser(any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.simulateQuickKyc("usr_vikram")

            val state = viewModel.uiState.value
            assertThat(state.kycStep).isEqualTo(5)
            assertThat(state.kycStatus).isEqualTo("VERIFIED")
            coVerify { userRepository.updateUser(match { it.kycStatus == "VERIFIED" }) }
        }
    }
}
