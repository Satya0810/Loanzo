package com.loanzo.app.ui.components

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString
import com.loanzo.app.util.toDateString

/**
 * Smooth curved line chart showing outstanding balance decreasing over time.
 * Built with Compose Canvas — no external chart library required.
 */
@Composable
fun BalanceLineChart(
    balanceHistory: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier
) {
    if (balanceHistory.size < 2) return

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "chart_anim"
    )

    val lineColor = Gold500
    val gradientTop = Gold500.copy(alpha = 0.25f)
    val gradientBottom = Gold500.copy(alpha = 0.0f)
    val gridColor = Gray600.copy(alpha = 0.3f)
    val textColor = Gray400

    val maxBalance = balanceHistory.maxOf { it.second }
    val minBalance = balanceHistory.minOf { it.second }
    val balanceRange = (maxBalance - minBalance).coerceAtLeast(1.0)
    val minTime = balanceHistory.minOf { it.first }
    val maxTime = balanceHistory.maxOf { it.first }
    val timeRange = (maxTime - minTime).coerceAtLeast(1L)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val paddingLeft = 0f
            val paddingBottom = 24f
            val drawWidth = chartWidth - paddingLeft
            val drawHeight = chartHeight - paddingBottom

            // Draw horizontal grid lines
            for (i in 0..3) {
                val y = drawHeight * i / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            // Calculate points
            val points = balanceHistory.mapIndexed { _, (time, balance) ->
                val x = paddingLeft + ((time - minTime).toFloat() / timeRange) * drawWidth
                val y = drawHeight - ((balance - minBalance).toFloat() / balanceRange.toFloat()) * drawHeight
                Offset(x, y)
            }

            // Animated subset
            val animatedPointCount = (points.size * animProgress).toInt().coerceIn(2, points.size)
            val animPoints = points.take(animatedPointCount)

            // Build smooth path using cubic bezier
            val linePath = Path().apply {
                moveTo(animPoints[0].x, animPoints[0].y)
                for (i in 1 until animPoints.size) {
                    val prev = animPoints[i - 1]
                    val curr = animPoints[i]
                    val controlX1 = prev.x + (curr.x - prev.x) / 2f
                    val controlX2 = prev.x + (curr.x - prev.x) / 2f
                    cubicTo(controlX1, prev.y, controlX2, curr.y, curr.x, curr.y)
                }
            }

            // Gradient fill beneath the line
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(animPoints.last().x, drawHeight)
                lineTo(animPoints.first().x, drawHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom),
                    startY = 0f,
                    endY = drawHeight
                )
            )

            // Draw the line
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw dots at each data point
            animPoints.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = point
                )
                drawCircle(
                    color = Navy800,
                    radius = 2.5f,
                    center = point
                )
            }
        }

        // Date labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = balanceHistory.first().first.toDateString("MMM yy"),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
            Text(
                text = balanceHistory.last().first.toDateString("MMM yy"),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

/**
 * Animated donut chart showing Principal vs Interest payment breakdown.
 * Built with Compose Canvas.
 */
@Composable
fun PaymentDonutChart(
    principalPaid: Double,
    interestPaid: Double,
    modifier: Modifier = Modifier
) {
    val total = principalPaid + interestPaid
    if (total <= 0) return

    val principalFraction = (principalPaid / total).toFloat()
    val interestFraction = (interestPaid / total).toFloat()

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "donut_anim"
    )

    val principalColor = Gold500
    val interestColor = Blue400
    val bgRingColor = Gray700.copy(alpha = 0.3f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 28f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Background ring
                drawArc(
                    color = bgRingColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Principal arc
                val principalSweep = principalFraction * 360f * animProgress
                drawArc(
                    color = principalColor,
                    startAngle = -90f,
                    sweepAngle = principalSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Interest arc
                val interestSweep = interestFraction * 360f * animProgress
                drawArc(
                    color = interestColor,
                    startAngle = -90f + principalSweep,
                    sweepAngle = interestSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toInrString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.total_paid),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                color = principalColor,
                label = "Principal",
                value = principalPaid.toInrString(),
                percentage = "${(principalFraction * 100).toInt()}%"
            )
            LegendItem(
                color = interestColor,
                label = "Interest",
                value = interestPaid.toInrString(),
                percentage = "${(interestFraction * 100).toInt()}%"
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String,
    percentage: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "$label ($percentage)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
