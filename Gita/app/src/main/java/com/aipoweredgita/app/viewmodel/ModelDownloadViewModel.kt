package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.services.ModelDownloadService
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.ui.ModelDownloadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * ViewModel for managing ML model downloads
 */
class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ModelDownloadViewModel"
    private var downloadService: ModelDownloadService? = null
    private var isBound = false
    private val manager by lazy { ModelDownloadManager(getApplication()) }

    private val _downloadProgress = MutableStateFlow(ModelDownloadProgress())
    val downloadProgress: StateFlow<ModelDownloadProgress> = _downloadProgress.asStateFlow()

    private val _overallProgress = MutableStateFlow(0)
    val overallProgress: StateFlow<Int> = _overallProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    // Track per-file progress for all models
    private val _fileProgressMap = MutableStateFlow<Map<String, com.aipoweredgita.app.ui.ModelDownloadProgress>>(emptyMap())
    val fileProgressList: StateFlow<List<com.aipoweredgita.app.ui.ModelDownloadProgress>> =
        _fileProgressMap.asStateFlow()
            .map { it.values.toList().sortedBy { p -> p.modelName } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Aggregate totals derived from per-file progress
    val totalExpectedBytes: StateFlow<Long> =
        _fileProgressMap.asStateFlow()
            .map { mp -> mp.values.sumOf { if (it.totalBytes > 0) it.totalBytes else 0L } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val totalDownloadedBytes: StateFlow<Long> =
        _fileProgressMap.asStateFlow()
            .map { mp -> mp.values.sumOf { val tb = if (it.totalBytes > 0) it.totalBytes else 0L; min(it.currentBytes, tb) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val remainingBytes: StateFlow<Long> =
        combine(totalExpectedBytes, totalDownloadedBytes) { total, downloaded ->
            val remain = total - downloaded
            if (remain > 0) remain else 0L
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val totalModels = 2  // Qwen3 0.6B + Gemma 4 2B
    val filesRemaining: StateFlow<Int> =
        _fileProgressMap.asStateFlow()
            .map { mp ->
                val completed = mp.values.count { it.percentage >= 100 || (it.totalBytes > 0 && it.currentBytes >= it.totalBytes) }
                val rem = totalModels - completed
                if (rem >= 0) rem else 0
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, totalModels)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ModelDownloadService.ModelDownloadBinder
            downloadService = binder.getService()
            isBound = true
            Log.d(TAG, "Service connected")

            // Subscribe to progress updates
            viewModelScope.launch {
                downloadService?.let { svc -> svc.downloadProgress.collect { progress ->
                    _downloadProgress.value = ModelDownloadProgress(
                        modelName = progress.modelName,
                        percentage = progress.percentage,
                        message = progress.message,
                        error = progress.error,
                        currentBytes = progress.currentBytes,
                        totalBytes = progress.totalBytes
                    )
                    _isDownloading.value = progress.status.name == "DOWNLOADING"
                } }
            }

            viewModelScope.launch {
                downloadService?.let { svc -> svc.overallProgress.collect { progress ->
                    _overallProgress.value = progress
                } }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            Log.d(TAG, "Service disconnected")
        }
    }

    init {
        bindToService()
    }

    private fun bindToService() {
        val intent = Intent(getApplication(), ModelDownloadService::class.java)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Start model downloads
     */
    fun startDownload() {
        if (downloadService != null) {
            downloadService!!.startBackgroundDownload()
            Log.d(TAG, "Download started")
        } else {
            Log.e(TAG, "Download service not available")
        }
    }

    /** Start manifest-based downloads using ModelDownloadManager (shows real progress) */
    fun startManagerDownload() {
        viewModelScope.launch {
            try {
                _isDownloading.value = true
                _overallProgress.value = 0
                val ok = manager.downloadAllModels { prog ->
                    _downloadProgress.value = com.aipoweredgita.app.ui.ModelDownloadProgress(
                        modelName = prog.fileName,
                        percentage = prog.percent,
                        message = prog.status,
                        error = null,
                        currentBytes = prog.bytesDownloaded,
                        totalBytes = prog.totalBytes
                    )
                    _fileProgressMap.value = _fileProgressMap.value.toMutableMap().apply {
                        put(prog.fileName, com.aipoweredgita.app.ui.ModelDownloadProgress(
                            modelName = prog.fileName,
                            percentage = prog.percent,
                            message = prog.status,
                            error = null,
                            currentBytes = prog.bytesDownloaded,
                            totalBytes = prog.totalBytes
                        ))
                    }
                    _overallProgress.value = prog.percent
                }
                _isDownloading.value = false
                if (!ok) Log.w(TAG, "Model downloads incomplete")
            } catch (e: Exception) {
                Log.e(TAG, "Manager download failed: ${e.message}")
                _isDownloading.value = false
            }
        }
    }

    /**
     * Start downloading a single specific model via manager
     */
    fun startSingleModelDownload(modelName: String) {
        if (_isDownloading.value) return
        viewModelScope.launch {
            try {
                _isDownloading.value = true
                _overallProgress.value = 0
                val ok = manager.downloadModel(modelName) { prog ->
                    _downloadProgress.value = com.aipoweredgita.app.ui.ModelDownloadProgress(
                        modelName = prog.fileName,
                        percentage = prog.percent,
                        message = prog.status,
                        error = null,
                        currentBytes = prog.bytesDownloaded,
                        totalBytes = prog.totalBytes
                    )
                    _fileProgressMap.value = _fileProgressMap.value.toMutableMap().apply {
                        put(prog.fileName, com.aipoweredgita.app.ui.ModelDownloadProgress(
                            modelName = prog.fileName,
                            percentage = prog.percent,
                            message = prog.status,
                            error = null,
                            currentBytes = prog.bytesDownloaded,
                            totalBytes = prog.totalBytes
                        ))
                    }
                    _overallProgress.value = prog.percent
                }
                _isDownloading.value = false
                if (!ok) Log.w(TAG, "Model $modelName download failed or incomplete")
            } catch (e: Exception) {
                Log.e(TAG, "$modelName download failed: ${e.message}")
                _isDownloading.value = false
            }
        }
    }

    /**
     * Cancel downloads
     */
    fun cancelDownload() {
        if (downloadService != null) {
            downloadService!!.cancelDownload()
            Log.d(TAG, "Download cancelled")
        }
    }

    /**
     * Check if models are downloaded - must be called from coroutine
     */
    suspend fun areModelsDownloaded(): Boolean {
        return try { manager.areAllModelsDownloaded() } catch (_: Exception) { false }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
        Log.d(TAG, "ViewModel cleared")
    }
}
