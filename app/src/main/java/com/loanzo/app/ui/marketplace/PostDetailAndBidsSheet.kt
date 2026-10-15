package com.loanzo.app.ui.marketplace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.CartoonAvatarHelper
import com.loanzo.app.util.toFormattedString
import com.loanzo.app.util.toRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailAndBidsSheet(
    post: MarketplacePostEntity,
    bids: List<MarketplaceBidEntity>,
    onAcceptBid: (MarketplaceBidEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Proposals & Competitive Bids (${bids.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: ${post.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (bids.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No active proposals on this post yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Community members will review your terms shortly!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(bids, key = { it.bidId }) { bid ->
                        val isClosed = post.status.equals("FUNDED", ignoreCase = true) || post.status.equals("CLOSED", ignoreCase = true)
                        BidCard(bid = bid, isPostClosed = isClosed, onAccept = { onAcceptBid(bid) })
                    }
                }
            }
        }
    }
}

@Composable
fun BidCard(
    bid: MarketplaceBidEntity,
    isPostClosed: Boolean = false,
    onAccept: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LoanzoAvatar(
                    avatarModel = bid.bidderAvatarUrl.takeIf { it.isNotBlank() } ?: bid.bidderId,
                    size = 36.dp,
                    showVerifiedBadge = bid.bidderKycVerified,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    borderWidth = 1.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bid.bidderName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        if (bid.bidderKycVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        "⭐ ${bid.bidderTrustScore}/100 Trust • ${bid.createdAt.toRelativeTime()}",
                        fontSize = 10.sp,
                        color = Gray400
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Emerald400.copy(alpha = 0.15f)
                ) {
                    Text(
                        "₹${bid.proposedAmount.toFormattedString()}",
                        color = Emerald400,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Rate: ${bid.proposedInterestRate}% p.a.",
                    fontSize = 12.sp,
                    color = Gold400,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Tenure: ${bid.proposedTenureMonths} Months",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bid.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${bid.message}\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            var isAccepting by remember { mutableStateOf(false) }
            val isBidAccepted = bid.status.equals("ACCEPTED", ignoreCase = true) || bid.status.equals("CONVERTED_TO_LOAN", ignoreCase = true)

            if (isBidAccepted) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald500.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bid Accepted & Legal Agreement Finalized ✓",
                            color = Emerald500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (isPostClosed) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Deal Finalized / Post Closed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            } else {
                com.loanzo.app.ui.components.LoanzoActionButton(
                    text = "Accept Bid & Finalize Legal Agreement ➔",
                    onClick = {
                        if (!isAccepting) {
                            isAccepting = true
                            onAccept()
                        }
                    },
                    isLoading = isAccepting,
                    loadingText = "Finalizing Legal Contract...",
                    enabled = !isAccepting,
                    icon = Icons.Default.Handshake,
                    containerColor = Emerald500,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
