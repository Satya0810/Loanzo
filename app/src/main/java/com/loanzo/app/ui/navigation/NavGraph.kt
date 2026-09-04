package com.loanzo.app.ui.navigation

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.loanzo.app.data.entity.*
import com.loanzo.app.ui.auth.*
import com.loanzo.app.ui.dashboard.*
import com.loanzo.app.ui.loan.*
import kotlinx.coroutines.launch
import com.loanzo.app.ui.theme.*
import com.loanzo.app.ui.components.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.animation.core.*
import com.loanzo.app.ui.agent.*
import com.loanzo.app.util.toDateString

// Route definitions
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val KYC = "kyc"
    const val MAIN = "main"
    const val DASHBOARD = "dashboard"
    const val LOANS = "loans"
    const val LOAN_DETAIL = "loan_detail/{loanId}"
    const val CREATE_LOAN = "create_loan"
    const val LOAN_CALCULATOR = "loan_calculator"
    const val TRANCHE_REQUEST = "tranche_request/{loanId}"
    const val REPAYMENT = "repayment/{loanId}"
    const val PLEDGE = "pledge/{loanId}"
    const val AUDIT_TRAIL = "audit_trail/{loanId}"
    const val CHAT = "chat/{loanId}"
    const val DOCUMENT_VIEWER = "document_viewer/{loanId}"
    const val GUARANTORS = "guarantors/{loanId}"
    const val PROFILE = "profile"
    const val PAYMENT_SUCCESS = "payment_success"
    const val NOTIFICATIONS = "notifications"

    // Agent Routes
    const val ROLE_SELECTION = "role_selection"
    const val AGENT_APPLICATION = "agent_application"
    const val AGENT_PENDING_APPROVAL = "agent_pending_approval"
    const val AGENT_MAIN = "agent_main"

    // Feature Routes
    const val FINANCIAL_HEALTH = "financial_health"
    const val APP_OWNER_HUB = "app_owner_hub"

    fun loanDetail(loanId: String) = "loan_detail/$loanId"
    fun trancheRequest(loanId: String) = "tranche_request/$loanId"
    fun repayment(loanId: String) = "repayment/$loanId"
    fun pledge(loanId: String) = "pledge/$loanId"
    fun auditTrail(loanId: String) = "audit_trail/$loanId"
    fun chat(loanId: String) = "chat/$loanId"
    fun documentViewer(loanId: String) = "document_viewer/$loanId"
    fun guarantors(loanId: String) = "guarantors/$loanId"
    
    const val AGREEMENT_SIGNING = "agreement_signing/{loanId}"
    fun agreementSigning(loanId: String) = "agreement_signing/$loanId"

    const val MARKETPLACE = "marketplace"
    const val CREATE_MARKETPLACE_POST = "create_marketplace_post"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// Global bottomNavItems removed in favor of localized dynamic items inside MainScaffold

