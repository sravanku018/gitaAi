package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import com.google.gson.Gson
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class ModelInfo(
    val name: String,
    val size: String,
    val fileName: String,
    val expectedBytes: Long
)

data class DownloadProgress(
    val fileName: String,
    val modelName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Int,
    val status: String
)

private data class Manifest(val urls: Map<String, String>)

class ModelDownloadManager(private val context: Context) {

    private val TAG = "ModelDownloadManager"
    private val legacyDir = File(context.cacheDir, "ml_models")
    private val modelsDir = File(context.filesDir, "ml_models")
    private val measuredSizes = mutableMapOf<String, Long>()
    private var initCompleted = false

    // FIX 5: Singleton OkHttpClient — not recreated per download
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val models = listOf(
        ModelInfo(
            name = "Qwen3 0.6B",
            size = "580 MB",
            fileName = "qwen3-0.6b-int4.litertlm",
            expectedBytes = 580_000_000L
        ),
        ModelInfo(
            name = "Gemma 4 2B",
            size = "2.58 GB",
            fileName = "gemma-4-E2B-it.litertlm",
            expectedBytes = 2_580_000_000L
        )
    )

    // Gemma 4 is the mandatory model for voice features
    private val mandatoryModelFileName = "gemma-4-E2B-it.litertlm"

    /**
     * Initialize model directories and migrate legacy models.
     * MUST be called from Dispatchers.IO or background thread.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initCompleted) return@withContext
        try {
            if (!modelsDir.exists()) modelsDir.mkdirs()
            if (legacyDir.exists()) {
                legacyDir.listFiles()?.forEach { src ->
                    val dest = File(modelsDir, src.name)
                    if (!dest.exists() && src.exists() && src.length() > 0) {
                        src.copyTo(dest, overwrite = false)
                        Log.d(TAG, "Migrated model: ${src.name}")
                    }
                }
            }
            initCompleted = true
            Log.d(TAG, "Model directory initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Model migration skipped: ${e.message}")
        }
    }

    /**
     * Check if all mandatory models are downloaded.
     * Mandatory = Gemma 4 2B (required for voice features).
     * MUST be called from Dispatchers.IO.
     */
    suspend fun areAllModelsDownloaded(): Boolean = withContext(Dispatchers.IO) {
        initialize()
        // FIX 2: Clearly documented — only Gemma 4 is mandatory
        val gemma4Exists = modelExists(mandatoryModelFileName)
        Log.d(TAG, "Mandatory model (Gemma 4 2B) present: $gemma4Exists")
        return@withContext gemma4Exists
    }

    /** Whether to show download screen. MUST be called from Dispatchers.IO */
    suspend fun shouldShowDownloadScreen(): Boolean = withContext(Dispatchers.IO) {
        val shouldShow = !areAllModelsDownloaded()
        Log.d(TAG, "Should show download screen: $shouldShow")
        shouldShow
    }

    /**
     * Find the best available Gemma model file path.
     * Priority: Gemma 4 2B (voice+text) → null
     */
    // FIX 1: Only companion object version kept; instance method removed
    fun getBestGemmaModelPath(): String? = Companion.getBestGemmaModelPath(context)

    /**
     * Download a specific model by name.
     */
    suspend fun downloadModel(targetModelName: String, onProgress: (DownloadProgress) -> Unit = {}): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val targetModelInfo = models.find { it.name.equals(targetModelName, ignoreCase = true) }
                if (targetModelInfo == null) {
                    Log.w(TAG, "Model $targetModelName not found in registry.")
                    return@withContext false
                }

                val manifest = loadManifest()
                val url = manifest.urls[targetModelInfo.fileName]
                if (url.isNullOrBlank()) {
                    Log.w(TAG, "No URL for ${targetModelInfo.fileName}; skipping")
                    return@withContext false
                }

                if (!measuredSizes.containsKey(targetModelInfo.fileName)) {
                    val length = contentLengthOf(normalizeHuggingFaceUrl(url, targetModelInfo.fileName))
                    if (length > 0) measuredSizes[targetModelInfo.fileName] = length
                }

