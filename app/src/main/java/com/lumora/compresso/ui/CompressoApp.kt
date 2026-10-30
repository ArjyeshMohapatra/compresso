package com.lumora.compresso.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lumora.compresso.ui.image.ImageCompressorScreen
import com.lumora.compresso.ui.pdf.PdfCompressorScreen
import com.lumora.compresso.ui.settings.SettingsScreen
import com.lumora.compresso.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

enum class Section(val label: String, val icon: ImageVector) {
    Image("Image", Icons.Default.Image),
    Pdf("PDF", Icons.Default.PictureAsPdf),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
fun CompressoApp(settingsViewModel: SettingsViewModel) {
    // 1. PagerState manages the current page smoothly
    val pagerState = rememberPagerState(pageCount = { Section.entries.size })
    val scope = rememberCoroutineScope()

    // Sync the bottom nav state with the pager
    val currentSection = Section.entries[pagerState.currentPage]

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(40.dp))
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    NavigationBar(
                        containerColor = Color.DarkGray,
                        tonalElevation = 0.dp
                    ) {
                        Section.entries.forEachIndexed { index, section ->
                            NavigationBarItem(
                                selected = currentSection == section,
                                onClick = {
                                    // Smoothly animate the pager to the selected tab
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                icon = { Icon(section.icon, contentDescription = section.label) },
                                label = { Text(section.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            // FIX 1: Pre-compose adjacent pages to make swiping buttery smooth
            beyondViewportPageCount = 1,
            // FIX 2: Allow vertical scrolling to work perfectly inside the pages
            userScrollEnabled = true
        ) { page ->
            when (Section.entries[page]) {
                Section.Image -> ImageCompressorScreen(settingsViewModel = settingsViewModel)
                Section.Pdf -> PdfCompressorScreen()
                Section.Settings -> SettingsScreen(settingsViewModel = settingsViewModel)
            }
        }
    }
}