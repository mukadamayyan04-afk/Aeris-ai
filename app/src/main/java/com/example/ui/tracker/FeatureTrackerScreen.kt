package com.example.ui.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FeatureItem
import com.example.domain.model.FeatureStatus
import com.example.features.FeatureTracker
import com.example.ui.components.HoloCard
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Typography

@Composable
fun FeatureTrackerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val features = FeatureTracker.features
    var selectedFilter by remember { mutableStateOf<FeatureStatus?>(null) }

    val filteredList = if (selectedFilter == null) {
        features
    } else {
        features.filter { it.status == selectedFilter }
    }

    val total = features.size
    val completedCount = features.count { it.status == FeatureStatus.COMPLETED || it.status == FeatureStatus.INTEGRATED }
    val progress = completedCount.toFloat() / total.toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = CyanNeon
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "FEATURE MATRIX TRACKER",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanNeon
                )
                Text(
                    text = "Permanent Structured Capability Registry",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Overall Progress Summary Card
            item {
                HoloCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FOUNDATION HEALTH",
                                style = Typography.labelLarge,
                                color = CyanNeon
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% READY",
                                style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = CyanBright
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = CyanNeon,
                            trackColor = HoloSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FeatureTracker.getSummary(),
                            style = Typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                }
            }

            // Status Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("ALL (${features.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanDeep,
                                selectedLabelColor = CyanNeon,
                                containerColor = HoloSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                    FeatureStatus.values().forEach { status ->
                        val count = features.count { it.status == status }
                        if (count > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == status,
                                    onClick = { selectedFilter = if (selectedFilter == status) null else status },
                                    label = { Text("${status.label} ($count)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanDeep,
                                        selectedLabelColor = CyanNeon,
                                        containerColor = HoloSurface,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Feature list items
            items(filteredList, key = { it.id }) { item ->
                FeatureItemCard(feature = item)
            }
        }
    }
}

@Composable
private fun FeatureItemCard(feature: FeatureItem) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (feature.status) {
        FeatureStatus.COMPLETED, FeatureStatus.IMPLEMENTED, FeatureStatus.INTEGRATED -> StatusSuccess
        FeatureStatus.DEVELOPED -> CyanBright
        FeatureStatus.PLANNED -> StatusWarning
        FeatureStatus.RECOGNIZED -> TextMuted
        FeatureStatus.TESTED -> CyanTeal
        FeatureStatus.UNAVAILABLE -> StatusError
    }

    HoloCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = HoloSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanTeal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = feature.id,
                            style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = CyanBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature.name,
                        style = Typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = feature.status.label.uppercase(),
                            style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = feature.description,
                style = Typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(HoloDark, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DetailRow(icon = Icons.Default.Code, label = "Source Files", value = feature.sourceFiles.joinToString(", "))
                    DetailRow(icon = Icons.Default.Rule, label = "Dependencies", value = feature.dependencies.joinToString(", "))
                    DetailRow(icon = Icons.Default.Check, label = "Test Status", value = feature.testStatus)
                    DetailRow(icon = Icons.Default.WarningAmber, label = "Limitations", value = feature.knownLimitations)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = CyanTeal, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
            Text(text = value, style = Typography.bodyMedium.copy(fontSize = 11.sp), color = TextPrimary)
        }
    }
}
