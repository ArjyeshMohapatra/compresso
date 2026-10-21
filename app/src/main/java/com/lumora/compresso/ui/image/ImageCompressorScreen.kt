package com.lumora.compresso.ui.image

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumora.compresso.viewmodel.ImageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressorScreen(viewModel: ImageViewModel = viewModel()) {
    val context = LocalContext.current

    var showResults by rememberSaveable { mutableStateOf(false) }
    // Tracks if we are waiting for the current processing task to finish
    var waitForResults by rememberSaveable { mutableStateOf(false) }

    val isProcessing by viewModel.isProcessing.collectAsState()
    val images by viewModel.selectedImages.collectAsState()
    val results by viewModel.results.collectAsState()

    // Watch for processing to finish, then swap screens
    LaunchedEffect(isProcessing, waitForResults) {
        if (waitForResults && !isProcessing) {
            waitForResults = false
            showResults = true
        }
    }

    BackHandler(enabled = showResults) {
        showResults = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (showResults) "Compression Results" else "Image Compressor")
                },
                navigationIcon = {
                    if (showResults) {
                        IconButton(onClick = { showResults = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            if (!isProcessing) {
                if (showResults && results.isNotEmpty()) {
                    FloatingActionButton(onClick = { /* Phase 5: Preview */ }) {
                        Text("Preview", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                } else if (!showResults && images.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            // 1. Set wait flag
                            waitForResults = true
                            // 2. Start processing (which immediately sets isProcessing = true)
                            viewModel.compressImages(context)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text("Compress", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showResults) {
                ImageResultsScreen(viewModel)
            } else {
                ImageConfigScreen(viewModel)
            }

            // Global Processing Modal
            if (isProcessing) {
                Dialog(
                    onDismissRequest = { },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Compressing Images...", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Please wait", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}