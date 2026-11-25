package com.example.ui.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FeatureStatus
import com.example.features.CatalogFeature
import com.example.features.FeatureCatalog
import com.example.features.FeatureCategory
import com.example.features.FeatureRouteTarget
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

private fun iconForCategory(category: FeatureCategory): ImageVector = when (category) {
    FeatureCategory.AI_INTELLIGENCE -> Icons.Default.Psychology
    FeatureCategory.CREATIVE_STUDIO -> Icons.Default.Image
    FeatureCategory.VOICE_AUDIO -> Icons.Default.Mic
    FeatureCategory.PRODUCTIVITY -> Icons.Default.ChatBubble
    FeatureCategory.DEVICE_CONTROL -> Icons.Default.Smartphone
    FeatureCategory.GAME_COACH -> Icons.Default.SportsEsports
    FeatureCategory.SYSTEM_MANAGEMENT -> Icons.Default.Settings
    FeatureCategory.ADDITIONAL -> Icons.Default.Build
}

@Composable
fun FeaturesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onLaunchPromptInChat: (String) -> Unit = { onNavigateToChat() },
    onNavigateToWorkMode: () -> Unit = {},
    onNavigateToWebAssistant: () -> Unit = {},
    onNavigateToGameCoach: () -> Unit = {},
    onNavigateToImageStudio: () -> Unit = {},
    onNavigateToVoiceAssistant: () -> Unit = {},
    onNavigateToVoiceSelection: () -> Unit = onNavigateToVoiceAssistant,
    onNavigateToVoiceSettings: () -> Unit = onNavigateToVoiceAssistant,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToModels: () -> Unit = {},
    onNavigateToAiProviders: () -> Unit = onNavigateToModels,
    onNavigateToMemory: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToPrivacyCenter: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToCustomize: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onExecuteQuickFeatureAction: (FeatureRouteTarget) -> Unit = {},
    featureUsageCounts: Map<String, Int> = emptyMap(),
    onRecordFeatureUsage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FeatureCategory?>(null) }
    var selectedFeatureForDetails by remember { mutableStateOf<CatalogFeature?>(null) }

    val filtered = remember(searchQuery, selectedCategory) {
        FeatureCatalog.search(searchQuery, selectedCategory)
    }

    val launchFeature: (CatalogFeature) -> Unit = { feat ->
        onRecordFeatureUsage(feat.id)
        when (feat.routeTarget) {
            FeatureRouteTarget.HOME -> onNavigateBack()
            FeatureRouteTarget.CHAT -> onNavigateToChat()
            FeatureRouteTarget.CHAT_WITH_PROMPT -> {
                val prompt = feat.starterPrompt.orEmpty()
                if (prompt.isNotBlank()) onLaunchPromptInChat(prompt) else onNavigateToChat()
            }
            FeatureRouteTarget.VOICE_ASSISTANT -> onNavigateToVoiceAssistant()
            FeatureRouteTarget.VOICE_SELECTION -> onNavigateToVoiceSelection()
            FeatureRouteTarget.VOICE_SETTINGS -> onNavigateToVoiceSettings()
            FeatureRouteTarget.IMAGE_STUDIO -> onNavigateToImageStudio()
            FeatureRouteTarget.GAME_COACH -> onNavigateToGameCoach()
            FeatureRouteTarget.WORK_MODE -> onNavigateToWorkMode()
            FeatureRouteTarget.MODELS -> onNavigateToModels()
            FeatureRouteTarget.AI_PROVIDERS -> onNavigateToAiProviders()
            FeatureRouteTarget.MEMORY -> onNavigateToMemory()
            FeatureRouteTarget.PROJECTS -> onNavigateToProjects()
            FeatureRouteTarget.NOTIFICATIONS -> onNavigateToNotifications()
            FeatureRouteTarget.WEB_ASSISTANT -> onNavigateToWebAssistant()
            FeatureRouteTarget.PERMISSIONS, FeatureRouteTarget.FLOATING_ASSISTANT -> onNavigateToPermissions()
            FeatureRouteTarget.PRIVACY_CENTER -> onNavigateToPrivacyCenter()
            FeatureRouteTarget.LIBRARY -> onNavigateToLibrary()
            FeatureRouteTarget.CUSTOMIZE -> onNavigateToCustomize()
            FeatureRouteTarget.SETTINGS -> onNavigateToSettings()
            FeatureRouteTarget.TRACKER -> onNavigateToTracker()
            FeatureRouteTarget.TOGGLE_FLASHLIGHT,
            FeatureRouteTarget.CHECK_BATTERY,
            FeatureRouteTarget.CHECK_STORAGE,
            FeatureRouteTarget.CHECK_DATETIME,
            FeatureRouteTarget.SET_TIMER_60S,
            FeatureRouteTarget.SET_ALARM_7AM -> onExecuteQuickFeatureAction(feat.routeTarget)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("features_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("Explore AERIS Features", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = TextWhite)
                    Text("${FeatureCatalog.allFeatures.size} Capabilities • Categories A–H", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
                }
            }
            IconButton(onClick = onNavigateToTracker, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.DeveloperBoard, contentDescription = "Module Tracker", tint = CyanBright, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("search_features_field"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanTeal) },
            placeholder = { Text("Search ${FeatureCatalog.allFeatures.size} capabilities...", style = Typography.bodyMedium, color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = HoloCardBorder.copy(alpha = 0.4f),
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                cursorColor = CyanNeon,
                focusedContainerColor = HoloSurface,
                unfocusedContainerColor = HoloSurface
            ),
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val isSelected = selectedCategory == null
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = null },
                    label = { Text("All (${FeatureCatalog.allFeatures.size})", style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanDeep, selectedLabelColor = CyanNeon, containerColor = HoloSurface, labelColor = TextCoolGrey),
                    shape = RoundedCornerShape(14.dp),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = if (isSelected) CyanNeon else HoloCardBorder.copy(alpha = 0.4f))
                )
            }
            items(FeatureCategory.entries) { cat ->
                val isSelected = cat == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text("${cat.code}. ${cat.shortLabel}", style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanDeep, selectedLabelColor = CyanNeon, containerColor = HoloSurface, labelColor = TextCoolGrey),
                    shape = RoundedCornerShape(14.dp),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = if (isSelected) CyanNeon else HoloCardBorder.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("feature_cat_${cat.code}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filtered.size} Matching Capabilities", style = Typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = TextWhite)
            Text("Tap to inspect or Open", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { feat ->
                FeatureCard(
                    feature = feat,
                    usageCount = featureUsageCounts[feat.id] ?: 0,
                    onClick = { selectedFeatureForDetails = feat },
                    onQuickOpen = { launchFeature(feat) }
                )
            }
        }
    }

    selectedFeatureForDetails?.let { feat ->
        AlertDialog(
            onDismissRequest = { selectedFeatureForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CyanDeep, CircleShape)
                            .border(1.dp, CyanNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconForCategory(feat.category), contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${feat.id}: ${feat.title}", style = Typography.titleMedium, color = CyanNeon)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(feat.description, style = Typography.bodyMedium, color = TextWhite)
                    Text("Category: ${feat.category.title}", style = Typography.labelSmall, color = CyanTeal)
                    Text("Honest Status: ${feat.status.label}", style = Typography.labelSmall, color = StatusSuccess)
                    if (feat.permissionRequired != null) {
                        Text("Permission: ${feat.permissionRequired}", style = Typography.labelSmall, color = AlertRed)
                    }
                    Text("Scope & Notes:\n${feat.honestLimitation}", style = Typography.bodyMedium.copy(fontSize = 11.sp), color = TextSecondary)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val targetFeat = feat
                        selectedFeatureForDetails = null
                        launchFeature(targetFeat)
                    }) {
                        Text("OPEN MODULE", color = CyanBright, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { selectedFeatureForDetails = null }) {
                        Text("CLOSE", color = TextCoolGrey)
                    }
                }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun FeatureCard(
    feature: CatalogFeature,
    usageCount: Int,
    onClick: () -> Unit,
    onQuickOpen: () -> Unit
) {
    val statusColor = when (feature.status) {
        FeatureStatus.COMPLETED, FeatureStatus.IMPLEMENTED, FeatureStatus.INTEGRATED -> StatusSuccess
        FeatureStatus.DEVELOPED -> StatusWarning
        else -> AlertRed
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("feature_card_${feature.id}"),
        shape = RoundedCornerShape(14.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, HoloCardBorder.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CyanDeep.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .border(1.dp, CyanBright.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconForCategory(feature.category), contentDescription = feature.title, tint = CyanNeon, modifier = Modifier.size(19.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = feature.title,
                            style = Typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (usageCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${usageCount}x", style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = CyanBright)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(feature.description, style = Typography.bodyMedium.copy(fontSize = 11.sp), color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${feature.category.code} • ${feature.status.label}", style = Typography.labelSmall.copy(fontSize = 9.sp), color = statusColor)
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyanDeep.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, CyanGlow),
                modifier = Modifier.clickable(onClick = onQuickOpen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = feature.status.label, tint = statusColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open", style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = CyanNeon)
                }
            }
        }
    }
}
