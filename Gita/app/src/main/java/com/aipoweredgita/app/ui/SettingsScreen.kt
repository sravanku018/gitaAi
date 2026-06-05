package com.aipoweredgita.app.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
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
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.GlassCard
import androidx.compose.ui.graphics.luminance

import com.aipoweredgita.app.ui.theme.*

@Composable
fun SettingsScreen(
    themePreferences: ThemePreferences,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dynamicColor by themePreferences.isDynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val accent by themePreferences.accent.collectAsStateWithLifecycle(initialValue = "Sacred")
    val context = LocalContext.current
    val modelManager = remember { ModelDownloadManager(context) }
    var totalModelSize by remember { mutableStateOf(0L) }
    var remainingBytes by remember { mutableStateOf(0L) }
    var measuredTotalMb by remember { mutableStateOf<Int?>(null) }
    var checkingModels by remember { mutableStateOf(false) }
    var modelStatuses by remember { mutableStateOf<List<ModelDownloadManager.ModelStatus>>(emptyList()) }
    val allDownloaded = remainingBytes <= 0L

    val qwenWorkInfo by com.aipoweredgita.app.services.QwenDownloadWorker
        .getDownloadStatusLive(context, "Qwen3 0.6B")
        .observeAsState()
    val isQwenDownloading = com.aipoweredgita.app.services.QwenDownloadWorker.isDownloading(context, "Qwen3 0.6B")
    val qwenDownloadProgress = qwenWorkInfo?.progress?.getInt("overallProgress", 0) ?: 0

    val gemmaWorkInfo by com.aipoweredgita.app.services.GemmaDownloadWorker
        .getDownloadStatusLive(context)
        .observeAsState()
    val isGemmaDownloading = com.aipoweredgita.app.services.GemmaDownloadWorker.isDownloading(context)
    val gemmaDownloadProgress = gemmaWorkInfo?.progress?.getInt("overallProgress", 0) ?: 0

    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val authPrefs = remember { com.aipoweredgita.app.utils.AuthPreferences.getInstance(context) }
    val uiCfg = LocalUiConfig.current
    val tier = com.aipoweredgita.app.utils.DeviceTierDetector.detect(context)
    val deviceTier = tier
    var selectedModel by remember { mutableStateOf(prefs.getString("selected_ai_model", "Auto (Recommended)") ?: "Auto (Recommended)") }
    val modelOptions = listOf("Auto (Recommended)", "Qwen3 0.6B", "Gemma 4 2B (Advanced)", "Cloud Proxy (Groq)")

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
    }

    LaunchedEffect(Unit) { refreshStats() }

    // Auto-refresh when downloads complete
    LaunchedEffect(qwenWorkInfo) {
        if (qwenWorkInfo?.state == androidx.work.WorkInfo.State.SUCCEEDED) refreshStats()
    }
    LaunchedEffect(gemmaWorkInfo) {
        if (gemmaWorkInfo?.state == androidx.work.WorkInfo.State.SUCCEEDED) refreshStats()
    }

    val isDark = rememberThemeIsDark()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        if (isDark) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
        }
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleLarge,
                color = gold
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 32.dp,
                elevation = 4.dp,
                tint = cardBg,
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Theme Mode Selection
                    Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = textPrimary.copy(alpha = 0.9f))
                    
                    val currentThemeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = com.aipoweredgita.app.utils.ThemeMode.SYSTEM)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.aipoweredgita.app.utils.ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = currentThemeMode == mode,
                                onClick = { scope.launch { themePreferences.setThemeMode(mode) } },
                                label = { Text(mode.name.lowercase().capitalize()) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gold,
                                    selectedLabelColor = Color.Black,
                                    labelColor = textSecondary,
                                    containerColor = cardBg
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorder)

                    // Dynamic Color Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamic Color", color = textPrimary.copy(alpha = 0.9f))
                            Text(
                                "Use system wallpaper colors (Android 12+)",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { enabled -> scope.launch { themePreferences.setDynamicColor(enabled) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = gold,
                                checkedTrackColor = gold.copy(alpha = 0.3f),
                                uncheckedThumbColor = textSecondary,
                                uncheckedTrackColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
                            )
                        )
                    }

                    HorizontalDivider(color = cardBorder)

                    // Accent Color Selection (Only relevant when Dynamic Color is off)
                    Column(
                        modifier = Modifier.graphicsLayer {
                            alpha = if (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 1f else 0.5f
                        }
                    ) {
                        Text("Accent Color", style = MaterialTheme.typography.titleMedium, color = textPrimary.copy(alpha = 0.9f))
                        Text(
                            "Custom brand colors (Effective when Dynamic Color is off)",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Sacred", "Lotus", "Ocean").forEach { name ->
                                FilterChip(
                                    selected = accent == name,
                                    onClick = { 
                                        if (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                            scope.launch { themePreferences.setAccent(name) }
                                        }
                                    },
                                    label = { Text(name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = gold,
                                        selectedLabelColor = Color.Black,
                                        labelColor = textSecondary,
                                        containerColor = cardBg
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HardwareSpecsCard(context)
            ModelRecommendationCard(context)
            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 32.dp,
                elevation = 6.dp,
                tint = cardBg,
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = gold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Model Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Choose which AI model to use for quiz generation and analysis:", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    modelOptions.forEach { model ->
                        val isDeviceHighEnd = tier == com.aipoweredgita.app.utils.DeviceTier.FLAGSHIP
                        val isGemma4 = model.contains("Gemma 4")
                        val isDisabled = isGemma4 && !isDeviceHighEnd
                        val modelDescription = when {
                            model.contains("Auto") -> "Dynamically select the best model based on your device specs"
                            model.contains("Qwen3") -> "Fast 580MB LLM optimized for multilingual text"
                            model.contains("Gemma 4") -> "Powerful 2.58GB LLM for voice + deep analysis (8GB+ RAM)"
                            model.contains("Groq") -> "Cloud-based Groq model (requires internet connection, free)"
                            else -> ""
                        }
                        val isSelected = selectedModel == model ||
                                (model == "Auto (Recommended)" && !selectedModel.contains("Qwen3") && !selectedModel.contains("Gemma") && !selectedModel.contains("Groq"))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { alpha = if (isDisabled) 0.5f else 1f },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = model, style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDisabled) textPrimary.copy(alpha = 0.4f)
                                    else if (isSelected) gold
                                    else textPrimary.copy(alpha = 0.9f))
                                Text(text = modelDescription, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                if (isDisabled) {
                                    Text("⚠ Requires 8GB+ RAM (your device: ${DeviceUtils.getFormattedRAM(context)})", style = MaterialTheme.typography.bodySmall, color = CrimsonDeep)
                                }
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { if (!isDisabled) saveModelSelection(model) },
                                enabled = !isDisabled,
                                colors = RadioButtonDefaults.colors(selectedColor = gold, unselectedColor = textSecondary.copy(alpha = 0.8f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val qwenStatus = modelStatuses.firstOrNull { it.name.contains("Qwen3") }
                    if (qwenStatus != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (qwenStatus.isDownloaded) "✓ Qwen3 model downloaded" else "⚠ Qwen3 model not downloaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (qwenStatus.isDownloaded) gold else CrimsonDeep
                                )
                                if (isQwenDownloading) {
                                    Text("Downloading: $qwenDownloadProgress%", style = MaterialTheme.typography.bodySmall, color = gold)
                                }
                            }
                            if (!qwenStatus.isDownloaded) {
                                if (isQwenDownloading) {
                                    TextButton(onClick = {
                                        com.aipoweredgita.app.services.QwenDownloadWorker.cancelDownload(context)
                                        scope.launch { delay(500); refreshStats() }
                                    }, colors = ButtonDefaults.textButtonColors(contentColor = gold)) { Text("Cancel") }
                                } else {
                                    TextButton(onClick = { com.aipoweredgita.app.services.QwenDownloadWorker.scheduleImmediateDownload(context) }, colors = ButtonDefaults.textButtonColors(contentColor = gold)) { Text("Download") }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val gemmaStatus = modelStatuses.firstOrNull { it.name.contains("Gemma 4") }
                    if (gemmaStatus != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (gemmaStatus.isDownloaded) "✓ Gemma 4 model downloaded" else "⚠ Gemma 4 model not downloaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (gemmaStatus.isDownloaded) gold else CrimsonDeep
                                )
                                if (isGemmaDownloading) {
                                    Text("Downloading: $gemmaDownloadProgress%", style = MaterialTheme.typography.bodySmall, color = gold)
                                }
                            }
                            if (!gemmaStatus.isDownloaded && deviceTier == com.aipoweredgita.app.utils.DeviceTier.FLAGSHIP) {
                                if (isGemmaDownloading) {
                                    TextButton(onClick = {
                                        com.aipoweredgita.app.services.GemmaDownloadWorker.cancelDownload(context)
                                        scope.launch { delay(500); refreshStats() }
                                    }, colors = ButtonDefaults.textButtonColors(contentColor = gold)) { Text("Cancel") }
                                } else {
                                    TextButton(onClick = { com.aipoweredgita.app.services.GemmaDownloadWorker.scheduleImmediateDownload(context) }, colors = ButtonDefaults.textButtonColors(contentColor = gold)) { Text("Download") }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Manage AI Models", style = MaterialTheme.typography.titleLarge, color = gold)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 32.dp,
                elevation = 4.dp,
                tint = cardBg,
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (checkingModels) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = gold, trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                        Text("Updating status…", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }

                    val downloadedMb = (totalModelSize / (1024 * 1024)).toInt()
                    val remainingMb = (remainingBytes / (1024 * 1024)).toInt()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total size: ${measuredTotalMb ?: (downloadedMb + remainingMb)} MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
                            Text("Downloaded: ${downloadedMb} MB", style = MaterialTheme.typography.bodySmall, color = gold)
                        }
                        Button(
                            onClick = { refreshStats() },
                            enabled = !checkingModels,
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black, disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), disabledContentColor = textSecondary)
                        ) { Text("Refresh") }
                    }

                    HorizontalDivider(color = cardBorder)
                    Text("Available Models", style = MaterialTheme.typography.titleMedium, color = textPrimary.copy(alpha = 0.9f))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        modelStatuses.forEach { modelStatus ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 12.dp,
                                tint = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
                                border = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(modelStatus.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
                                            Text(
                                                if (modelStatus.isDownloaded) "Downloaded (${modelStatus.actualSizeBytes / (1024 * 1024)} MB)"
                                                else "Not Downloaded (Size: ${modelStatus.size})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (modelStatus.isDownloaded) gold else textSecondary
                                            )
                                        }
                                        if (modelStatus.isDownloaded) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Ready", tint = gold)
                                        } else {
                                            val isThisModelDownloading = (isGemmaDownloading && modelStatus.name.contains("Gemma 4", ignoreCase = true)) ||
                                                    (isQwenDownloading && modelStatus.name.contains("Qwen3", ignoreCase = true))
                                            
                                            if (isThisModelDownloading) {
                                                val prog = if (modelStatus.name.contains("Qwen3", ignoreCase = true)) qwenDownloadProgress 
                                                           else gemmaDownloadProgress
                                                Text("$prog%", color = gold)
                                            } else {
                                                val isGemma4Locked = modelStatus.name.contains("Gemma 4") && deviceTier != com.aipoweredgita.app.utils.DeviceTier.FLAGSHIP
                                                
                                                OutlinedButton(
                                                    onClick = {
                                                        if (modelStatus.name.contains("Gemma 4", ignoreCase = true)) {
                                                            com.aipoweredgita.app.services.GemmaDownloadWorker.scheduleImmediateDownload(context)
                                                        } else if (modelStatus.name.contains("Qwen", ignoreCase = true)) {
                                                            com.aipoweredgita.app.services.QwenDownloadWorker.scheduleImmediateDownload(context, modelStatus.name)
                                                        }
                                                    },
                                                    enabled = !checkingModels && !isGemmaDownloading && !isQwenDownloading && !isGemma4Locked,
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = if (isGemma4Locked) textSecondary else gold
                                                    ),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp, 
                                                        if (isGemma4Locked) cardBorder else gold
                                                    )
                                                ) { 
                                                    Text(if (isGemma4Locked) "Locked" else "Download") 
                                                }
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
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDeep, disabledContentColor = textSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if ((totalModelSize / (1024 * 1024)).toInt() > 0 && !checkingModels) CrimsonDeep else cardBorder)
                        ) { Text("Clear All Models", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                    }
                }
            }

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

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 32.dp,
                elevation = 6.dp,
                tint = cardBg,
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📚", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quiz Question Bank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
                    }
                    OrnamentRule()
                    Text(
                        if (hasQuestions) "$importedCount questions already imported from the QA dataset."
                        else "Import 3,500+ curated Bhagavad Gita questions from the official QA dataset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    if (datasetImportProgress.isNotEmpty()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = gold, trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                        Text(datasetImportProgress, style = MaterialTheme.typography.bodySmall, color = gold)
                    }
                    if (importSuccess == true) {
                        Text("✓ Questions imported successfully!", style = MaterialTheme.typography.bodySmall, color = gold)
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
                                enabled = !isImportingDataset,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = gold,
                                    contentColor = Color.Black,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    disabledContentColor = textSecondary
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(if (isImportingDataset) "Importing..." else "Import 3,500+ Questions", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = gold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All questions imported", style = MaterialTheme.typography.bodyMedium, color = gold, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            AboutSectionCard(context)

            // Danger Zone — Delete Account
            var showDeleteDialog by remember { mutableStateOf(false) }
            var isDeleting by remember { mutableStateOf(false) }
            var deleteError by remember { mutableStateOf<String?>(null) }

            if (authPrefs.isGuestUser) {
                // Only show for registered users
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    tint = CrimsonDeep.copy(alpha = 0.05f),
                    border = CrimsonDeep.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CrimsonDeep)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CrimsonDeep)
                        }
                        Text(
                            "Permanently delete your account and all associated data. This action cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonDeep,
                                contentColor = Color.White
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Delete My Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Delete Account Confirmation Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                    title = { Text("Delete Account?", color = textPrimary) },
                    text = {
                        Column {
                            Text(
                                "This will permanently delete your account and all data including coins, progress, and favorites.",
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "This action CANNOT be undone.",
                                color = CrimsonDeep,
                                fontWeight = FontWeight.Bold
                            )
                            deleteError?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, color = CrimsonDeep, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isDeleting = true
                                deleteError = null
                                scope.launch {
                                    val authManager = com.aipoweredgita.app.repository.AuthManager.getInstance(context)
                                    val result = authManager.deleteAccount()
                                    if (result.isSuccess) {
                                        // Reset Room DB
                                        try {
                                            val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                                            db.userStatsDao().updateUserId("")
                                            db.userStatsDao().updateProfile("", "")
                                        } catch (e: Exception) {
                                            android.util.Log.e("SettingsScreen", "Failed to reset Room DB", e)
                                        }
                                        showDeleteDialog = false
                                        onAccountDeleted()
                                    } else {
                                        deleteError = "Failed to delete account: ${result.exceptionOrNull()?.message}"
                                    }
                                    isDeleting = false
                                }
                            },
                            enabled = !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonDeep,
                                contentColor = Color.White
                            )
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Deleting...")
                            } else {
                                Text("Delete Permanently")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            enabled = !isDeleting
                        ) {
                            Text("Cancel")
                        }
                    },
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Back to Wisdom", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HardwareSpecsCard(context: android.content.Context) {
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Device Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            SpecRow("Model", DeviceUtils.getModelName())
            SpecRow("RAM", DeviceUtils.getFormattedRAM(context))
            SpecRow("OS", DeviceUtils.getAndroidVersion())
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textPrimary.copy(alpha = 0.9f))
    }
}

