package com.loanzo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.Navy900
import com.loanzo.app.ui.theme.SurfaceDarkCard

/**
 * Dunio-inspired Segmented Capsule Tab Selector.
 * Replaces generic underline TabRow with a tactile pill container and animated sliding capsule.
 */
@Composable
fun SegmentedCapsuleTab(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Gold500,
    activeTextColor: Color = Navy900,
    inactiveTextColor: Color = Color(0xFF94A3B8),
    containerColor: Color = SurfaceDarkCard.copy(alpha = 0.85f)
) {
    if (tabs.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .padding(4.dp)
    ) {
        val tabCount = tabs.size
        // Calculate bias between -1f (leftmost) and 1f (rightmost)
        val targetBias = if (tabCount > 1) {
            -1f + (2f * selectedIndex / (tabCount - 1))
        } else 0f

        val animatedBias by animateFloatAsState(
            targetValue = targetBias,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "tabIndicatorBias"
        )

        // Sliding Capsule Pill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(1f / tabCount)
                .align(BiasAlignment(animatedBias, 0f))
                .clip(CircleShape)
                .background(activeColor)
        )

        // Tab Text Row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) activeTextColor else inactiveTextColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "tabTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
