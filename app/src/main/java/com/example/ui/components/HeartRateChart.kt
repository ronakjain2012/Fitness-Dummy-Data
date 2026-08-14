package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.HeartRatePoint
import com.example.ui.theme.FitnessHeart
import com.example.ui.theme.FitnessSurfaceVariant

@Composable
fun HeartRateMonitorCard(
    currentBpm: Int,
    isRunning: Boolean,
    points: List<HeartRatePoint>,
    modifier: Modifier = Modifier
) {
    // Pulse animation scaled to current heart rate BPM
    val beatsPerSecond = (currentBpm.coerceAtLeast(40) / 60.0f).coerceIn(0.5f, 3.0f)
    val pulseMillis = (1000f / beatsPerSecond).toInt()

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("heart_rate_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FitnessHeart.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart Rate",
                            tint = FitnessHeart,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(if (isRunning) heartScale else 1.0f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HEART RATE MONITOR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isRunning) "Real-time Pulse Wave" else "Idle (Press Start)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (currentBpm > 0) "$currentBpm" else "--",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = FitnessHeart
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time Canvas Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw grid lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val gridLines = 3
                    for (i in 1..gridLines) {
                        val y = height * (i.toFloat() / (gridLines + 1))
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            pathEffect = dashEffect,
                            strokeWidth = 1f
                        )
                    }

                    if (points.size >= 2) {
                        val minBpm = (points.minOf { it.bpm } - 10).coerceAtLeast(40)
                        val maxBpm = (points.maxOf { it.bpm } + 10).coerceAtMost(200)
                        val bpmRange = (maxBpm - minBpm).coerceAtLeast(20).toFloat()

                        val path = Path()
                        val fillPath = Path()

                        val stepX = width / (points.size - 1).coerceAtLeast(1)

                        points.forEachIndexed { index, pt ->
                            val x = index * stepX
                            val normalizedY = 1.0f - ((pt.bpm - minBpm) / bpmRange)
                            val y = (normalizedY * (height - 20f)) + 10f

                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (index - 1) * stepX
                                val prevPt = points[index - 1]
                                val prevNormalizedY = 1.0f - ((prevPt.bpm - minBpm) / bpmRange)
                                val prevY = (prevNormalizedY * (height - 20f)) + 10f

                                val controlX1 = prevX + (stepX / 2f)
                                val controlY1 = prevY
                                val controlX2 = prevX + (stepX / 2f)
                                val controlY2 = y

                                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                            }
                        }

                        val lastX = (points.size - 1) * stepX
                        fillPath.lineTo(lastX, height)
                        fillPath.close()

                        // Draw Gradient fill below line
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    FitnessHeart.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Draw ECG line
                        drawPath(
                            path = path,
                            color = FitnessHeart,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw current lead point indicator
                        val lastPoint = points.last()
                        val lastNormalizedY = 1.0f - ((lastPoint.bpm - minBpm) / bpmRange)
                        val lastY = (lastNormalizedY * (height - 20f)) + 10f

                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                        drawCircle(
                            color = FitnessHeart,
                            radius = 3.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                    } else {
                        // Demo ECG idle wave when starting
                        val path = Path()
                        val centerY = height / 2f
                        path.moveTo(0f, centerY)
                        val waveWidth = width / 5f
                        for (i in 0..4) {
                            val startX = i * waveWidth
                            path.lineTo(startX + waveWidth * 0.3f, centerY)
                            path.lineTo(startX + waveWidth * 0.4f, centerY - 25f)
                            path.lineTo(startX + waveWidth * 0.5f, centerY + 30f)
                            path.lineTo(startX + waveWidth * 0.6f, centerY - 10f)
                            path.lineTo(startX + waveWidth * 0.7f, centerY)
                            path.lineTo(startX + waveWidth, centerY)
                        }
                        drawPath(
                            path = path,
                            color = FitnessHeart.copy(alpha = 0.3f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
