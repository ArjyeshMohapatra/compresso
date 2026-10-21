package com.lumora.compresso.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.lumora.compresso.ui.image.ImageCompressorScreen
import com.lumora.compresso.ui.pdf.PdfCompressorScreen
import com.lumora.compresso.ui.settings.SettingsScreen

enum class Section(val label: String, val icon: ImageVector) {
    Image("Image", Icons.Default.Image),
    Pdf("PDF", Icons.Default.PictureAsPdf),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
fun CompressoApp() {
    var currentSection by rememberSaveable { mutableStateOf(Section.Image) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { section ->
                    NavigationBarItem(
                        selected = currentSection == section,
                        onClick = { currentSection = section },
                        icon = { Icon(section.icon, contentDescription = section.label) },
                        label = { Text(section.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Only apply the bottom bar padding here.
        AnimatedContent(
            targetState = currentSection,
            label = "section",
            modifier = Modifier.padding(innerPadding)
        ) { section ->
            when (section) {
                Section.Image -> ImageCompressorScreen()
                Section.Pdf -> PdfCompressorScreen()
                Section.Settings -> SettingsScreen()
            }
        }
    }
}