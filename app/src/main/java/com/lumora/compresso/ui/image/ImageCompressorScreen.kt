package com.lumora.compresso.ui.image

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumora.compresso.viewmodel.ImageViewModel
import com.lumora.compresso.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressorScreen(
    viewModel: ImageViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    var showResults by rememberSaveable { mutableStateOf(false) }
    var waitForResults by rememberSaveable { mutableStateOf(false) }

    val isProcessing by viewModel.isProcessing.collectAsState()
    val images by viewModel.selectedImages.collectAsState()
    val results by viewModel.results.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(context, uris)
    }

    val launchPicker = {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

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
                title = { Text(if (showResults) "Compression Results" else "Image Compressor") },
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
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 80.dp) // Space above NavBar
                ) {
                    // Add More Button
                    if (!showResults && images.isNotEmpty() && images.size < 10) {
                        SmallFloatingActionButton(
                            onClick = launchPicker,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Images")
                        }
                    }

                    // Clear All Button
                    if (!showResults && images.isNotEmpty()) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.clearAll() },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }

                    // Compress / Preview Button
                    if (showResults && results.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { /* Phase 5 */ },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            text = { Text("Preview") }
                        )
                    } else if (!showResults && images.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                waitForResults = true
                                viewModel.compressImages(context)
                            },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            text = { Text("Compress") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (showResults) {
                ImageResultsScreen(viewModel)
            } else {
                ImageConfigScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    onSelectImagesClick = launchPicker // <-- Pass the click action down
                )
            }

            // Full-Screen Loading Overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.Center
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

                    Button(
                        onClick = {
                            viewModel.cancelProcessing()
                            waitForResults = false
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .fillMaxWidth(0.6f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel Processing")
                    }
                }
            }
        }
    }
}