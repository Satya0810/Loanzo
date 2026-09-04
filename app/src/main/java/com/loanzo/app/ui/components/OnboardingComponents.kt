package com.loanzo.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loanzo.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Data classes for Guided Tour system
// ─────────────────────────────────────────────────────────────────────────────

data class TourStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val targetRoute: String? = null
)

data class TourDefinition(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val steps: List<TourStep>
)

/** All 5 available guided tours */
object AppTours {
    const val REQUEST_LOAN   = "tour_request_loan"
    const val PROFILE_SETUP  = "tour_profile_setup"
    const val MARKETPLACE    = "tour_marketplace"
    const val LOAN_TRACKING  = "tour_loan_tracking"
    const val DOCUMENT_VAULT = "tour_document_vault"

    val all = listOf(
        TourDefinition(
            id = REQUEST_LOAN,
            title = "How to Request a Loan",
            icon = Icons.Default.RequestPage,
            steps = listOf(
                TourStep("Open the Dashboard", "Your financial home screen shows active loans, health score, and quick-action buttons.", Icons.Default.Dashboard),
                TourStep("Tap the '+' Post Button", "The gold button at the bottom centre opens a radial menu of posting options.", Icons.Default.Add),
                TourStep("Select 'Direct P2P'", "Choose Direct P2P to send a loan request to someone you know, or 'Borrow' to post publicly.", Icons.Default.People),
                TourStep("Fill the Loan Form", "Enter loan amount, purpose, tenure, repayment frequency, and interest terms agreed upon.", Icons.Default.EditNote),
                TourStep("Review & Submit", "Double-check all details, agree to terms, and submit. Your counterparty will receive a notification.", Icons.Default.CheckCircle)
            )
        ),
        TourDefinition(
            id = PROFILE_SETUP,
            title = "Setting Up Your Profile",
            icon = Icons.Default.ManageAccounts,
            steps = listOf(
                TourStep("Open Profile", "Tap the Profile icon on the bottom navigation bar to access your identity hub.", Icons.Default.Person),
                TourStep("Personal Information", "Tap Personal Info to update your name, date of birth, and address details.", Icons.Default.Badge),
                TourStep("Complete Your KYC", "Tap Document Vault to upload PAN, Aadhaar, and selfie for full KYC verification.", Icons.Default.VerifiedUser),
                TourStep("Link Bank Account", "Tap Bank and UPI to link your bank account and UPI ID for seamless fund transfers.", Icons.Default.AccountBalance),
                TourStep("Preferences and Security", "Configure dark/light theme, language, biometric login, and notification settings.", Icons.Default.Tune)
            )
        ),
        TourDefinition(
            id = MARKETPLACE,
            title = "Using the Marketplace",
            icon = Icons.Default.Storefront,
            steps = listOf(
                TourStep("Open Marketplace", "Tap the '+' Post button and select Lend Offer or navigate via the loans tab to reach the Marketplace.", Icons.Default.Explore),
                TourStep("Browse Offers and Requests", "Use tabs to switch between lender offers and borrower requests. Filter by amount, rate, or category.", Icons.Default.FilterList),
                TourStep("Submit a Bid", "Found an offer? Tap it and hit Submit Bid to propose your lending terms. The poster reviews your bid.", Icons.Default.Gavel),
                TourStep("Vouch for a Post", "Trust a poster? Hit Vouch to endorse them - it increases their visibility and credibility.", Icons.Default.Recommend),
                TourStep("Post Your Own Offer", "Tap Post Offer to list your lending availability. Set amount range, interest rate, and tenure.", Icons.Default.PostAdd)
            )
        ),
        TourDefinition(
            id = LOAN_TRACKING,
            title = "Track Your Loans",
            icon = Icons.Default.TrackChanges,
            steps = listOf(
                TourStep("Go to the Loans Tab", "The Loans tab shows all loans you have given or taken, with status badges.", Icons.Default.Receipt),
                TourStep("Open Loan Detail", "Tap any loan to see the full breakdown: outstanding amount, tenure, repayment schedule, and tranche history.", Icons.Default.Info),
                TourStep("Record a Repayment", "As a borrower, tap Make Repayment to log a payment. As a lender, you will see it appear in the trail.", Icons.Default.Payments),
                TourStep("Chat with Counterparty", "Use the in-app chat icon to communicate securely with the other party.", Icons.Default.Chat),
                TourStep("View Audit Trail", "The audit trail logs every event: disbursal, repayments, restructuring, and agreement signings.", Icons.Default.ManageSearch)
            )
        ),
        TourDefinition(
            id = DOCUMENT_VAULT,
            title = "Document Vault and Security",
            icon = Icons.Default.Lock,
            steps = listOf(
                TourStep("Open Profile", "Navigate to Profile using the bottom navigation bar.", Icons.Default.Person),
                TourStep("Tap Document Vault", "The vault is a secure section containing all your KYC documents. It requires password verification.", Icons.Default.FolderSpecial),
                TourStep("Enter Vault Password", "Enter the password you set during registration to unlock the vault. It is hashed and never stored in plain text.", Icons.Default.Password),
                TourStep("View Your Documents", "Inside the vault, you can view your PAN card image, Aadhaar image, and verification status for each.", Icons.Default.Article),
                TourStep("Upload New Documents", "If any document is unverified, tap Upload to re-submit. Documents are stored encrypted on Firebase.", Icons.Default.Upload)
            )
        )
    )