@Composable
fun LoanzoNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            if (initialState.destination.route == Routes.SPLASH) {
                fadeIn(animationSpec = tween(400))
            } else {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(320))
            }
        },
        exitTransition = {
            if (initialState.destination.route == Routes.SPLASH) {
                fadeOut(animationSpec = tween(250))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(320))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250))
        }
    ) {
        // Session Gate / Branded Splash
        composable(Routes.SPLASH) {
            SplashScreen()
            val userRepository = com.loanzo.app.util.LocalUserRepository.current

            LaunchedEffect(authState.isSessionChecking, authState.isLoggedIn) {
                if (!authState.isSessionChecking) {
                    val destination = if (authState.isLoggedIn) {
                        val activeUserId = authState.currentUserId
                        val user = if (!activeUserId.isNullOrBlank()) userRepository.getUserById(activeUserId) else null
                        if (user?.role == "AGENT") {
                            if (user.agentStatus == "APPROVED") Routes.AGENT_MAIN else Routes.AGENT_PENDING_APPROVAL
                        } else {
                            Routes.MAIN
                        }
                    } else {
                        Routes.LOGIN
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        // Auth flow
        composable(Routes.LOGIN) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = com.loanzo.app.util.BiometricAuthManager.getActivity(context)
            
            // Truecaller OAuth removed per security audit

            // 2. Google Sign-In options & launcher
            val gso = remember {
                val builder = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestEmail()
                    .requestProfile()

                try {
                    val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    val webClientId = if (webClientIdResId != 0) {
                        context.getString(webClientIdResId)
                    } else {
                        "1028516460996-e11ufotnevs2krggi3jni9qdhnvmjuhl.apps.googleusercontent.com"
                    }
                    builder.requestIdToken(webClientId)
                } catch (_: Exception) {
                    builder.requestIdToken("1028516460996-e11ufotnevs2krggi3jni9qdhnvmjuhl.apps.googleusercontent.com")
                }

                builder.build()
            }

            val googleSignInClient = remember(gso) {
                try {
                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                } catch (_: Throwable) {
                    null
                }
            }

            var prefilledUsernameForGoogle by remember { mutableStateOf("") }

            val googleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        authViewModel.handleGoogleSignIn(idToken, "BORROWER", targetUsername = "")
                    } else if (account != null) {
                        authViewModel.register(
                            name = account.displayName ?: "Google User",
                            email = account.email ?: "",
                            phone = "",
                            pass = "",
                            role = "BORROWER",
                            username = account.email?.substringBefore("@") ?: ""
                        )
                    }
                } catch (e: com.google.android.gms.common.api.ApiException) {
                    val msg = when (e.statusCode) {
                        10 -> "Google Sign-In Error (Code 10: DEVELOPER_ERROR): The debug SHA-1 fingerprint is not added to your Firebase Console under com.loanzo.app, or Google Sign-In is not enabled in Firebase Auth."
                        12500 -> "Google Sign-In Error (12500): Sign-in failed. Please verify Google Play Services."
                        12501 -> "Google Sign-In cancelled."
                        12502 -> "Google Sign-In in progress..."
                        else -> "Google Sign-In Error (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                    }
                    authViewModel.setError(msg)
                } catch (e: Exception) {
                    authViewModel.setError("Google Sign-In: ${e.localizedMessage ?: "Operation cancelled"}")
                }
            }

            val isBiometricAvailable = remember(context) {
                com.loanzo.app.util.BiometricAuthManager.isBiometricAvailable(context)
            }

            var showRegisterBiometricsDialog by remember { mutableStateOf(false) }
            var prefilledUsernameForBiometrics by remember { mutableStateOf("") }
            
            LoginScreen(
                onLogin = { userId, pass ->
                    authViewModel.loginWithCredentials(userId, pass)
                },
                onBiometricLogin = { currentTypedUser ->
                    val fragmentActivity = activity
                    fragmentActivity?.let { fa ->
                        authViewModel.checkBiometricEnrollment(
                            onEnrolled = { _ ->
                                com.loanzo.app.util.BiometricAuthManager.authenticate(
                                    activity = fa,
                                    title = "Biometric Login",
                                    subtitle = "Scan your fingerprint or face to sign in",
                                    onSuccess = { authViewModel.handleBiometricLogin() },
                                    onError = { err -> authViewModel.setError(err) }
                                )
                            },
                            onNotEnrolled = {
                                prefilledUsernameForBiometrics = currentTypedUser
                                showRegisterBiometricsDialog = true
                            }
                        )
                    }
                },
                onGoogleLogin = { _ ->
                    prefilledUsernameForGoogle = ""
                    val client = googleSignInClient
                    if (client != null) {
                        try {
                            client.signOut().addOnCompleteListener {
                                googleLauncher.launch(client.signInIntent)
                            }
                        } catch (_: Exception) {
                            googleLauncher.launch(client.signInIntent)
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                isBiometricAvailable = com.loanzo.app.util.BiometricAuthManager.isBiometricAvailable(context),
                isLoading = authState.isLoading,
                error = authState.error,
                onClearError = { authViewModel.clearError() },
                isUserIdVerified = authState.isUserIdVerified,
                onVerifyUserId = { authViewModel.verifyUserIdExists(it) },
                onResetUserIdVerification = { authViewModel.resetUserIdVerification() }
            )

            if (showRegisterBiometricsDialog) {
                com.loanzo.app.ui.components.RegisterBiometricsDialog(
                    initialUsername = prefilledUsernameForBiometrics,
                    onDismiss = { showRegisterBiometricsDialog = false },
                    onConfirmPasswordAndScan = { username, password, onVerificationError ->
                        val fragmentActivity = activity
                        if (fragmentActivity != null) {
                            authViewModel.verifyCredentialsAndEnrollBiometrics(
                                usernameInput = username,
                                passwordInput = password,
                                activity = fragmentActivity,
                                onSuccess = {
                                    showRegisterBiometricsDialog = false
                                },
                                onError = onVerificationError
                            )
                        } else {
                            onVerificationError("Activity context unavailable")
                        }
                    }
                )
            }

            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            LaunchedEffect(authState.isLoggedIn) {
                if (authState.isLoggedIn) {
                    val activeUserId = authState.currentUserId
                    val user = if (!activeUserId.isNullOrBlank()) userRepository.getUserById(activeUserId) else null
                    val destination = if (user?.role == "AGENT") {
                        if (user.agentStatus == "APPROVED") Routes.AGENT_MAIN else Routes.AGENT_PENDING_APPROVAL
                    } else {
                        Routes.MAIN
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegister = { name, email, phone, pass, role, enableBiometrics, username ->
                    authViewModel.register(name, email, phone, pass, role, enableBiometrics, username)
                },
                onSendEmailVerification = { authViewModel.sendEmailVerification(it) },
                onSetPhoneVerified = { authViewModel.setPhoneVerified(it) },
                onInitiatePhoneVerification = { p, t, u -> authViewModel.initiatePhoneVerification(p, t, u) },
                onResetAuthState = { authViewModel.resetAuthState() },
                isEmailVerified = authState.isEmailVerified,
                isCheckingEmailVerification = authState.isCheckingEmailVerification,
                isPhoneVerified = authState.isPhoneVerified,
                isCheckingPhoneVerification = authState.isCheckingPhoneVerification,
                isUsernameUnique = authState.isUsernameUnique,
                onCheckUsernameUnique = { authViewModel.checkUsernameUnique(it) },
                onNavigateToLogin = { navController.popBackStack() },
                isLoading = authState.isLoading,
                error = authState.error,
                onClearError = { authViewModel.clearError() }
            )

            LaunchedEffect(authState.registrationSuccess) {
                if (authState.registrationSuccess) {
                    navController.navigate(Routes.KYC) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                step = authState.forgotPasswordStep,
                verifiedFactors = authState.verified2FAFactors,
                resetUserEmail = authState.resetUserEmail,
                resetUserPhone = authState.resetUserPhone,
                isEmailVerified = authState.isEmailVerified,
                onInitiate = { authViewModel.initiateForgotPassword(it) },
                onAddFactor = { authViewModel.add2FAFactor(it) },
                onResetPassword = { authViewModel.resetPassword(it) },
                onSendEmailVerification = { authViewModel.sendEmailVerification(it) },
                onVerifyEmailOtp = { authViewModel.verifyEmailOtp(it) },
                onSetPhoneVerified = { authViewModel.setPhoneVerified(it) },
                onResetAuthState = { authViewModel.resetAuthState() },
                isLoading = authState.isLoading,
                error = authState.error,
                onNavigateToLogin = { navController.popBackStack() },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable(Routes.KYC) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
            val activeUserId = authState.currentUserId ?: currentUserId ?: ""

            val user by (if (activeUserId.isNotBlank()) userRepository.observeUser(activeUserId) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

            KycScreen(
                currentStep = authState.kycStep,
                user = user,
                isDigiLockerLoading = authState.isDigiLockerLoading,
                isUploadingPan = authState.isUploadingPan,
                isUploadingAadhaar = authState.isUploadingAadhaar,
                isUploadingSelfie = authState.isUploadingSelfie,
                error = authState.error, // Pass error to show toast/snackbar
                onClearError = { authViewModel.clearError() },
                onStartDigiLockerKyc = { authViewModel.startDigiLockerVerification(context) },
                onQuickSimulate = { authViewModel.simulateQuickKyc(authState.currentUserId ?: currentUserId) },
                onCompleteStep = { step, data ->
                    val uid = authState.currentUserId ?: currentUserId
                    authViewModel.completeKycStep(step, uid, data)
                },
                onUploadSelfie = { bmp -> authViewModel.uploadLivenessSelfie(context, bmp) },
                onUploadDocument = { type, uri -> authViewModel.uploadSingleKycDocument(context, type, uri) },
                onSkip = {
                    navController.navigate(Routes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onFinish = {
                    navController.navigate(Routes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Main scaffold with bottom nav (Strictly for Normal Members: Borrowers & Lenders)
        composable(Routes.MAIN) {
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            LaunchedEffect(authState.isLoggedIn) {
                if (!authState.isLoggedIn) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    val activeUserId = authState.currentUserId
                    val user = if (!activeUserId.isNullOrBlank()) userRepository.getUserById(activeUserId) else null
                    if (user?.role == "AGENT" && user.agentStatus == "APPROVED") {
                        navController.navigate(Routes.AGENT_MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }

            MainScaffold(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // --- Agent Role Workflows ---
        composable(Routes.ROLE_SELECTION) {
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
            val activeUserId = authState.currentUserId ?: currentUserId ?: ""
            val user by (if (activeUserId.isNotBlank()) userRepository.observeUser(activeUserId) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

            RoleSelectionScreen(
                userName = user?.name ?: "",
                onSelectNormalMember = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSelectAgent = {
                    navController.navigate(Routes.AGENT_APPLICATION)
                }
            )
        }

        composable(Routes.AGENT_APPLICATION) {
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            val agentRepository = com.loanzo.app.util.LocalAgentRepository.current
            val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
            val activeUserId = authState.currentUserId ?: currentUserId ?: ""
            val user by (if (activeUserId.isNotBlank()) userRepository.observeUser(activeUserId) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

            AgentApplicationScreen(
                userId = activeUserId,
                userName = user?.name ?: "",
                userPhone = user?.phone ?: "",
                userEmail = user?.email ?: "",
                onNavigateBack = { navController.popBackStack() },
                onSubmitSuccess = {
                    navController.navigate(Routes.AGENT_PENDING_APPROVAL) {
                        popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                    }
                },
                onSubmitApplication = { app ->
                    agentRepository.submitApplication(app)
                }
            )
        }

        composable(Routes.AGENT_PENDING_APPROVAL) {
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            val agentRepository = com.loanzo.app.util.LocalAgentRepository.current
            val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
            val activeUserId = authState.currentUserId ?: currentUserId ?: ""
            val application by (if (activeUserId.isNotBlank()) agentRepository.getApplication(activeUserId) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

            AgentPendingApprovalScreen(
                application = application,
                onEnterAgentDashboard = {
                    navController.navigate(Routes.AGENT_MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onReapply = {
                    navController.navigate(Routes.AGENT_APPLICATION)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AGENT_MAIN) {
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            val agentRepository = com.loanzo.app.util.LocalAgentRepository.current
            val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
            val activeUserId = authState.currentUserId ?: currentUserId ?: ""
            val user by (if (activeUserId.isNotBlank()) userRepository.observeUser(activeUserId) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
            val visits by (if (activeUserId.isNotBlank()) agentRepository.getVisitsForAgent(activeUserId) else kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())
            val scope = rememberCoroutineScope()

            LaunchedEffect(user?.role, user?.agentStatus) {
                if (user != null && (user?.role != "AGENT" || user?.agentStatus != "APPROVED")) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            AgentDashboardScreen(
                user = user,
                visits = visits,
                onToggleDutyStatus = { isOnDuty ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        agentRepository.setDutyStatus(activeUserId, isOnDuty)
                    }
                },
                onCompleteVisit = { visitId, remarks, collateralOk, borrowerOk, lenderOk, proof ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        agentRepository.completeVisit(
                            visitId = visitId,
                            agentRemarks = remarks,
                            isCollateralAuthentic = collateralOk,
                            isBorrowerIdentityVerified = borrowerOk,
                            isLenderIdentityVerified = lenderOk,
                            proofPhotoUris = proof
                        )
                    }
                },
                onSwitchToConsumer = {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        user?.let { userRepository.updateUser(it.copy(role = "USER")) }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                },
                onSwitchToAdmin = {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        user?.let { userRepository.updateUser(it.copy(role = "ADMIN")) }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            navController.navigate(Routes.APP_OWNER_HUB) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Loan detail and sub-screens
        composable(
            Routes.LOAN_DETAIL,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            val loanViewModel: LoanViewModel = hiltViewModel()

            LaunchedEffect(loanId) { loanViewModel.loadLoanDetail(loanId) }
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

            val context = androidx.compose.ui.platform.LocalContext.current
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isLender = loanState.selectedLoan?.lenderId == currentUserId

            if (loanState.signUrl != null) {
                ESignWebViewScreen(
                    signUrl = loanState.signUrl!!,
                    onSuccess = { loanViewModel.markAgreementSigned(loanId) },
                    onCancel = { loanViewModel.clearSignUrl() }
                )
            } else {
                LoanDetailScreen(
                    state = loanState,
                    isLender = isLender,
                    onBack = { navController.popBackStack() },
                    onRequestTranche = { navController.navigate(Routes.trancheRequest(loanId)) },
                    onMakeRepayment = { navController.navigate(Routes.repayment(loanId)) },
                    onAddPledge = { navController.navigate(Routes.pledge(loanId)) },
                    onViewAuditTrail = { navController.navigate(Routes.auditTrail(loanId)) },
                    onSignAgreement = { navController.navigate(Routes.agreementSigning(loanId)) },
                    onNavigateToChat = { navController.navigate(Routes.chat(loanId)) },
                    onNavigateToDocument = { navController.navigate(Routes.documentViewer(loanId)) },
                    onNavigateToGuarantors = { navController.navigate(Routes.guarantors(loanId)) },
                    onSendReminder = { loanViewModel.sendPaymentReminder(context) },
                    onExportLoanSummaryPdf = { loanViewModel.exportLoanSummary(context) },
                    onExportInterestCertPdf = { loanViewModel.exportInterestCertificate(context) },
                    onExportRepaymentsCsv = { loanViewModel.exportRepaymentsCsv(context) },
                    onWaivePenalty = { repayment -> loanViewModel.waivePenalty(repayment) },
                    onRestructureLoan = { newTenure, moratorium ->
                        loanViewModel.restructureLoan(loanId, newTenure, moratorium)
                    },
                    onAcceptProposal = { loanViewModel.acceptProposal(loanId) },
                    onDeclineProposal = { loanViewModel.declineProposal(loanId) },
                    onDisburseLoan = { amount, utr -> loanViewModel.disburseLoan(loanId, amount, utr) },
                    onDownloadNocCertificate = {
                        loanState.selectedLoan?.let { loan ->
                            loanViewModel.exportNocCertificate(context, loan)
                        }
                    }
                )
            }
        }

        composable(
            route = "${Routes.CREATE_LOAN}?mode={mode}",
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "REQUEST"
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "REQUEST"
            val isGrantMode = mode.equals("GRANT", ignoreCase = true)
            val loanViewModel: LoanViewModel = hiltViewModel()
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()
            val registeredUsers by loanViewModel.getAllRegisteredUsers().collectAsStateWithLifecycle(initialValue = emptyList())
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
            val currentUser by (if (!currentUserId.isNullOrBlank()) userRepository.observeUser(currentUserId!!) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
            val isKycCompleted = currentUser?.kycStatus == "VERIFIED"

            CreateLoanScreen(
                isGrantMode = isGrantMode,
                isKycCompleted = isKycCompleted,
                onNavigateToKyc = { navController.navigate(Routes.KYC) },
                onCreateLoan = { counterpartyId, amount, purpose, type, rate, model, tenure, freq, notes, pRate, pModel, pGrace ->
                    loanViewModel.createLoan(
                        counterpartyId = counterpartyId,
                        amount = amount,
                        purpose = purpose,
                        loanType = type,
                        interestRate = rate,
                        interestModel = model,
                        tenureMonths = tenure,
                        repaymentFrequency = freq,
                        notes = notes,
                        penaltyRate = pRate,
                        penaltyModel = pModel,
                        penaltyGraceDays = pGrace,
                        isGrantMode = isGrantMode
                    )
                },
                onBack = { navController.popBackStack() },
                loanCreated = loanState.loanCreated,
                registeredUsers = registeredUsers
            )
        }

        composable(Routes.LOAN_CALCULATOR) {
            LoanCalculatorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.TRANCHE_REQUEST,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            val loanViewModel: LoanViewModel = hiltViewModel()
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

            TrancheRequestScreen(
                loanId = loanId,
                ruleEvaluation = loanState.ruleEvaluation,
                onSubmit = { amount, payeeName, payeeUpiId, purpose, category, note ->
                    loanViewModel.requestTranche(loanId, amount, null, payeeName, payeeUpiId, purpose, category, note)
                },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(loanState.message) {
                if (loanState.message != null) {
                    navController.popBackStack()
                }
            }
        }

        composable(
            Routes.REPAYMENT,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            val loanViewModel: LoanViewModel = hiltViewModel()
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(loanId) { loanViewModel.loadLoanDetail(loanId) }

            MakeRepaymentScreen(
                loanId = loanId,
                outstandingAmount = loanState.selectedLoan?.outstandingAmount ?: 0.0,
                onSubmit = { amount, ref ->
                    loanViewModel.recordRepayment(loanId, amount, ref)
                },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(loanState.message) {
                if (loanState.message != null) {
                    navController.popBackStack()
                }
            }
        }

        composable(
            Routes.PLEDGE,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            val loanViewModel: LoanViewModel = hiltViewModel()
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

            PledgeScreen(
                loanId = loanId,
                onSubmit = { desc, type, value, weight ->
                    loanViewModel.addPledge(loanId, desc, type, value, weight)
                },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(loanState.message) {
                if (loanState.message != null) {
                    navController.popBackStack()
                }
            }
        }

        composable(
            Routes.AUDIT_TRAIL,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            val loanViewModel: LoanViewModel = hiltViewModel()
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(loanId) { loanViewModel.loadLoanDetail(loanId) }

            AuditTrailScreen(
                events = loanState.auditTrail,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.CHAT,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            ChatScreen(
                loanId = loanId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.DOCUMENT_VIEWER,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            DocumentViewerScreen(
                loanId = loanId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.GUARANTORS,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            val loanViewModel: LoanViewModel = hiltViewModel()
            LaunchedEffect(loanId) { loanViewModel.loadLoanDetail(loanId) }
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

            GuarantorScreen(
                loanId = loanId,
                guarantors = loanState.guarantors,
                onAddGuarantor = { name, phone, email, pan, rel ->
                    loanViewModel.addGuarantor(loanId, name, phone, email, pan, rel)
                },
                onUpdateConsent = { guarantorId, status ->
                    loanViewModel.updateGuarantorConsent(guarantorId, status)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.APP_OWNER_HUB) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val database = remember { com.loanzo.app.di.DatabaseModule.provideDatabase(context) }
            val verifications by database.verificationDao().getAllVerifications()
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val scope = rememberCoroutineScope()

            // Real-time Firestore listener — syncs PENDING tokens from user-side into local Room DB
            // so they appear on the owner dashboard and can be matched by SMS/WhatsApp interceptors
            LaunchedEffect(Unit) {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("verifications")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                val token = doc.getString("token") ?: continue
                                val phone = doc.getString("phone") ?: continue
                                val status = doc.getString("status") ?: "PENDING"
                                val username = doc.getString("username") ?: ""
                                
                                // Only insert if this token doesn't already exist locally
                                val existing = database.verificationDao().getByToken(token)
                                if (existing == null) {
                                    database.verificationDao().insertVerification(
                                        com.loanzo.app.data.entity.VerificationEntity(
                                            token = token,
                                            phone = phone,
                                            channel = "Firestore (User Request)",
                                            status = status,
                                            username = username,
                                            createdAt = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                        )
                                    )
                                } else if (existing.status != status && status == "VERIFIED") {
                                    // If Firestore says VERIFIED but local says PENDING, sync it
                                    database.verificationDao().markAsVerified(
                                        token = token,
                                        phone = phone,
                                        verifiedAt = System.currentTimeMillis()
                                    )
                                }
                            }
                        }
                    }
            }

            val agentApplications by database.agentDao().getAllApplications()
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val agentRepository = com.loanzo.app.util.LocalAgentRepository.current

            com.loanzo.app.ui.admin.AppOwnerVerificationScreen(
                verifications = verifications,
                agentApplications = agentApplications,
                onApproveVerification = { token, phone ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        database.verificationDao().markAsVerified(token = token, phone = phone)
                        val cleanPhone = phone.replace("\\D".toRegex(), "").takeLast(10)
                        if (cleanPhone.isNotEmpty()) {
                            try {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("verifications")
                                    .document(cleanPhone)
                                    .update("status", "VERIFIED")
                            } catch (_: Exception) {}
                        }
                    }
                },
                onManualVerify = { input ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        database.verificationDao().markAsVerified(token = input, phone = input)
                        val cleanPhone = input.replace("\\D".toRegex(), "").takeLast(10)
                        if (cleanPhone.isNotEmpty()) {
                            try {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("verifications")
                                    .document(cleanPhone)
                                    .update("status", "VERIFIED")
                            } catch (_: Exception) {}
                        }
                    }
                },
                onApproveAgentApplication = { appId ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        agentRepository.approveApplication(appId)
                    }
                },
                onRejectAgentApplication = { appId, remarks ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        agentRepository.rejectApplication(appId, remarks)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MARKETPLACE) {
            val marketplaceViewModel: com.loanzo.app.ui.marketplace.MarketplaceViewModel = hiltViewModel()
            val marketState by marketplaceViewModel.uiState.collectAsStateWithLifecycle()

            com.loanzo.app.ui.marketplace.MarketplaceFeedScreen(
                state = marketState,
                onTabSelected = { marketplaceViewModel.setTab(it) },
                onSearchQueryChange = { marketplaceViewModel.setSearchQuery(it) },
                onCategoryTagSelected = { marketplaceViewModel.setCategoryTag(it) },
                onVouchPost = { marketplaceViewModel.vouchForPost(it) },
                onSubmitBid = { postId, amount, rate, tenure, msg ->
                    marketplaceViewModel.submitBid(postId, amount, rate, tenure, msg) {}
                },
                onNavigateToCreatePost = { mode ->
                    navController.navigate("${Routes.CREATE_MARKETPLACE_POST}?mode=$mode")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            "${Routes.CREATE_MARKETPLACE_POST}?mode={mode}",
            arguments = listOf(navArgument("mode") { defaultValue = "OFFER_TO_LEND"; type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "OFFER_TO_LEND"
            val marketplaceViewModel: com.loanzo.app.ui.marketplace.MarketplaceViewModel = hiltViewModel()
            val userRepository = com.loanzo.app.util.LocalUserRepository.current
            val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
            val currentUser by (if (!currentUserId.isNullOrBlank()) userRepository.observeUser(currentUserId!!) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
            val isKycCompleted = currentUser?.kycStatus == "VERIFIED"

            com.loanzo.app.ui.marketplace.CreateMarketplacePostScreen(
                initialMode = mode,
                isKycCompleted = isKycCompleted,
                onNavigateToKyc = { navController.navigate(Routes.KYC) },
                onPublish = { title, desc, postType, min, max, rate, tenure, cat, city, col ->
                    marketplaceViewModel.publishPost(
                        title = title,
                        description = desc,
                        postType = postType,
                        minAmount = min,
                        maxAmount = max,
                        interestRate = rate,
                        tenureMonths = tenure,
                        purposeCategory = cat,
                        locationCity = city,
                        collateralOffered = col,
                        onSuccess = {
                            navController.popBackStack()
                        }
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hideBottomNav = currentRoute == Routes.PROFILE || currentRoute == Routes.NOTIFICATIONS

    var isQuickActionMenuOpen by rememberSaveable { mutableStateOf(false) }

    // ─── Onboarding state ───────────────────────────────────────────────────
    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val scope = rememberCoroutineScope()

    val welcomeCompleted by userRepository.isWelcomeOnboardingCompleted()
        .collectAsStateWithLifecycle(initialValue = true) // default true to avoid flash
    val navTooltipsSeen by userRepository.areNavTooltipsSeen()
        .collectAsStateWithLifecycle(initialValue = true)
    val postButtonSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_POST_BUTTON_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val activeTourId by userRepository.getActiveTourId()
        .collectAsStateWithLifecycle(initialValue = null)
    val activeTourStep by userRepository.getActiveTourStep()
        .collectAsStateWithLifecycle(initialValue = 0)

    val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
    val currentUser by (if (!currentUserId.isNullOrBlank()) userRepository.observeUser(currentUserId!!) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val isKycCompleted = currentUser?.kycStatus == "VERIFIED"

    var showKycRequiredDialog by remember { mutableStateOf(false) }
    var kycDialogMessage by remember { mutableStateOf("") }

    // Back navigation handling for modal quick action menu and guided tour overlay
    androidx.activity.compose.BackHandler(enabled = isQuickActionMenuOpen) {
        isQuickActionMenuOpen = false
    }
    androidx.activity.compose.BackHandler(enabled = activeTourId != null) {
        scope.launch { userRepository.clearActiveTour() }
    }
    // ────────────────────────────────────────────────────────────────────────

    val rotationAngle by animateFloatAsState(
        targetValue = if (isQuickActionMenuOpen) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    val radialProgress by animateFloatAsState(
        targetValue = if (isQuickActionMenuOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "radial_progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!hideBottomNav) {
                    val notifViewModel: com.loanzo.app.ui.notification.NotificationViewModel = hiltViewModel()
                val notifState by notifViewModel.uiState.collectAsStateWithLifecycle()
                val unreadCount = notifState.unreadCount

                val bottomNavItems = listOf(
                    BottomNavItem(Routes.DASHBOARD, stringResource(R.string.nav_home), Icons.Filled.Home, Icons.Outlined.Home),
                    BottomNavItem(Routes.LOANS, stringResource(R.string.nav_loans), Icons.Filled.Receipt, Icons.Outlined.Receipt),
                    BottomNavItem(Routes.NOTIFICATIONS, stringResource(R.string.nav_alerts), Icons.Filled.Notifications, Icons.Outlined.Notifications),
                    BottomNavItem(Routes.PROFILE, stringResource(R.string.nav_profile), Icons.Filled.Person, Icons.Outlined.Person)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // Navigation Bar Surface with 5-slot layout
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = BrandIceBlue,
                            unselectedIconColor = TextSlateMuted,
                            unselectedTextColor = TextSlateMuted
                        )

                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            // 1. Home
                            val homeItem = bottomNavItems[0]
                            val homeSelected = currentRoute == homeItem.route
                            NavigationBarItem(
                                selected = homeSelected,
                                onClick = {
                                    if (currentRoute != homeItem.route) {
                                        innerNavController.navigate(homeItem.route) {
                                            popUpTo(Routes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(if (homeSelected) homeItem.selectedIcon else homeItem.unselectedIcon, contentDescription = homeItem.label) },
                                label = { Text(homeItem.label, fontWeight = if (homeSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = navItemColors
                            )

                            // 2. Loans
                            val loansItem = bottomNavItems[1]
                            val loansSelected = currentRoute == loansItem.route
                            NavigationBarItem(
                                selected = loansSelected,
                                onClick = {
                                    if (currentRoute != loansItem.route) {
                                        innerNavController.navigate(loansItem.route) {
                                            popUpTo(Routes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(if (loansSelected) loansItem.selectedIcon else loansItem.unselectedIcon, contentDescription = loansItem.label) },
                                label = { Text(loansItem.label, fontWeight = if (loansSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = navItemColors
                            )

                            // 3. Center Cradle Slot (aligned beneath the floating action button)
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    isQuickActionMenuOpen = !isQuickActionMenuOpen
                                },
                                enabled = true,
                                icon = { Spacer(modifier = Modifier.size(24.dp)) },
                                label = {
                                    Text(
                                        text = "Post",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                            )

                            // 4. Alerts
                            val notifItem = bottomNavItems[2]
                            val notifSelected = currentRoute == notifItem.route
                            NavigationBarItem(
                                selected = notifSelected,
                                onClick = {
                                    if (currentRoute != notifItem.route) {
                                        innerNavController.navigate(notifItem.route) {
                                            popUpTo(Routes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    if (unreadCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = Red400) {
                                                    Text(if (unreadCount > 99) "99+" else unreadCount.toString(), fontSize = 10.sp)
                                                }
                                            }
                                        ) {
                                            Icon(if (notifSelected) notifItem.selectedIcon else notifItem.unselectedIcon, contentDescription = notifItem.label)
                                        }
                                    } else {
                                        Icon(if (notifSelected) notifItem.selectedIcon else notifItem.unselectedIcon, contentDescription = notifItem.label)
                                    }
                                },
                                label = { Text(notifItem.label, fontWeight = if (notifSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = navItemColors
                            )

                            // 5. Profile
                            val profileItem = bottomNavItems[3]
                            val profileSelected = currentRoute == profileItem.route
                            NavigationBarItem(
                                selected = profileSelected,
                                onClick = {
                                    if (currentRoute != profileItem.route) {
                                        innerNavController.navigate(profileItem.route) {
                                            popUpTo(Routes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(if (profileSelected) profileItem.selectedIcon else profileItem.unselectedIcon, contentDescription = profileItem.label) },
                                label = { Text(profileItem.label, fontWeight = if (profileSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = navItemColors
                            )
                        }
                    }

                    // Round Center Plus Button with Adaptive Styling
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-28).dp)
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                isQuickActionMenuOpen = !isQuickActionMenuOpen
                                if (!postButtonSeen) {
                                    scope.launch {
                                        userRepository.markGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_POST_BUTTON_SEEN)
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 10.dp
                            ),
                            modifier = Modifier.size(55.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick Action",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(rotationAngle)
                            )
                        }
                    }
                }
                }
            }
        ) { padding ->
            NavHost(
                navController = innerNavController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220, easing = FastOutSlowInEasing)) },
                popExitTransition = { fadeOut(animationSpec = tween(180)) }
            ) {
            composable(Routes.DASHBOARD) {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                val marketplaceViewModel: com.loanzo.app.ui.marketplace.MarketplaceViewModel = hiltViewModel()
                val state by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                val marketState by marketplaceViewModel.uiState.collectAsStateWithLifecycle()

                DashboardScreen(
                    state = state,
                    marketState = marketState,
                    onTabSelected = { marketplaceViewModel.setTab(it) },
                    onSearchQueryChange = { marketplaceViewModel.setSearchQuery(it) },
                    onCategoryTagSelected = { marketplaceViewModel.setCategoryTag(it) },
                    onVouchPost = { marketplaceViewModel.vouchForPost(it) },
                    onSubmitBid = { postId, amount, rate, tenure, msg ->
                        if (isKycCompleted) {
                            marketplaceViewModel.submitBid(postId, amount, rate, tenure, msg) {}
                        } else {
                            kycDialogMessage = "In compliance with lending rules, identity verification (KYC) must be completed before you can propose loan bids or borrow."
                            showKycRequiredDialog = true
                        }
                    },
                    onNavigateToCreatePost = { mode -> 
                        if (isKycCompleted) {
                            navController.navigate("${Routes.CREATE_MARKETPLACE_POST}?mode=$mode")
                        } else {
                            kycDialogMessage = "You must complete identity verification (KYC) before publishing loan offers or requests."
                            showKycRequiredDialog = true
                        }
                    },
                    onNavigateToCreateLoan = { 
                        if (isKycCompleted) {
                            navController.navigate("${Routes.CREATE_LOAN}?mode=REQUEST")
                        } else {
                            kycDialogMessage = "You must complete identity verification (KYC) before borrowing or requesting loans."
                            showKycRequiredDialog = true
                        }
                    },
                    onNavigateToCalculator = { navController.navigate(Routes.LOAN_CALCULATOR) },
                    onNavigateToLoanDetail = { loanId -> navController.navigate(Routes.loanDetail(loanId)) },
                    onNavigateToProfile = { innerNavController.navigate(Routes.PROFILE) },
                    onNavigateToApproval = { disbursementId ->
                        val relatedLoan = state.loansAsLender.find { l -> l.loanId == disbursementId || state.pendingApprovals.any { p -> p.disbursementId == disbursementId && p.loanId == l.loanId } }
                        if (relatedLoan != null) {
                            navController.navigate(Routes.loanDetail(relatedLoan.loanId))
                        }
                    },
                    onNavigateToLoansTab = { innerNavController.navigate(Routes.LOANS) },
                    onNavigateToChat = { loanId -> navController.navigate(Routes.chat(loanId)) },
                    onNavigateToKyc = { navController.navigate(Routes.KYC) },
                    onPushDemoData = { authViewModel.pushDemoData() }
                )
            }

            composable(Routes.LOANS) {
                val loanViewModel: LoanViewModel = hiltViewModel()
                LaunchedEffect(Unit) { loanViewModel.loadLoans() }
                val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()
                val userRepository = com.loanzo.app.util.LocalUserRepository.current
                val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
                val loansGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_LOANS_SEEN)
                    .collectAsStateWithLifecycle(initialValue = true)
                val scope = rememberCoroutineScope()

                Box(modifier = Modifier.fillMaxSize()) {
                    LoanListScreen(
                        loans = loanState.loans,
                        currentUserId = currentUserId ?: "",
                        isLoading = loanState.isLoading,
                        onNavigateToLoanDetail = { navController.navigate(Routes.loanDetail(it)) },
                        onNavigateToCreateLoan = { mode -> 
                            if (isKycCompleted) {
                                navController.navigate("${Routes.CREATE_LOAN}?mode=$mode")
                            } else {
                                kycDialogMessage = "You must complete identity verification (KYC) before borrowing or granting loans."
                                showKycRequiredDialog = true
                            }
                        }
                    )

                    if (!loansGuideSeen) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            ContextualGuideCard(
                                visible = true,
                                icon = Icons.Default.Receipt,
                                title = "Track Every Loan",
                                body = "Tap any loan card to see repayment schedules, document agreements, audit history, or chat directly with your counterparty.",
                                onDismiss = {
                                    scope.launch {
                                        userRepository.markGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_LOANS_SEEN)
                                    }
                                },
                                autoDismissSeconds = 8
                            )
                        }
                    }
                }
            }

            composable(Routes.NOTIFICATIONS) {
                val notifViewModel: com.loanzo.app.ui.notification.NotificationViewModel = hiltViewModel()
                val notifState by notifViewModel.uiState.collectAsStateWithLifecycle()

                com.loanzo.app.ui.notification.NotificationScreen(
                    state = notifState,
                    onFilterChange = { notifViewModel.filter(it) },
                    onSearchQueryChange = { notifViewModel.setSearchQuery(it) },
                    onDateFilterChange = { notifViewModel.setDateFilter(it) },
                    onCategoryTagChange = { notifViewModel.setCategoryTag(it) },
                    onClearAllFilters = { notifViewModel.clearAllFilters() },
                    onMarkAsRead = { notifViewModel.markAsRead(it) },
                    onMarkAllAsRead = { notifViewModel.markAllAsRead() },
                    onDelete = { notifViewModel.deleteNotification(it) },
                    onClearAll = { notifViewModel.clearAll() },
                    onRefresh = { notifViewModel.refreshDeadlines() },
                    onNavigateToLoan = { loanId -> navController.navigate(Routes.loanDetail(loanId)) },
                    onBack = {
                        if (!innerNavController.popBackStack()) {
                            innerNavController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.DASHBOARD) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.PROFILE) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val userRepository = com.loanzo.app.util.LocalUserRepository.current
                val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
                val targetUserId = authState.currentUserId ?: currentUserId
                val user by (if (targetUserId != null) userRepository.observeUser(targetUserId) else kotlinx.coroutines.flow.flowOf(null))
                    .collectAsStateWithLifecycle(initialValue = null)
                val themeMode by userRepository.getThemeMode().collectAsStateWithLifecycle(initialValue = "SYSTEM")
                val appLanguage by userRepository.getAppLanguage().collectAsStateWithLifecycle(initialValue = "en")
                val scope = rememberCoroutineScope()

                ProfileScreen(
                    user = user,
                    onNavigateToKyc = { navController.navigate(Routes.KYC) },
                    onNavigateToAdminHub = { navController.navigate(Routes.APP_OWNER_HUB) },
                    currentLanguageCode = appLanguage,
                    onSelectLanguage = { code -> scope.launch { userRepository.setAppLanguage(code) } },
                    onUploadKycDocument = { uri, type -> authViewModel.uploadSingleKycDocument(context, type, uri) },
                    onUpdateBankDetails = { accNum, ifsc -> authViewModel.updateBankDetails(accNum, ifsc) },
                    onPushDemoData = { cb -> authViewModel.pushDemoData(cb) },
                    onClearDemoData = { cb -> authViewModel.clearDemoData(cb) },
                    isUploadingPan = authState.isUploadingPan,
                    isUploadingAadhaar = authState.isUploadingAadhaar,
                    uploadMessage = authState.error,
                    onClearUploadMessage = { authViewModel.clearError() },
                    onNavigateToAgent = {
                        if (user?.role == "AGENT" && user?.agentStatus == "APPROVED") {
                            navController.navigate(Routes.AGENT_MAIN)
                        } else if (user?.role == "AGENT" && user?.agentStatus == "PENDING") {
                            navController.navigate(Routes.AGENT_PENDING_APPROVAL)
                        } else {
                            navController.navigate(Routes.ROLE_SELECTION)
                        }
                    },
                    themeMode = themeMode,
                    onSetThemeMode = { mode -> scope.launch { userRepository.setThemeMode(mode) } },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        if (!innerNavController.popBackStack()) {
                            innerNavController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.DASHBOARD) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.FINANCIAL_HEALTH) {
                FinancialHealthScreen(onBack = { innerNavController.popBackStack() })
            }
        }
    }

    // ─── Welcome Onboarding Carousel (first-time login gate) ────────────────
    if (!welcomeCompleted) {
        WelcomeOnboardingCarousel(
            onComplete = { scope.launch { userRepository.setWelcomeOnboardingCompleted() } },
            onSkip    = { scope.launch { userRepository.setWelcomeOnboardingCompleted() } }
        )
    }

    // ─── Active Guided Tour Overlay ──────────────────────────────────────────
    if (activeTourId != null) {
        GuidedTourOverlay(
            tourId      = activeTourId!!,
            currentStep = activeTourStep,
            onNext      = { nextStep -> scope.launch { userRepository.advanceTourStep(nextStep) } },
            onBack      = { prevStep -> scope.launch { userRepository.advanceTourStep(prevStep) } },
            onFinish    = { scope.launch { userRepository.clearActiveTour() } },
            onDismiss   = { scope.launch { userRepository.clearActiveTour() } }
        )
    }

    // Dimmed backdrop scrim
    AnimatedVisibility(
        visible = !hideBottomNav && isQuickActionMenuOpen,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { isQuickActionMenuOpen = false }
        )
    }

    // Circular Radial Satellite Menu (Fan-out arc around the center yellow button)
    if (!hideBottomNav && radialProgress > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 54.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Satellite 1: Top-Left (140°) - Post Loan Lending Offer (Lender)
            CircularSatelliteItem(
                icon = Icons.Default.Upload,
                label = "Lend Offer",
                containerColor = Gold500,
                iconTint = Navy900,
                progress = radialProgress,
                offsetX = (-90 * radialProgress).dp,
                offsetY = (-66 * radialProgress).dp,
                onClick = {
                    isQuickActionMenuOpen = false
                    if (isKycCompleted) {
                        navController.navigate("${Routes.CREATE_MARKETPLACE_POST}?mode=OFFER_TO_LEND")
                    } else {
                        kycDialogMessage = "You must complete identity verification (KYC) before posting lending offers."
                        showKycRequiredDialog = true
                    }
                }
            )

            // Satellite 2: Center Straight-Up (90°) - Direct P2P (Known Contact)
            CircularSatelliteItem(
                icon = Icons.Default.People,
                label = "Direct P2P",
                containerColor = Color(0xFF7C4DFF),
                iconTint = Color.White,
                size = 56.dp,
                progress = radialProgress,
                offsetX = 0.dp,
                offsetY = (-120 * radialProgress).dp,
                onClick = {
                    isQuickActionMenuOpen = false
                    if (isKycCompleted) {
                        navController.navigate("${Routes.CREATE_LOAN}?mode=GRANT")
                    } else {
                        kycDialogMessage = "You must complete identity verification (KYC) before creating or granting direct loans."
                        showKycRequiredDialog = true
                    }
                }
            )

            // Satellite 3: Top-Right (40°) - Post Loan Request (Borrower)
            CircularSatelliteItem(
                icon = Icons.Default.Download,
                label = "Seek Loan",
                containerColor = Emerald400,
                iconTint = Navy900,
                progress = radialProgress,
                offsetX = (90 * radialProgress).dp,
                offsetY = (-66 * radialProgress).dp,
                onClick = {
                    isQuickActionMenuOpen = false
                    if (isKycCompleted) {
                        navController.navigate("${Routes.CREATE_MARKETPLACE_POST}?mode=SEEKING_LOAN")
                    } else {
                        kycDialogMessage = "You must complete identity verification (KYC) before seeking or borrowing loans."
                        showKycRequiredDialog = true
                    }
                }
            )
        }
    }

        // ─── Layer 3: Guided Tour Overlay ───────────────────────────────────────
        if (activeTourId != null) {
            val currentTourId = activeTourId!!
            val tour = AppTours.getById(currentTourId)
            if (tour != null) {
                GuidedTourOverlay(
                    tourId = currentTourId,
                    currentStep = activeTourStep,
                    onNext = { nextStep ->
                        scope.launch { userRepository.advanceTourStep(nextStep) }
                    },
                    onBack = { prevStep ->
                        scope.launch { userRepository.advanceTourStep(prevStep) }
                    },
                    onFinish = {
                        scope.launch { userRepository.clearActiveTour() }
                    },
                    onDismiss = {
                        scope.launch { userRepository.clearActiveTour() }
                    }
                )
            }
        }

        // ─── Layer 1: Welcome Onboarding Carousel Overlay ───────────────────────
        if (!welcomeCompleted) {
            WelcomeOnboardingCarousel(
                onComplete = {
                    scope.launch { userRepository.setWelcomeOnboardingCompleted() }
                },
                onSkip = {
                    scope.launch { userRepository.setWelcomeOnboardingCompleted() }
                }
            )
        }

        // ─── KYC Required Dialog ────────────────────────────────────────────────
        if (showKycRequiredDialog) {
            AlertDialog(
                onDismissRequest = { showKycRequiredDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GoldCoinCream),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = GoldCoinAmber,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text(
                        "KYC Verification Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Text(
                        if (kycDialogMessage.isNotBlank()) kycDialogMessage else "In compliance with lending rules and security protocols, identity verification (KYC) must be completed before you can borrow, request, or grant loans.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showKycRequiredDialog = false
                            navController.navigate(Routes.KYC)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCoinRich, contentColor = Navy900)
                    ) {
                        Text("Complete KYC Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showKycRequiredDialog = false }
                    ) {
                        Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            )
        }
    }
}

/**
 * Circular radial satellite item fanning out from the center yellow button.
 */
@Composable
fun CircularSatelliteItem(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color = Color.White,
    size: Dp = 50.dp,
    progress: Float,
    offsetX: Dp,
    offsetY: Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                scaleX = progress
                scaleY = progress
                alpha = progress.coerceIn(0f, 1f)
            }
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            shadowElevation = (8 * progress).dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            modifier = Modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    loans: List<com.loanzo.app.data.entity.LoanEntity>,
    currentUserId: String,
    isLoading: Boolean,
    onNavigateToLoanDetail: (String) -> Unit,
    onNavigateToCreateLoan: (mode: String) -> Unit
) {
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) } // 0: Lent, 1: Borrowed
    
    val lentLoans = remember(loans, currentUserId) {
        if (currentUserId.isBlank()) loans.filter { it.loanType == "GRANT" || it.lenderId.isNotBlank() }
        else loans.filter { it.lenderId == currentUserId }
    }
    val borrowedLoans = remember(loans, currentUserId) {
        if (currentUserId.isBlank()) loans.filter { it.loanType == "REQUEST" || it.borrowerId.isNotBlank() }
        else loans.filter { it.borrowerId == currentUserId }
    }

    val currentList = if (selectedTab == 0) lentLoans else borrowedLoans
    val totalDisbursed = currentList.sumOf { it.disbursedAmount }
    val totalOutstanding = currentList.sumOf { it.outstandingAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_loans), fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val mode = if (selectedTab == 0) "GRANT" else "REQUEST"
                    onNavigateToCreateLoan(mode)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Grant Loan" else "Request Loan",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Segmented Capsule Selector at the top of Loans screen
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedCapsuleTab(
                    tabs = listOf(
                        stringResource(R.string.tab_lent),
                        stringResource(R.string.tab_borrowed)
                    ),
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Portfolio Card for the active tab (Lent / Borrowed)
            item {
                HeroPortfolioCard(
                    isLenderPerspective = (selectedTab == 0),
                    totalDisbursed = totalDisbursed,
                    totalOutstanding = totalOutstanding,
                    activeLoanCount = currentList.count { it.status == "ACTIVE" },
                    overdueCount = currentList.count { it.status == "DEFAULTED" },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section Header
            item {
                SectionHeader(
                    title = if (selectedTab == 0) "Loans You Gave (${currentList.size})" else "Loans You Took (${currentList.size})",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold500)
                    }
                }
            } else if (currentList.isEmpty()) {
                item {
                    EmptyState(
                        icon = if (selectedTab == 0) Icons.Default.Upload else Icons.Default.Download,
                        title = if (selectedTab == 0) "No loans given yet" else "No loans taken yet",
                        subtitle = if (selectedTab == 0) "Tap '+' below to grant a loan to a borrower" else "Tap '+' below to request a loan from a lender"
                    )
                }
            } else {
                items(currentList.size) { index ->
                    val loan = currentList[index]
                    LoanSummaryCard(
                        loanId = loan.loanId,
                        purpose = loan.purpose,
                        amount = loan.sanctionedAmount,
                        outstanding = loan.outstandingAmount,
                        status = loan.status,
                        counterpartyName = if (selectedTab == 0) "Borrower" else "Lender",
                        date = loan.createdAt.toDateString(),
                        onClick = { onNavigateToLoanDetail(loan.loanId) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        loanType = loan.loanType
                    )
                }
            }
        }
    }
}
