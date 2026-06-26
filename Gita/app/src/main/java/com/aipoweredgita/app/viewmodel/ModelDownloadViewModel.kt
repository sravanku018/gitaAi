package com.aipoweredgita.app.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.domain.model.ModelDownloadEvent
import com.aipoweredgita.app.domain.model.ModelDownloadUiState
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.services.ModelDownloadService
import com.aipoweredgita.app.services.ModelDownloadProgress
import com.aipoweredgita.app.utils.DeviceTier
import com.aipoweredgita.app.utils.DeviceTierDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "ModelDownloadViewModel"
    private var downloadService: ModelDownloadService? = null
    private var isBound = false
    private val manager by lazy { ModelDownloadManager(context) }
    private var downloadJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(ModelDownloadUiState())
    val uiState: StateFlow<ModelDownloadUiState> = _uiState.asStateFlow()

    private val totalModels: Int
        get() {
            val tier = DeviceTierDetector.detect(context)
            return if (tier == DeviceTier.FLAGSHIP) 3 else 2
        }

    // Legacy support — delegate to _uiState so consumers get real updates
    val downloadProgress: StateFlow<ModelDownloadProgress>
        get() = _uiState.map { it.downloadProgress }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.downloadProgress)
    val overallProgress: StateFlow<Int>
        get() = _uiState.map { it.overallProgress }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.overallProgress)
    val isDownloading: StateFlow<Boolean>
        get() = _uiState.map { it.isDownloading }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.isDownloading)
    val modelsStatus: StateFlow<List<ModelDownloadManager.ModelStatus>>
        get() = _uiState.map { it.modelsStatus }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.modelsStatus)
    val errorMessage: StateFlow<String?>
        get() = _uiState.map { it.error }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.error)
    val fileProgressList: StateFlow<List<ModelDownloadProgress>>
        get() = _uiState.map { it.fileProgressList }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.fileProgressList)
    val totalExpectedBytes: StateFlow<Long>
        get() = _uiState.map { it.totalExpectedBytes }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.totalExpectedBytes)
    val totalDownloadedBytes: StateFlow<Long>
        get() = _uiState.map { it.totalDownloadedBytes }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.totalDownloadedBytes)
    val remainingBytes: StateFlow<Long>
        get() = _uiState.map { it.remainingBytes }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.remainingBytes)
    val filesRemaining: StateFlow<Int>
        get() = _uiState.map { it.filesRemaining }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.filesRemaining)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ModelDownloadService.ModelDownloadBinder
            downloadService = binder.getService()
            isBound = true
            Log.d(TAG, "Service connected")

            viewModelScope.launch {
                downloadService?.let { svc -> svc.downloadProgress.collect { progress ->
                    _uiState.update { 
                        it.copy(
                            downloadProgress = ModelDownloadProgress(
                                modelName = progress.modelName,
                                percentage = progress.percentage,
                                message = progress.message,
                                error = progress.error,
                                currentBytes = progress.currentBytes,
                                totalBytes = progress.totalBytes
                            ),
                            isDownloading = progress.status.name == "DOWNLOADING"
                        )
                    }
                } }
            }

            viewModelScope.launch {
                downloadService?.let { svc -> svc.overallProgress.collect { progress ->
                    _uiState.update { it.copy(overallProgress = progress) }
                } }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            isBound = false
            Log.d(TAG, "Service disconnected unexpectedly")
        }

        override fun onBindingDied(name: ComponentName?) {
            downloadService = null
            isBound = false
            Log.w(TAG, "Service binding died — service process crashed")
        }
    }

    init {
        bindToService()
        refreshModelStatus()
    }

    fun onEvent(event: ModelDownloadEvent) {
        when (event) {
            is ModelDownloadEvent.ClearError -> clearError()
            is ModelDownloadEvent.RefreshModelStatus -> refreshModelStatus()
            is ModelDownloadEvent.StartDownload -> startDownload()
            is ModelDownloadEvent.StartManagerDownload -> startManagerDownload()
            is ModelDownloadEvent.StartSingleModelDownload -> startSingleModelDownload(event.modelName)
            is ModelDownloadEvent.CancelDownload -> cancelDownload()
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun refreshModelStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(modelsStatus = manager.getModelsStatus()) }
        }
    }

    private fun bindToService() {
        val intent = Intent(context, ModelDownloadService::class.java)
        context.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun startDownload() {
        if (downloadService != null) {
            downloadService!!.startBackgroundDownload()
            Log.d(TAG, "Download started")
        } else {
            Log.e(TAG, "Download service not available")
        }
    }

    private fun updateFileProgress(prog: com.aipoweredgita.app.ml.DownloadProgress) {
        _uiState.update { state ->
            val updatedMap = state.fileProgressMap.toMutableMap().apply {
                put(prog.fileName, ModelDownloadProgress(
                    modelName = prog.fileName,
                    percentage = prog.percent,
                    message = prog.status,
                    error = null,
                    currentBytes = prog.bytesDownloaded,
                    totalBytes = prog.totalBytes
                ))
            }
            val completed = updatedMap.values.count { it.percentage >= 100 || (it.totalBytes > 0 && it.currentBytes >= it.totalBytes) }
            val rem = totalModels - completed
            state.copy(
                fileProgressMap = updatedMap,
                downloadProgress = ModelDownloadProgress(
                    modelName = prog.fileName,
                    percentage = prog.percent,
                    message = prog.status,
                    error = null,
                    currentBytes = prog.bytesDownloaded,
                    totalBytes = prog.totalBytes
                ),
                overallProgress = prog.percent,
                filesRemaining = if (rem >= 0) rem else 0
            )
        }
    }

    private fun startManagerDownload() {
        if (_uiState.value.isDownloading) return
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            try {
                val targets = manager.models.filter { !it.name.contains("Gemma 4") }
                val expectedTotal = targets.sumOf { it.expectedBytes }
                if (!manager.hasEnoughSpaceForModel(expectedTotal)) {
                    _uiState.update { it.copy(error = "Insufficient disk space for automatic downloads. 1.5x of the model size is required.") }
                    return@launch
                }

                _uiState.update { it.copy(isDownloading = true, overallProgress = 0, error = null) }
                refreshModelStatus()
                val ok = manager.downloadAllModels { prog ->
                    if (prog.status == "failed_insufficient_space") {
                        _uiState.update { it.copy(error = "Insufficient disk space for ${prog.modelName}.") }
                    }
                    updateFileProgress(prog)
                }
                _uiState.update { it.copy(isDownloading = false) }
                refreshModelStatus()
                if (!ok) Log.w(TAG, "Model downloads incomplete")
            } catch (e: Exception) {
                Log.e(TAG, "Manager download failed: ${e.message}")
                _uiState.update { it.copy(isDownloading = false) }
                refreshModelStatus()
            }
        }
    }

    private fun startSingleModelDownload(modelName: String) {
        if (_uiState.value.isDownloading) return
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            try {
                val targetModelInfo = manager.getModelInfo(modelName)
                if (targetModelInfo != null && !manager.hasEnoughSpaceForModel(targetModelInfo.expectedBytes)) {
                    _uiState.update { it.copy(error = "Insufficient disk space to download ${targetModelInfo.name}. 1.5x of the model size (${targetModelInfo.size}) is required.") }
                    return@launch
                }

                _uiState.update { it.copy(isDownloading = true, overallProgress = 0, error = null) }
                refreshModelStatus()
                val ok = manager.downloadModel(modelName) { prog ->
                    if (prog.status == "failed_insufficient_space") {
                        _uiState.update { it.copy(error = "Insufficient disk space for ${prog.modelName}.") }
                    }
                    updateFileProgress(prog)
                }
                _uiState.update { it.copy(isDownloading = false) }
                refreshModelStatus()
                if (!ok) Log.w(TAG, "Model $modelName download failed or incomplete")
            } catch (e: Exception) {
                Log.e(TAG, "$modelName download failed: ${e.message}")
                _uiState.update { it.copy(isDownloading = false) }
                refreshModelStatus()
            }
        }
    }

    private fun cancelDownload() {
        if (downloadService != null) {
            downloadService!!.cancelDownload()
            Log.d(TAG, "Download cancelled")
        }
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(isDownloading = false) }
        refreshModelStatus()
    }

    suspend fun areModelsDownloaded(): Boolean {
        return try { manager.areAllModelsDownloaded() } catch (_: Exception) { false }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unbindService(serviceConnection)
        } catch (_: IllegalArgumentException) {
            Log.d(TAG, "Service already unbound in onCleared")
        }
        isBound = false
        downloadService = null
        Log.d(TAG, "ViewModel cleared")
    }
}
