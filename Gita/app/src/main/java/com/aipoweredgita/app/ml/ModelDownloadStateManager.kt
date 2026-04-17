package com.aipoweredgita.app.ml

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager

/**
 * Singleton that provides shared download progress state across all screens.
 * Tracks Gemma (GemmaDownloadWorker) which is now the primary WorkManager-managed model.
 */
object ModelDownloadStateManager {
    private var gemmaWorkInfoLiveData: LiveData<List<WorkInfo>>? = null

    /**
     * Get the LiveData for Gemma download work info.
     */
    fun getGemmaWorkInfoLiveData(context: Context): LiveData<List<WorkInfo>> {
        if (gemmaWorkInfoLiveData == null) {
            gemmaWorkInfoLiveData = WorkManager.getInstance(context.applicationContext)
                .getWorkInfosForUniqueWorkLiveData("gemma_download")
        }
        return gemmaWorkInfoLiveData!!
    }

    /**
     * Get the LiveData for immediate download work info.
     */
    fun getWorkInfoLiveData(context: Context): LiveData<List<WorkInfo>> {
        return getGemmaWorkInfoLiveData(context)
    }

    /**
     * Check if ANY download is currently running.
     */
    fun isDownloading(context: Context): Boolean {
        return isGemmaDownloading(context)
    }

    /**
     * Check if Gemma specifically is downloading.
     */
    fun isGemmaDownloading(context: Context): Boolean {
        return getGemmaWorkInfoLiveData(context).value?.any {
            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        } == true
    }

    /**
     * Get overall download progress percentage (0-100).
     */
    fun getOverallProgress(context: Context): Int {
        return getGemmaWorkInfoLiveData(context).value?.firstOrNull()?.progress?.getInt("overallProgress", 0) ?: 0
    }

    /**
     * Get the current model being downloaded.
     */
    fun getCurrentModel(context: Context): String {
        return getGemmaWorkInfoLiveData(context).value?.firstOrNull()?.progress?.getString("currentModel") ?: ""
    }

    /**
     * Check if Gemma 4 model file exists and is valid (required for voice features).
     */
    fun isGemmaDownloaded(context: Context): Boolean {
        return com.aipoweredgita.app.ml.ModelAvailability.getInstance(context).getGemma4Path() != null
    }

    /**
     * Schedule an immediate Gemma 4 2B download via WorkManager.
     * This survives screen changes and app closure.
     */
    fun startDownload(context: Context) {
        com.aipoweredgita.app.services.GemmaDownloadWorker.scheduleImmediateDownload(context)
    }

    /**
     * Cancel any ongoing download.
     */
    fun cancelDownload(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork("gemma_download")
    }
}
