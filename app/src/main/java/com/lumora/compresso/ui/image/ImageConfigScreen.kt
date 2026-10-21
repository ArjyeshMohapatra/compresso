package com.lumora.compresso.ui.image

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lumora.compresso.model.CompressionMode
import com.lumora.compresso.model.ImageOutputFormat
import com.lumora.compresso.viewmodel.ImageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageConfigScreen(viewModel: ImageViewModel) {
    val context = LocalContext.current
    val images by viewModel.selectedImages.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(context, uris)
    }

    LaunchedEffect(settings.outputFormat, settings.compressionMode) {
        if (settings.outputFormat == ImageOutputFormat.PNG && settings.compressionMode == CompressionMode.TARGET_SIZE) {
            viewModel.updateCompressionMode(CompressionMode.QUALITY)
            snackbarHostState.showSnackbar("PNG is lossless. Switched to Quality mode.")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (images.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Image, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("No images selected", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Select Images")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${images.size} image(s) selected", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { viewModel.clearAll() }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.compressionMode == CompressionMode.QUALITY,
                                onClick = { viewModel.updateCompressionMode(CompressionMode.QUALITY) },
                                label = { Text("By Quality") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.compressionMode == CompressionMode.TARGET_SIZE,
                                onClick = { viewModel.updateCompressionMode(CompressionMode.TARGET_SIZE) },
                                label = { Text("By Target Size") },
                                enabled = settings.outputFormat != ImageOutputFormat.PNG,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (settings.compressionMode == CompressionMode.QUALITY) {
                            Text("Quality: ${settings.quality}%", style = MaterialTheme.typography.bodyLarge)
                            Slider(
                                value = settings.quality.toFloat(),
                                onValueChange = { viewModel.updateQuality(it.toInt()) },
                                valueRange = 5f..100f
                            )
                        } else {
                            OutlinedTextField(
                                value = if (settings.targetSizeKb == 0) "" else settings.targetSizeKb.toString(),
                                onValueChange = { viewModel.updateTargetSizeKb(it.toIntOrNull() ?: 0) },
                                label = { Text("Target Size (KB)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = settings.targetSizeKb <= 0
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("Output Format", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ImageOutputFormat.entries.forEach { format ->
                                FilterChip(
                                    selected = settings.outputFormat == format,
                                    onClick = { viewModel.updateOutputFormat(format) },
                                    label = { Text(format.name) }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = settings.resizeEnabled,
                                onCheckedChange = { viewModel.updateResize(it, settings.targetWidth, settings.targetHeight) }
                            )
                            Text("Resize Image")
                        }

                        if (settings.resizeEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Checkbox(
                                    checked = settings.preserveAspectRatio,
                                    onCheckedChange = { viewModel.updatePreserveAspectRatio(it) }
                                )
                                Text("Preserve Aspect Ratio", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = if (settings.targetWidth == 0) "" else settings.targetWidth.toString(),
                                    onValueChange = { viewModel.updateResize(true, it.toIntOrNull() ?: 0, settings.targetHeight) },
                                    label = { Text("Max Width") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    isError = settings.targetWidth <= 0
                                )
                                OutlinedTextField(
                                    value = if (settings.targetHeight == 0) "" else settings.targetHeight.toString(),
                                    onValueChange = { viewModel.updateResize(true, settings.targetWidth, it.toIntOrNull() ?: 0) },
                                    label = { Text("Max Height") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    isError = settings.targetHeight <= 0
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    items(images, key = { it.uri.toString() }) { image ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = image.uri, contentDescription = null,
                                    modifier = Modifier.size(56.dp), contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(image.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "${image.width} × ${image.height}  •  ${image.getFormattedSize()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.removeImage(image.uri) }) {
                                    Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}