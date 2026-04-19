package com.aipoweredgita.app.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.core.app.NotificationCompat
import com.aipoweredgita.app.MainActivity
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Premium color palette
private val GradientSaffron = Color(0xFFFF8F00)
private val GradientAmber = Color(0xFFFFC107)

@Composable
fun TryNowDialog(
    onDismiss: () -> Unit,
    onDownloadStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    val modelManager = remember { com.aipoweredgita.app.ml.ModelDownloadManager(context) }

    // Observe WorkManager status (shared across all screens)
    val workInfo by com.aipoweredgita.app.ml.ModelDownloadStateManager.getGemmaWorkInfoLiveData(context)
        .observeAsState(initial = emptyList())
    val isDownloading = com.aipoweredgita.app.ml.ModelDownloadStateManager.isGemmaDownloading(context)
    val overallProgress = workInfo.firstOrNull()?.progress?.getInt("overallProgress", 0) ?: 0
    val currentModel = workInfo.firstOrNull()?.progress?.getString("currentModel") ?: ""

    // Get remaining bytes from ModelDownloadManager
    var remainingBytes by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        try {
            remainingBytes = modelManager.getRemainingDownloadSizeBytes()
        } catch (_: Exception) {}
    }

    var showMobileDataWarning by remember { mutableStateOf(false) }
    var hasConfirmedMobileData by remember { mutableStateOf(false) }
    var isDownloadingInBackground by remember { mutableStateOf(false) }

    val networkType = remember(context) { NetworkUtils.getNetworkType(context) }
    val isOnMobileData = networkType == NetworkUtils.NetworkType.CELLULAR
    val isOnWiFi = networkType == NetworkUtils.NetworkType.WIFI

    LaunchedEffect(isOnMobileData) {
        if (isOnMobileData && !hasConfirmedMobileData) {
            showMobileDataWarning = true
        }
    }

    if (showMobileDataWarning && !hasConfirmedMobileData) {
        MobileDataConfirmationDialog(
            onConfirm = {
                hasConfirmedMobileData = true
                showMobileDataWarning = false
            },
            onCancel = onDismiss
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = GradientSaffron,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "Download AI Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOnWiFi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnWiFi) Icons.Default.NetworkWifi else Icons.Default.NetworkCell,
                            contentDescription = null,
                            tint = if (isOnWiFi) MaterialTheme.colorScheme.primary else GradientSaffron,
                            modifier = Modifier.size(20.dp)
                        )
                        val sizeText = if (remainingBytes > 0) " (${remainingBytes / (1024 * 1024)} MB)" else ""
                        Text(
                            text = if (isOnWiFi) "Connected to WiFi" else "Using Mobile Data$sizeText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOnWiFi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Download Progress: $overallProgress%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { overallProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GradientSaffron,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "Remaining: ${remainingBytes / (1024 * 1024)} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (currentModel.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentModel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text("$overallProgress%", style = MaterialTheme.typography.bodySmall, color = GradientSaffron)
                            }
                            LinearProgressIndicator(
                                progress = { overallProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = GradientSaffron,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        if (isDownloadingInBackground) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "✓ Downloading in background. You can minimize the app and continue using other features.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Gemma 4 2B - Voice AI Engine (2.58 GB)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("✓ Voice Chat — ask questions in Telugu/English", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("✓ Studio Quiz — hands-free quiz in your language", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("✓ Smart context-aware questions", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("✓ 100% offline & private", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        if (isOnMobileData) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "You're on mobile data. Download will continue in background even if you minimize the app.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                Button(
                    onClick = {
                        // ONLY use Gemma WorkManager - survives screen navigation/app closure
                        com.aipoweredgita.app.services.GemmaDownloadWorker.scheduleImmediateDownload(context)
                        isDownloadingInBackground = true
                        onDownloadStarted()
                        if (isOnMobileData) {
                            showDownloadNotification(context, remainingBytes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientSaffron),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Now", fontWeight = FontWeight.Bold)
                }
            } else {
                // FIX: OutlinedButtonDefaults.buttonColors doesn't exist
                // Use ButtonDefaults.outlinedButtonColors instead
                OutlinedButton(
                    onClick = {
                        isDownloadingInBackground = true
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Continue in Background", fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Not Now", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun MobileDataConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val cellularGen = NetworkUtils.getCellularGeneration(context)

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        },
        title = {
            Text("Mobile Data Download", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "You're about to download the AI engine (2.58 GB) on mobile data.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (cellularGen != null) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Network: ${if (cellularGen == NetworkUtils.CellularGeneration.FIVE_G) "5G" else "4G/LTE"}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("✓ Download continues in background", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("✓ Works even if you minimize the app", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("✓ Persistent notification shows progress", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("✓ Resumes automatically if interrupted", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = GradientSaffron)) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

private fun showDownloadNotification(context: Context, totalBytes: Long) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "gita_model_download"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Model Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows progress of AI model downloads"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Downloading AI Engine")
        .setContentText("Gemma 2B model - ${totalBytes / (1024 * 1024)} MB")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

    notificationManager.notify(7005, notificationBuilder.build())
}

fun updateDownloadNotification(context: Context, progress: Int, downloadedBytes: Long, totalBytes: Long) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "gita_model_download"

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Downloading AI Engine")
        .setContentText("$progress% complete - ${downloadedBytes / (1024 * 1024)} MB / ${totalBytes / (1024 * 1024)} MB")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setProgress(100, progress, false)
        .setContentIntent(
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

    notificationManager.notify(7005, notificationBuilder.build())
}

fun dismissDownloadNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(7005)
}

/**
 * Show notification for dataset import progress.
 */
fun showDatasetImportNotification(context: Context, progress: Int, message: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "dataset_import_channel"

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId,
            "Dataset Imports",
            android.app.NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress of quiz question dataset imports"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Importing Quiz Questions")
        .setContentText(message)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setProgress(100, progress, false)
        .build()

    notificationManager.notify(7006, notification)
}

/**
 * Show completion notification for dataset import.
 */
fun showDatasetImportCompleteNotification(context: Context, questionCount: Int) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "dataset_import_channel"

    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Quiz Questions Ready")
        .setContentText("$questionCount questions imported and ready for use.")
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(7007, notification)
}

/**
 * Dismiss dataset import notification.
 */
fun dismissDatasetImportNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(7006)
    notificationManager.cancel(7007)
}