package com.example

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ai.configureAppCheck
import com.example.device.FloatingAssistantService
import com.example.domain.model.OperatingMode
import com.example.ui.MainViewModel
import com.example.ui.navigation.AerisNavGraph
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var requestedLaunchRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        requestedLaunchRoute = intent?.getStringExtra(FloatingAssistantService.EXTRA_TARGET_ROUTE)
        enableEdgeToEdge()
        setContent {
            val uiState by mainViewModel.uiState.collectAsState()

            LaunchedEffect(uiState.operatingMode) {
                applyRequestedOrientation(uiState.operatingMode)
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = HoloBlack
                ) {
                    AerisNavGraph(
                        viewModel = mainViewModel,
                        launchRoute = requestedLaunchRoute
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedLaunchRoute = intent.getStringExtra(FloatingAssistantService.EXTRA_TARGET_ROUTE)
    }

    private fun applyRequestedOrientation(mode: OperatingMode) {
        val targetOrientation = when (mode) {
            OperatingMode.WORK -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OperatingMode.NORMAL -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OperatingMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (requestedOrientation != targetOrientation) {
            try {
                requestedOrientation = targetOrientation
            } catch (_: Exception) {
                // Multi-window / desktop windowing environments may restrict programmatic orientation changes.
            }
        }
    }
}
