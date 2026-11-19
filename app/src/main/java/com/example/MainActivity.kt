package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        handleDeepLinkIntent(intent)

        setContent {
            val selectedTheme by mainViewModel.selectedTheme.collectAsState()
            MyApplicationTheme(appTheme = selectedTheme) {
                MainScreen(viewModel = mainViewModel)
            }
        }
    }

    private fun enableHighRefreshRate() {
        try {
            val activeDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            val currentMode = activeDisplay.mode
            val bestMode = activeDisplay.supportedModes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight &&
                        it.refreshRate <= 120.5f
                }
                .maxByOrNull { it.refreshRate }
                ?: activeDisplay.supportedModes
                    .filter { it.refreshRate <= 120.5f }
                    .maxByOrNull { it.refreshRate }

            if (bestMode != null && bestMode.modeId != currentMode.modeId) {
                val params = window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                window.attributes = params
            }
        } catch (_: Exception) {
            // Keep default refresh rate if display modes are unavailable
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val data = intent?.data
        if (data?.scheme == "melvinwaterrider" && data.host == "add_delivery") {
            mainViewModel.setSelectedTab(0)
            mainViewModel.openAddDeliveryWizard()
        }
    }
}
