package com.lumora.compresso.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.lumora.compresso.ui.image.ImageCompressorScreen
import com.lumora.compresso.ui.pdf.PdfCompressorScreen
import com.lumora.compresso.ui.settings.SettingsScreen

enum class Section(val label: String, val icon: ImageVector) {
    Image("Image", Icons.Default.Image),
    Pdf("PDF", Icons.Default.PictureAsPdf),
    Settings("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressoApp() {
    var currentSection by rememberSaveable { mutableStateOf(Section.Image) }
    val activity = LocalContext.current as? Activity

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titleFor(currentSection)) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
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

private fun titleFor(section: Section): String = when (section) {
    Section.Image -> "Image Compressor"
    Section.Pdf -> "PDF Compressor"
    Section.Settings -> "Settings"
}