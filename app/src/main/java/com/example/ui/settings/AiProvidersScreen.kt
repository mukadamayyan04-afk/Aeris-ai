package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiOperatingMode
import com.example.ai.AiProviderType
import com.example.ai.CredentialVault
import com.example.ai.GeminiFirebaseProviderAdapter
import com.example.ai.LocalModelLifecycleState
import com.example.ai.ModelHealthState
import com.example.ai.ModelHealthStatus
import com.example.ai.ModelManager
import com.example.ai.ProviderConnectionUiStatus
import com.example.ai.ProviderDirectory
import com.example.ai.ProviderIntegrationManager
import com.example.ui.MainViewModel
import com.example.ui.components.HoloCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

internal fun descriptorIdForProvider(providerType: AiProviderType): String = when (providerType) {
    AiProviderType.GEMINI_FIREBASE -> GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL
    AiProviderType.OPENAI_COMPATIBLE -> ProviderIntegrationManager.OPENAI_COMPAT_DESCRIPTOR_ID
    AiProviderType.OPENROUTER -> ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID
    AiProviderType.GROQ -> ProviderIntegrationManager.GROQ_DESCRIPTOR_ID
    AiProviderType.HUGGING_FACE -> ProviderIntegrationManager.HUGGING_FACE_DESCRIPTOR_ID
    AiProviderType.CLOUDFLARE_WORKERS_AI -> ProviderIntegrationManager.CLOUDFLARE_DESCRIPTOR_ID
    AiProviderType.GPT_OSS_HOSTED -> ProviderIntegrationManager.GPT_OSS_DESCRIPTOR_ID
    AiProviderType.QWEN_HOSTED -> ProviderIntegrationManager.QWEN_DESCRIPTOR_ID
    AiProviderType.LOCAL_GGUF -> ProviderIntegrationManager.GGUF_MODEL_DESCRIPTOR_ID
    AiProviderType.ON_DEVICE_CORE -> "aeris-local-core"
}

/**
 * Derives an honest UI connection state. Saving an API key alone NEVER produces CONNECTED;
 * only an actual verified endpoint response sets CONNECTED.
 */
internal fun resolveProviderConnectionUiStatus(
    isEnabled: Boolean,
    providerType: AiProviderType,
    hasSavedKey: Boolean,
    hasEndpointUrl: Boolean,
    health: ModelHealthState
): ProviderConnectionUiStatus {
    if (!isEnabled) return ProviderConnectionUiStatus.DISABLED
    return when (health.status) {
        ModelHealthStatus.CONNECTED -> ProviderConnectionUiStatus.CONNECTED
        ModelHealthStatus.TESTING -> ProviderConnectionUiStatus.CONNECTING
        ModelHealthStatus.RATE_LIMITED, ModelHealthStatus.QUOTA_EXCEEDED -> ProviderConnectionUiStatus.RATE_LIMITED
        ModelHealthStatus.UNAVAILABLE -> ProviderConnectionUiStatus.TEMPORARILY_UNAVAILABLE
        ModelHealthStatus.ERROR -> ProviderConnectionUiStatus.ERROR
        ModelHealthStatus.NEEDS_AUTH -> ProviderConnectionUiStatus.NEEDS_AUTHENTICATION
        ModelHealthStatus.AVAILABLE, ModelHealthStatus.NOT_TESTED, ModelHealthStatus.UNCONNECTED -> {
            when {
                providerType == AiProviderType.GEMINI_FIREBASE -> ProviderConnectionUiStatus.NOT_CONNECTED
                providerType.requiresApiKey && !hasSavedKey -> ProviderConnectionUiStatus.NEEDS_AUTHENTICATION
                !hasEndpointUrl -> ProviderConnectionUiStatus.NOT_CONNECTED
                else -> ProviderConnectionUiStatus.NOT_CONNECTED
            }
        }
    }
}

