package com.example.ui.customize

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AerisPersonalityStyle
import com.example.ai.AerisResponseLength
import com.example.ui.MainViewModel
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun CustomizeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val style by viewModel.personalityStyle.collectAsState()
    val length by viewModel.responseLength.collectAsState()
    val savedInstruction by viewModel.customSystemInstruction.collectAsState()
    val memoryEnabled by viewModel.memoryEnabled.collectAsState()
    val animationsEnabled by viewModel.animationsEnabled.collectAsState()
    val batterySaverHolo by viewModel.batterySaverHolo.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()

    var draftInstruction by remember { mutableStateOf("") }
    LaunchedEffect(savedInstruction) {
        if (draftInstruction.isEmpty() && savedInstruction.isNotEmpty()) {
            draftInstruction = savedInstruction
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
            .testTag("customize_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text("Customize AERIS", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                Text("Personality, Custom Instructions & Hologram Behavior", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("CUSTOM INSTRUCTIONS", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tell AERIS how you want responses tailored (applied to every prompt alongside your chosen style).",
                    style = Typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextCoolGrey
                )
                OutlinedTextField(
                    value = draftInstruction,
                    onValueChange = { draftInstruction = it },
                    modifier = Modifier.fillMaxWidth().testTag("custom_instruction_input"),
                    placeholder = {
                        Text("Example: Prefer concise Kotlin examples and explain commands clearly.", color = TextMuted, fontSize = 12.sp)
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = HoloBlack,
                        unfocusedContainerColor = HoloBlack
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            viewModel.setCustomSystemInstruction(draftInstruction)
                            Toast.makeText(context, "Custom instructions saved, Sir ji.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_custom_instruction_button")
                    ) {
                        Text("Save Instructions", color = HoloBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("PERSONALITY STYLE", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
        Spacer(modifier = Modifier.height(6.dp))
        AerisPersonalityStyle.entries.forEach { item ->
            val selected = item == style
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clickable { viewModel.setPersonalityStyle(item) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) CyanDeep.copy(alpha = 0.45f) else HoloSurface,
                border = BorderStroke(1.dp, if (selected) CyanNeon else HoloCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (selected) CyanNeon else TextWhite)
                        Text(item.directive, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
                    }
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("RESPONSE LENGTH", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AerisResponseLength.entries.forEach { item ->
                val selected = item == length
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setResponseLength(item) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) CyanDeep.copy(alpha = 0.55f) else HoloSurface,
                    border = BorderStroke(1.dp, if (selected) CyanNeon else HoloCardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.title, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) CyanNeon else TextWhite)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("MEMORY, VOICE & HOLOGRAM", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomizeToggleRow("Long-Term Memory Injection", memoryEnabled) { viewModel.setMemoryEnabled(it) }
                CustomizeToggleRow("Holographic Orbital Animations", animationsEnabled) { viewModel.setAnimationsEnabled(it) }
                CustomizeToggleRow("Battery-Saver Reduced Motion", batterySaverHolo) { viewModel.setBatterySaverHolo(it) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToVoiceSettings() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Voice Style & Engine", style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                            Text(selectedVoice?.name ?: "Female-priority Android TTS", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
                        }
                    }
                    Text("Configure", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
                }
            }
        }
    }
}

@Composable
private fun CustomizeToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = Typography.bodySmall, color = TextWhite, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
        )
    }
}
