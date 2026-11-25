package com.example.ui.coach

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.HoloBlack
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
fun GameCoachScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isObserving by GameCoachService.isObserving.collectAsState()
    val secondsElapsed by GameCoachService.observedGameTimeSeconds.collectAsState()
    val captureStatus by GameCoachService.captureStatus.collectAsState()
    val cloudVisionEnabled by viewModel.gameCoachCloudAnalysisEnabled.collectAsState()
    val coachAdviceFeed by viewModel.coachAdviceFeed.collectAsState()

    val scrollState = rememberScrollState()
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, GameCoachService::class.java).apply {
                action = GameCoachService.ACTION_START
                putExtra(GameCoachService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(GameCoachService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Toast.makeText(context, "AERIS Game Coach observation started, Sir ji!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Screen capture permission required to observe gameplay", Toast.LENGTH_SHORT).show()
        }
    }

    val supportedVoiceCommands = listOf(
        "Observe my game.",
        "Mera game dekho.",
        "Gameplay observe karo.",
        "Mera game dekh kar galtiyan batao."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("game_coach_screen")
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "GAME COACH",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = TextWhite
                    )
                    Text(
                        text = "Consented MediaProjection + Vision Analysis",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright
                    )
                }
            }

            Surface(
                color = if (isObserving) StatusSuccess.copy(alpha = 0.2f) else HoloSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isObserving) StatusSuccess else CyanGlow)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isObserving) StatusSuccess else AlertRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isObserving) "ACTIVE (${secondsElapsed}s)" else "STANDBY",
                        style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (isObserving) StatusSuccess else TextCoolGrey
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Primary Control Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, if (isObserving) StatusSuccess else CyanGlow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(CyanDeep.copy(alpha = 0.4f), CircleShape)
                            .border(1.dp, CyanNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isObserving) "Observation in Progress" else "Ready to Observe",
                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Text(
                            text = captureStatus,
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextCoolGrey
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isObserving) {
                    Button(
                        onClick = {
                            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            projectionLauncher.launch(mpm.createScreenCaptureIntent())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_game_coach_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = HoloBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Screen Observation", color = HoloBlack, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val stopIntent = Intent(context, GameCoachService::class.java).apply {
                                action = GameCoachService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                            Toast.makeText(context, "Game Coach observation stopped", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stop_game_coach_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = TextWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP COACH IMMEDIATELY", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Opt-in Live Gemini Vision Toggle Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, if (cloudVisionEnabled) CyanNeon else HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Live Gemini Vision Coach",
                                style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = TextWhite
                            )
                            Text(
                                text = if (cloudVisionEnabled) "Active: Analyzing sampled frames (~4s)" else "Off: Screen frames stay local only",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (cloudVisionEnabled) StatusSuccess else TextCoolGrey
                            )
                        }
                    }
                    Switch(
                        checked = cloudVisionEnabled,
                        onCheckedChange = { viewModel.setGameCoachCloudAnalysisEnabled(it) },
                        modifier = Modifier.testTag("game_coach_vision_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Requires internet & Firebase AI Logic free-tier quota. Frames are downscaled and never saved unless exported.",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Live Tactical Advice Feed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE TACTICAL ADVICE FEED",
                style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = CyanBright
            )
            if (coachAdviceFeed.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearCoachFeed() }) {
                    Icon(Icons.Default.ClearAll, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = CyanNeon, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (coachAdviceFeed.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = HoloSurfaceVariant,
                border = BorderStroke(1.dp, HoloCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (cloudVisionEnabled) {
                            "Start Screen Observation above to receive periodic tactical callouts from Gemini Vision, Sir ji."
                        } else {
                            "Local observation mode: Turn on 'Live Gemini Vision Coach' above for AI screen coaching, or run screen capture privately."
                        },
                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextCoolGrey
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                coachAdviceFeed.forEach { entry ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = HoloSurface,
                        border = BorderStroke(1.dp, if (entry.isError) AlertRed else CyanGlow.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.sourceModel,
                                    style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = if (entry.isError) AlertRed else CyanNeon
                                )
                                Text(
                                    text = timeFormatter.format(Date(entry.timestamp)),
                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.text,
                                style = Typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextWhite
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Multilingual Voice Triggers Card
        Text(
            text = "SUPPORTED VOICE TRIGGERS",
            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = CyanBright
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                supportedVoiceCommands.forEach { cmd ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = CyanTeal, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\"$cmd\"",
                            style = Typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = TextWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Fair Play & Security Assurance
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, HoloCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fair-Play Protected: AERIS never uses cheats, memory injection, or unauthorized automation.",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextCoolGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
