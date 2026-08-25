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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.loanzo.app.ui.auth.*
import com.loanzo.app.ui.dashboard.*
import com.loanzo.app.ui.loan.*
import kotlinx.coroutines.launch
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toDateString

// Route definitions
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
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
    const val PROFILE = "profile"
    const val PAYMENT_SUCCESS = "payment_success"

    // Feature Routes
    const val TRANSLATION_DEMO = "translation_demo"
    const val FINANCIAL_HEALTH = "financial_health"

    fun loanDetail(loanId: String) = "loan_detail/$loanId"
    fun trancheRequest(loanId: String) = "tranche_request/$loanId"
    fun repayment(loanId: String) = "repayment/$loanId"
    fun pledge(loanId: String) = "pledge/$loanId"
    fun auditTrail(loanId: String) = "audit_trail/$loanId"
    fun chat(loanId: String) = "chat/$loanId"
    fun documentViewer(loanId: String) = "document_viewer/$loanId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.LOANS, "Loans", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun LoanzoNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (authState.isLoggedIn) Routes.MAIN else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth flow
        composable(Routes.LOGIN) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            
            LoginScreen(
                onSendOtp = { phone -> activity?.let { authViewModel.sendOtp(phone, it) } },
                onVerifyOtp = { code -> authViewModel.verifyOtp(code) },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                isLoading = authState.isLoading,
                error = authState.error,
                isOtpSent = authState.isOtpSent,
                onClearError = { authViewModel.clearError() }
            )

            LaunchedEffect(authState.isLoggedIn) {
                if (authState.isLoggedIn) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegister = { name, email, phone, role ->
                    authViewModel.register(name, email, phone, role)
                },
                onNavigateToLogin = { navController.popBackStack() },
                isLoading = authState.isLoading,
                error = authState.error,
                onClearError = { authViewModel.clearError() }
            )

            LaunchedEffect(authState.registrationSuccess) {
                if (authState.registrationSuccess) {
                    navController.navigate(Routes.KYC) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.KYC) {
            val context = androidx.compose.ui.platform.LocalContext.current
            KycScreen(
                currentStep = authState.kycStep,
                isDiditLoading = authState.isDiditLoading,
                diditStatus = authState.diditStatus,
                onStartDiditKyc = { authViewModel.startDiditVerification(context) },
                onCompleteStep = { step, data ->
                    authState.currentUserId?.let { userId ->
                        authViewModel.completeKycStep(step, userId, data)
                    }
                },
                onFinish = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Main scaffold with bottom nav
        composable(Routes.MAIN) {
            MainScaffold(
                navController = navController,
                authViewModel = authViewModel
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

            LoanDetailScreen(
                state = loanState,
                onBack = { navController.popBackStack() },
                onRequestTranche = { navController.navigate(Routes.trancheRequest(loanId)) },
                onMakeRepayment = { navController.navigate(Routes.repayment(loanId)) },
                onAddPledge = { navController.navigate(Routes.pledge(loanId)) },
                onViewAuditTrail = { navController.navigate(Routes.auditTrail(loanId)) },
                onSignAgreement = { loanViewModel.initiateSigning(loanId) },
                onNavigateToChat = { navController.navigate(Routes.chat(loanId)) },
                onNavigateToDocument = { navController.navigate(Routes.documentViewer(loanId)) }
            )
        }

        composable(Routes.CREATE_LOAN) {
            val loanViewModel: LoanViewModel = hiltViewModel()
            val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

            CreateLoanScreen(
                onCreateLoan = { lenderId, amount, purpose, type, rate, model, tenure, freq, notes ->
                    loanViewModel.createLoan(lenderId, amount, purpose, type, rate, model, tenure, freq, notes)
                },
                onBack = { navController.popBackStack() },
                loanCreated = loanState.loanCreated
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.route) {
                                innerNavController.navigate(item.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Gold500,
                            selectedTextColor = Gold500,
                            indicatorColor = Gold500.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNavController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                val state by dashboardViewModel.uiState.collectAsStateWithLifecycle()

                if (state.role == "LENDER") {
                    LenderDashboardScreen(
                        state = state,
                        onNavigateToLoanDetail = { navController.navigate(Routes.loanDetail(it)) },
                        onNavigateToProfile = { innerNavController.navigate(Routes.PROFILE) },
                        onNavigateToApproval = { /* Navigate to approval detail */ }
                    )
                } else {
                    BorrowerDashboardScreen(
                        state = state,
                        onNavigateToCreateLoan = { navController.navigate(Routes.CREATE_LOAN) },
                        onNavigateToCalculator = { navController.navigate(Routes.LOAN_CALCULATOR) },
                        onNavigateToLoanDetail = { loanId -> navController.navigate(Routes.loanDetail(loanId)) },
                        onNavigateToProfile = { innerNavController.navigate(Routes.PROFILE) },
                        onNavigateToFinancialHealth = { innerNavController.navigate(Routes.FINANCIAL_HEALTH) }
                    )
                }
            }

            composable(Routes.LOANS) {
                val loanViewModel: LoanViewModel = hiltViewModel()
                LaunchedEffect(Unit) { loanViewModel.loadLoans() }
                val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()

                LoanListScreen(
                    loans = loanState.loans,
                    isLoading = loanState.isLoading,
                    onNavigateToLoanDetail = { navController.navigate(Routes.loanDetail(it)) },
                    onNavigateToCreateLoan = { navController.navigate(Routes.CREATE_LOAN) }
                )
            }

            composable(Routes.PROFILE) {
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val userRepo = authViewModel // We'll get user data from VM
                var user by remember { mutableStateOf<com.loanzo.app.data.entity.UserEntity?>(null) }
                
                // Theme state
                val userRepository = com.loanzo.app.util.LocalUserRepository.current
                val themeMode by userRepository.getThemeMode().collectAsStateWithLifecycle(initialValue = "SYSTEM")
                val scope = rememberCoroutineScope()

                // Simple profile with data from auth state
                ProfileScreen(
                    user = user,
                    onNavigateToKyc = { navController.navigate(Routes.KYC) },
                    onNavigateToTranslation = { navController.navigate(Routes.TRANSLATION_DEMO) },
                    themeMode = themeMode,
                    onSetThemeMode = { mode -> scope.launch { userRepository.setThemeMode(mode) } },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { innerNavController.popBackStack() }
                )

                // Load user data
                LaunchedEffect(authState.currentUserId) {
                    authState.currentUserId?.let { userId ->
                        // We need to access the user repo through a proper mechanism
                        // For now, create a simple user from auth state
                        user = com.loanzo.app.data.entity.UserEntity(
                            userId = userId,
                            name = "User",
                            email = "",
                            phone = "",
                            role = authState.currentRole ?: "BORROWER",
                            kycStatus = authState.kycStatus
                        )
                    }
                }
            }

            composable(Routes.FINANCIAL_HEALTH) {
                FinancialHealthScreen(onBack = { innerNavController.popBackStack() })
            }
            composable(Routes.TRANSLATION_DEMO) {
                TranslationDemoScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    loans: List<com.loanzo.app.data.entity.LoanEntity>,
    isLoading: Boolean,
    onNavigateToLoanDetail: (String) -> Unit,
    onNavigateToCreateLoan: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_loans), fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateLoan,
                containerColor = Gold500,
                contentColor = Navy900
            ) {
                Icon(Icons.Default.Add, "Create Loan")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (loans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                com.loanzo.app.ui.components.EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "No loans yet",
                    subtitle = "Tap + to create your first loan"
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(loans.size) { index ->
                    val loan = loans[index]
                    com.loanzo.app.ui.components.LoanSummaryCard(
                        loanId = loan.loanId,
                        purpose = loan.purpose,
                        amount = loan.sanctionedAmount,
                        outstanding = loan.outstandingAmount,
                        status = loan.status,
                        counterpartyName = "Counterparty",
                        date = loan.createdAt.toDateString(),
                        onClick = { onNavigateToLoanDetail(loan.loanId) }
                    )
                }
            }
        }
    }
}
