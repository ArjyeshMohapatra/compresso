package com.lumora.compresso.ui.image

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lumora.compresso.model.CompressionMode
import com.lumora.compresso.model.ImageOutputFormat
import com.lumora.compresso.model.SwipeAction
import com.lumora.compresso.viewmodel.ImageViewModel
import com.lumora.compresso.viewmodel.SettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageConfigScreen(
    viewModel: ImageViewModel,
    settingsViewModel: SettingsViewModel,
    onSelectImagesClick: () -> Unit
) {
    val images by viewModel.selectedImages.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val leftAction by settingsViewModel.swipeLeftAction.collectAsState()
    val rightAction by settingsViewModel.swipeRightAction.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var widthText by remember { mutableStateOf(if (settings.targetWidth > 0) settings.targetWidth.toString() else "") }
    var heightText by remember { mutableStateOf(if (settings.targetHeight > 0) settings.targetHeight.toString() else "") }
    var targetSizeText by remember { mutableStateOf(if (settings.targetSizeKb > 0) settings.targetSizeKb.toString() else "") }

    var isConfigExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(settings.outputFormat, settings.compressionMode) {
        if (settings.outputFormat == ImageOutputFormat.PNG && settings.compressionMode == CompressionMode.TARGET_SIZE) {
            viewModel.updateCompressionMode(CompressionMode.QUALITY)
            snackbarHostState.showSnackbar("PNG is lossless. Switched to Quality mode.")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (images.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text("No images selected", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Select one or more images to compress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onSelectImagesClick) {
                    Text("Select Images")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp)
            ) {
                item {
                    Text(
                        text = "${images.size} image(s) selected",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(bottom = if (isConfigExpanded) 16.dp else 0.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isConfigExpanded = !isConfigExpanded }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Image Config", style = MaterialTheme.typography.titleMedium)
                                Icon(
                                    imageVector = if (isConfigExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Config"
                                )
                            }

                            AnimatedVisibility(visible = isConfigExpanded) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
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
                                            value = targetSizeText,
                                            onValueChange = {
                                                targetSizeText = it
                                                it.toIntOrNull()?.let { size -> viewModel.updateTargetSizeKb(size) }
                                            },
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
                                        Text("Resize Image (Max Bounds)")
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
                                                value = widthText,
                                                onValueChange = {
                                                    widthText = it
                                                    viewModel.updateResize(true, it.toIntOrNull() ?: 0, settings.targetHeight)
                                                },
                                                label = { Text("Max Width") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                isError = settings.targetWidth <= 0
                                            )
                                            OutlinedTextField(
                                                value = heightText,
                                                onValueChange = {
                                                    heightText = it
                                                    viewModel.updateResize(true, settings.targetWidth, it.toIntOrNull() ?: 0)
                                                },
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
                        }
                    }
                }

                // --- MORPHE STYLE SWIPE (ZERO-RECOMPOSITION & SINGLE-STATE) ---
                items(images, key = { it.uri.toString() }) { image ->
                    val density = LocalDensity.current
                    val maxSwipePx = with(density) { 110.dp.toPx() }
                    val triggerThresholdPx = maxSwipePx * 0.65f

                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var bounceJob by remember { mutableStateOf<Job?>(null) }
                    val scope = rememberCoroutineScope()

                    val canSwipeLeft = leftAction == SwipeAction.DELETE
                    val canSwipeRight = rightAction == SwipeAction.DELETE

                    // Only recomposes when crossing 0f (changing swipe direction), NOT on every pixel
                    val isSwipingRight by remember(canSwipeRight) {
                        derivedStateOf { offsetX > 0f || (offsetX == 0f && canSwipeRight) }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        // 1. Rounded Red Background (Always in layout tree; hidden in Draw phase at 0f)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = if (offsetX != 0f) 1f else 0f
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        // 2. Foreground Card (Reads offsetX only in Layout phase via .offset {})
                        ElevatedCard(
                            modifier = Modifier
                                .offset { IntOffset(offsetX.roundToInt(), 0) }
                                .fillMaxWidth()
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        offsetX = (offsetX + delta).coerceIn(
                                            minimumValue = if (canSwipeLeft) -maxSwipePx else 0f,
                                            maximumValue = if (canSwipeRight) maxSwipePx else 0f
                                        )
                                    },
                                    onDragStarted = {
                                        // Stop any ongoing bounce-back if the user grabs the card again
                                        bounceJob?.cancel()
                                    },
                                    onDragStopped = {
                                        if (abs(offsetX) >= triggerThresholdPx) {
                                            viewModel.removeImage(image.uri)
                                        } else {
                                            bounceJob = scope.launch {
                                                animate(
                                                    initialValue = offsetX,
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                ) { value, _ ->
                                                    offsetX = value
                                                }
                                            }
                                        }
                                    }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            ListItem(
                                leadingContent = {
                                    AsyncImage(
                                        model = image.uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                headlineContent = {
                                    Text(image.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(
                                        text = "${image.width} × ${image.height}  •  ${image.getFormattedSize()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}