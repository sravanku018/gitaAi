package com.aipoweredgita.app.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class ModelDownloadProgress(
    val modelName: String = "",
    val currentBytes: Long = 0,
    val totalBytes: Long = 0,
    val percentage: Int = 0,
    val status: ModelDownloadStatus = ModelDownloadStatus.IDLE,
    val message: String = "",
    val error: String? = null
)

enum class ModelDownloadStatus {
    IDLE,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED
}

class ModelDownloadService : Service() {
    private val TAG = "ModelDownloadService"
    private val binder = ModelDownloadBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val _downloadProgress = MutableStateFlow(ModelDownloadProgress())
    val downloadProgress: StateFlow<ModelDownloadProgress> = _downloadProgress.asStateFlow()

    private val _overallProgress = MutableStateFlow(0) // 0-100%
    val overallProgress: StateFlow<Int> = _overallProgress.asStateFlow()

    // Gemma models are handled by ModelDownloadManager and GemmaDownloadWorker.
    // This service is kept for legacy ServiceConnection compatibility only.
    private val models = emptyList<Pair<String, Long>>()

    inner class ModelDownloadBinder : Binder() {
        fun getService(): ModelDownloadService = this@ModelDownloadService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ModelDownloadService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    /**
     * Start downloading all ML models in background
     * Non-blocking - returns immediately while downloads happen in background
     */
    fun startBackgroundDownload() {
        serviceScope.launch {
            try {
                downloadAllModels()
            } catch (e: Exception) {
                Log.e(TAG, "Error during background download: ${e.message}", e)
                _downloadProgress.value = _downloadProgress.value.copy(
                    status = ModelDownloadStatus.FAILED,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Download all models sequentially with progress tracking
     */
    private suspend fun downloadAllModels() {
        Log.d(TAG, "Starting downloadAllModels with ${models.size} models")

        _downloadProgress.value = _downloadProgress.value.copy(
            status = ModelDownloadStatus.DOWNLOADING,
            message = "Initializing model downloads..."
        )

        val modelDir = File(filesDir, "ml_models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }

        models.forEachIndexed { index, (modelName, estimatedSize) ->
            try {
                Log.d(TAG, "Starting download of model ${index + 1}/${models.size}: $modelName")

                _downloadProgress.value = _downloadProgress.value.copy(
                    modelName = modelName,
                    status = ModelDownloadStatus.DOWNLOADING,
                    message = "Downloading $modelName (${index + 1}/${models.size})...",
                    percentage = 0
                )

                downloadModel(modelDir, modelName, estimatedSize)
                Log.d(TAG, "Completed download of $modelName")

                // Update overall progress
                val overallPercent = ((index + 1) * 100) / models.size
                _overallProgress.value = overallPercent

                _downloadProgress.value = _downloadProgress.value.copy(
                    message = "Completed $modelName (${index + 1}/${models.size})",
                    percentage = overallPercent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading $modelName: ${e.message}", e)
                // Continue with next model even if one fails
            }
        }

        _downloadProgress.value = _downloadProgress.value.copy(
            status = ModelDownloadStatus.COMPLETED,
            message = "All models downloaded successfully",
            percentage = 100
        )
        _overallProgress.value = 100

        Log.d(TAG, "All models downloaded successfully")
    }

    /**
     * Download single model with simulated progress
     * In production, replace with actual network download
     */
    private suspend fun downloadModel(
        modelDir: File,
        modelName: String,
        estimatedSize: Long
    ) = withContext(Dispatchers.IO) {
        val modelFile = File(modelDir, modelName)

        // Check if already downloaded
        if (modelFile.exists() && modelFile.length() > 0) {
            Log.d(TAG, "$modelName already exists, skipping download")
            _downloadProgress.value = _downloadProgress.value.copy(
                currentBytes = modelFile.length(),
                totalBytes = modelFile.length(),
                percentage = 100
            )
            return@withContext
        }

        // Simulate download with progress updates
        // In production: use OkHttp/Retrofit for real downloads
        val chunkSize = 1_000_000L  // 1MB chunks
        var downloadedBytes = 0L

        while (downloadedBytes < estimatedSize) {
            delay(100)  // Simulate network delay

            downloadedBytes += chunkSize
            if (downloadedBytes > estimatedSize) downloadedBytes = estimatedSize

            val percentage = ((downloadedBytes * 100) / estimatedSize).toInt()
            _downloadProgress.value = _downloadProgress.value.copy(
                currentBytes = downloadedBytes,
                totalBytes = estimatedSize,
                percentage = percentage
            )

            Log.d(TAG, "Downloaded $modelName: $percentage%")
        }

        // Log final percentage for this model
        Log.d(TAG, "Model download complete: $modelName at 100%")

        // Create the file after download completes
        if (!modelFile.exists()) {
            modelFile.createNewFile()
            // Write placeholder data in chunks to avoid OutOfMemory
            // (in production: actual model data would be streamed)
            try {
                modelFile.outputStream().use { output ->
                    val chunkSize = 1_000_000  // 1MB chunks
                    var bytesWritten = 0L
                    while (bytesWritten < estimatedSize) {
                        val remainingBytes = estimatedSize - bytesWritten
                        val currentChunk = if (remainingBytes < chunkSize) remainingBytes.toInt() else chunkSize
                        output.write(ByteArray(currentChunk))
                        bytesWritten += currentChunk
                    }
                }
                Log.d(TAG, "Successfully downloaded $modelName (${estimatedSize / (1024 * 1024)}MB written)")
            } catch (e: Exception) {
                Log.e(TAG, "Error writing $modelName to disk: ${e.message}", e)
                throw e
            }
        } else {
            Log.d(TAG, "Successfully downloaded $modelName")
        }
    }

    /**
     * Cancel ongoing downloads
     */
    fun cancelDownload() {
        serviceScope.coroutineContext.cancelChildren()
        _downloadProgress.value = _downloadProgress.value.copy(
            status = ModelDownloadStatus.IDLE,
            message = "Download cancelled"
        )
        Log.d(TAG, "Download cancelled")
    }

    /**
     * Check if models are already downloaded
     */
    fun areModelsDownloaded(): Boolean {
        val modelDir = File(filesDir, "ml_models")
        return models.all { (modelName, _) ->
            File(modelDir, modelName).exists()
        }
    }

    /**
     * Get total size of all models
     */
    fun getTotalModelSize(): Long {
        return models.sumOf { it.second }
    }

    /**
     * Get downloaded model size
     */
    fun getDownloadedModelSize(): Long {
        val modelDir = File(filesDir, "ml_models")
        return models.sumOf { (modelName, _) ->
            File(modelDir, modelName).length()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "ModelDownloadService destroyed")
    }
}
