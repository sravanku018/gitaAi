package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class ModelInferenceEngine(private val context: Context) : Closeable {

    private val TAG = "ModelInferenceEngine"
    private var interpreter: Interpreter? = null

    @Synchronized
    fun loadModel(fileName: String): Boolean {
        close() // close existing interpreter first
        return try {
            val modelsDir = File(context.filesDir, "ml_models")
            val modelFile = File(modelsDir, fileName)
            val mapped: MappedByteBuffer = if (modelFile.exists()) {
                mapModelFile(modelFile)
            } else {
                mapAssetModel("ml_models/$fileName") ?: run {
                    Log.e(TAG, "Model not found in filesDir or assets: $fileName")
                    return false
                }
            }
            interpreter = Interpreter(mapped)
            Log.d(TAG, "Loaded TFLite model: $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
            false
        }
    }

    fun isReady(): Boolean = interpreter != null

    @Synchronized
    fun computeEmbedding(inputIds: IntArray, attentionMask: IntArray): FloatArray? {
        val intr = interpreter ?: return null
        return try {
            val outputSize = intr.getOutputTensor(0).shape()[1]
            val inputIds2d = arrayOf(inputIds)
            val attention2d = arrayOf(attentionMask)
            val output = Array(1) { FloatArray(outputSize) }
            intr.runForMultipleInputsOutputs(
                arrayOf(inputIds2d, attention2d),
                mapOf(0 to output)
            )
            output[0]
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            null
        }
    }

    fun cosineSim(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom == 0f) 0f else dot / denom
    }

    @Synchronized
    override fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun mapModelFile(file: File): MappedByteBuffer {
        return java.io.FileInputStream(file).use { input ->
            input.channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            }
        }
    }

    private fun mapAssetModel(assetPath: String): MappedByteBuffer? {
        return try {
            context.assets.openFd(assetPath).use { afd ->
                java.io.FileInputStream(afd.fileDescriptor).use { input ->
                    input.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