@Composable
fun AiProvidersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenApiKeyEntry: (AiProviderType) -> Unit,
    onNavigateToLocalModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val healthStates by viewModel.healthStates.collectAsState()
    val operatingMode by viewModel.aiOperatingMode.collectAsState()
    val freeOnlyMode by viewModel.freeOnlyMode.collectAsState()
    val autoSwitchEnabled by viewModel.autoSwitchEnabled.collectAsState()
    val manualModelLock by viewModel.manualModelLock.collectAsState()
    val localFallbackEnabled by viewModel.localFallbackEnabled.collectAsState()
    val disabledProviders by viewModel.disabledProviders.collectAsState()
    val openAiCfg by viewModel.openAiConfig.collectAsState()
    val openRouterCfg by viewModel.openRouterConfig.collectAsState()
    val groqCfg by viewModel.groqConfig.collectAsState()
    val hfCfg by viewModel.huggingFaceConfig.collectAsState()
    val cfCfg by viewModel.cloudflareConfig.collectAsState()
    val gptOssCfg by viewModel.gptOssConfig.collectAsState()
    val qwenCfg by viewModel.qwenConfig.collectAsState()
    val localModels by viewModel.models.collectAsState()
    val lifecycleStates by viewModel.modelManager.lifecycleStates.collectAsState()

    var showPaidConsentDialog by remember { mutableStateOf(false) }
    var pendingRemoveProvider by remember { mutableStateOf<AiProviderType?>(null) }

    val defaultLocalModel = localModels.firstOrNull { it.id == ModelManager.DEFAULT_RECOMMENDED_MODEL_ID }
    val defaultLocalLifecycle = lifecycleStates[ModelManager.DEFAULT_RECOMMENDED_MODEL_ID]
        ?: if (defaultLocalModel?.isInstalled == true) LocalModelLifecycleState.READY else LocalModelLifecycleState.NOT_DOWNLOADED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("ai_providers_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(34.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                        .testTag("ai_providers_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Providers & API Keys",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                    Text(
                        text = "Encrypted KeyStore • Official API Setup • Mode Routing",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextCoolGrey
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.testAllConfiguredProviders() },
                border = BorderStroke(1.dp, CyanGlow),
                modifier = Modifier.testTag("providers_test_all_button")
            ) {
                Text("Test All", style = Typography.labelSmall, color = CyanBright)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .testTag("ai_providers_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. AI Operating Mode Selector
            item {
                HoloCard(modifier = Modifier.fillMaxWidth().testTag("ai_operating_mode_card")) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI OPERATING MODE", style = Typography.labelLarge, color = CyanNeon)
                        AiOperatingMode.entries.forEach { mode ->
                            val selected = operatingMode == mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAiOperatingMode(mode) }
                                    .testTag("ai_mode_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) CyanDeep.copy(alpha = 0.45f) else HoloSurfaceVariant,
                                border = BorderStroke(1.dp, if (selected) CyanNeon else HoloCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.title,
                                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) CyanNeon else TextWhite
                                        )
                                        Text(
                                            text = mode.subtitle,
                                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                                            color = TextCoolGrey
                                        )
                                    }
                                    if (selected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = CyanNeon, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        ProviderToggleRow(
                            title = "Free-Only Mode",
                            subtitle = "Blocks paid / Blaze models unless opted in",
                            checked = freeOnlyMode,
                            onCheckedChange = { enableFreeOnly ->
                                if (enableFreeOnly) viewModel.setFreeOnlyMode(true) else showPaidConsentDialog = true
                            },
                            testTag = "providers_free_only_switch"
                        )
                        ProviderToggleRow(
                            title = "Auto-Switch on Failure / Rate Limit",
                            subtitle = "Fails over across connected free providers",
                            checked = autoSwitchEnabled,
                            onCheckedChange = { viewModel.setAutoSwitchEnabled(it) },
                            testTag = "providers_auto_switch"
                        )
                        ProviderToggleRow(
                            title = "Manual Model Lock",
                            subtitle = "Keeps your preferred model without automatic switching",
                            checked = manualModelLock,
                            onCheckedChange = { viewModel.setManualModelLock(it) },
                            testTag = "providers_manual_lock_switch"
                        )
                        ProviderToggleRow(
                            title = "Local Model Fallback (Hybrid)",
                            subtitle = "Allows Offline Core / Local GGUF when cloud fails",
                            checked = localFallbackEnabled,
                            onCheckedChange = { viewModel.setLocalFallbackEnabled(it) },
                            testTag = "providers_local_fallback_switch"
                        )
                    }
                }
            }

            // 2. Offline Gemma 3 1B Q4 Card
            item {
                HoloCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Text("OFFLINE DEFAULT: GEMMA 3 1B Q4", style = Typography.labelLarge, color = CyanNeon)
                            }
                            Text(
                                text = defaultLocalLifecycle.label,
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (defaultLocalLifecycle == LocalModelLifecycleState.READY || defaultLocalLifecycle == LocalModelLifecycleState.RUNNING) StatusSuccess else CyanBright
                            )
                        }
                        Text(
                            text = "~806 MB Q4_K_M GGUF recommended for Redmi 13 5G (8 GB RAM). Wi-Fi download with Pause/Resume & SHA-256 verification available in Offline Model Manager.",
                            style = Typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextCoolGrey
                        )
                        OutlinedButton(
                            onClick = onNavigateToLocalModels,
                            border = BorderStroke(1.dp, CyanNeon),
                            modifier = Modifier.fillMaxWidth().testTag("open_local_model_manager_button")
                        ) {
                            Text("Manage Offline Models & Downloads", style = Typography.labelSmall, color = CyanNeon)
                        }
                    }
                }
            }

            // 3. Hosted & Cloud Provider Cards
            items(ProviderDirectory.allCloudSetupTargets(), key = { it.name }) { providerType ->
                val meta = ProviderDirectory.metadataFor(providerType)
                val cfg = when (providerType) {
                    AiProviderType.OPENAI_COMPATIBLE -> openAiCfg
                    AiProviderType.OPENROUTER -> openRouterCfg
                    AiProviderType.GROQ -> groqCfg
                    AiProviderType.HUGGING_FACE -> hfCfg
                    AiProviderType.CLOUDFLARE_WORKERS_AI -> cfCfg
                    AiProviderType.GPT_OSS_HOSTED -> gptOssCfg
                    AiProviderType.QWEN_HOSTED -> qwenCfg
                    else -> null
                }
                val savedToken = cfg?.third.orEmpty()
                val hasSavedKey = savedToken.isNotBlank()
                val hasEndpointUrl = cfg?.first?.isNotBlank() == true && !cfg.first.contains("ACCOUNT_ID", ignoreCase = true)
                val isEnabled = providerType !in disabledProviders
                val descriptorId = descriptorIdForProvider(providerType)
                val health = healthStates[descriptorId] ?: ModelHealthState(descriptorId)
                val uiStatus = resolveProviderConnectionUiStatus(
                    isEnabled = isEnabled,
                    providerType = providerType,
                    hasSavedKey = hasSavedKey,
                    hasEndpointUrl = hasEndpointUrl,
                    health = health
                )

                val badgeColor = when (uiStatus) {
                    ProviderConnectionUiStatus.CONNECTED -> StatusSuccess
                    ProviderConnectionUiStatus.CONNECTING -> CyanNeon
                    ProviderConnectionUiStatus.RATE_LIMITED, ProviderConnectionUiStatus.TEMPORARILY_UNAVAILABLE -> StatusWarning
                    ProviderConnectionUiStatus.NEEDS_AUTHENTICATION -> GoldAccent
                    ProviderConnectionUiStatus.ERROR -> AlertRed
                    ProviderConnectionUiStatus.DISABLED -> TextMuted
                    ProviderConnectionUiStatus.NOT_CONNECTED -> if (hasSavedKey && hasEndpointUrl) CyanBright else TextCoolGrey
                }

                val statusBadgeLabel = if (
                    uiStatus == ProviderConnectionUiStatus.NOT_CONNECTED &&
                    hasSavedKey && hasEndpointUrl
                ) {
                    "Not Connected (Key Saved)"
                } else {
                    uiStatus.label
                }

                HoloCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider_card_${providerType.name.lowercase()}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meta.shortName,
                                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Text(
                                    text = statusBadgeLabel,
                                    style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = badgeColor,
                                    modifier = Modifier.testTag("provider_status_${providerType.name.lowercase()}")
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.setProviderEnabled(providerType, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep),
                                modifier = Modifier.testTag("provider_enable_switch_${providerType.name.lowercase()}")
                            )
                        }

                        if (providerType == AiProviderType.GEMINI_FIREBASE) {
                            Text(
                                text = "Auth: Firebase App Check (no manual key stored)",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = CyanBright
                            )
                        } else {
                            val keyStatusLine = if (hasSavedKey) {
                                "Saved Key: ${CredentialVault.maskKey(savedToken)} (Encrypted)"
                            } else {
                                "Saved Key: Not saved"
                            }
                            Text(
                                text = keyStatusLine,
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (hasSavedKey) StatusSuccess else TextCoolGrey,
                                modifier = Modifier.testTag("provider_masked_key_${providerType.name.lowercase()}")
                            )
                            val activeModelLabel = cfg?.second?.ifBlank { providerType.suggestedModelId } ?: providerType.suggestedModelId
                            Text(
                                text = "Model: $activeModelLabel",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!health.lastError.isNullOrBlank()) {
                            Text(
                                text = health.lastError.orEmpty(),
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = AlertRed
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (providerType != AiProviderType.GEMINI_FIREBASE) {
                                Button(
                                    onClick = { onOpenApiKeyEntry(providerType) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("provider_add_edit_key_${providerType.name.lowercase()}")
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (hasSavedKey) "Edit Key" else "Add Key", style = Typography.labelSmall, color = TextWhite)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        ProviderDirectory.openOfficialApiPage(context, providerType).onFailure {
                                            Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    border = BorderStroke(1.dp, CyanGlow),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Official Page", style = Typography.labelSmall, color = CyanBright)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.testModelConnection(descriptorId) },
                                enabled = isEnabled,
                                border = BorderStroke(1.dp, CyanGlow),
                                modifier = Modifier
                                    .weight(0.85f)
                                    .testTag("provider_test_${providerType.name.lowercase()}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Test", style = Typography.labelSmall, color = CyanBright)
                            }

                            if (providerType != AiProviderType.GEMINI_FIREBASE && hasSavedKey) {
                                IconButton(
                                    onClick = { pendingRemoveProvider = providerType },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("provider_remove_${providerType.name.lowercase()}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Key", tint = AlertRed)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }

    if (showPaidConsentDialog) {
        AlertDialog(
            onDismissRequest = { showPaidConsentDialog = false },
            title = { Text("ALLOW BILLING MODELS?", style = Typography.titleMedium, color = GoldAccent) },
            text = {
                Text(
                    text = "Disabling Free-Only Mode allows Gemini 4 Argon and non-:free OpenRouter endpoints which can incur provider charges. Continue, Sir ji?",
                    style = Typography.bodyMedium,
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setFreeOnlyMode(false)
                    showPaidConsentDialog = false
                }) { Text("ALLOW", color = GoldAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showPaidConsentDialog = false }) { Text("KEEP FREE-ONLY", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    pendingRemoveProvider?.let { target ->
        val targetMeta = ProviderDirectory.metadataFor(target)
        AlertDialog(
            onDismissRequest = { pendingRemoveProvider = null },
            title = { Text("REMOVE API KEY?", style = Typography.titleMedium, color = AlertRed) },
            text = {
                Text(
                    text = "Delete saved encrypted API key for ${targetMeta.shortName}? Provider status will reset to Needs Authentication / Not Connected.",
                    style = Typography.bodyMedium,
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeProviderKey(target)
                        pendingRemoveProvider = null
                        Toast.makeText(context, "${targetMeta.shortName} key removed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_remove_key_button")
                ) { Text("REMOVE KEY", color = AlertRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveProvider = null }) { Text("CANCEL", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ApiKeyEntryScreen(
    viewModel: MainViewModel,
    providerType: AiProviderType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val meta = ProviderDirectory.metadataFor(providerType)
    val descriptorId = descriptorIdForProvider(providerType)
    val healthStates by viewModel.healthStates.collectAsState()
    val health = healthStates[descriptorId] ?: ModelHealthState(descriptorId)

    val cfg by when (providerType) {
        AiProviderType.OPENAI_COMPATIBLE -> viewModel.openAiConfig
        AiProviderType.OPENROUTER -> viewModel.openRouterConfig
        AiProviderType.GROQ -> viewModel.groqConfig
        AiProviderType.HUGGING_FACE -> viewModel.huggingFaceConfig
        AiProviderType.CLOUDFLARE_WORKERS_AI -> viewModel.cloudflareConfig
        AiProviderType.GPT_OSS_HOSTED -> viewModel.gptOssConfig
        AiProviderType.QWEN_HOSTED -> viewModel.qwenConfig
        else -> viewModel.openAiConfig
    }.collectAsState()

    val savedToken = cfg.third
    val hasSavedKey = savedToken.isNotBlank()
    val maskedSavedKey = CredentialVault.maskKey(savedToken)

    var baseUrl by remember(providerType, cfg.first) {
        mutableStateOf(cfg.first.ifBlank { providerType.defaultBaseUrl })
    }
    var cloudflareAccountId by remember(providerType, cfg.first) {
        val extracted = Regex("""/accounts/([^/]+)/""").find(cfg.first)?.groupValues?.getOrNull(1).orEmpty()
        mutableStateOf(if (extracted == "ACCOUNT_ID") "" else extracted)
    }
    var modelId by remember(providerType, cfg.second) {
        mutableStateOf(cfg.second.ifBlank { providerType.suggestedModelId })
    }
    // CRITICAL SECURITY RULE: Never pre-fill plaintext saved API key into the input box!
    var newApiKeyInput by remember(providerType) { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("api_key_entry_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(34.dp)
                    .background(HoloSurfaceVariant, CircleShape)
                    .border(1.dp, CyanGlow, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${meta.shortName} Key Setup",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanNeon
                )
                Text(
                    text = "Saved encrypted locally in Android KeyStore",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextCoolGrey
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HoloCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PROVIDER INSTRUCTIONS", style = Typography.labelLarge, color = CyanNeon)
                    Text(meta.setupSummary, style = Typography.bodySmall, color = TextWhite)
                    Text(meta.freeTierDisclosure, style = Typography.labelSmall.copy(fontSize = 10.sp), color = GoldAccent)

                    OutlinedButton(
                        onClick = {
                            ProviderDirectory.openOfficialApiPage(context, providerType).onFailure {
                                Toast.makeText(context, "Could not open official API page", Toast.LENGTH_SHORT).show()
                            }
                        },
                        border = BorderStroke(1.dp, CyanNeon),
                        modifier = Modifier.fillMaxWidth().testTag("open_official_api_page_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(meta.officialPageLabel, style = Typography.labelSmall, color = CyanNeon)
                    }
                }
            }

            // Masked existing credential card — never shows plaintext secret
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = HoloSurfaceVariant,
                border = BorderStroke(1.dp, if (hasSavedKey) StatusSuccess.copy(alpha = 0.6f) else HoloCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = if (hasSavedKey) StatusSuccess else TextCoolGrey, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (hasSavedKey) "Stored Key: $maskedSavedKey" else "No API Key Stored",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (hasSavedKey) StatusSuccess else TextCoolGrey,
                                modifier = Modifier.testTag("api_key_masked_preview")
                            )
                            Text(
                                text = "Connection status: ${health.status.label}",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = CyanBright,
                                modifier = Modifier.testTag("api_key_connection_status")
                            )
                        }
                    }
                    if (hasSavedKey) {
                        TextButton(
                            onClick = { showRemoveConfirm = true },
                            modifier = Modifier.testTag("api_key_remove_button")
                        ) {
                            Text("Remove Key", color = AlertRed, fontSize = 11.sp)
                        }
                    }
                }
            }

            HoloCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (providerType == AiProviderType.CLOUDFLARE_WORKERS_AI) {
                        OutlinedTextField(
                            value = cloudflareAccountId,
                            onValueChange = { acc ->
                                cloudflareAccountId = acc
                                baseUrl = ProviderDirectory.buildCloudflareBaseUrl(acc)
                            },
                            label = { Text("Cloudflare Account ID") },
                            placeholder = { Text("32-char Cloudflare Account ID", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth().testTag("cloudflare_account_id_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = HoloCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )
                    }

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Endpoint Base URL (HTTPS)") },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_base_url_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = HoloCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    OutlinedTextField(
                        value = modelId,
                        onValueChange = { modelId = it },
                        label = { Text("Model ID") },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_model_id_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = HoloCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    if (health.discoveredModels.isNotEmpty()) {
                        Text("Discovered Models:", style = Typography.labelSmall, color = CyanBright)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            health.discoveredModels.take(15).forEach { discovered ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (modelId == discovered) CyanDeep else HoloSurfaceVariant,
                                    border = BorderStroke(1.dp, if (modelId == discovered) CyanNeon else HoloCardBorder),
                                    modifier = Modifier.clickable { modelId = discovered }
                                ) {
                                    Text(
                                        text = discovered,
                                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                                        color = TextWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newApiKeyInput,
                        onValueChange = { newApiKeyInput = it },
                        label = {
                            Text(if (hasSavedKey) "Enter New API Key (to replace)" else "API Key")
                        },
                        placeholder = {
                            Text(if (hasSavedKey) "Leave blank to keep $maskedSavedKey" else meta.keyHint, color = TextMuted)
                        },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                        if (pasted.isNotEmpty()) {
                                            newApiKeyInput = pasted
                                            Toast.makeText(context, "Pasted key from clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("api_key_paste_button")
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste Key", tint = CyanBright)
                                }
                                IconButton(
                                    onClick = { showKey = !showKey },
                                    modifier = Modifier.testTag("api_key_visibility_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showKey) "Hide Key" else "Show Key",
                                        tint = CyanNeon
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_secret_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = HoloCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    if (!health.lastError.isNullOrBlank()) {
                        Text(
                            text = "Last Check: ${health.lastError}",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = AlertRed
                        )
                    }

                    val tokenToPersist = { if (newApiKeyInput.isNotBlank()) newApiKeyInput.trim() else savedToken }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveEndpointConfig(providerType, baseUrl, modelId, tokenToPersist())
                                newApiKeyInput = ""
                                viewModel.discoverProviderModels(descriptorId)
                            },
                            border = BorderStroke(1.dp, CyanGlow),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discover", style = Typography.labelSmall, color = CyanBright)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.saveEndpointConfig(providerType, baseUrl, modelId, tokenToPersist())
                                newApiKeyInput = ""
                                viewModel.testModelConnection(descriptorId)
                            },
                            border = BorderStroke(1.dp, CyanNeon),
                            modifier = Modifier.weight(1f).testTag("api_key_test_connection_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test", style = Typography.labelSmall, color = CyanNeon)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            border = BorderStroke(1.dp, HoloCardBorder),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Cancel", style = Typography.labelSmall, color = TextCoolGrey)
                        }
                        Button(
                            onClick = {
                                viewModel.saveEndpointConfig(providerType, baseUrl, modelId, tokenToPersist())
                                newApiKeyInput = ""
                                Toast.makeText(context, "Encrypted locally. Run Test to verify.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                            modifier = Modifier.weight(1.2f).testTag("api_key_save_button")
                        ) {
                            Text("Save Encrypted Key", style = Typography.labelSmall, color = TextWhite)
                        }
                    }
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("REMOVE API KEY?", style = Typography.titleMedium, color = AlertRed) },
            text = {
                Text(
                    text = "Remove saved encrypted key for ${meta.shortName}? Stored credentials will be cleared immediately.",
                    style = Typography.bodyMedium,
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeProviderKey(providerType)
                        newApiKeyInput = ""
                        showRemoveConfirm = false
                        Toast.makeText(context, "API key removed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_remove_single_key_button")
                ) { Text("REMOVE", color = AlertRed) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("CANCEL", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ProviderToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            Text(subtitle, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
        )
    }
}
