package com.aipoweredgita.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel

// ═══════════════════════════════════════════════════════════════════════════
//  AI DOWNLOAD DIALOG — Sacred scroll style
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AIDownloadDialog(
    viewModel         : ModelDownloadViewModel,
    onConfirmDownload : (String) -> Unit,
    onCancel          : () -> Unit,
    language          : String = "tel"
) {
    val context = LocalContext.current
    val manager = remember { ModelDownloadManager(context) }
    var modelStatuses by remember { mutableStateOf<List<ModelDownloadManager.ModelStatus>>(emptyList()) }
    var selectedModel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        modelStatuses = manager.getModelsStatus()
        selectedModel = modelStatuses.firstOrNull { !it.isDownloaded }?.name
    }

    AIDownloadDialogContent(
        modelStatuses = modelStatuses,
        selectedModel = selectedModel,
        onModelSelect = { selectedModel = it },
        onConfirmDownload = onConfirmDownload,
        onCancel = onCancel,
        language = language
    )
}

@Composable
fun AIDownloadDialogContent(
    modelStatuses     : List<ModelDownloadManager.ModelStatus>,
    selectedModel     : String?,
    onModelSelect     : (String) -> Unit,
    onConfirmDownload : (String) -> Unit,
    onCancel          : () -> Unit,
    language          : String = "tel"
) {
    val missingModels = modelStatuses.filter { !it.isDownloaded }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
        title = {
            Column {
                Text(
                    "ॐ",
                    fontSize = 22.sp,
                    color    = GoldSpark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = translateConfigText("Download AI Engine", language),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OrnamentRule()
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (missingModels.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(Forest.copy(0.2f))
                            .border(0.5.dp, ForestMid.copy(0.5f), MaterialTheme.shapes.medium)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✦", color = ForestMid, fontSize = 14.sp)
                        Text(
                            text = translateConfigText("All models ready", language),
                            fontSize   = 14.sp,
                            color      = if (rememberThemeIsDark()) Color(0xFFC0DD97) else Forest,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = translateConfigText("Select a model to download:", language),
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                    missingModels.forEach { model ->
                        val isSelected = selectedModel == model.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    brush = if (isSelected)
                                        Brush.linearGradient(listOf(GoldSpark, Saffron))
                                    else
                                        Brush.linearGradient(listOf(GoldSpark.copy(0.15f), GoldSpark.copy(0.15f))),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable { onModelSelect(model.name) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick  = { onModelSelect(model.name) },
                                colors   = RadioButtonDefaults.colors(
                                    selectedColor   = GoldSpark,
                                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(0.3f)
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    model.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.sp,
                                    color      = if (isSelected) GoldSpark else MaterialTheme.colorScheme.onSurface
                                )
                                if (model.size.isNotEmpty()) {
                                    Text(
                                        "Size: ${model.size}",
                                        fontSize = 12.sp,
                                        color    = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Features unlocked panel
                Column(
                     modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.background)
                        .border(0.5.dp, GoldSpark.copy(0.2f), MaterialTheme.shapes.medium)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = translateConfigText("UNLOCKS", language),
                        fontSize      = 10.sp,
                        color         = GoldSpark.copy(0.8f),
                        letterSpacing = 2.sp,
                        fontWeight    = FontWeight.Bold
                    )
                    listOf(
                        "Smart context-aware questions",
                        "Telugu language support",
                        "Intelligent difficulty scaling",
                        "Offline & private"
                    ).forEach { feat ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(ForestMid)
                            )
                            Text(translateConfigText(feat, language), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                        }
                    }
                }

                Text(
                    text = translateConfigText("Download once, quiz anytime — fully offline.", language),
                    fontSize  = 12.sp,
                    color     = GoldPale.copy(0.75f),
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (selectedModel != null)
                            Brush.horizontalGradient(listOf(GoldSpark, Saffron))
                        else
                            Brush.horizontalGradient(listOf(GoldSpark.copy(0.25f), GoldSpark.copy(0.25f)))
                    )
                    .clickable(enabled = selectedModel != null) {
                        selectedModel?.let { onConfirmDownload(it) }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = translateConfigText("Download  →", language),
                    color      = if (selectedModel != null) Color.White else Color.White.copy(0.3f),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(translateConfigText("Not now", language), color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 13.sp)
            }
        }
    )
}

