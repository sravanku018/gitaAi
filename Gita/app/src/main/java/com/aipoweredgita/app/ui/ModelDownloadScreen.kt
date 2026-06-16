package com.aipoweredgita.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.domain.model.ModelDownloadEvent
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel
import com.aipoweredgita.app.ui.theme.*

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
    viewModel: ModelDownloadViewModel = hiltViewModel()
) {
    val uiCfg = LocalUiConfig.current
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val downloadProgress = state.downloadProgress
    val overallProgress = state.overallProgress
    val isDownloading = state.isDownloading
    val perFileProgress = state.fileProgressList
    val aggRemaining = state.remainingBytes
    val filesRemaining = state.filesRemaining
    val modelsStatus = state.modelsStatus
    val errorMessage = state.error

    var remainingMb by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(aggRemaining) {
        try {
            remainingMb = (aggRemaining / (1024 * 1024)).toInt()
        } catch (e: Exception) {
            android.util.Log.w("ModelDownloadScreen", "Failed to compute remaining MB", e)
        }
    }

    val freeSpaceGb = remember(context, isDownloading, modelsStatus) {
        try {
            val freeBytes = context.filesDir.freeSpace
            String.format("%.2f GB", freeBytes.toDouble() / (1024 * 1024 * 1024))
        } catch (e: Exception) {
            "Unknown"
        }
    }

    val statuses = if (modelsStatus.isEmpty()) {
        listOf(
            ModelDownloadManager.ModelStatus("Qwen3 0.6B", "580 MB", false, 0L, ""),
            ModelDownloadManager.ModelStatus("Gemma 4 2B", "2.58 GB", false, 0L, "")
        )
    } else {
        modelsStatus
    }

    val isDark = rememberThemeIsDark()
    val headerGradient = Brush.verticalGradient(
        colors = listOf(Saffron.copy(alpha = 0.15f), Color.Transparent)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(top = if (uiCfg.isLandscape) 16.dp else 32.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AI Model Manager",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldSpark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Private on-device artificial intelligence. No data is sent online.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (uiCfg.isLandscape) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Storage Information Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Storage Info",
                        tint = GoldSpark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Available Storage: $freeSpaceGb",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Requires 1.5x model size to safely unpack and install.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Error Banner
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CrimsonDeep.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, CrimsonDeep),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = CrimsonDeep
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.onEvent(ModelDownloadEvent.ClearError) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Loop through each model status and draw a beautiful card
            statuses.forEach { status ->
                val isQwen = status.name.contains("Qwen", ignoreCase = true)
                val isThisModelDownloading = isDownloading && (
                    downloadProgress.modelName.contains(if (isQwen) "qwen3" else "gemma", ignoreCase = true)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isThisModelDownloading) Saffron else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = status.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldSpark
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (isQwen) Saffron.copy(alpha = 0.2f) else DeepBrown,
                                        contentColor = if (isQwen) Saffron else GoldPale,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = if (isQwen) "Mandatory" else "Optional",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    text = if (isQwen) "Core Offline Translation & Study" else "Voice Studio & Advanced Counsel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = status.size,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Saffron
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        Text(
                            text = if (isQwen) {
                                "Performs immediate Sanskrit-to-English/Hindi translation. Crucial for offline quizzes and reading verses."
                            } else {
                                "Provides fully natural, conversational audio chat with your spiritual guide, and contextual deep-dive queries."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action area
                        if (isThisModelDownloading) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading... ${downloadProgress.percentage}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Saffron,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${downloadProgress.currentBytes / (1024 * 1024)} MB / ${downloadProgress.totalBytes / (1024 * 1024)} MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress.percentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(MaterialTheme.shapes.extraSmall),
                                    color = Saffron,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.onEvent(ModelDownloadEvent.CancelDownload) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDeep),
                                        border = BorderStroke(1.dp, CrimsonDeep)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (status.isDownloaded) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Ready Offline",
                                            tint = ForestMid,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ready Offline",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ForestMid,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Not Downloaded",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Not downloaded",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.onEvent(ModelDownloadEvent.StartSingleModelDownload(status.name)) },
                                        enabled = !isDownloading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isQwen) Saffron else DeepBrown,
                                            contentColor = Color.White
                                        ),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = "Download",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Download")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Overall Progress Summary if multiple things download
            if (isDownloading && !perFileProgress.none { it.percentage < 100 }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Remaining to download: ${remainingMb ?: 0} MB (Files: $filesRemaining)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldSpark
                        )
                        LinearProgressIndicator(
                            progress = { overallProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = GoldSpark,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "Overall progress: $overallProgress%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
