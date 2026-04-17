package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * PRODUCTION model download manager with resume capability.
 * Uses OkHttp with Range headers for resumable downloads.
 * 
 * Features:
 * - Resume interrupted downloads
 * - .part temp file prevents corruption
 * - File size integrity check
 * - Storage space check before download
 */
class ModelDownloader(private val context: Context) {
    private val TAG = "ModelDownloader"
    
    private val modelDir = File(context.filesDir, "ml_models").apply { 
        if (!exists()) mkdirs() 
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class ModelInfo(
        val name: String,
        val fileName: String,
        val sizeBytes: Long,
        val url: String
    )

    /**
     * Check if a model is fully downloaded and valid.
     */
    fun isModelDownloaded(model: ModelInfo): Boolean {
        val file = getModelFile(model)
        return file.exists() && file.length() >= model.sizeBytes * 0.95 // Allow 5% tolerance
    }

    /**
     * Get the file path for a model.
     */
    fun getModelFile(model: ModelInfo): File {
        return File(modelDir, model.fileName)
    }

    /**
     * Check if there's enough storage space.
     */
    fun hasEnoughStorage(requiredBytes: Long): Boolean {
        val stat = android.os.StatFs(modelDir.path)
        val availableBytes = stat.availableBytes
        return availableBytes > requiredBytes
    }

    /**
     * Delete a downloaded model.
     */
    fun deleteModel(model: ModelInfo) {
        getModelFile(model).delete()
        File(modelDir, "${model.fileName}.part").delete()
    }

    /**
     * Download a model with resume capability.
     * 
     * @param model The model to download
     * @param onProgress Callback with (bytesDownloaded, totalBytes, percentage)
     * @return true if download completed successfully
     */
    suspend fun downloadModel(
        model: ModelInfo,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percentage: Int) -> Unit = { _, _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val file = getModelFile(model)
        val tempFile = File(file.absolutePath + ".part")

        // Check existing bytes for resume
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
        
        // If file already complete, skip
        if (file.exists() && file.length() >= model.sizeBytes * 0.95) {
            Log.d(TAG, "Model already downloaded: ${model.name}")
            return@withContext true
        }

        // Check storage space
        if (!hasEnoughStorage(model.sizeBytes - existingBytes)) {
            Log.e(TAG, "Not enough storage for ${model.name}")
            return@withContext false
        }

        Log.d(TAG, "Downloading ${model.name} from $existingBytes bytes (${existingBytes / (1024 * 1024)}MB)")

        try {
            val request = Request.Builder()
                .url(model.url)
                .addHeader("Range", "bytes=$existingBytes-")
                .addHeader("User-Agent", "GitaApp/1.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.code !in 200..299 && response.code != 206) {
                    Log.e(TAG, "HTTP ${response.code} for ${model.url}")
                    return@withContext false
                }

                val body = response.body ?: run {
                    Log.e(TAG, "Empty response body")
                    return@withContext false
                }

                val contentLength = body.contentLength()
                val total = if (contentLength > 0) contentLength + existingBytes else model.sizeBytes

                tempFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var downloaded = existingBytes
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read

                            val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                            onProgress(downloaded, total, progress)
                        }
                    }
                }
            }

            // Integrity check
            if (tempFile.exists() && tempFile.length() >= model.sizeBytes * 0.95) {
                // Rename .part to final file
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
                Log.d(TAG, "✓ Download complete: ${model.name} (${file.length() / (1024 * 1024)}MB)")
                return@withContext true
            } else {
                Log.e(TAG, "Integrity check failed: ${tempFile.length()} < ${model.sizeBytes}")
                tempFile.delete()
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${model.name}: ${e.message}", e)
            return@withContext false
        }
    }
}
