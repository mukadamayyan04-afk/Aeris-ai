package com.example.ui.voice

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AerisVoice
import com.example.domain.model.VoiceGender
import com.example.domain.model.VoiceQuality
import com.example.domain.model.VoiceState
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

enum class VoiceCategoryTab(val label: String) {
    ALL("All Voices"),
    FEMALE("Female"),
    MALE("Male"),
    OTHER("System / Other")
}

@Composable
fun VoiceSelectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val availableVoices by viewModel.availableVoices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val availableTtsEngines by viewModel.availableTtsEngines.collectAsState()
    val activeTtsEnginePackage by viewModel.activeTtsEnginePackage.collectAsState()
    val voiceFallbackNotice by viewModel.voiceFallbackNotice.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(VoiceCategoryTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageFilter by remember { mutableStateOf("All") }

    // Derive available languages from actual voices
    val distinctLanguages = remember(availableVoices) {
        listOf("All") + availableVoices.map { it.languageDisplayName }.distinct().sorted()
    }

    // Filter voices based on category, search, and language
    val filteredVoices = remember(availableVoices, selectedTab, searchQuery, selectedLanguageFilter) {
        availableVoices.filter { voice ->
            val matchesCategory = when (selectedTab) {
                VoiceCategoryTab.ALL -> true
                VoiceCategoryTab.FEMALE -> voice.gender == VoiceGender.FEMALE
                VoiceCategoryTab.MALE -> voice.gender == VoiceGender.MALE
                VoiceCategoryTab.OTHER -> voice.gender == VoiceGender.UNSPECIFIED
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                voice.name.lowercase().contains(q) ||
                        voice.languageDisplayName.lowercase().contains(q) ||
                        voice.countryDisplayName.lowercase().contains(q) ||
                        voice.engineName.lowercase().contains(q)
            }

            val matchesLanguage = if (selectedLanguageFilter == "All") {
                true
            } else {
                voice.languageDisplayName.equals(selectedLanguageFilter, ignoreCase = true)
            }

            matchesCategory && matchesSearch && matchesLanguage
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("voice_selection_screen")
    ) {
        // 1. TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .background(HoloSurfaceVariant, CircleShape)
                    .border(1.dp, CyanGlow, CircleShape)
                    .testTag("voice_selection_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VOICE SELECTION",
                    style = Typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontSize = 15.sp
                    ),
                    color = CyanNeon
                )
                Text(
                    text = "${availableVoices.size} Neural & System Voices Available",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = CyanBright
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        viewModel.refreshVoices()
                        Toast.makeText(context, "Scanning installed TTS engines...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow.copy(alpha = 0.5f), CircleShape)
                        .testTag("refresh_voices_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Voices",
                        tint = CyanNeon,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onNavigateToVoiceSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow.copy(alpha = 0.5f), CircleShape)
                        .testTag("voice_settings_shortcut")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Voice Settings",
                        tint = CyanNeon,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. ACTIVE SELECTED VOICE HERO CARD
        selectedVoice?.let { current ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = HoloSurface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(CyanDeep.copy(alpha = 0.45f), HoloSurface, HoloDark)
                            )
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CyanNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (current.gender == VoiceGender.FEMALE) Icons.Default.Female else Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = HoloBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ACTIVE AERIS VOICE",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = CyanNeon
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(StatusSuccess, CircleShape)
                                )
                            }
                            Text(
                                text = current.name,
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${current.languageDisplayName} (${current.countryDisplayName}) • ${current.gender.label}",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextCoolGrey
                            )
                        }
                    }

                    // Test Preview Button
                    Surface(
                        modifier = Modifier
                            .clickable {
                                viewModel.previewVoice(current, "Hello Sir ji, this is ${current.name}. I am your AERIS voice.")
                            }
                            .testTag("preview_active_voice_button"),
                        shape = RoundedCornerShape(8.dp),
                        color = CyanNeon,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = HoloBlack, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TEST", style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp), color = HoloBlack)
                        }
                    }
                }
            }
        }

        // 2b. FALLBACK NOTICE BANNER (shown honestly if saved voice is unavailable)
        AnimatedVisibility(visible = voiceFallbackNotice != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = AlertRed.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = voiceFallbackNotice.orEmpty(),
                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dismiss",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = CyanNeon,
                        modifier = Modifier
                            .clickable { viewModel.clearVoiceFallbackNotice() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 2c. INSTALLED TTS ENGINE CHIPS (if multiple installed)
        if (availableTtsEngines.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableTtsEngines.forEach { engineInfo ->
                    val isEngineSelected = engineInfo.packageName == activeTtsEnginePackage
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.switchTtsEngine(engineInfo.packageName) }
                            .testTag("selection_engine_${engineInfo.packageName}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEngineSelected) CyanNeon else HoloDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEngineSelected) CyanBright else HoloCardBorder
                        )
                    ) {
                        Text(
                            text = engineInfo.label,
                            style = Typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isEngineSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isEngineSelected) HoloBlack else TextCoolGrey,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. SEARCH FIELD
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("search_voices_input"),
            placeholder = { Text("Search by name, language or country...", style = Typography.bodySmall.copy(fontSize = 12.sp), color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanNeon, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextCoolGrey, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = HoloCardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = HoloSurface,
                unfocusedContainerColor = HoloSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // 4. CATEGORY TABS (All, Female, Male, System/Other)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VoiceCategoryTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { selectedTab = tab }
                        .testTag("tab_${tab.name.lowercase()}"),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CyanNeon else HoloSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanBright else HoloCardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = tab.label,
                            style = Typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) HoloBlack else TextWhite
                        )
                    }
                }
            }
        }

        // 5. LANGUAGE FILTER PILLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            distinctLanguages.forEach { lang ->
                val isSelected = selectedLanguageFilter == lang
                Surface(
                    modifier = Modifier
                        .clickable { selectedLanguageFilter = lang }
                        .testTag("filter_lang_$lang"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) CyanDeep else HoloDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanNeon else HoloCardBorder)
                ) {
                    Text(
                        text = lang,
                        style = Typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) CyanNeon else TextCoolGrey,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 6. VOICES LIST OR EMPTY STATE
        if (filteredVoices.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = HoloSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextCoolGrey, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Voices Found in This Category",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your device's installed Text-to-Speech engine (${selectedVoice?.engineName ?: "System TTS"}) reported ${availableVoices.size} total voices. Try clearing filters or installing additional voice data in Android Settings.",
                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextCoolGrey,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent("com.android.settings.TTS_SETTINGS")
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = HoloSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow)
                    ) {
                        Text(
                            text = "OPEN ANDROID TTS SETTINGS",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = CyanNeon,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("voices_lazy_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredVoices, key = { it.id }) { voice ->
                    val isSelected = selectedVoice?.id == voice.id
                    VoiceItemCard(
                        voice = voice,
                        isSelected = isSelected,
                        onSelect = { viewModel.selectVoice(voice) },
                        onPreview = {
                            viewModel.previewVoice(
                                voice,
                                "Hello Sir ji, this is ${voice.name}. I am ready to serve as your voice assistant."
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Voices are directly enumerated from your Android system TTS provider (${selectedVoice?.engineName ?: "AOSP"}). All synthesis runs 100% locally on device with zero cloud APIs.",
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun VoiceItemCard(
    voice: AerisVoice,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("voice_item_${voice.id}"),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) HoloSurface else HoloDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CyanNeon else HoloCardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isSelected) CyanNeon.copy(alpha = 0.25f) else HoloSurfaceVariant,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isSelected) CyanNeon else CyanGlow.copy(alpha = 0.4f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (voice.gender) {
                                VoiceGender.FEMALE -> Icons.Default.Female
                                VoiceGender.MALE -> Icons.Default.Male
                                VoiceGender.UNSPECIFIED -> Icons.Default.RecordVoiceOver
                            },
                            contentDescription = voice.gender.label,
                            tint = if (isSelected) CyanNeon else CyanBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = voice.name,
                                style = Typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) CyanNeon else TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = "${voice.languageDisplayName} (${voice.countryDisplayName}) • ${voice.locale.toLanguageTag()}",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                }

                // Preview Button
                Surface(
                    modifier = Modifier
                        .clickable(onClick = onPreview)
                        .testTag("preview_button_${voice.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = HoloSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview Voice",
                            tint = CyanNeon,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Preview",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Badges (Gender, Quality, Offline/Network, Engine)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gender badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HoloDark,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyanGlow.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = voice.gender.label,
                        style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                        color = CyanBright,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Quality badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HoloDark,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, HoloCardBorder)
                ) {
                    Text(
                        text = voice.quality.label,
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextCoolGrey,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Local vs Network badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (voice.isInstalledLocally) StatusSuccess.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (voice.isInstalledLocally) StatusSuccess else AlertRed
                    )
                ) {
                    Text(
                        text = if (voice.isInstalledLocally) "Offline Ready" else "Network Req",
                        style = Typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (voice.isInstalledLocally) StatusSuccess else AlertRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Engine tag
                Text(
                    text = voice.engineName.takeLast(18),
                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
