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
import com.lumora.compresso.model.ProcessingStatus
import com.lumora.compresso.viewmodel.ImageViewModel

@Composable
fun ImageResultsScreen(viewModel: ImageViewModel) {
    val results by viewModel.results.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(results, key = { it.originalImage.uri.toString() }) { result ->
                    val image = result.originalImage
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = image.uri, contentDescription = null,
                                modifier = Modifier.size(56.dp), contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(image.name, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                when (result.status) {
                                    ProcessingStatus.SKIPPED -> {
                                        Text(
                                            text = "Skipped • ${image.getFormattedSize()} (Under Target)",
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
                                            text = "Target not met • ${result.compressedSizeBytes / 1024} KB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    ProcessingStatus.SUCCESS -> {
                                        val savedPercent = if (image.sizeBytes > 0) {
                                            ((image.sizeBytes - result.compressedSizeBytes).toFloat() / image.sizeBytes * 100).toInt()
                                        } else 0

                                        Text(
                                            text = "${image.getFormattedSize()} ➔ ${result.compressedSizeBytes / 1024} KB (-$savedPercent%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
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