@Composable
fun ModelRecommendationCard(context: android.content.Context) {
    val tier = com.aipoweredgita.app.utils.DeviceTierDetector.detect(context)
    val recommendedModel = when(tier) {
        com.aipoweredgita.app.utils.DeviceTier.FLAGSHIP -> "Gemma 4 2B (Advanced Insight)"
        else -> "Qwen3 0.6B (Fast Wisdom)"
    }
    
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        border = gold.copy(alpha = 0.25f),
        tint = gold.copy(alpha = if (isDark) 0.05f else 0.03f),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recommended for You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Based on your device hardware, we suggest using:", style = MaterialTheme.typography.bodySmall, color = textSecondary)
            Text(recommendedModel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = gold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("You can still manually select other models in the Download section if you have a stable connection.", style = MaterialTheme.typography.bodySmall, color = textSecondary.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun AboutSectionCard(context: android.content.Context) {
    val versionText = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = packageInfo.versionName ?: "1.7.0"
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "Version: v$name - Build $code"
        } catch (e: Exception) {
            "Version: v1.7.0 - Build 5"
        }
    }

    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("About AI-Powered Gita", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
            }
            OrnamentRule()
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(versionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textPrimary.copy(alpha = 0.9f))
                Text(
                    "AI-Powered Gita is a modern, premium spiritual companion that brings the eternal wisdom of the Bhagavad Gita to life through on-device AI and a serene user experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
            
            HorizontalDivider(color = cardBorder)
            
            Text(
                "(c) 2026 Bhagavad Gita Dev Team",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

