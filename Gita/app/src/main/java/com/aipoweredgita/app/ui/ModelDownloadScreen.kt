package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.aipoweredgita.app.ui.LocalUiConfig
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel

// Keep the UI-facing progress data class for ViewModel coupling
data class ModelDownloadProgress(
    val modelName: String = "",
    val percentage: Int = 0,
    val message: String = "",
    val error: String? = null,
    val currentBytes: Long = 0L,
    val totalBytes: Long = 0L
)

@Composable
fun ModelDownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    val uiCfg = LocalUiConfig.current
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val perFileProgress by viewModel.fileProgressList.collectAsState()
    val aggRemaining by viewModel.remainingBytes.collectAsState()
    val filesRemaining by viewModel.filesRemaining.collectAsState()

    var remainingMb by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(aggRemaining) {
        try { remainingMb = (aggRemaining / (1024 * 1024)).toInt() } catch (e: Exception) {
            android.util.Log.w("ModelDownloadScreen", "Failed to compute remaining MB", e)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 16.dp else 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI Model Manager",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Total download size remaining: ${remainingMb ?: 0} MB (files: ${filesRemaining})",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Overall Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isDownloading -> "Downloading..."
                        overallProgress == 100 -> "All Models Ready"
                        else -> "Ready to Download"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressIndicator(
                    progress = { overallProgress / 100f },
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 6.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$overallProgress%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Multi-file progress summary (names hidden)
        if (perFileProgress.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (uiCfg.isLandscape) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Files downloading: ${filesRemaining}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val firstHalf = perFileProgress.size / 2
                            perFileProgress.take(firstHalf).forEachIndexed { idx, p ->
                                Text(text = "File ${idx + 1}: ${p.percentage}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LinearProgressIndicator(progress = { p.percentage / 100f }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            perFileProgress.drop(perFileProgress.size / 2).forEachIndexed { idx, p ->
                                val i = idx + (perFileProgress.size / 2)
                                Text(text = "File ${i + 1}: ${p.percentage}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LinearProgressIndicator(progress = { p.percentage / 100f }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Files downloading: ${filesRemaining}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        perFileProgress.forEachIndexed { idx, p ->
                            Text(text = "File ${idx + 1}: ${p.percentage}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(progress = { p.percentage / 100f }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Current progress (names hidden)
        if (isDownloading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percentage / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${downloadProgress.percentage}% (${downloadProgress.currentBytes / (1024 * 1024)}MB / ${downloadProgress.totalBytes / (1024 * 1024)}MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (downloadProgress.message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = downloadProgress.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Download Button
        Button(
            onClick = { viewModel.startManagerDownload() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isDownloading
        ) {
            Text(
                text = if (isDownloading) "Downloading..." else "Download Models",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isDownloading) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.cancelDownload() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Models download in background (WiFi recommended)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
