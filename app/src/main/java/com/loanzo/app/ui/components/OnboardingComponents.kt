package com.loanzo.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loanzo.app.R
import com.loanzo.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// 1. Tour Definitions & Data Models with Rich 3D Visuals
// ─────────────────────────────────────────────────────────────────────────────

data class TourStep(
    val title: String,
    val description: String,
    val imageRes: Int,
    val targetTag: String? = null,
    val actionLabel: String? = null
)

data class TourDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverImageRes: Int,
    val steps: List<TourStep>
)

object AppTours {
    const val REQUEST_LOAN   = "tour_request_loan"
    const val COMMUNITY_WALL = "tour_community_wall"
    const val DOCUMENT_VAULT = "tour_document_vault"
    const val LOAN_TRACKING  = "tour_loan_tracking"

    val all = listOf(
        TourDefinition(
            id = REQUEST_LOAN,
            title = "Direct P2P Lending",
            subtitle = "Lend or borrow directly with custom interest & zero middlemen",
            coverImageRes = R.drawable.guide_p2p_handshake,
            steps = listOf(
                TourStep(
                    title = "Set Your Own Terms",
                    description = "Choose your loan amount, interest rate (e.g. 10%-14% p.a.), and flexible monthly tenure agreed with your peer.",
                    imageRes = R.drawable.guide_p2p_handshake,
                    targetTag = "btn_post",
                    actionLabel = "Try Calculator"
                ),
                TourStep(
                    title = "Legally Binding e-Agreement",
                    description = "Loanzo automatically generates a digital Promissory Note under the Negotiable Instruments Act with Aadhaar e-Sign.",
                    imageRes = R.drawable.guide_digital_contract,
                    targetTag = "satellite_p2p",
                    actionLabel = "View Agreement Spec"
                ),
                TourStep(
                    title = "Automated EMI Ledger",
                    description = "Track upcoming EMIs, log UPI transaction IDs, and maintain a tamper-proof audit trail for both parties.",
                    imageRes = R.drawable.guide_hero_shield,
                    targetTag = "nav_loans",
                    actionLabel = "Explore Loans"
                )
            )
        ),
        TourDefinition(
            id = COMMUNITY_WALL,
            title = "Community Loan Wall & Bidding",
            subtitle = "Discover verified borrowers, place custom interest bids, and vouch for peers",
            coverImageRes = R.drawable.guide_community_wall,
            steps = listOf(
                TourStep(
                    title = "Discover Loan Requests",
                    description = "Browse real requests for business, medical, or education funding posted by verified Loanzo members.",
                    imageRes = R.drawable.guide_community_wall,
                    targetTag = "community_wall",
                    actionLabel = "Browse Posts"
                ),
                TourStep(
                    title = "Place Transparent Bids",
                    description = "Lenders propose competitive rates and tenure. The borrower selects the best proposal that fits their budget.",
                    imageRes = R.drawable.guide_community_wall,
                    targetTag = "bid_button",
                    actionLabel = "How Bidding Works"
                ),
                TourStep(
                    title = "Social Vouching & Trust",
                    description = "Vouch for trusted acquaintances to boost their credibility and unlock lower interest borrowing tiers.",
                    imageRes = R.drawable.guide_hero_shield,
                    targetTag = "vouch_chip",
                    actionLabel = "Got It"
                )
            )
        ),
        TourDefinition(
            id = DOCUMENT_VAULT,
            title = "DigiLocker Vault & Security",
            subtitle = "256-bit encrypted personal vault for PAN, Aadhaar, and bank records",
            coverImageRes = R.drawable.guide_cyber_vault,
            steps = listOf(
                TourStep(
                    title = "Government-Verified KYC",
                    description = "Seamlessly fetch and verify your official identity through DigiLocker with zero paperwork.",
                    imageRes = R.drawable.guide_cyber_vault,
                    targetTag = "profile_vault",
                    actionLabel = "Check My KYC"
                ),
                TourStep(
                    title = "Biometric Security",
                    description = "Your sensitive documents and contract copies can only be unlocked with your fingerprint or face recognition.",
                    imageRes = R.drawable.guide_cyber_vault,
                    targetTag = "biometric_toggle",
                    actionLabel = "Understood"
                )
            )
        ),
        TourDefinition(
            id = LOAN_TRACKING,
            title = "Loan Lifecycle & Repayments",
            subtitle = "Track active EMIs, simulate early prepayments, and download NOCs",
            coverImageRes = R.drawable.guide_digital_contract,
            steps = listOf(
                TourStep(
                    title = "Real-Time Repayment Status",
                    description = "See exact days remaining for the next EMI, principal vs interest splits, and overall outstanding balance.",
                    imageRes = R.drawable.guide_hero_shield,
                    targetTag = "nav_loans",
                    actionLabel = "View Loans"
                ),
                TourStep(
                    title = "Simulate Prepayments",
                    description = "Test how paying an extra ₹1,000 saves thousands in cumulative interest and reduces your loan tenure.",
                    imageRes = R.drawable.guide_p2p_handshake,
                    targetTag = "prepayment_simulator",
                    actionLabel = "Open Simulator"
                )
            )
        )
    )

