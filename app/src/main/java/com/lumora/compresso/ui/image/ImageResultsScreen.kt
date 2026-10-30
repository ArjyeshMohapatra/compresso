package com.lumora.compresso.ui.image

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lumora.compresso.model.ImageOutputFormat
import com.lumora.compresso.model.ProcessingStatus
import com.lumora.compresso.viewmodel.ImageViewModel

@Composable
fun ImageResultsScreen(viewModel: ImageViewModel) {
    val results by viewModel.results.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp)
            ) {
                items(results, key = { it.originalImage.uri.toString() }) { result ->
                    val image = result.originalImage

                    // FIX: Match the exact extension StorageManager uses
                    val displayName = if (result.status == ProcessingStatus.SUCCESS) {
                        val baseName = image.name.substringBeforeLast(".")
                        val newExt = when (settings.outputFormat) {
                            ImageOutputFormat.JPEG -> "jpg"
                            ImageOutputFormat.PNG -> "png"
                            ImageOutputFormat.WEBP -> "webp"
                        }
                        "$baseName.$newExt"
                    } else {
                        image.name
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = image.uri, contentDescription = null,
                                modifier = Modifier.size(56.dp), contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                when (result.status) {
                                    ProcessingStatus.SKIPPED -> {
                                        Text(
                                            text = result.errorMessage ?: "Unchanged • ${image.getFormattedSize()} (Already optimal)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    ProcessingStatus.FAILED -> {
                                        Text(
                                            text = "Failed: ${result.errorMessage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    ProcessingStatus.TARGET_NOT_MET -> {
                                        Text(
                                            // FIX: Show the exact failure reason from the Processor
                                            text = result.errorMessage ?: "Target not met",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    ProcessingStatus.SUCCESS -> {
                                        val diffBytes = result.compressedSizeBytes - image.sizeBytes
                                        val diffPercent = if (image.sizeBytes > 0) {
                                            (diffBytes.toFloat() / image.sizeBytes * 100).toInt()
                                        } else 0

                                        val diffText = when {
                                            diffPercent == 0 -> "(0%)"
                                            diffPercent > 0 -> "(+${diffPercent}%)"
                                            else -> "(${diffPercent}%)"
                                        }

                                        val color = if (diffPercent >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                                        Text(
                                            text = "${image.getFormattedSize()} ➔ ${result.compressedSizeBytes / 1024} KB $diffText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}