package com.lumora.compresso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumora.compresso.ui.CompressoApp
import com.lumora.compresso.ui.theme.CompressoTheme
import com.lumora.compresso.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()

            CompressoTheme(themeMode = themeMode) {
                // Pass the ViewModel down so we don't create two instances
                CompressoApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}