package com.example.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ImageAspectOption
import com.example.ai.ImageGenResult
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

/**
 * Stage 7B: Dedicated Image Studio
 * Uses Gemini Nano Banana 2.1 (Firebase AI Logic) or user-configured OpenAI-compatible image endpoint.
 * Displays transparent Blaze billing disclosures and never simulates fake images when unavailable.
 */
@Composable
fun ImageStudioScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val result by viewModel.imageGenResult.collectAsState()
    val freeOnlyMode by viewModel.freeOnlyMode.collectAsState()

    var promptText by remember { mutableStateOf("") }
    var aspectOption by remember { mutableStateOf(ImageAspectOption.SQUARE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("image_studio_screen")
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("AERIS IMAGE STUDIO", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = TextWhite)
                    Text("Gemini Nano Banana 2.1 / Custom Endpoint", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Billing / Free-Only Disclosure Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = HoloSurfaceVariant,
            border = BorderStroke(1.dp, if (freeOnlyMode) GoldAccent else CyanGlow)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (freeOnlyMode) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (freeOnlyMode) GoldAccent else StatusSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (freeOnlyMode) "Free-Only Mode is ON" else "Blaze / Paid Image Models Opted-In",
                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = if (freeOnlyMode) GoldAccent else StatusSuccess
                    )
                }
                Text(
                    text = if (freeOnlyMode) {
                        "Gemini Nano Banana 2.1 requires a Firebase Blaze billing plan. Enable Blaze opt-in below or configure a custom /images/generations endpoint in Model Hub."
                    } else {
                        "Ready for Gemini Nano Banana 2.1 generation (standard Firebase Blaze quota & charges apply)."
                    },
                    style = Typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
                if (freeOnlyMode) {
                    OutlinedButton(
                        onClick = { viewModel.setFreeOnlyMode(false) },
                        border = BorderStroke(1.dp, GoldAccent),
                        modifier = Modifier.testTag("studio_allow_blaze_button")
                    ) {
                        Text("Allow Billing Models (Turn Free-Only OFF)", style = Typography.labelSmall, color = GoldAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prompt Input Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier.fillMaxWidth().testTag("studio_prompt_input"),
                    label = { Text("Describe the image to generate", color = TextCoolGrey) },
                    placeholder = { Text("e.g., Futuristic glowing cyan orbital ring above night skyline...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextPrimary
                    ),
                    minLines = 3,
                    maxLines = 5
                )

                // Aspect Selector Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageAspectOption.entries.forEach { option ->
                        val selected = aspectOption == option
                        FilterChip(
                            selected = selected,
                            onClick = { aspectOption = option },
                            label = { Text(option.label, style = Typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanDeep,
                                selectedLabelColor = CyanNeon,
                                containerColor = HoloSurfaceVariant,
                                labelColor = TextCoolGrey
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.enhanceImagePrompt(promptText) { improved ->
                                promptText = improved
                            }
                        },
                        enabled = promptText.isNotBlank() && result !is ImageGenResult.Loading,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CyanGlow)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanBright, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enhance", style = Typography.labelSmall, color = CyanBright)
                    }

                    Button(
                        onClick = { viewModel.generateImage(promptText, aspectOption) },
                        enabled = promptText.isNotBlank() && result !is ImageGenResult.Loading,
                        modifier = Modifier.weight(1.3f).testTag("studio_generate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = HoloBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate", color = HoloBlack, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.testImageModel() },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, HoloCardBorder)
                ) {
                    Text("Test Gemini Nano Banana 2.1 Connection", style = Typography.labelSmall, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Output Preview Card (Real Bitmap / Honest Loading / Honest Error)
        when (val res = result) {
            ImageGenResult.Idle -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.dp, HoloCardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Generated images appear here cleanly without placeholders or simulated canvas art.",
                            style = Typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            ImageGenResult.Loading -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.dp, CyanNeon)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CyanNeon)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Generating image (timeout 60s)...", style = Typography.titleSmall, color = CyanBright)
                        Text("Sir ji, please wait while the image model renders.", style = Typography.labelSmall, color = TextSecondary)
                    }
                }
            }

            is ImageGenResult.Error -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AlertRed.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AlertRed)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AlertRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Image Generation Unavailable", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AlertRed)
                        }
                        Text(res.message, style = Typography.bodyMedium.copy(fontSize = 12.sp), color = TextWhite)
                    }
                }
            }

            is ImageGenResult.Success -> {
                val aspectRatio = if (res.bitmap.height > 0) res.bitmap.width.toFloat() / res.bitmap.height.toFloat() else 1f
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.dp, CyanNeon)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rendered by ${res.modelUsed}", style = Typography.labelSmall, color = StatusSuccess)
                        Image(
                            bitmap = res.bitmap.asImageBitmap(),
                            contentDescription = "Generated image from AERIS Image Studio",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio.coerceIn(0.6f, 1.85f))
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveGeneratedImage(res.bitmap) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, CyanNeon)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Gallery", style = Typography.labelSmall, color = CyanNeon)
                            }
                            Button(
                                onClick = {
                                    viewModel.shareGeneratedImage(res.bitmap) { shareIntent ->
                                        context.startActivity(shareIntent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanDeep)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", color = TextWhite)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
