package com.example.ui.modelmanager

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiProviderType
import com.example.ai.LocalModelLifecycleState
import com.example.ai.ModelHealthState
import com.example.ai.ModelManager
import com.example.domain.model.ModelMetadata
import com.example.ui.MainViewModel
import com.example.ui.components.HoloCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography
import java.util.Locale

@Composable
fun ModelManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToAiProviders: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aiEngineState by viewModel.aiEngineState.collectAsState()
    val models by viewModel.models.collectAsState()
    val catalogModels by viewModel.catalogModels.collectAsState()
    val healthStates by viewModel.healthStates.collectAsState()
    val telemetrySummary by viewModel.telemetrySummary.collectAsState()
    val freeOnlyMode by viewModel.freeOnlyMode.collectAsState()
    val autoSwitchEnabled by viewModel.autoSwitchEnabled.collectAsState()
    val aiOperatingMode by viewModel.aiOperatingMode.collectAsState()
    val manualModelLock by viewModel.manualModelLock.collectAsState()
    val localFallbackEnabled by viewModel.localFallbackEnabled.collectAsState()
    val defaultLocalModelPrompted by viewModel.defaultLocalModelPrompted.collectAsState()
    val preferredModelId by viewModel.preferredModelId.collectAsState()
    val openAiCfg by viewModel.openAiConfig.collectAsState()
    val openRouterCfg by viewModel.openRouterConfig.collectAsState()
    val groqCfg by viewModel.groqConfig.collectAsState()
    val hfCfg by viewModel.huggingFaceConfig.collectAsState()
    val cfCfg by viewModel.cloudflareConfig.collectAsState()
    val gptOssCfg by viewModel.gptOssConfig.collectAsState()
    val qwenCfg by viewModel.qwenConfig.collectAsState()
    val downloadProgress by viewModel.modelManager.downloadProgress.collectAsState(initial = emptyMap())
    val downloadStatus by viewModel.modelManager.downloadStatus.collectAsState(initial = emptyMap())
    val lifecycleStates by viewModel.modelManager.lifecycleStates.collectAsState()
    val sha256Digests by viewModel.modelManager.sha256Digests.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: AI Brain & Cloud, 1: Offline GGUF
    var showPaidConsentDialog by remember { mutableStateOf(false) }

    val ggufPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: uri.lastPathSegment ?: "imported_model.gguf"
            viewModel.importGgufFromUri(uri, fileName)
        }
    }

    val freeBytes = viewModel.modelManager.getFreeStorageBytes()
    val totalBytes = viewModel.modelManager.getTotalStorageBytes()
    val usedRatio = if (totalBytes > 0) ((totalBytes - freeBytes).toFloat() / totalBytes.toFloat()) else 0.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("model_manager_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("MODEL PERFORMANCE DASHBOARD", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
                Text("Gemini • OpenRouter • Groq • HF • Cloudflare • Qwen • GGUF", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
            }
        }

        // Segmented Tabs
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, HoloCardBorder)
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                listOf("Providers & Telemetry", "Offline GGUF").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = idx }
                            .background(if (selectedTab == idx) CyanDeep else Color.Transparent, RoundedCornerShape(16.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = Typography.labelSmall.copy(fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal),
                            color = if (selectedTab == idx) CyanNeon else TextCoolGrey
                        )
                    }
                }
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .testTag("model_dashboard_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Measured Performance Telemetry Card
                item {
                    HoloCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_performance_summary_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                    Text("LIVE RESPONSE TELEMETRY", style = Typography.labelLarge, color = CyanNeon)
                                }
                                Text(
                                    text = "Active: ${aiEngineState.modelId ?: "None"}",
                                    style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = StatusSuccess
                                )
                            }

                            val ttftStr = telemetrySummary.averageTtftMs?.let { "${it}ms" } ?: "--"
                            val totalStr = telemetrySummary.averageTotalGenMs?.let { "${it}ms" } ?: "--"
                            val prepStr = telemetrySummary.averageAndroidPrepMs?.let { "${it}ms" } ?: "--"
                            val speedStr = telemetrySummary.averageTokensPerSec?.let {
                                String.format(Locale.US, "%.1f tok/s", it)
                            } ?: "--"

                            Text(
                                text = "Avg TTFT: $ttftStr • Avg Total: $totalStr • Android Prep: $prepStr • Speed: $speedStr",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextWhite
                            )
                            Text(
                                text = "Mode: ${aiOperatingMode.title} • Requests: ${telemetrySummary.totalRequests} (${telemetrySummary.successfulRequests} OK, ${telemetrySummary.failoverCount} auto-switches)",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextCoolGrey
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.testAllConfiguredProviders() },
                                    border = BorderStroke(1.dp, CyanGlow),
                                    modifier = Modifier.weight(1f).testTag("test_all_providers_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test All Ready", style = Typography.labelSmall, color = CyanBright)
                                }
                                OutlinedButton(
                                    onClick = onNavigateToAiProviders,
                                    border = BorderStroke(1.dp, CyanNeon),
                                    modifier = Modifier.weight(1f).testTag("open_ai_providers_button")
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("API Keys", style = Typography.labelSmall, color = CyanNeon)
                                }
                            }
                        }
                    }
                }

                item {
                    HoloCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Free-Only Mode", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (freeOnlyMode) StatusSuccess else GoldAccent)
                                    Text("Blocks paid/Blaze-only models like Gemini 4 Argon & non-:free OpenRouter models unless opted in.", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
                                }
                                Switch(
                                    checked = freeOnlyMode,
                                    onCheckedChange = { enableFreeOnly ->
                                        if (enableFreeOnly) viewModel.setFreeOnlyMode(true)
                                        else showPaidConsentDialog = true
                                    },
                                    modifier = Modifier.testTag("free_only_switch"),
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Switch on Quota/Timeout/Error", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                                    Text("Automatically fails over with explicit continuity handoff across free providers.", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
                                }
                                Switch(
                                    checked = autoSwitchEnabled,
                                    onCheckedChange = { viewModel.setAutoSwitchEnabled(it) },
                                    modifier = Modifier.testTag("auto_switch_toggle"),
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Manual Model Lock", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                                    Text("Force preferred model without auto-failover (${if (localFallbackEnabled) "Hybrid fallback ON" else "Local fallback OFF"}).", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
                                }
                                Switch(
                                    checked = manualModelLock,
                                    onCheckedChange = { viewModel.setManualModelLock(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                                )
                            }
                        }
                    }
                }

                items(catalogModels, key = { it.id }) { desc ->
                    val cfg = when (desc.provider) {
                        AiProviderType.OPENAI_COMPATIBLE -> openAiCfg
                        AiProviderType.OPENROUTER -> openRouterCfg
                        AiProviderType.GROQ -> groqCfg
                        AiProviderType.HUGGING_FACE -> hfCfg
                        AiProviderType.CLOUDFLARE_WORKERS_AI -> cfCfg
                        AiProviderType.GPT_OSS_HOSTED -> gptOssCfg
                        AiProviderType.QWEN_HOSTED -> qwenCfg
                        else -> null
                    }
                    BrainModelDescriptorCard(
                        descriptor = desc,
                        health = healthStates[desc.id] ?: ModelHealthState(desc.id),
                        isActive = aiEngineState.modelId == desc.id,
                        isPreferred = preferredModelId == desc.id,
                        isBlockedByFreeOnly = freeOnlyMode && (!desc.isFreeTierEligible || desc.requiresBilling),
                        initialConfig = cfg,
                        onSelectPreferred = { viewModel.setPreferredModelId(desc.id) },
                        onTestConnection = { viewModel.testModelConnection(desc.id) },
                        onDiscoverModels = { viewModel.discoverProviderModels(desc.id) },
                        onSaveEndpoint = { url, modelId, token ->
                            viewModel.saveEndpointConfig(desc.provider, url, modelId, token)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(14.dp)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.modelManager.canOfferFirstTimeSetup(defaultLocalModelPrompted)) {
                    item {
                        val isWifi = viewModel.modelManager.isWifiConnected()
                        HoloCard(modifier = Modifier.fillMaxWidth().testTag("gemma_first_time_setup_card")) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("RECOMMENDED OFFLINE SETUP: GEMMA 3 1B Q4", style = Typography.labelLarge, color = CyanNeon)
                                Text(
                                    text = if (isWifi) {
                                        "Wi-Fi connected. Download Gemma 3 1B Q4 (~806 MB) for fast offline AI on Redmi 13 5G."
                                    } else {
                                        "Wi-Fi not detected. You can wait for Wi-Fi before downloading the ~806 MB Gemma 3 1B Q4 model."
                                    },
                                    style = Typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextWhite
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.markDefaultLocalModelPrompted()
                                            viewModel.startGgufDownload(ModelManager.DEFAULT_RECOMMENDED_MODEL_ID)
                                        },
                                        border = BorderStroke(1.dp, CyanNeon)
                                    ) {
                                        Text("Download Now", style = Typography.labelSmall, color = CyanNeon)
                                    }
                                    TextButton(onClick = { viewModel.markDefaultLocalModelPrompted() }) {
                                        Text("Dismiss", style = Typography.labelSmall, color = TextCoolGrey)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    HoloCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("INTERNAL FLASH STORAGE", style = Typography.labelLarge, color = CyanNeon)
                                Text(
                                    text = String.format(Locale.US, "%.1f GB free", freeBytes / (1024.0 * 1024.0 * 1024.0)),
                                    style = Typography.labelSmall,
                                    color = CyanBright
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { usedRatio },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = CyanNeon,
                                trackColor = HoloSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Target Device: Redmi 13 5G (Snapdragon 4 Gen 2, 8GB RAM). Default offline pick: Gemma 3 1B Q4 (~806 MB, 1.5 GB RAM).",
                                style = Typography.bodyMedium.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }

                items(models, key = { it.id }) { model ->
                    val lifecycle = lifecycleStates[model.id]
                        ?: if (model.isInstalled) LocalModelLifecycleState.READY else LocalModelLifecycleState.NOT_DOWNLOADED
                    ModelCatalogCard(
                        model = model,
                        lifecycleState = lifecycle,
                        sha256Digest = sha256Digests[model.id],
                        progress = downloadProgress[model.id],
                        statusLabel = downloadStatus[model.id],
                        onDownload = { viewModel.startGgufDownload(model.id) },
                        onPause = { viewModel.pauseGgufDownload(model.id) },
                        onCancel = { viewModel.modelManager.cancelDownload(model.id) },
                        onVerifySha = { viewModel.verifyGgufSha256(model.id) },
                        onDelete = { viewModel.deleteGgufModel(model.id) },
                        onActivate = {
                            model.localFilePath?.let { path ->
                                viewModel.loadGgufModel(path, model.id)
                            }
                        }
                    )
                }

                item {
                    HoloCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("LOCAL FILE IMPORT", style = Typography.labelLarge, color = CyanNeon)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Import any .gguf model file directly from device storage with zero cloud access.",
                                style = Typography.bodyMedium.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { ggufPickerLauncher.launch(arrayOf("*/*")) },
                                border = BorderStroke(1.dp, CyanNeon),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = CyanDeep.copy(alpha = 0.2f)),
                                modifier = Modifier.testTag("import_gguf_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Import GGUF", tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select .gguf File From Device", style = Typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(14.dp)) }
            }
        }
    }

    if (showPaidConsentDialog) {
        AlertDialog(
            onDismissRequest = { showPaidConsentDialog = false },
            title = { Text("ALLOW BILLING MODELS?", style = Typography.titleMedium, color = GoldAccent) },
            text = {
                Text(
                    text = "Disabling Free-Only Mode permits Gemini 4 Argon, paid OpenRouter endpoints, and Gemini Nano Banana 2.1 image generation, which may require billing or external credits. Continue, Sir ji?",
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
}

@Composable
private fun ModelCatalogCard(
    model: ModelMetadata,
    lifecycleState: LocalModelLifecycleState,
    sha256Digest: String?,
    progress: Float?,
    statusLabel: String?,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onVerifySha: () -> Unit,
    onDelete: () -> Unit,
    onActivate: () -> Unit
) {
    val isDownloading = lifecycleState == LocalModelLifecycleState.DOWNLOADING || statusLabel == "Downloading"
    val isPaused = lifecycleState == LocalModelLifecycleState.PAUSED || statusLabel == "Paused"
    val badgeColor = when (lifecycleState) {
        LocalModelLifecycleState.READY, LocalModelLifecycleState.RUNNING -> StatusSuccess
        LocalModelLifecycleState.DOWNLOADING, LocalModelLifecycleState.LOADING -> CyanNeon
        LocalModelLifecycleState.PAUSED -> StatusWarning
        LocalModelLifecycleState.ERROR -> AlertRed
        LocalModelLifecycleState.NOT_DOWNLOADED -> CyanTeal
    }

    HoloCard(modifier = Modifier.fillMaxWidth().testTag("local_model_card_${model.id}")) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.name, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                    if (model.isDefaultRecommended) {
                        Text("Recommended Default", style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = GoldAccent)
                    }
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, badgeColor)
                ) {
                    Text(
                        text = lifecycleState.label.uppercase(),
                        style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Size: ${model.fileSizeBytes / (1024 * 1024)} MB • ${model.parameterSize} • ${model.quantization}", style = Typography.labelSmall, color = TextSecondary)
            Text("RAM Required: ~${model.requiredRamMb} MB • ${model.license}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)

            if (!sha256Digest.isNullOrBlank()) {
                Text(
                    text = "SHA-256: ${sha256Digest.take(20)}... (${if (model.sha256Verified == false) "MISMATCH" else "Verified"})",
                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (model.sha256Verified == false) AlertRed else StatusSuccess
                )
            }

            if ((isDownloading || isPaused) && progress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = if (isPaused) StatusWarning else CyanNeon,
                    trackColor = HoloSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDownloading) {
                    OutlinedButton(onClick = onPause, border = BorderStroke(1.dp, StatusWarning)) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", style = Typography.labelSmall, color = StatusWarning)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = onCancel, border = BorderStroke(1.dp, AlertRed)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", style = Typography.labelSmall, color = AlertRed)
                    }
                } else if (isPaused) {
                    OutlinedButton(onClick = onDownload, border = BorderStroke(1.dp, CyanNeon)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", style = Typography.labelSmall, color = CyanNeon)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = onCancel, border = BorderStroke(1.dp, AlertRed)) {
                        Text("Cancel", style = Typography.labelSmall, color = AlertRed)
                    }
                } else if (model.isInstalled) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete GGUF", tint = AlertRed)
                    }
                    OutlinedButton(onClick = onVerifySha, border = BorderStroke(1.dp, CyanGlow)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHA-256", style = Typography.labelSmall, color = CyanBright)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = onActivate, border = BorderStroke(1.dp, CyanNeon)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load GGUF", style = Typography.labelSmall, color = TextPrimary)
                    }
                } else {
                    OutlinedButton(onClick = onDownload, border = BorderStroke(1.dp, CyanTeal)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download GGUF", style = Typography.labelSmall, color = TextPrimary)
                    }
                }
            }
        }
    }
}
