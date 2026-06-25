package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Method
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class ModelInferenceEngine(private val context: Context) : Closeable {

    private val TAG = "ModelInferenceEngine"
    private var interpreter: Interpreter? = null
    private var mappedBuffer: MappedByteBuffer? = null  // tracked for explicit unmap
    private var fileChannel: FileChannel? = null         // tracked for proper close

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
        // Force-unmap the MappedByteBuffer to free native memory on Android
        mappedBuffer?.let { buffer ->
            try {
                val cleanerMethod: Method = buffer.javaClass.getMethod("cleaner")
                cleanerMethod.isAccessible = true
                val cleaner = cleanerMethod.invoke(buffer)
                cleaner?.javaClass?.getMethod("clean")?.invoke(cleaner)
            } catch (_: Exception) {
                // Unmap failed — GC will eventually reclaim, but native memory lingers
                Log.w(TAG, "Buffer unmap failed; relying on GC")
            }
            mappedBuffer = null
        }
        fileChannel?.close()
        fileChannel = null
    }

    private fun mapModelFile(file: File): MappedByteBuffer {
        val input = FileInputStream(file)
        val channel = input.channel
        fileChannel = channel  // track for close()
        return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).also {
            mappedBuffer = it  // track for explicit unmap
        }
    }

    private fun mapAssetModel(assetPath: String): MappedByteBuffer? {
        return try {
            val afd = context.assets.openFd(assetPath)
            val input = java.io.FileInputStream(afd.fileDescriptor)
            val channel = input.channel
            fileChannel = channel  // track for close()
            channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length).also {
                mappedBuffer = it  // track for explicit unmap in close()
            }
        } catch (e: Exception) {
            null
        }
    }
}
