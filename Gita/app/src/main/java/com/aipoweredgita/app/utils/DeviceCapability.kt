package com.aipoweredgita.app.utils

import android.content.Context

data class SamplerParams(val topK: Int, val topP: Float, val temperature: Float)

object DeviceCapability {
    fun getDeviceTier(context: Context): DeviceTier {
        return DeviceTierDetector.detect(context)
    }

    fun getOptimalMaxTokens(context: Context, modelName: String? = null): Int {
        return when {
            // Qwen3 0.6B — small model, limited KV cache
            modelName?.contains("qwen", ignoreCase = true) == true -> 4096
            // Gemma 4 2B — larger but still constrained on mobile
            modelName?.contains("gemma", ignoreCase = true) == true -> 8192
            else -> {
                val tier = getDeviceTier(context)
                when (tier) {
                    DeviceTier.FLAGSHIP -> 16384
                    DeviceTier.HIGH_MID -> 8192
                    else -> 4096
                }
            }
        }
    }

    fun getOptimalTimeout(context: Context): Long {
        val tier = getDeviceTier(context)
        return when (tier) {
            DeviceTier.FLAGSHIP -> 120_000L
            else -> 240_000L
        }
    }

    fun getOptimalSampler(modelName: String?): SamplerParams {
        return if (modelName?.contains("gemma", ignoreCase = true) == true) {
            SamplerParams(topK = 32, topP = 0.90f, temperature = 0.4f) // Gemma 4 2B
        } else {
            // Qwen 3 0.6B settings
            SamplerParams(topK = 40, topP = 0.95f, temperature = 0.7f)
        }
    }

    fun getOptimalSentenceCount(context: Context): Int {
        val tier = getDeviceTier(context)
        return when (tier) {
            DeviceTier.FLAGSHIP -> 5
            DeviceTier.HIGH_MID -> 4
            else -> 3
        }
    }

    fun getOptimalExplanationLength(context: Context): Int {
        val tier = getDeviceTier(context)
        return when (tier) {
            DeviceTier.FLAGSHIP -> 1000
            DeviceTier.HIGH_MID -> 800
            else -> 600
        }
    }
}
