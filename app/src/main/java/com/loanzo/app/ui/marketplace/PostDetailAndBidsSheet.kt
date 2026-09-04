package com.loanzo.app.ui.marketplace

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.ui.theme.*
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
        containerColor = SurfaceDarkElevated,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Gold500, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Proposals & Competitive Bids (${bids.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: ${post.title}",
                style = MaterialTheme.typography.bodySmall,
                color = Gray400
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
                            color = Gray400
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Community members will review your terms shortly!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray500
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(bids, key = { it.bidId }) { bid ->
                        BidCard(bid = bid, onAccept = { onAcceptBid(bid) })
                    }
                }
            }
        }
    }
}

@Composable
fun BidCard(
    bid: MarketplaceBidEntity,
    onAccept: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(GlassBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Gold500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Gold500, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bid.bidderName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
                    color = Gray300
                )
            }

            if (bid.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${bid.message}\"",
                    fontSize = 12.sp,
                    color = Gray300,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Accept Bid & Finalize Legal Agreement ➔", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
