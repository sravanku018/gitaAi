package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.services.ModelDownloadProgress

data class ModelDownloadUiState(
    val downloadProgress: ModelDownloadProgress = ModelDownloadProgress(),
    val overallProgress: Int = 0,
    val isDownloading: Boolean = false,
    val modelsStatus: List<ModelDownloadManager.ModelStatus> = emptyList(),
    val fileProgressMap: Map<String, ModelDownloadProgress> = emptyMap(),
    val filesRemaining: Int = 2,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseUiState {
    
    val fileProgressList: List<ModelDownloadProgress>
        get() = fileProgressMap.values.toList().sortedBy { it.modelName }
        
    val totalExpectedBytes: Long
        get() = fileProgressMap.values.sumOf { if (it.totalBytes > 0) it.totalBytes else 0L }
        
    val totalDownloadedBytes: Long
        get() = fileProgressMap.values.sumOf { 
            val tb = if (it.totalBytes > 0) it.totalBytes else 0L
            kotlin.math.min(it.currentBytes, tb) 
        }
        
    val remainingBytes: Long
        get() = (totalExpectedBytes - totalDownloadedBytes).coerceAtLeast(0L)
}
