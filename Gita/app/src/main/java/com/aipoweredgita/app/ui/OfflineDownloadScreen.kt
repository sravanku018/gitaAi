package com.aipoweredgita.app.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import kotlinx.coroutines.flow.map
import com.aipoweredgita.app.viewmodel.OfflineDownloadViewModel
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.repository.DownloadStatus
import com.aipoweredgita.app.repository.DownloadProgress

@Composable
fun OfflineDownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: OfflineDownloadViewModel = viewModel()
) {
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val cachedCount by viewModel.cachedCount.collectAsStateWithLifecycle()
    val isFullyCached by viewModel.isFullyCached.collectAsStateWithLifecycle()
    val missingVerses by viewModel.missingVerses.collectAsStateWithLifecycle()
    var showVerseReader by remember { mutableStateOf(false) }
    val context = LocalContext.current


    // Observe offline verse download worker for failures using modern flow-based API
    val verseWorkInfos by produceState(initialValue = emptyList<androidx.work.WorkInfo>()) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("offline_verse_download")
            .map { it.toList() }
            .collect { newValue -> value = newValue }
    }
    val verseBgFailed = remember(verseWorkInfos) {
        verseWorkInfos.any { info -> info.state == androidx.work.WorkInfo.State.FAILED }
    }
    var showVerseBgFailureDialog by remember { mutableStateOf(false) }
    LaunchedEffect(verseBgFailed) {
        if (verseBgFailed) showVerseBgFailureDialog = true
    }

    if (showVerseReader && cachedCount > 0) {
        // Use unified verse screen instead of duplicating reader UI
        VerseScreen()
    } else {
        // Show download manager
        OfflineDownloadManagerScreen(
            downloadProgress = downloadProgress,
            cachedCount = cachedCount,
            isFullyCached = isFullyCached,
            missingVerses = missingVerses,
            viewModel = viewModel,
            onReadOffline = { showVerseReader = true },
            modifier = modifier
        )
    }


    if (showVerseBgFailureDialog) {
        AlertDialog(
            onDismissRequest = { showVerseBgFailureDialog = false },
            title = { Text(stringResource(id = com.aipoweredgita.app.R.string.offline_download_failed_title)) },
            text = { Text(stringResource(id = com.aipoweredgita.app.R.string.offline_download_failed_body)) },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        com.aipoweredgita.app.services.OfflineVerseDownloadWorker.scheduleBackgroundDownload(context)
                    } catch (_: Exception) {}
                    showVerseBgFailureDialog = false
                }) { Text(stringResource(id = com.aipoweredgita.app.R.string.offline_retry_background)) }
            },
            dismissButton = {
                TextButton(onClick = { showVerseBgFailureDialog = false }) { Text(stringResource(id = com.aipoweredgita.app.R.string.generic_dismiss)) }
            }
        )
    }
}

@Composable
fun OfflineDownloadManagerScreen(
    downloadProgress: DownloadProgress,
    cachedCount: Int,
    isFullyCached: Boolean,
    missingVerses: List<Pair<Int, Int>>,
    viewModel: OfflineDownloadViewModel,
    onReadOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiCfg = LocalUiConfig.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val ctaText = when {
            isFullyCached -> stringResource(id = com.aipoweredgita.app.R.string.offline_all_downloaded)
            cachedCount > 0 -> stringResource(id = com.aipoweredgita.app.R.string.offline_resume_download)
            else -> stringResource(id = com.aipoweredgita.app.R.string.offline_download_all)
        }
        Text(
            text = ctaText,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Download all verses (always enabled)
        Button(
            onClick = {
                try {
                    viewModel.startDownload()
                } catch (_: Exception) { }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(id = com.aipoweredgita.app.R.string.offline_download_all),
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Read offline button (available when verses are cached)
        if (cachedCount > 0) {
            Button(
                onClick = onReadOffline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = stringResource(id = com.aipoweredgita.app.R.string.offline_read_offline, cachedCount),
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    try {
                        viewModel.checkMissingVerses()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Show Missing Verses${if (missingVerses.isNotEmpty()) " (${missingVerses.size})" else ""}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    try {
                        viewModel.clearCache()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("🗑️ Clear Offline Data")
            }
        }

        // Display missing verses if available
        if (missingVerses.isNotEmpty() && downloadProgress.status != DownloadStatus.DOWNLOADING) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Missing Verses (${missingVerses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = missingVerses.take(50).joinToString(", ") { "${it.first}:${it.second}" } +
                                if (missingVerses.size > 50) "\n... and ${missingVerses.size - 50} more" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Show a popup on verse download failure with a retry action
        if (downloadProgress.status == DownloadStatus.ERROR) {
            var showVerseDlError by remember { mutableStateOf(true) }
            if (showVerseDlError) {
                AlertDialog(
                    onDismissRequest = { showVerseDlError = false },
                    title = { Text(stringResource(id = com.aipoweredgita.app.R.string.offline_download_failed_title)) },
                    text = { Text(downloadProgress.message ?: stringResource(id = com.aipoweredgita.app.R.string.offline_download_failed_body)) },
                    confirmButton = {
                        TextButton(onClick = {
                            try { viewModel.startDownload() } catch (_: Exception) {}
                            showVerseDlError = false
                        }) { Text(stringResource(id = com.aipoweredgita.app.R.string.generic_retry)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showVerseDlError = false }) { Text(stringResource(id = com.aipoweredgita.app.R.string.generic_dismiss)) }
                    }
                )
            }
        }

        // Background status banner when verse download is active
        if (downloadProgress.status == DownloadStatus.DOWNLOADING) {
            Text(
                text = "Downloading verses in background...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Info Text
        Text(
            text = stringResource(id = com.aipoweredgita.app.R.string.widget_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}