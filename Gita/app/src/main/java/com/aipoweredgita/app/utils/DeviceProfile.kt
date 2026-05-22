package com.aipoweredgita.app.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class DeviceProfile(
    // Rendering
    val targetFps           : Int,
    val shadowQuality       : ShadowQuality,
    val particleMultiplier  : Float,   // 1.0 = full, 0.5 = half, 0.0 = off
    val useBloom            : Boolean,
    val textureResScale     : Float,   // 1.0 = full-res, 0.5 = half-res

    // Networking / data
    val prefetchAheadCount  : Int,     // how many items to prefetch
    val imageCacheMaxMb     : Int,

    // ML / inference
    val mlDelegate          : MlDelegate,
    val maxConcurrentInference: Int,

    // Animation
    val enableHeavyAnimations: Boolean,
    val reducedMotion       : Boolean,

    // Background work
    val backgroundSyncInterval: Duration,
) {
    enum class ShadowQuality { NONE, LOW, MEDIUM, HIGH }
    enum class MlDelegate    { CPU, NNAPI, GPU }

    companion object {
        fun from(tier: DeviceTier): DeviceProfile = when (tier) {

            DeviceTier.FLAGSHIP -> DeviceProfile(
                targetFps              = 120,
                shadowQuality          = ShadowQuality.HIGH,
                particleMultiplier     = 1.0f,
                useBloom               = true,
                textureResScale        = 1.0f,
                prefetchAheadCount     = 10,
                imageCacheMaxMb        = 256,
                mlDelegate             = MlDelegate.GPU,
                maxConcurrentInference = 4,
                enableHeavyAnimations  = true,
                reducedMotion          = false,
                backgroundSyncInterval = 15.minutes,
            )

            DeviceTier.HIGH_MID -> DeviceProfile(
                targetFps              = 90,
                shadowQuality          = ShadowQuality.MEDIUM,
                particleMultiplier     = 0.75f,
                useBloom               = true,
                textureResScale        = 1.0f,
                prefetchAheadCount     = 7,
                imageCacheMaxMb        = 192,
                mlDelegate             = MlDelegate.NNAPI,
                maxConcurrentInference = 2,
                enableHeavyAnimations  = true,
                reducedMotion          = false,
                backgroundSyncInterval = 20.minutes,
            )

            DeviceTier.MID -> DeviceProfile(
                targetFps              = 60,
                shadowQuality          = ShadowQuality.LOW,
                particleMultiplier     = 0.5f,
                useBloom               = false,
                textureResScale        = 0.75f,
                prefetchAheadCount     = 5,
                imageCacheMaxMb        = 128,
                mlDelegate             = MlDelegate.NNAPI,
                maxConcurrentInference = 1,
                enableHeavyAnimations  = false,
                reducedMotion          = false,
                backgroundSyncInterval = 30.minutes,
            )

            DeviceTier.LOW_MID -> DeviceProfile(
                targetFps              = 60,
                shadowQuality          = ShadowQuality.NONE,
                particleMultiplier     = 0.25f,
                useBloom               = false,
                textureResScale        = 0.5f,
                prefetchAheadCount     = 3,
                imageCacheMaxMb        = 64,
                mlDelegate             = MlDelegate.CPU,
                maxConcurrentInference = 1,
                enableHeavyAnimations  = false,
                reducedMotion          = true,
                backgroundSyncInterval = 45.minutes,
            )

            DeviceTier.LOW -> DeviceProfile(
                targetFps              = 30,
                shadowQuality          = ShadowQuality.NONE,
                particleMultiplier     = 0.0f,
                useBloom               = false,
                textureResScale        = 0.5f,
                prefetchAheadCount     = 2,
                imageCacheMaxMb        = 32,
                mlDelegate             = MlDelegate.CPU,
                maxConcurrentInference = 1,
                enableHeavyAnimations  = false,
                reducedMotion          = true,
                backgroundSyncInterval = 60.minutes,
            )
        }
    }
}
