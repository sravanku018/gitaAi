package com.aipoweredgita.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.domain.model.OfflineDownloadEvent
import com.aipoweredgita.app.domain.model.OfflineDownloadUiState
import com.aipoweredgita.app.repository.DownloadProgress
import com.aipoweredgita.app.repository.DownloadStatus
import com.aipoweredgita.app.repository.OfflineCacheRepository
import com.aipoweredgita.app.services.OfflineDownloadNotificationManager
import com.aipoweredgita.app.util.GitaConstants
import com.aipoweredgita.app.utils.NetworkUtils
import com.aipoweredgita.app.utils.OfflinePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineDownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: OfflineCacheRepository
) : ViewModel() {

    private val offlinePrefs = OfflinePreferences(context)
    private val notifManager = OfflineDownloadNotificationManager(context)

    private val _uiState = MutableStateFlow(OfflineDownloadUiState())
    val uiState: StateFlow<OfflineDownloadUiState> = _uiState.asStateFlow()

    // Keep legacy state aliases — delegate to _uiState so consumers get real updates
    val downloadProgress: StateFlow<DownloadProgress>
        get() = _uiState.map { it.downloadProgress }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.downloadProgress)
    val cachedCount: StateFlow<Int>
        get() = _uiState.map { it.cachedCount }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.cachedCount)
    val isFullyCached: StateFlow<Boolean>
        get() = _uiState.map { it.isFullyCached }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.isFullyCached)

    init {
        viewModelScope.launch {
            repository.getCachedCount().collect { count ->
                val fullyCached = repository.isFullyCached()
                _uiState.update { 
                    it.copy(
                        cachedCount = count,
                        isFullyCached = fullyCached
                    )
                }

                if (_uiState.value.missingVerses.isNotEmpty() || fullyCached) {
                    checkMissingVerses()
                }

                if (fullyCached) {
                    try {
                        val alreadyNotified = offlinePrefs.isAllDownloadedNotified.first()
                        if (!alreadyNotified) {
                            notifManager.showCompletionNotification()
                            offlinePrefs.setAllDownloadedNotified(true)
                        }
                    } catch (_: Exception) { }
                } else {
                    try {
                        checkMissingVerses()
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun onEvent(event: OfflineDownloadEvent) {
        when (event) {
            is OfflineDownloadEvent.StartDownload -> startDownload()
            is OfflineDownloadEvent.ClearCache -> clearCache()
            is OfflineDownloadEvent.CheckMissingVerses -> checkMissingVerses()
        }
    }

    private fun checkMissingVerses() {
        viewModelScope.launch {
            val missing = repository.getMissingVerses()
            _uiState.update { it.copy(missingVerses = missing) }
            try {
                if (missing.isNotEmpty()) {
                    notifManager.showIncompleteNotification(missing.size)
                }
            } catch (_: Exception) { }
        }
    }

    private fun startDownload() {
        if (_uiState.value.downloadProgress.status == DownloadStatus.DOWNLOADING) {
            return
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            try {
                com.aipoweredgita.app.services.OfflineVerseDownloadWorker.scheduleBackgroundDownload(context)
            } catch (_: Exception) { }

            val currentCount = _uiState.value.cachedCount
            _uiState.update { 
                it.copy(
                    downloadProgress = DownloadProgress(
                        current = currentCount,
                        total = GitaConstants.TOTAL_VERSES,
                        percentage = (currentCount * 100) / GitaConstants.TOTAL_VERSES,
                        status = DownloadStatus.DOWNLOADING,
                        message = "Queued for background; will resume when online."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                try { notifManager.showInProgressNotification() } catch (_: Exception) { }
                repository.downloadAllVerses().collect { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }

                    if (progress.status == DownloadStatus.COMPLETED ||
                        progress.status == DownloadStatus.COMPLETED_WITH_ERRORS) {
                        try { notifManager.cancelInProgressNotification() } catch (_: Exception) { }

                        checkMissingVerses()

                        try {
                            val fullyCached = repository.isFullyCached()
                            if (fullyCached) {
                                val alreadyNotified = offlinePrefs.isAllDownloadedNotified.first()
                                if (!alreadyNotified) {
                                    notifManager.showCompletionNotification()
                                    offlinePrefs.setAllDownloadedNotified(true)
                                }
                            } else {
                                val missing = repository.getMissingVerses()
                                if (missing.isNotEmpty()) {
                                    notifManager.showIncompleteNotification(missing.size)
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
            } catch (e: Exception) {
                try { notifManager.cancelInProgressNotification() } catch (_: Exception) { }
                val currentCount = _uiState.value.cachedCount
                _uiState.update { 
                    it.copy(
                        downloadProgress = DownloadProgress(
                            current = currentCount,
                            total = GitaConstants.TOTAL_VERSES,
                            percentage = (currentCount * 100) / GitaConstants.TOTAL_VERSES,
                            status = DownloadStatus.ERROR,
                            message = "Download failed: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _uiState.update { 
                it.copy(
                    downloadProgress = DownloadProgress(0, GitaConstants.TOTAL_VERSES, 0, DownloadStatus.IDLE),
                    missingVerses = emptyList()
                )
            }
            try { offlinePrefs.resetAllDownloadedNotified() } catch (_: Exception) {}
        }
    }
}