    fun getById(id: String): TourDefinition? = all.find { it.id == id }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable 1: WelcomeOnboardingCarousel
// ─────────────────────────────────────────────────────────────────────────────

data class OnboardingSlide(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBgStart: Color,
    val title: String,
    val subtitle: String
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WelcomeOnboardingCarousel(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val slides = listOf(
        OnboardingSlide(
            icon = Icons.Default.AccountBalance,
            iconTint = Gold500,
            iconBgStart = Gold500,
            title = "Welcome to Loanzo",
            subtitle = "India's most transparent P2P lending platform. Lend, borrow, and track - all in one place, securely."
        ),
        OnboardingSlide(
            icon = Icons.Default.Handshake,
            iconTint = Color(0xFF7C4DFF),
            iconBgStart = Color(0xFF7C4DFF),
            title = "P2P Lending, Simplified",
            subtitle = "Connect directly with trusted contacts or browse the marketplace. Set your own terms - no middlemen, no hidden fees."
        ),
        OnboardingSlide(
            icon = Icons.Default.Shield,
            iconTint = Color(0xFF00BFA5),
            iconBgStart = Color(0xFF00BFA5),
            title = "Your Documents, Secured",
            subtitle = "KYC docs live in an encrypted, password-protected vault. Your identity is yours - accessed only by you."
        ),
        OnboardingSlide(
            icon = Icons.Default.Storefront,
            iconTint = Color(0xFFFF6D00),
            iconBgStart = Color(0xFFFF6D00),
            title = "Explore the Marketplace",
            subtitle = "Browse lending offers, submit bids, vouch for trusted users, and build your financial reputation."
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Skip button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val slide = slides[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(slide.iconBgStart.copy(alpha = 0.25f), Color.Transparent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = slide.iconTint,
                                modifier = Modifier.size(80.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = slide.subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }
                }

                // Dots + CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(slides.size) { idx ->
                            val isSelected = pagerState.currentPage == idx
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 28.dp else 8.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val isLastPage = pagerState.currentPage == slides.lastIndex
                    Button(
                        onClick = {
                            if (isLastPage) {
                                onComplete()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isLastPage) "Get Started" else "Next",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable 2: ContextualGuideCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContextualGuideCard(
    visible: Boolean,
    icon: ImageVector,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    autoDismissSeconds: Int = 8
) {
    var isVisible by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) { isVisible = visible }

    LaunchedEffect(isVisible) {
        if (isVisible && autoDismissSeconds > 0) {
            delay(autoDismissSeconds * 1000L)
            isVisible = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Gold500.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = Gold500, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = body, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { isVisible = false; onDismiss() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Got it ✓", color = Gold500, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable 3: BlinkingNavTooltip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BlinkingNavTooltip(
    shouldBlink: Boolean,
    tooltipTitle: String,
    tooltipBody: String,
    tooltipIcon: ImageVector,
    onTooltipSeen: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (shouldBlink) 0.85f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (shouldBlink) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(modifier = modifier) {
        if (shouldBlink) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(glowScale)
                    .drawBehind {
                        drawCircle(color = Gold500.copy(alpha = glowAlpha * 0.4f), radius = size.maxDimension * 0.6f)
                    }
            )
        }
        content()
    }

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(3000)
            showTooltip = false
            onTooltipSeen()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable 4: GuidedTourOverlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GuidedTourOverlay(
    tourId: String,
    currentStep: Int,
    onNext: (nextStep: Int) -> Unit,
    onBack: (prevStep: Int) -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    val tour = AppTours.getById(tourId) ?: return
    if (currentStep >= tour.steps.size) return
    val step = tour.steps[currentStep]
    val totalSteps = tour.steps.size
    val isLast = currentStep == totalSteps - 1

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(Gold500)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Step ${currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(tour.icon, contentDescription = null, tint = Gold500, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = tour.title, style = MaterialTheme.typography.labelSmall, color = Gold500, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Gold500.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(step.icon, contentDescription = null, tint = Gold500, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(text = step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(text = step.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 22.sp)

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { onBack(currentStep - 1) },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) { Text("Back") }
                        } else {
                            TextButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                            ) { Text("Exit Tour") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(totalSteps) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }

                        Button(
                            onClick = { if (isLast) onFinish() else onNext(currentStep + 1) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text(if (isLast) "Finish" else "Next", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable 5: GuideSpotlight
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GuideSpotlight(
    active: Boolean,
    ringColor: Color = Gold500,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (active) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (active) 0.0f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (active) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(pulseScale)
                    .drawBehind {
                        drawCircle(color = ringColor.copy(alpha = pulseAlpha), radius = size.minDimension / 2f)
                    }
            )
        }
        content()
    }
}