    fun getById(id: String): TourDefinition? = all.find { it.id == id }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Interactive Welcome Carousel with 3D Visuals
// ─────────────────────────────────────────────────────────────────────────────

data class WelcomeSlide(
    val imageRes: Int,
    val badge: String,
    val title: String,
    val subtitle: String
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WelcomeOnboardingCarousel(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val slides = remember {
        listOf(
            WelcomeSlide(
                imageRes = R.drawable.guide_hero_shield,
                badge = "WELCOME TO LOANZO",
                title = "India's Transparent P2P Financial Platform",
                subtitle = "Lend directly, borrow with confidence, and eliminate predatory bank fees with legally verified promissory agreements."
            ),
            WelcomeSlide(
                imageRes = R.drawable.guide_p2p_handshake,
                badge = "DIRECT PEER-TO-PEER",
                title = "Borrow & Lend on Your Own Terms",
                subtitle = "Connect directly with trusted friends or verified community members. Set custom interest rates and flexible tenures with complete transparency."
            ),
            WelcomeSlide(
                imageRes = R.drawable.guide_community_wall,
                badge = "COMMUNITY LOAN WALL",
                title = "Social Marketplace & Bidding",
                subtitle = "Discover verified loan offers and borrower requests. Place competitive bids, vouch for peers, and build your social financial score."
            ),
            WelcomeSlide(
                imageRes = R.drawable.guide_digital_contract,
                badge = "LEGAL PROTECTION",
                title = "Legally Enforceable e-Contracts",
                subtitle = "Every loan automatically generates a compliant digital Promissory Note under Indian law with cryptographic signatures and tamper-proof audit trails."
            ),
            WelcomeSlide(
                imageRes = R.drawable.guide_cyber_vault,
                badge = "DIGILOCKER SECURITY",
                title = "256-Bit Encrypted Document Vault",
                subtitle = "Your PAN, Aadhaar, and bank records are encrypted and protected by biometric authentication. Access is strictly controlled by you."
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D131F), Color(0xFF06090E))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = "Loanzo",
                            modifier = Modifier.size(20.dp)
                        )
                        Text("LOANZO", color = BrandAmberGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }

                    // Skip button
                    TextButton(onClick = onSkip) {
                        Text("Skip ➔", color = Gray400, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                // Horizontal Pager for 3D Visual Slides
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
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 3D Visual Card Container
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Brush.radialGradient(listOf(BrandAmberGold.copy(alpha = 0.15f), Color.Transparent)))
                                .border(1.5.dp, Brush.linearGradient(listOf(BrandAmberGold.copy(alpha = 0.4f), Color.Transparent)), RoundedCornerShape(32.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(slide.imageRes),
                                contentDescription = slide.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Category Badge
                        Surface(
                            shape = CircleShape,
                            color = BrandAmberGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = slide.badge,
                                color = BrandAmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = slide.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray300,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Bottom Controls: Page Dots & Next / Finish CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern Animated Capsule Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(slides.size) { idx ->
                            val isSelected = pagerState.currentPage == idx
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 32.dp else 8.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BrandAmberGold else Color.White.copy(alpha = 0.2f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandAmberGold,
                            contentColor = Color(0xFF0F172A)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isLastPage) "Explore Loanzo Now" else "Next Step",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                softWrap = false
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
// 3. True Element Spotlight Coachmark Engine (Dynamic Target Cutout)
// ─────────────────────────────────────────────────────────────────────────────

enum class SpotlightShape {
    CIRCLE,
    ROUNDED_RECT
}

@Composable
fun InteractiveSpotlightOverlay(
    visible: Boolean,
    targetRect: Rect?,
    shape: SpotlightShape = SpotlightShape.ROUNDED_RECT,
    title: String,
    description: String,
    currentStep: Int = 1,
    totalSteps: Int = 1,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    onNext: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!visible || targetRect == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // Darkened Scrim with Clean Luminous Cutout
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fill background with rich dark scrim
            drawRect(color = Color(0xDC05080E))

            // Cutout spotlight over target element
            if (shape == SpotlightShape.CIRCLE) {
                val radius = (targetRect.width.coerceAtLeast(targetRect.height) / 2f) + 16.dp.toPx()
                drawCircle(
                    color = Color.Transparent,
                    radius = radius,
                    center = targetRect.center,
                    blendMode = BlendMode.Clear
                )
                // Outer glowing gold ring
                drawCircle(
                    color = BrandAmberGold.copy(alpha = 0.5f),
                    radius = radius * pulseScale,
                    center = targetRect.center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                )
            } else {
                val padding = 12.dp.toPx()
                val cutoutRect = Rect(
                    left = targetRect.left - padding,
                    top = targetRect.top - padding,
                    right = targetRect.right + padding,
                    bottom = targetRect.bottom + padding
                )
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(cutoutRect.left, cutoutRect.top),
                    size = Size(cutoutRect.width, cutoutRect.height),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
                // Outer glowing gold border
                drawRoundRect(
                    color = BrandAmberGold.copy(alpha = 0.6f),
                    topLeft = Offset(cutoutRect.left, cutoutRect.top),
                    size = Size(cutoutRect.width, cutoutRect.height),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        // Floating Contextual Speech Card
        val isTargetInTopHalf = targetRect.top < 600f
        val cardAlignment = if (isTargetInTopHalf) Alignment.BottomCenter else Alignment.TopCenter

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 48.dp),
            contentAlignment = cardAlignment
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF131B2B),
                border = BorderStroke(1.5.dp, BrandAmberGold.copy(alpha = 0.6f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Don't dismiss when tapping the card itself
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Row with Step Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandAmberGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "TIP $currentStep OF $totalSteps",
                                color = BrandAmberGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Gray400, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray300,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (actionLabel != null) {
                            OutlinedButton(
                                onClick = onActionClick,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandAmberGold)
                            ) {
                                Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("Dismiss", color = Gray400, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = onNext,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAmberGold, contentColor = Color(0xFF0F172A))
                        ) {
                            Text(if (currentStep >= totalSteps) "Got It ✓" else "Next ➔", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Interactive Getting Started Quest Card (Fintech Gamified Checklist)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InteractiveGettingStartedQuestCard(
    isCommunityDone: Boolean,
    isCalculatorDone: Boolean,
    isKycDone: Boolean,
    isDemoDone: Boolean,
    onExploreCommunity: () -> Unit,
    onOpenCalculator: () -> Unit,
    onVerifyKyc: () -> Unit,
    onSeedDemo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = listOf(isCommunityDone, isCalculatorDone, isKycDone, isDemoDone).count { it }
    val progress = completedCount / 4f
    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF111827),
        border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.35f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Quest Progress & Expand/Collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandAmberGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (completedCount == 4) "🏆" else "⚡",
                            fontSize = 18.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (completedCount == 4) "Quest Complete! Pioneer Unlocked" else "Get Started with Loanzo",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$completedCount of 4 steps completed (${(progress * 100).toInt()}%)",
                            color = Gray400,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = BrandAmberGold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Gray500, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Gold Progress Bar
            val animatedProgress by animateFloatAsState(targetValue = progress, label = "quest_progress")
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = BrandAmberGold,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            // Expandable Step Items
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    QuestStepItem(
                        title = "Explore Community Loan Wall",
                        subtitle = "See live offers & borrower requests on the wall",
                        isCompleted = isCommunityDone,
                        actionLabel = "Explore",
                        onClick = onExploreCommunity
                    )
                    QuestStepItem(
                        title = "Calculate Loan EMI & Savings",
                        subtitle = "Try our interactive loan math & interest simulator",
                        isCompleted = isCalculatorDone,
                        actionLabel = "Calculate",
                        onClick = onOpenCalculator
                    )
                    QuestStepItem(
                        title = "Verify Identity (DigiLocker KYC)",
                        subtitle = "Enable borrowing & lending with verified credentials",
                        isCompleted = isKycDone,
                        actionLabel = "Verify",
                        onClick = onVerifyKyc
                    )
                    QuestStepItem(
                        title = "Push Demo Playground",
                        subtitle = "Instantly seed realistic loans, ledger & notifications",
                        isCompleted = isDemoDone,
                        actionLabel = "Seed Data",
                        onClick = onSeedDemo
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestStepItem(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isCompleted) Emerald400 else Gray500,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isCompleted) Gray400 else Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Gray400,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (!isCompleted) {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAmberGold, contentColor = Color(0xFF0F172A)),
                modifier = Modifier.height(32.dp)
            ) {
                Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        } else {
            Surface(
                shape = CircleShape,
                color = Emerald400.copy(alpha = 0.15f)
            ) {
                Text(
                    "Done ✓",
                    color = Emerald400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Loanzo Academy & Hands-on Interactive Simulator Modal
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanzoAcademySimulatorSheet(
    onDismiss: () -> Unit,
    onNavigateToCreateLoan: () -> Unit = {}
) {
    var principal by remember { mutableFloatStateOf(50000f) }
    var interestRate by remember { mutableFloatStateOf(12f) }
    var tenureMonths by remember { mutableFloatStateOf(12f) }

    // Live Math Calculation
    val monthlyRate = (interestRate / 12f) / 100f
    val n = tenureMonths.toInt()
    val emi = if (monthlyRate > 0f) {
        val factor = (1f + monthlyRate).pow(n)
        (principal * monthlyRate * factor) / (factor - 1f)
    } else {
        principal / n
    }
    val totalPayable = emi * n
    val totalInterest = (totalPayable - principal).coerceAtLeast(0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D131F),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandAmberGold.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎓", fontSize = 20.sp)
                        }
                    }
                    Column {
                        Text("Loanzo Academy", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Interactive Loan & EMI Simulator", color = Gray400, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Gray400)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3D Visual Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BrandAmberGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.guide_p2p_handshake),
                    contentDescription = "P2P Simulator",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                        Text("Experiment Live", color = BrandAmberGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Drag sliders to see how interest rate & tenure affect your EMI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sliders Section
            // 1. Amount
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Loan Amount", color = Gray300, fontSize = 13.sp)
                Text("₹${principal.roundToInt().toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")}", color = BrandAmberGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Slider(
                value = principal,
                onValueChange = { principal = it },
                valueRange = 5000f..500000f,
                steps = 98,
                colors = SliderDefaults.colors(thumbColor = BrandAmberGold, activeTrackColor = BrandAmberGold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Interest Rate
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Annual Interest Rate", color = Gray300, fontSize = 13.sp)
                Text("${interestRate.roundToInt()}% p.a.", color = BrandAmberGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Slider(
                value = interestRate,
                onValueChange = { interestRate = it },
                valueRange = 6f..30f,
                steps = 23,
                colors = SliderDefaults.colors(thumbColor = BrandAmberGold, activeTrackColor = BrandAmberGold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Tenure Months
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tenure", color = Gray300, fontSize = 13.sp)
                Text("${n} Months", color = BrandAmberGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Slider(
                value = tenureMonths,
                onValueChange = { tenureMonths = it },
                valueRange = 3f..36f,
                steps = 32,
                colors = SliderDefaults.colors(thumbColor = BrandAmberGold, activeTrackColor = BrandAmberGold)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Computed EMI Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF162032),
                border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Monthly EMI", color = Gray400, fontSize = 12.sp, maxLines = 1, softWrap = false)
                            Text("₹${emi.roundToInt()}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = BrandAmberGold.copy(alpha = 0.2f)) {
                            Text("Live Calculated", color = BrandAmberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Interest", color = Gray400, fontSize = 11.sp, maxLines = 1, softWrap = false)
                            Text("₹${totalInterest.roundToInt()}", color = BrandAmberGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Total Payable", color = Gray400, fontSize = 11.sp, maxLines = 1, softWrap = false)
                            Text("₹${totalPayable.roundToInt()}", color = Emerald400, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action CTA: Create Real Loan
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToCreateLoan()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAmberGold, contentColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Start Real Loan with These Terms ➔", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Contextual Guide Card (Floating Mini Helper)
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
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF131B2B),
                border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.35f)),
                shadowElevation = 8.dp
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandAmberGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = BrandAmberGold, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = body, style = MaterialTheme.typography.bodySmall, color = Gray300, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { isVisible = false; onDismiss() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Got it ✓", color = BrandAmberGold, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Blinking Nav Tooltip (Bottom Bar Pulsing Highlights)
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

    Box(modifier = modifier) {
        if (shouldBlink) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawCircle(color = BrandAmberGold.copy(alpha = glowAlpha * 0.4f), radius = size.maxDimension * 0.55f)
                    }
            )
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Guide Spotlight Pulsing Ring for Action Buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GuideSpotlight(
    active: Boolean,
    ringColor: Color = BrandAmberGold,
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

// ─────────────────────────────────────────────────────────────────────────────
// 9. Guided Tour Overlay (Interactive Walkthrough with 3D Visuals)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                color = Color(0xFF111827),
                border = BorderStroke(1.5.dp, BrandAmberGold.copy(alpha = 0.5f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Tag & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandAmberGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = tour.title.uppercase(),
                                color = BrandAmberGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Gray400, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3D Illustration
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, BrandAmberGold.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painterResource(step.imageRes),
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray300,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dots & Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { onBack(currentStep - 1) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Gray600),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray300)
                            ) {
                                Text("Back", fontSize = 12.sp)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("Exit Tour", color = Gray400, fontSize = 12.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(totalSteps) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == currentStep) BrandAmberGold else Color.White.copy(alpha = 0.2f))
                                )
                            }
                        }

                        Button(
                            onClick = { if (isLast) onFinish() else onNext(currentStep + 1) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAmberGold, contentColor = Color(0xFF0F172A))
                        ) {
                            Text(if (isLast) "Finish ✓" else "Next ➔", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

