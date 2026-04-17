package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.repository.DownloadProgress
import com.aipoweredgita.app.repository.DownloadStatus
import com.aipoweredgita.app.repository.OfflineCacheRepository
import com.aipoweredgita.app.utils.NetworkUtils
import com.aipoweredgita.app.utils.OfflinePreferences
import com.aipoweredgita.app.services.OfflineDownloadNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OfflineDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OfflineCacheRepository
    private val offlinePrefs = OfflinePreferences(application)
    private val notifManager = OfflineDownloadNotificationManager(application)

    private val _downloadProgress = MutableStateFlow(
        DownloadProgress(0, com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES, 0, DownloadStatus.IDLE)
    )
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val _cachedCount = MutableStateFlow(0)
    val cachedCount: StateFlow<Int> = _cachedCount.asStateFlow()

    private val _isFullyCached = MutableStateFlow(false)
    val isFullyCached: StateFlow<Boolean> = _isFullyCached.asStateFlow()

    private val _missingVerses = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val missingVerses: StateFlow<List<Pair<Int, Int>>> = _missingVerses.asStateFlow()

    init {
        val database = GitaDatabase.getDatabase(application)
        repository = OfflineCacheRepository(database.cachedVerseDao())

        // Load initial cached count
        viewModelScope.launch {
            repository.getCachedCount().collect { count ->
                _cachedCount.value = count
                _isFullyCached.value = repository.isFullyCached()

                // Auto-update missing verses when cache count changes
                // Only if we've already shown the missing verses list OR if fully cached
                if (_missingVerses.value.isNotEmpty() || _isFullyCached.value) {
                    checkMissingVerses()
                }

                // One-time completion notification when all verses cached
                if (_isFullyCached.value) {
                    try {
                        val alreadyNotified = offlinePrefs.isAllDownloadedNotified.first()
                        if (!alreadyNotified) {
                            notifManager.showCompletionNotification()
                            offlinePrefs.setAllDownloadedNotified(true)
                        }
                    } catch (_: Exception) { /* ignore */ }
                } else {
                    // Not fully cached: proactively check and notify incomplete verses
                    try {
                        checkMissingVerses()
                    } catch (_: Exception) { /* ignore */ }
                }
            }
        }
    }

    fun checkMissingVerses() {
        viewModelScope.launch {
            val missing = repository.getMissingVerses()
            _missingVerses.value = missing
            try {
                if (missing.isNotEmpty()) {
                    notifManager.showIncompleteNotification(missing.size)
                }
            } catch (_: Exception) { /* ignore */ }
        }
    }

    fun startDownload() {
        // Prevent starting download if already downloading
        if (_downloadProgress.value.status == DownloadStatus.DOWNLOADING) {
            return
        }

        // If offline, queue a background download via WorkManager and show a status message
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            try {
                com.aipoweredgita.app.services.OfflineVerseDownloadWorker.scheduleBackgroundDownload(getApplication())
            } catch (_: Exception) { /* ignore */ }

            _downloadProgress.value = DownloadProgress(
                current = _cachedCount.value,
                total = com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES,
                percentage = (_cachedCount.value * 100) / com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES,
                status = DownloadStatus.DOWNLOADING,
                message = "Queued for background; will resume when online."
            )
            return
        }

        viewModelScope.launch {
            try {
                // Notify background in-progress
                try { notifManager.showInProgressNotification() } catch (_: Exception) { }
                repository.downloadAllVerses().collect { progress ->
                    _downloadProgress.value = progress

                    if (progress.status == DownloadStatus.DOWNLOADING) {
                        // keep in-progress notification alive
                    }

                    // After download completes, check for missing verses
                    if (progress.status == DownloadStatus.COMPLETED ||
                        progress.status == DownloadStatus.COMPLETED_WITH_ERRORS) {
                        // cancel in-progress notification
                        try { notifManager.cancelInProgressNotification() } catch (_: Exception) { }

                        checkMissingVerses()

                        // Double-check completion and notify once
                        try {
                            val fullyCached = repository.isFullyCached()
                            if (fullyCached) {
                                val alreadyNotified = offlinePrefs.isAllDownloadedNotified.first()
                                if (!alreadyNotified) {
                                    notifManager.showCompletionNotification()
                                    offlinePrefs.setAllDownloadedNotified(true)
                                }
                            } else {
                                // Show incomplete notification with current missing count
                                val missing = repository.getMissingVerses()
                                if (missing.isNotEmpty()) {
                                    notifManager.showIncompleteNotification(missing.size)
                                }
                            }
                        } catch (_: Exception) { /* ignore */ }
                    }
                }
            } catch (e: Exception) {
                try { notifManager.cancelInProgressNotification() } catch (_: Exception) { }
                _downloadProgress.value = DownloadProgress(
                    current = _cachedCount.value,
                    total = com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES,
                    percentage = (_cachedCount.value * 100) / com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES,
                    status = DownloadStatus.ERROR,
                    message = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _downloadProgress.value = DownloadProgress(0, com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES, 0, DownloadStatus.IDLE)
            _missingVerses.value = emptyList() // Clear missing verses list
            try { offlinePrefs.resetAllDownloadedNotified() } catch (_: Exception) {}
        }
    }
}