                val outFile = File(modelsDir, targetModelInfo.fileName)
                if (!modelExists(targetModelInfo.fileName)) {
                    val ok = tryDownload(url, outFile) { bytes, total ->
                        if (total > 0) measuredSizes[targetModelInfo.fileName] = total
                        val percent = if (total > 0) ((bytes * 100) / total).toInt() else 0
                        onProgress(DownloadProgress(targetModelInfo.fileName, targetModelInfo.name, bytes, total, percent, "downloading"))
                    }
                    if (ok) {
                        onProgress(DownloadProgress(targetModelInfo.fileName, targetModelInfo.name, outFile.length(), outFile.length(), 100, "completed"))
                    } else {
                        onProgress(DownloadProgress(targetModelInfo.fileName, targetModelInfo.name, outFile.length(), measuredSizes[targetModelInfo.fileName] ?: targetModelInfo.expectedBytes, 0, "failed"))
                    }
                    return@withContext ok
                } else {
                    val target = measuredSizes[targetModelInfo.fileName] ?: targetModelInfo.expectedBytes
                    onProgress(DownloadProgress(targetModelInfo.fileName, targetModelInfo.name, target, target, 100, "exists"))
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error individually downloading $targetModelName", e)
                false
            }
        }
    }

    /**
     * Download all models. Returns true if all required models are present.
     */
    suspend fun downloadAllModels(onProgress: (DownloadProgress) -> Unit = {}): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting model download/verification...")
                val manifest = loadManifest()
                probeModelSizes(manifest)
                val expectedTotal = getExpectedTotalSizeBytes()
                var baseDownloaded = computeAlreadyDownloaded(models)

                models.forEach { model ->
                    val outFile = File(modelsDir, model.fileName)
                    if (!modelExists(model.fileName)) {
                        val url = manifest.urls[model.fileName]
                        if (url.isNullOrBlank()) {
                            Log.w(TAG, "No URL for ${model.fileName} in manifest; skipping download")
                            onProgress(DownloadProgress(model.fileName, model.name, 0, 0,
                                overallPercent(baseDownloaded, expectedTotal), "missing_url"))
                        } else {
                            val ok = tryDownload(url, outFile) { bytes, total ->
                                if (total > 0) measuredSizes[model.fileName] = total
                                onProgress(DownloadProgress(model.fileName, model.name, bytes, total,
                                    overallPercent(baseDownloaded + bytes, expectedTotal), "downloading"))
                            }
                            val written = if (outFile.exists()) outFile.length() else 0L
                            val target = measuredSizes[model.fileName] ?: model.expectedBytes
                            baseDownloaded = (baseDownloaded + minOf(written, target)).coerceAtMost(expectedTotal)
                            onProgress(DownloadProgress(model.fileName, model.name, written, written,
                                overallPercent(baseDownloaded, expectedTotal), if (ok) "completed" else "failed"))
                        }
                    } else {
                        // FIX 4: Update baseDownloaded for already-existing files too
                        val target = measuredSizes[model.fileName] ?: model.expectedBytes
                        val completed = minOf(outFile.length(), target)
                        baseDownloaded = (baseDownloaded + completed).coerceAtMost(expectedTotal)
                        onProgress(DownloadProgress(model.fileName, model.name, completed, completed,
                            overallPercent(baseDownloaded, expectedTotal), "exists"))
                    }
                }

                val allReady = areAllModelsDownloaded()
                Log.d(TAG, "Model verification complete. ready=$allReady")
                allReady
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error during model download", e)
                false
            }
        }
    }


    // ─── Private Helpers ──────────────────────────────────────────────────────

    private fun overallPercent(downloaded: Long, total: Long): Int =
        if (total > 0) (downloaded.coerceAtLeast(0) * 100 / total).toInt().coerceIn(0, 100) else 0

    private fun computeAlreadyDownloaded(targetModels: List<ModelInfo>): Long {
        return targetModels.sumOf { model ->
            val file = File(modelsDir, model.fileName)
            if (file.exists() && file.length() > 0) {
                val target = measuredSizes[model.fileName] ?: model.expectedBytes
                minOf(file.length(), target)
            } else 0L
        }
    }

    private fun probeModelSizes(manifest: Manifest) {
        models.forEach { model ->
            if (!measuredSizes.containsKey(model.fileName)) {
                val url = manifest.urls[model.fileName] ?: return@forEach
                val length = contentLengthOf(normalizeHuggingFaceUrl(url, model.fileName))
                if (length > 0) measuredSizes[model.fileName] = length
            }
        }
    }

    private fun contentLengthOf(urlStr: String): Long {
        return try {
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) GitaLearningApp")
            conn.setRequestProperty("Accept", "application/octet-stream, */*")
            conn.connect()
            val len = conn.contentLengthLong
            conn.disconnect()
            if (len > 0) len else {
                val c = url.openConnection() as java.net.HttpURLConnection
                c.requestMethod = "GET"
                c.connectTimeout = 15000
                c.readTimeout = 15000
                c.setRequestProperty("Range", "bytes=0-0")
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) GitaLearningApp")
                c.setRequestProperty("Accept", "application/octet-stream, */*")
                c.connect()
                val l = c.contentLengthLong
                c.disconnect()
                if (l > 0) l else -1
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to probe content length: ${e.message}")
            -1
        }
    }

    private fun loadManifest(): Manifest {
        return try {
            val input = context.assets.open("ml_models/download_manifest.json")
            InputStreamReader(input).use { reader ->
                Gson().fromJson(reader, Manifest::class.java)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No manifest found in assets; using empty map")
            Manifest(urls = emptyMap())
        }
    }

    /** FIX 5: Uses singleton httpClient instead of creating one per call */
    private fun tryDownload(urlStr: String, outFile: File, onProgress: (Long, Long) -> Unit): Boolean {
        val normalizedUrlStr = normalizeHuggingFaceUrl(urlStr, outFile.name)
        val tempFile = File(outFile.absolutePath + ".part")

        var attempt = 0
        val maxAttempts = 3
        while (attempt < maxAttempts) {
            attempt++
            try {
                val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

                val request = okhttp3.Request.Builder()
                    .url(normalizedUrlStr)
                    .addHeader("Range", "bytes=$existingBytes-")
                    .addHeader("User-Agent", "GitaApp/1.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val isSuccess = response.code in 200..299
                    val isRangeSuccess = response.code == 206
                    
                    if (!isSuccess && !isRangeSuccess) {
                        Log.w(TAG, "HTTP ${response.code} for $normalizedUrlStr (attempt $attempt)")
                        return@use
                    }

                    val body = response.body ?: run {
                        Log.w(TAG, "Empty response body (attempt $attempt)")
                        return@use
                    }

                    val contentLength = body.contentLength()
                    val total = if (contentLength > 0) {
                        if (isRangeSuccess) contentLength + existingBytes else contentLength
                    } else {
                        measuredSizes[outFile.name] ?: outFile.length().coerceAtLeast(0L)
                    }

                    java.io.FileOutputStream(tempFile, existingBytes > 0 && isRangeSuccess).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var downloaded = if (isRangeSuccess) existingBytes else 0L
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                onProgress(downloaded, total)
                            }
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        // Validate file size before renaming if we have a total
                        if (total > 0 && tempFile.length() < total * 0.98) {
                            Log.w(TAG, "Download finished but size mismatch: ${tempFile.length()} vs $total")
                        } else {
                            if (outFile.exists()) outFile.delete()
                            if (tempFile.renameTo(outFile)) {
                                return true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Download attempt $attempt failed: ${e.message}")
            }
        }
        return false
    }

    private fun normalizeHuggingFaceUrl(original: String, expectedFileName: String): String {
        return try {
            if (!original.contains("huggingface.co")) return original
            val uri = java.net.URI(original)
            var path = uri.path ?: ""

            if (path.contains("/resolve/")) {
                val query = if (uri.query.isNullOrBlank()) "download=true" else uri.query
                return java.net.URI(uri.scheme, uri.authority, uri.path, query, uri.fragment).toString()
            }

            val trimmed = path.trim('/')
            val parts = trimmed.split('/')
            if (parts.size >= 2) {
                val user = parts[0]
                val repo = parts[1]
                val fileName = if (repo.endsWith(".tflite") || repo.endsWith(".litertlm")) repo else expectedFileName
                val newPath = "/$user/$repo/resolve/main/$fileName"
                val query = if (uri.query.isNullOrBlank()) "download=true" else uri.query
                java.net.URI(uri.scheme, uri.authority, newPath, query, uri.fragment).toString()
            } else {
                original
            }
        } catch (_: Exception) {
            original
        }
    }

    private fun modelExists(fileName: String): Boolean {
        val modelTarget = models.find { it.fileName == fileName } ?: return false
        val expectedTarget = measuredSizes[fileName] ?: modelTarget.expectedBytes

        val file = File(modelsDir, fileName)
        if (file.exists() && file.length() > 0 && file.length() >= expectedTarget * 0.9f) return true

        return try {
            val afd = context.assets.openFd("ml_models/$fileName")
            afd.length > 0 && afd.length >= expectedTarget * 0.9f
        } catch (_: Exception) {
            false
        }
    }

    // ─── Public Utility Functions ─────────────────────────────────────────────

    fun getModelDirPath(): String = modelsDir.absolutePath

    suspend fun getTotalDownloadedSize(): Long = withContext(Dispatchers.IO) {
        models.sumOf { model ->
            val file = File(modelsDir, model.fileName)
            if (file.exists()) file.length() else 0L
        }
    }

    fun getExpectedTotalSizeBytes(): Long {
        val dynamicTotal = models.sumOf { measuredSizes[it.fileName] ?: 0L }
        return if (dynamicTotal > 0) dynamicTotal else models.sumOf { it.expectedBytes }
    }

    suspend fun getMeasuredTotalSizeBytes(): Long = withContext(Dispatchers.IO) {
        try {
            if (measuredSizes.isEmpty()) probeModelSizes(loadManifest())
            models.sumOf { measuredSizes[it.fileName] ?: 0L }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun getRemainingDownloadSizeBytes(): Long = withContext(Dispatchers.IO) {
        models.sumOf { model ->
            val file = File(modelsDir, model.fileName)
            if (file.exists() && file.length() > 0) 0L
            else measuredSizes[model.fileName] ?: model.expectedBytes
        }
    }

    suspend fun clearAllModels(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (modelsDir.exists()) {
                modelsDir.deleteRecursively()
                modelsDir.mkdirs()
                Log.d(TAG, "All models cleared")
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing models: ${e.message}")
            false
        }
    }

    fun getModelInfo(name: String): ModelInfo? {
        return models.find { it.name == name }
    }

    suspend fun getModelsStatus(): List<ModelStatus> = withContext(Dispatchers.IO) {
        models.map { model ->
            val file = File(modelsDir, model.fileName)
            val expectedTarget = measuredSizes[model.fileName] ?: model.expectedBytes
            val actualSize = if (file.exists()) file.length() else 0L
            val isFullyDownloaded = actualSize > 0 && actualSize >= expectedTarget * 0.9f
            ModelStatus(
                name = model.name,
                size = model.size,
                isDownloaded = isFullyDownloaded,
                actualSizeBytes = actualSize,
                path = file.absolutePath
            )
        }
    }

    data class ModelStatus(
        val name: String,
        val size: String,
        val isDownloaded: Boolean,
        val actualSizeBytes: Long,
        val path: String
    )

    companion object {
        fun getBestTextModelPath(context: Context): String? {
            val modelsDir = File(context.filesDir, "ml_models")
            val qwenFile = File(modelsDir, "qwen3-0.6b-int4.litertlm")
            if (qwenFile.exists() && qwenFile.length() > 500_000_000L) return qwenFile.absolutePath
            val gemma4File = File(modelsDir, "gemma-4-E2B-it.litertlm")
            if (gemma4File.exists() && gemma4File.length() > 2_500_000_000L) return gemma4File.absolutePath
            return null
        }

        /** @deprecated Use {@link #getBestTextModelPath(Context)} */
        @Deprecated("Use getBestTextModelPath", ReplaceWith("getBestTextModelPath(context)"))
        fun getBestGemmaModelPath(context: Context): String? = getBestTextModelPath(context)
    }
}