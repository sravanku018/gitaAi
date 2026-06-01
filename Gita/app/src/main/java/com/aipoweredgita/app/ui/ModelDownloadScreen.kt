package com.aipoweredgita.app.ui

<<<<<<< HEAD
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
=======
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
<<<<<<< HEAD
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
=======
import androidx.compose.ui.text.font.FontWeight
import com.aipoweredgita.app.ui.LocalUiConfig
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
<<<<<<< HEAD
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel
import com.aipoweredgita.app.ui.theme.*
=======
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

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
<<<<<<< HEAD
    val context = LocalContext.current
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val perFileProgress by viewModel.fileProgressList.collectAsState()
    val aggRemaining by viewModel.remainingBytes.collectAsState()
    val filesRemaining by viewModel.filesRemaining.collectAsState()
<<<<<<< HEAD
    val modelsStatus by viewModel.modelsStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var remainingMb by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(aggRemaining) {
        try {
            remainingMb = (aggRemaining / (1024 * 1024)).toInt()
        } catch (e: Exception) {
=======

    var remainingMb by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(aggRemaining) {
        try { remainingMb = (aggRemaining / (1024 * 1024)).toInt() } catch (e: Exception) {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            android.util.Log.w("ModelDownloadScreen", "Failed to compute remaining MB", e)
        }
    }

<<<<<<< HEAD
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
                        IconButton(onClick = { viewModel.clearError() }) {
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
                                        onClick = { viewModel.cancelDownload() },
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
                                        onClick = { viewModel.startSingleModelDownload(status.name) },
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
=======
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
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
<<<<<<< HEAD

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
=======
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
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
<<<<<<< HEAD
=======

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
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    }
}
