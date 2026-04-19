package com.aipoweredgita.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.ml.ModelStateManager
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProtectedQuizConfigScreen(
    onStartQuiz: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiCfg = LocalUiConfig.current
    var questionCount by remember { mutableStateOf(15) }

    // Go straight to quiz config — model downloads are handled in Settings
    if (uiCfg.isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                QuizConfigScreen(
                    onStartQuiz = { count ->
                        questionCount = count
                        onStartQuiz(count, "tel")
                    }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                // Optional: add secondary info/help panel
            }
        }
    } else {
        QuizConfigScreen(
            onStartQuiz = { count ->
                questionCount = count
                onStartQuiz(count, "tel")
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizConfigScreen(
    onStartQuiz: (Int) -> Unit
) {
    var questionCount by remember { mutableStateOf(15) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "Quiz Configuration",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        // Question Count Section
        Text(
            "Number of Questions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(15, 25).forEach { count ->
                Button(
                    onClick = { questionCount = count },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (questionCount == count)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("$count")
                }
            }
        }

        // Language Info Section (Telugu Only)
        Text(
            "Quiz Language",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Language Display Card (Telugu only, not selectable)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Language",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "🇮🇳 Telugu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }



        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onStartQuiz(questionCount)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Start Quiz (${questionCount} questions in Telugu)")
        }
    }
}

@Composable
fun AIDownloadDialog(
    viewModel: ModelDownloadViewModel,
    onConfirmDownload: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { ModelDownloadManager(context) }
    var modelStatuses by remember { mutableStateOf<List<ModelDownloadManager.ModelStatus>>(emptyList()) }
    var selectedModel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        modelStatuses = manager.getModelsStatus()
        // Auto-select first missing model
        selectedModel = modelStatuses.firstOrNull { !it.isDownloaded }?.name
    }

    val missingModels = modelStatuses.filter { !it.isDownloaded }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "Download AI Engine",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select a model to download:",
                    fontSize = 14.sp
                )

                if (missingModels.isEmpty()) {
                    Text(
                        "All models are already downloaded! ✓",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        missingModels.forEach { model ->
                            val isSelected = selectedModel == model.name
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedModel = model.name },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                    2.dp, MaterialTheme.colorScheme.primary
                                ) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedModel = model.name }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (model.size.isNotEmpty()) {
                                            Text(
                                                "Size: ${model.size}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "Features Unlocked:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("✓ Smart context-aware questions", fontSize = 12.sp)
                    Text("✓ Telugu language support", fontSize = 12.sp)
                    Text("✓ Intelligent difficulty scaling", fontSize = 12.sp)
                    Text("✓ Offline & private", fontSize = 12.sp)
                }

                HorizontalDivider()

                Text(
                    "Download once, then quiz anytime!",
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedModel?.let { onConfirmDownload(it) } },
                enabled = selectedModel != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Download ${selectedModel ?: ""}", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Not Now", color = MaterialTheme.colorScheme.error)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun QuizNotReadyScreen(
    onBackClick: () -> Unit,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.primary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Icon
            Text(
                text = "⏳",
                fontSize = 60.sp
            )

            // Title
            Text(
                text = "AI Models Loading",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = "The AI models are still being downloaded on first launch. This ensures your quiz questions are intelligent and context-aware.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val ctx = LocalContext.current
                    val mgr = remember { ModelDownloadManager(ctx) }
                    var remMb by remember { mutableStateOf<Int?>(null) }
                    LaunchedEffect(Unit) {
                        try { remMb = (mgr.getRemainingDownloadSizeBytes() / (1024 * 1024)).toInt() } catch (e: Exception) {
                            android.util.Log.w("ProtectedQuizConfig", "Failed to get remaining size (dialog)", e)
                        }
                    }
                    LaunchedEffect(Unit) {
                        while (true) {
                            try { remMb = (mgr.getRemainingDownloadSizeBytes() / (1024 * 1024)).toInt() } catch (e: Exception) {
                                android.util.Log.w("ProtectedQuizConfig", "Failed to get remaining size (dialog refresh)", e)
                            }
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                    Text(
                        text = "Remaining to download: ${remMb ?: 0} MB",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Mirror per-file summary (names hidden)
                    val perFileProgress by viewModel.fileProgressList.collectAsState()
                    val filesRemaining by viewModel.filesRemaining.collectAsState()
                    if (perFileProgress.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Files downloading: ${filesRemaining}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                perFileProgress.forEachIndexed { idx, p ->
                                    Text(
                                        text = "File ${idx + 1}: ${p.percentage}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LinearProgressIndicator(
                                        progress = { p.percentage / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Features
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureRow("✓ Intelligent Questions")
                FeatureRow("✓ Theme-Based Learning")
                FeatureRow("✓ 100% Offline")
                FeatureRow("✓ Context Aware")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Back Button
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Back to Home",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info Text
            Text(
                text = "Please wait for models to download before starting quiz.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun FeatureRow(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
    )
}