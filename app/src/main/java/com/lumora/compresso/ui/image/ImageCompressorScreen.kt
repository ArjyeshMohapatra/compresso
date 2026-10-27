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
import com.lumora.compresso.model.ProcessingState
import com.lumora.compresso.viewmodel.ImageViewModel
import com.lumora.compresso.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressorScreen(
    viewModel: ImageViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    val maxImagesSelected = 10
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showResults by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val images by viewModel.selectedImages.collectAsState()
    val results by viewModel.results.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImagesSelected)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(context, uris)
    }

    val launchPicker = {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Handle state transitions and errors
    LaunchedEffect(uiState) {
        when (uiState) {
            is ProcessingState.Done -> showResults = true
            is ProcessingState.Error -> {
                val errorMsg = (uiState as ProcessingState.Error).message
                snackbarHostState.showSnackbar("Error: $errorMsg")
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    // System Back Handler for Results
    BackHandler(enabled = showResults) {
        showResults = false
        viewModel.resetUiState()
    }

    // System Back Handler for Processing (Cancels processing)
    BackHandler(enabled = uiState is ProcessingState.Processing) {
        viewModel.cancelProcessing()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (showResults) "Compression Results" else "Image Compressor") },
                navigationIcon = {
                    if (showResults) {
                        IconButton(onClick = {
                            showResults = false
                            viewModel.resetUiState()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            if (uiState !is ProcessingState.Processing) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 100.dp)
                ) {
                    if (!showResults && images.isNotEmpty() && images.size < maxImagesSelected) {
                        SmallFloatingActionButton(
                            onClick = launchPicker,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Images")
                        }
                    }

                    if (!showResults && images.isNotEmpty()) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.clearAll() },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }

                    if (showResults && results.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Preview screen is coming in Phase 5!")
                                }
                            },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            text = { Text("Preview") }
                        )
                    } else if (!showResults && images.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.compressImages(context) },
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
                .padding(paddingValues)
        ) {
            if (showResults) {
                ImageResultsScreen(viewModel)
            } else {
                ImageConfigScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    onSelectImagesClick = launchPicker
                )
            }

            // Full-Screen Loading Overlay
            if (uiState is ProcessingState.Processing) {
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
                            Text("Processing...", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(24.dp))

                            // RESTORED CANCEL BUTTON
                            Button(
                                onClick = { viewModel.cancelProcessing() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}