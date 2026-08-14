package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ActivityPreset
import com.example.data.models.FitnessMetrics
import com.example.ui.theme.FitnessHeart
import com.example.ui.theme.FitnessPrimary
import com.example.ui.theme.FitnessSecondary
import java.util.Locale

@Composable
fun StepCountHeroCard(
    metrics: FitnessMetrics,
    modifier: Modifier = Modifier
) {
    val targetSteps = 10000
    val progress = (metrics.steps.toFloat() / targetSteps.toFloat()).coerceIn(0.0f, 1.0f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "stepProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("step_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                            .background(FitnessPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = "Step Counter",
                            tint = FitnessPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GENERATED STEPS STATS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% of 10k Goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitnessPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Radial Ring Gauge + Large Numbers
            Box(
                modifier = Modifier.size(210.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = FitnessPrimary
                val secondaryColor = FitnessSecondary

                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokePx = 16.dp.toPx()

                    // Background ring
                    drawArc(
                        color = primaryColor.copy(alpha = 0.15f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // Active progress arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryColor, secondaryColor, primaryColor)
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,d", metrics.steps),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "STEPS GENERATED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-metrics row: Distance, Speed, Pace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f km", metrics.distanceKm),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f km/h", metrics.currentSpeedKmh),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FitnessSecondary
                    )
                    Text(
                        text = "Speed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val paceText = if (metrics.paceMinPerKm > 0) {
                        val paceMin = metrics.paceMinPerKm.toInt()
                        val paceSec = ((metrics.paceMinPerKm - paceMin) * 60).toInt()
                        String.format(Locale.getDefault(), "%d'%02d\"", paceMin, paceSec)
                    } else "--'--"

                    Text(
                        text = paceText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FitnessPrimary
                    )
                    Text(
                        text = "Pace /km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SecondaryMetricsGrid(
    metrics: FitnessMetrics,
    modifier: Modifier = Modifier
) {
    val minutes = metrics.activeSeconds / 60
    val seconds = metrics.activeSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Calories & Active Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricSmallCard(
                modifier = Modifier.weight(1f),
                title = "CALORIES BURNED",
                value = String.format(Locale.getDefault(), "%.1f", metrics.caloriesBurned),
                unit = "kcal",
                subtitle = "Active + MET energy",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = Color(0xFFF97316),
                testTag = "calories_card"
            )

            MetricSmallCard(
                modifier = Modifier.weight(1f),
                title = "ACTIVE TIME",
                value = timeFormatted,
                unit = "mm:ss",
                subtitle = "Moving & cardio",
                icon = Icons.Default.Timer,
                iconColor = FitnessSecondary,
                testTag = "active_time_card"
            )
        }

        // Row 2: Cadence/Floors & Heart Rate Zone Range
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricSmallCard(
                modifier = Modifier.weight(1f),
                title = "CADENCE & FLOORS",
                value = "${metrics.currentCadenceSpm}",
                unit = "spm",
                subtitle = "${metrics.floorsClimbed} floors climbed",
                icon = Icons.Default.Stairs,
                iconColor = Color(0xFF3B82F6),
                testTag = "cadence_floors_card"
            )

            MetricSmallCard(
                modifier = Modifier.weight(1f),
                title = "HR ZONE & RANGE",
                value = metrics.heartRateZone.displayName,
                unit = "",
                subtitle = "Range: ${metrics.minHeartRate}-${metrics.maxHeartRate} BPM",
                icon = Icons.Default.MonitorHeart,
                iconColor = FitnessHeart,
                testTag = "hr_zone_card"
            )
        }
    }
}

@Composable
fun MetricSmallCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SimulationControlsCard(
    currentPreset: ActivityPreset,
    speedMultiplier: Float,
    onPresetSelected: (ActivityPreset) -> Unit,
    onSpeedMultiplierChanged: (Float) -> Unit,
    onAddManualSteps: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("simulation_controls_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FitnessSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Simulation Profile",
                            tint = FitnessSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HEALTHY PERSON PROFILES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    contentPadding = ButtonDefaults.ContentPadding,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("reset_metrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Data",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Selection Chips
            Text(
                text = "Select Activity Pattern (Mimics Real Person Data)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val presets = ActivityPreset.values()
                val chunkedPresets = presets.toList().chunked(2)
                for (row in chunkedPresets) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (preset in row) {
                            FilterChip(
                                selected = preset == currentPreset,
                                onClick = { onPresetSelected(preset) },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(preset.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(preset.description, fontSize = 9.sp, maxLines = 1)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preset_chip_${preset.name}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FitnessPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intensity / Multiplier Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed & Cadence Pace",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1fx Speed Multiplier", speedMultiplier),
                    style = MaterialTheme.typography.labelMedium,
                    color = FitnessPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = speedMultiplier,
                onValueChange = onSpeedMultiplierChanged,
                valueRange = 0.5f..2.5f,
                steps = 19,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speed_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = FitnessPrimary,
                    activeTrackColor = FitnessPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Step Booster Buttons for Fast Testing
            Text(
                text = "Quick Step Data Generation Boosters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(500, 1000, 5000).forEach { count ->
                    AssistChip(
                        onClick = { onAddManualSteps(count) },
                        label = { Text("+$count", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add $count steps",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_steps_$count"),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}
