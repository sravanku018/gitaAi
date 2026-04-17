package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.aipoweredgita.app.utils.ThemePreferences
import androidx.compose.ui.platform.LocalContext
import com.aipoweredgita.app.utils.DeviceUtils
import com.aipoweredgita.app.utils.DeviceConfigCategory
import com.aipoweredgita.app.ml.ModelDownloadManager

@Composable
fun SettingsScreen(
    themePreferences: ThemePreferences,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dynamicColor by themePreferences.isDynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val accent by themePreferences.accent.collectAsStateWithLifecycle(initialValue = "Saffron")
    val context = LocalContext.current
    val modelManager = remember { ModelDownloadManager(context) }
    var totalModelSize by remember { mutableStateOf(0L) }
    var remainingBytes by remember { mutableStateOf(0L) }
    var measuredTotalMb by remember { mutableStateOf<Int?>(null) }
    var checkingModels by remember { mutableStateOf(false) }
    var modelStatuses by remember { mutableStateOf<List<ModelDownloadManager.ModelStatus>>(emptyList()) }
    val allDownloaded = remainingBytes <= 0L

    val qwenWorkInfo by com.aipoweredgita.app.services.QwenDownloadWorker
        .getDownloadStatusLive(context)
        .observeAsState()
    val isQwenDownloading = com.aipoweredgita.app.services.QwenDownloadWorker.isDownloading(context)
    val qwenDownloadProgress = qwenWorkInfo?.progress?.getInt("overallProgress", 0) ?: 0
    
    val gemmaWorkInfo by com.aipoweredgita.app.services.GemmaDownloadWorker
        .getDownloadStatusLive(context)
        .observeAsState()
    val isGemmaDownloading = com.aipoweredgita.app.services.GemmaDownloadWorker.isDownloading(context)
    val gemmaDownloadProgress = gemmaWorkInfo?.progress?.getInt("overallProgress", 0) ?: 0

    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    var selectedModel by remember { mutableStateOf(prefs.getString("selected_ai_model", "Auto (Recommended)") ?: "Auto (Recommended)") }
    val deviceCategory = DeviceUtils.getDeviceCategory(context)
    val modelOptions = listOf("Auto (Recommended)", "Qwen3 0.6B", "Gemma 4 2B (Advanced)")

    // FIX: declare refreshStats BEFORE LaunchedEffect that calls it
    fun refreshStats() {
        scope.launch {
            checkingModels = true
            try {
                totalModelSize = modelManager.getTotalDownloadedSize()
                remainingBytes = modelManager.getRemainingDownloadSizeBytes()
                measuredTotalMb = (modelManager.getMeasuredTotalSizeBytes() / (1024 * 1024)).toInt()
                modelStatuses = modelManager.getModelsStatus()
            } catch (e: Exception) {
                android.util.Log.w("SettingsScreen", "Failed to read stats", e)
            }
            checkingModels = false
        }
    }

    fun saveModelSelection(model: String) {
        selectedModel = model
        com.aipoweredgita.app.ml.ModelAvailability.getInstance(context).updateSelectedModel(model)
        scope.launch(Dispatchers.IO) {
            try {
                val mlManager = com.aipoweredgita.app.ml.HuggingFaceMLManager(context)
                mlManager.initializeModels()
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Error reinitializing models", e)
            }
        }
    }

    LaunchedEffect(Unit) { refreshStats() }

    // Auto-refresh when downloads complete
    LaunchedEffect(qwenWorkInfo) {
        if (qwenWorkInfo?.state == androidx.work.WorkInfo.State.SUCCEEDED) refreshStats()
    }
    LaunchedEffect(gemmaWorkInfo) {
        if (gemmaWorkInfo?.state == androidx.work.WorkInfo.State.SUCCEEDED) refreshStats()
    }

    val uiCfg = LocalUiConfig.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dark Theme")
            Switch(checked = isDarkTheme, onCheckedChange = { enabled -> scope.launch { themePreferences.setDarkTheme(enabled) } })
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dynamic Color (Android 12+)")
            Switch(checked = dynamicColor, onCheckedChange = { enabled -> scope.launch { themePreferences.setDynamicColor(enabled) } })
        }

        Text("Accent Color", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Saffron", "Lotus", "Ocean").forEach { name ->
                FilterChip(selected = accent == name, onClick = { scope.launch { themePreferences.setAccent(name) } }, label = { Text(name) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HardwareSpecsCard(context)
        ModelRecommendationCard(context)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Model Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Choose which AI model to use for quiz generation and analysis:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                modelOptions.forEach { model ->
                    val isDeviceHighEnd = deviceCategory == DeviceConfigCategory.HIGH
                    val isGemma4 = model.contains("Gemma 4")
                    val isDisabled = isGemma4 && !isDeviceHighEnd
                    val modelDescription = when {
                        model.contains("Auto") -> "Automatically selects best model for your device"
                        model.contains("Qwen3") -> "Fast 580MB LLM optimized for multilingual text"
                        model.contains("Gemma 4") -> "Powerful 2.58GB LLM for voice + deep analysis (8GB+ RAM)"
                        else -> ""
                    }
                    val isSelected = selectedModel == model ||
                            (model == "Auto (Recommended)" && !selectedModel.contains("Qwen3") && !selectedModel.contains("Gemma"))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { alpha = if (isDisabled) 0.5f else 1f },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = model, style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                            Text(text = modelDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            if (isDisabled) {
                                Text("⚠ Requires 8GB+ RAM (your device: ${DeviceUtils.getFormattedRAM(context)})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        RadioButton(selected = isSelected, onClick = { if (!isDisabled) saveModelSelection(model) }, enabled = !isDisabled)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val qwenStatus = modelStatuses.firstOrNull { it.name.contains("Qwen") }
                if (qwenStatus != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (qwenStatus.isDownloaded) "✓ Qwen3 model downloaded" else "⚠ Qwen3 model not downloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (qwenStatus.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            if (isQwenDownloading) {
                                Text("Downloading: $qwenDownloadProgress%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (!qwenStatus.isDownloaded) {
                            if (isQwenDownloading) {
                                TextButton(onClick = {
                                    com.aipoweredgita.app.services.QwenDownloadWorker.cancelDownload(context)
                                    scope.launch { delay(500); refreshStats() }
                                }) { Text("Cancel") }
                            } else {
                                TextButton(onClick = { com.aipoweredgita.app.services.QwenDownloadWorker.scheduleImmediateDownload(context) }) { Text("Download") }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val gemmaStatus = modelStatuses.firstOrNull { it.name.contains("Gemma") }
                if (gemmaStatus != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (gemmaStatus.isDownloaded) "✓ Gemma model downloaded" else "⚠ Gemma model not downloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (gemmaStatus.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            if (isGemmaDownloading) {
                                Text("Downloading: $gemmaDownloadProgress%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (!gemmaStatus.isDownloaded && deviceCategory == DeviceConfigCategory.HIGH) {
                            if (isGemmaDownloading) {
                                TextButton(onClick = {
                                    com.aipoweredgita.app.services.GemmaDownloadWorker.cancelDownload(context)
                                    scope.launch { delay(500); refreshStats() }
                                }) { Text("Cancel") }
                            } else {
                                TextButton(onClick = { com.aipoweredgita.app.services.GemmaDownloadWorker.scheduleImmediateDownload(context) }) { Text("Download") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Manage AI Models", style = MaterialTheme.typography.titleLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (checkingModels) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Updating status…", style = MaterialTheme.typography.bodySmall)
                }

                val downloadedMb = (totalModelSize / (1024 * 1024)).toInt()
                val remainingMb = (remainingBytes / (1024 * 1024)).toInt()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total size: ${measuredTotalMb ?: (downloadedMb + remainingMb)} MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Downloaded: ${downloadedMb} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = { refreshStats() }, enabled = !checkingModels) { Text("Refresh") }
                }

                HorizontalDivider()
                Text("Available Models", style = MaterialTheme.typography.titleMedium)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    modelStatuses.forEach { modelStatus ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(modelStatus.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (modelStatus.isDownloaded) "Downloaded (${modelStatus.actualSizeBytes / (1024 * 1024)} MB)"
                                            else "Not Downloaded (Size: ${modelStatus.size})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (modelStatus.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (modelStatus.isDownloaded) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Ready", tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        val isThisModelDownloading = (isGemmaDownloading && modelStatus.name.contains("Gemma", ignoreCase = true)) ||
                                                (isQwenDownloading && modelStatus.name.contains("Qwen", ignoreCase = true))
                                        
                                        if (isThisModelDownloading) {
                                            val prog = if (modelStatus.name.contains("Qwen", ignoreCase = true)) qwenDownloadProgress else gemmaDownloadProgress
                                            Text("$prog%")
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    if (modelStatus.name.contains("Gemma", ignoreCase = true)) {
                                                        com.aipoweredgita.app.services.GemmaDownloadWorker.scheduleImmediateDownload(context)
                                                    } else if (modelStatus.name.contains("Qwen", ignoreCase = true)) {
                                                        com.aipoweredgita.app.services.QwenDownloadWorker.scheduleImmediateDownload(context)
                                                    }
                                                },
                                                enabled = !checkingModels && !isGemmaDownloading && !isQwenDownloading
                                            ) { Text("Download") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                checkingModels = true
                                try { modelManager.clearAllModels() } catch (e: Exception) {
                                    android.util.Log.w("SettingsScreen", "Failed to clear models", e)
                                }
                                refreshStats()
                            }
                        },
                        enabled = (!checkingModels && (totalModelSize / (1024 * 1024)).toInt() > 0),
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear All Models", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quiz Question Dataset Import
        var isImportingDataset by remember { mutableStateOf(false) }
        var datasetImportProgress by remember { mutableStateOf("") }
        var importSuccess by remember { mutableStateOf<Boolean?>(null) }
        var hasQuestions by remember { mutableStateOf(false) }
        var importedCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val importer = com.aipoweredgita.app.ml.BhagavadGitaQAImporter(
                    context,
                    com.aipoweredgita.app.database.GitaDatabase.getDatabase(context).quizQuestionBankDao()
                )
                hasQuestions = importer.hasQuestions()
                if (hasQuestions) {
                    val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                    importedCount = db.quizQuestionBankDao().getQuestionsBySource("dataset_import")
                }
            } catch (_: Exception) { }
        }
    }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📚", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quiz Question Bank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (hasQuestions) "$importedCount questions already imported from the QA dataset."
                    else "Import 3,500+ curated Bhagavad Gita questions from the official QA dataset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (datasetImportProgress.isNotEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(datasetImportProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (importSuccess == true) {
                    Text("✓ Questions imported successfully!", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                    LaunchedEffect(Unit) {
                        hasQuestions = true
                    }
                }
                if (!hasQuestions || importSuccess == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isImportingDataset = true
                                datasetImportProgress = "Downloading questions..."
                                importSuccess = null

                                // Show notification
                                com.aipoweredgita.app.ui.showDatasetImportNotification(
                                    context, 0, "Downloading quiz question dataset..."
                                )
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val importer = com.aipoweredgita.app.ml.BhagavadGitaQAImporter(
                                            context,
                                            com.aipoweredgita.app.database.GitaDatabase.getDatabase(context).quizQuestionBankDao()
                                        )
                                        val count = importer.importDataset(language = "english") { imported, total ->
                                            datasetImportProgress = "Imported $imported questions..."
                                            val progress = if (total > 0) (imported * 100 / total) else 0
                                            com.aipoweredgita.app.ui.showDatasetImportNotification(
                                                context, progress, "$imported / ~$total questions imported"
                                            )
                                        }
                                        datasetImportProgress = "Done! $count questions imported."
                                        importedCount = count
                                        importSuccess = true
                                        com.aipoweredgita.app.ui.showDatasetImportCompleteNotification(context, count)
                                    } catch (e: Exception) {
                                        datasetImportProgress = "Import failed: ${e.message}"
                                        importSuccess = false
                                        android.util.Log.e("SettingsScreen", "Dataset import failed", e)
                                    } finally {
                                        isImportingDataset = false
                                        com.aipoweredgita.app.ui.dismissDatasetImportNotification(context)
                                    }
                                }
                            },
                            enabled = !isImportingDataset
                        ) {
                            Text(if (isImportingDataset) "Importing..." else "Import 3,500+ Questions")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All questions imported", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF10B981), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
fun HardwareSpecsCard(context: android.content.Context) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Device Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            SpecRow("Model", DeviceUtils.getModelName())
            SpecRow("RAM", DeviceUtils.getFormattedRAM(context))
            SpecRow("OS", DeviceUtils.getAndroidVersion())
            val category = DeviceUtils.getDeviceCategory(context)
            val categoryColor = when(category) {
                DeviceConfigCategory.HIGH -> Color(0xFF10B981)
                DeviceConfigCategory.MEDIUM -> Color(0xFFF59E0B)
                DeviceConfigCategory.LOW -> Color(0xFFEF4444)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = categoryColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))) {
                Text("Performance Tier: ${category.name}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = categoryColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ModelRecommendationCard(context: android.content.Context) {
    val category = DeviceUtils.getDeviceCategory(context)
    val recommendedModel = when(category) {
        DeviceConfigCategory.HIGH -> "Gemma 4 2B (Advanced Insight)"
        else -> "Qwen3 0.6B (Fast & Lightweight)"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recommended for You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Based on your device hardware, we suggest using:", style = MaterialTheme.typography.bodySmall)
            Text(recommendedModel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("You can still manually select other models in the Download section if you have a stable connection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}