package com.example.ui.modelmanager

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiModelDescriptor
import com.example.ai.AiProviderType
import com.example.ai.CredentialVault
import com.example.ai.ModelHealthState
import com.example.ai.ModelHealthStatus
import com.example.ai.ProviderDirectory
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BrainModelDescriptorCard(
    descriptor: AiModelDescriptor,
    health: ModelHealthState,
    isActive: Boolean,
    isPreferred: Boolean,
    isBlockedByFreeOnly: Boolean,
    initialConfig: Triple<String, String, String>?,
    onSelectPreferred: () -> Unit,
    onTestConnection: () -> Unit,
    onDiscoverModels: () -> Unit,
    onSaveEndpoint: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val isHostedEndpoint = descriptor.provider in setOf(
        AiProviderType.OPENAI_COMPATIBLE,
        AiProviderType.OPENROUTER,
        AiProviderType.GROQ,
        AiProviderType.HUGGING_FACE,
        AiProviderType.CLOUDFLARE_WORKERS_AI,
        AiProviderType.GPT_OSS_HOSTED,
        AiProviderType.QWEN_HOSTED
    )
    var expanded by remember { mutableStateOf(false) }
    var baseUrl by remember(initialConfig?.first) {
        mutableStateOf(initialConfig?.first?.ifBlank { descriptor.provider.defaultBaseUrl } ?: descriptor.provider.defaultBaseUrl)
    }
    var modelName by remember(initialConfig?.second) {
        mutableStateOf(initialConfig?.second?.ifBlank { descriptor.provider.suggestedModelId } ?: descriptor.provider.suggestedModelId)
    }
    // Never pre-populate a saved plaintext API key back into the editable field.
    val savedToken = initialConfig?.third.orEmpty()
    val hasSavedToken = savedToken.isNotBlank()
    val maskedSavedToken = CredentialVault.maskKey(savedToken)
    var newTokenInput by remember(descriptor.id) { mutableStateOf("") }
    var showTokenInput by remember { mutableStateOf(false) }

    val statusColor = when {
        isBlockedByFreeOnly -> GoldAccent
        health.status == ModelHealthStatus.CONNECTED -> StatusSuccess
        health.status == ModelHealthStatus.AVAILABLE -> CyanBright
        health.status == ModelHealthStatus.TESTING -> CyanNeon
        health.status == ModelHealthStatus.RATE_LIMITED || health.status == ModelHealthStatus.QUOTA_EXCEEDED -> StatusWarning
        health.status == ModelHealthStatus.NEEDS_AUTH -> GoldAccent
        health.status == ModelHealthStatus.ERROR -> AlertRed
        else -> TextCoolGrey
    }

    val statusLabel = when {
        isBlockedByFreeOnly -> "BLOCKED (BILLING)"
        health.status == ModelHealthStatus.CONNECTED && (health.lastTtftMs ?: health.lastLatencyMs) != null ->
            "Connected (${health.lastTtftMs ?: health.lastLatencyMs}ms)"
        health.status == ModelHealthStatus.AVAILABLE -> "Key Saved • Not Tested"
        else -> health.status.label
    }

    val authLabel = when {
        descriptor.provider == AiProviderType.ON_DEVICE_CORE || descriptor.provider == AiProviderType.LOCAL_GGUF ->
            "On-Device (No Key)"
        descriptor.provider == AiProviderType.GEMINI_FIREBASE ->
            if (health.status == ModelHealthStatus.NEEDS_AUTH) "App Check Error" else "Firebase App Check"
        hasSavedToken -> "Saved: $maskedSavedToken (Encrypted)"
        descriptor.provider.requiresApiKey -> "Needs Authentication"
        else -> "Optional Token"
    }

    val freeTierBadge = when {
        descriptor.requiresBilling -> "Paid / Billing"
        descriptor.provider == AiProviderType.OPENROUTER && descriptor.isFreeTierEligible -> "Free-Tier (:free)"
        descriptor.isFreeTierEligible -> "Free-Tier Eligible"
        else -> "Check Provider"
    }

    val lastSuccessLabel = if (health.lastSuccessfulAtMillis > 0L) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(health.lastSuccessfulAtMillis))
    } else "Never tested"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("brain_model_card_${descriptor.id}"),
        shape = RoundedCornerShape(14.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, if (isActive) StatusSuccess else if (isPreferred) CyanNeon else HoloCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = descriptor.displayName,
                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = if (isPreferred || isActive) CyanNeon else TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusSuccess.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, StatusSuccess)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = Typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = StatusSuccess,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${descriptor.provider.displayName} • ${descriptor.modelName.ifBlank { "unconfigured" }}",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HoloSurfaceVariant,
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = statusLabel,
                        style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Auth: $authLabel • Tier: $freeTierBadge • Last OK: $lastSuccessLabel",
                style = Typography.labelSmall.copy(fontSize = 10.sp),
                color = TextCoolGrey
            )

            val perfDetails = buildList {
                health.lastTtftMs?.let { add("TTFT: ${it}ms") }
                health.lastTotalGenMs?.let { add("Total: ${it}ms") }
                health.quotaStatus?.let { add(it) }
            }
            if (perfDetails.isNotEmpty()) {
                Text(
                    text = perfDetails.joinToString(" • "),
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = CyanBright
                )
            }

            if (descriptor.disclosureNote.isNotBlank()) {
                Text(
                    text = descriptor.disclosureNote,
                    style = Typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextMuted
                )
            }

            if (!health.lastError.isNullOrBlank()) {
                Text(
                    text = "Status note: ${health.lastError}",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = AlertRed
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectPreferred,
                    enabled = !isBlockedByFreeOnly,
                    border = BorderStroke(1.dp, if (isPreferred) CyanNeon else HoloCardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isPreferred) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isPreferred) "Preferred" else "Set Preferred",
                        style = Typography.labelSmall,
                        color = if (isPreferred) CyanNeon else TextWhite
                    )
                }

                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isBlockedByFreeOnly,
                    border = BorderStroke(1.dp, CyanGlow),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Ping", style = Typography.labelSmall, color = CyanBright)
                }

                if (isHostedEndpoint) {
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Configure Endpoint",
                            tint = CyanNeon
                        )
                    }
                }
            }

            if (isHostedEndpoint && expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("HTTPS Base URL (e.g. https://host/v1)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Remote Model ID", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
                OutlinedTextField(
                    value = newTokenInput,
                    onValueChange = { newTokenInput = it },
                    label = {
                        Text(
                            text = if (hasSavedToken) "Replace API Key (saved: $maskedSavedToken)" else "Enter API Key / Bearer Token",
                            fontSize = 11.sp
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (hasSavedToken) "Leave blank to keep existing encrypted key" else ProviderDirectory.metadataFor(descriptor.provider).keyHint,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    },
                    visualTransformation = if (showTokenInput) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showTokenInput = !showTokenInput }) {
                            Icon(
                                imageVector = if (showTokenInput) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showTokenInput) "Hide API Key" else "Show API Key",
                                tint = CyanBright
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                if (health.discoveredModels.isNotEmpty()) {
                    Text(
                        text = "Discovered Models (tap to select):",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        health.discoveredModels.take(15).forEach { discoveredId ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (modelName == discoveredId) CyanDeep else HoloSurfaceVariant,
                                border = BorderStroke(1.dp, if (modelName == discoveredId) CyanNeon else HoloCardBorder),
                                modifier = Modifier.clickable { modelName = discoveredId }
                            ) {
                                Text(
                                    text = discoveredId,
                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                val effectiveTokenToSave = { if (newTokenInput.isNotBlank()) newTokenInput.trim() else savedToken }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ProviderDirectory.openOfficialApiPage(context, descriptor.provider).onFailure {
                                Toast.makeText(context, "Unable to open browser page", Toast.LENGTH_SHORT).show()
                            }
                        },
                        border = BorderStroke(1.dp, HoloCardBorder),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("API Page", style = Typography.labelSmall, color = TextCoolGrey)
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveEndpoint(baseUrl, modelName, effectiveTokenToSave())
                            newTokenInput = ""
                            onDiscoverModels()
                        },
                        border = BorderStroke(1.dp, CyanBright),
                        modifier = Modifier.weight(0.95f)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Discover", style = Typography.labelSmall, color = CyanBright)
                    }
                    Button(
                        onClick = {
                            onSaveEndpoint(baseUrl, modelName, effectiveTokenToSave())
                            newTokenInput = ""
                            Toast.makeText(context, "Encrypted locally in KeyStore", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = TextWhite, style = Typography.labelSmall)
                    }
                }
            }
        }
    }
}
