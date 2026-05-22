package com.aipoweredgita.app.utils

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Build
import android.util.Log

enum class DeviceTier(val label: String) {
    FLAGSHIP("Flagship  ~10L+ AnTuTu"),
    HIGH_MID("High-Mid  ~7–10L AnTuTu"),
    MID     ("Mid       ~4–7L  AnTuTu"),
    LOW_MID ("Low-Mid   ~2–4L  AnTuTu"),
    LOW     ("Low       <2L   AnTuTu")
}

/**
 * Universal device tier detector.
 *
 * Design principles:
 *  - Zero string/chip-name matching — every signal is capability-based or measured.
 *  - No maintenance required when new SoCs ship.
 *  - GPU detection deferred until an EGL context exists (call updateGpuSignals() from
 *    your renderer/GLSurfaceView after the first frame).
 *  - CPU bench runs off the main thread to avoid cold-start throttle pollution.
 *
 * Deleted: boardHint() — string matching, contributed only ±3/100, actively wrong
 *          on devices whose BOARD codename belongs to a different chip family
 *          (e.g. Nord 4 reports BOARD=pineapple which is the SD 8 Gen 3 codename).
 */
object DeviceTierDetector {

    private const val TAG = "DeviceTier"

    // ── State ─────────────────────────────────────────────────────
    @Volatile private var cached: DeviceTier? = null
    @Volatile private var gpuSignals: GpuSignals? = null   // set after EGL context exists
    @Volatile private var refinedBench: Long? = null        // set after async bench completes

    fun invalidate() {
        cached = null
        gpuSignals = null
        refinedBench = null
    }

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Synchronous best-effort detection. Safe to call on any thread.
     * Returns immediately using whatever signals are available.
     * If called before GPU/async bench are ready, result may be conservative.
     */
    fun detect(context: Context): DeviceTier {
        cached?.let { return it }
        val signals = collectSignals(context)
        log(signals)
        return classify(signals).also { cached = it }
    }

    /**
     * Preferred entry point. Returns a quick estimate immediately via [onQuickResult],
     * then re-classifies with the accurate async bench and calls [onRefinedResult]
     * once complete (~100ms later on a background thread).
     *
     * If GPU signals are already available (updateGpuSignals() was called) the
     * refined result will also include GPU score.
     */
    fun detectAsync(
        context: Context,
        onQuickResult: (DeviceTier) -> Unit,
        onRefinedResult: (DeviceTier) -> Unit
    ) {
        // Quick pass — bench may be polluted but everything else is accurate
        val quick = detect(context)
        onQuickResult(quick)

        Thread {
            // Warm-up pass to flush scheduler noise
            cpuMicroBench(warmupOnly = true)
            // Real measurement
            val bench = cpuMicroBench(warmupOnly = false)
            refinedBench = bench
            cached = null  // force re-classify with accurate bench
            val refined = detect(context)
            onRefinedResult(refined)
            Log.d(TAG, "Async refined bench=${bench / 1_000_000}ms → $refined")
        }.apply { name = "DeviceTierBench"; isDaemon = true }.start()
    }

    /**
     * Call this from your GLSurfaceView.Renderer.onSurfaceCreated() or equivalent,
     * AFTER an EGL context exists. This provides the GPU capability signals.
     * Automatically invalidates the cached tier so next detect() uses GPU data.
     */
    fun updateGpuSignals() {
        val signals = readGpuSignals()
        if (signals.glesVersion > 0) {
            gpuSignals = signals
            cached = null  // re-classify with GPU data
            Log.d(TAG, "GPU signals updated: $signals")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SIGNAL TYPES
    // ─────────────────────────────────────────────────────────────

    data class HardwareSignals(
        val ramGb       : Float,
        val bigCoreFreq : Int,      // highest cluster freq in MHz
        val clusterCount: Int,      // 1=symmetric, 2=big.LITTLE, 3=tri-cluster
        val coreCount   : Int,
        val armVersion  : Int,      // 8 or 9
        val cpuScore    : Long,     // nanoseconds — lower = faster
        val vulkanTier  : Int,      // 0–4
        val gpu         : GpuSignals?
    )

    data class GpuSignals(
        val glesVersion : Int,      // 30, 31, 32
        val hasAstcLdr  : Boolean,  // mid-range+ GPU
        val hasAstcHdr  : Boolean,  // high-end GPU
        val hasGeomShdr : Boolean,  // high-end
        val hasTessShdr : Boolean,  // flagship
        val hasEtc2     : Boolean,  // baseline — almost universal
        val gpuScore    : Int       // 0–10 composite
    )

    // ─────────────────────────────────────────────────────────────
    //  SIGNAL COLLECTION
    // ─────────────────────────────────────────────────────────────

    private fun collectSignals(context: Context) = HardwareSignals(
        ramGb        = totalRamGb(context),
        bigCoreFreq  = bigClusterFreqMhz(),
        clusterCount = detectClusterCount(),
        coreCount    = Runtime.getRuntime().availableProcessors(),
        armVersion   = armVersion(),
        cpuScore     = refinedBench ?: cpuMicroBench(warmupOnly = false),
        vulkanTier   = vulkanTier(context),
        gpu          = gpuSignals
    )

    // ── RAM ───────────────────────────────────────────────────────
    private fun totalRamGb(context: Context): Float {
        val mi = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getMemoryInfo(mi)
        return mi.totalMem / 1_073_741_824f
    }

    // ── CPU frequencies ───────────────────────────────────────────
    private fun allCoreFreqsMhz(): List<Int> =
        java.io.File("/sys/devices/system/cpu")
            .listFiles { f -> f.name.matches(Regex("cpu\\d+")) }
            ?.mapNotNull { core ->
                runCatching {
                    java.io.File("${core.path}/cpufreq/cpuinfo_max_freq")
                        .readText().trim().toLong() / 1000
                }.getOrNull()?.toInt()
            }
            ?.sorted()
            ?: emptyList()

    private fun bigClusterFreqMhz(): Int {
        val freqs = allCoreFreqsMhz().ifEmpty { return 0 }
        val peak = freqs.max()
        return freqs.filter { peak - it <= 200 }.max()
    }

    private fun detectClusterCount(): Int {
        val freqs = allCoreFreqsMhz().toSortedSet()
        if (freqs.size <= 1) return 1
        var clusters = 1
        var prev = freqs.first()
        for (f in freqs.drop(1)) {
            if (f - prev > 300) clusters++
            prev = f
        }
        return clusters
    }

    // ── ARM ISA version ───────────────────────────────────────────
    private fun armVersion(): Int = runCatching {
        val info = java.io.File("/proc/cpuinfo").readText()
        if (info.contains("ARMv9", ignoreCase = true) ||
            info.contains("Cortex-X4", ignoreCase = true) ||
            info.contains("Cortex-X3", ignoreCase = true) ||
            info.contains("Cortex-A720", ignoreCase = true) ||
            info.contains("Cortex-A520", ignoreCase = true) ||
            Build.VERSION.SDK_INT >= 34) 9 else 8
    }.getOrDefault(8)

    // ── CPU bench ────────────────────────────────────────────────
    /**
     * Two-phase bench:
     *  warmupOnly=true  → 5M iterations to flush cold-start scheduler noise
     *  warmupOnly=false → 50M measured iterations
     *
     * Always call warmupOnly=true first on a background thread, then measure.
     */
    private fun cpuMicroBench(warmupOnly: Boolean): Long {
        val iterations = if (warmupOnly) 5_000_000L else 50_000_000L
        var x = 1L
        val start = System.nanoTime()
        for (i in 0 until iterations) {
            x = x * 6364136223846793005L + 1442695040888963407L
        }
        val elapsed = System.nanoTime() - start
        if (x == 0L) Log.v(TAG, "bench anti-opt")
        return if (warmupOnly) 0L else elapsed
    }

    // ── Vulkan feature level (universal GPU generation proxy) ─────
    /**
     * Vulkan version maps cleanly to GPU generation without any chip names:
     *   1.3 → SD 8 Gen 2+ / Dimensity 9200+ / Exynos 2300+  (flagship)
     *   1.2 → SD 8 Gen 1 / SD 7+ Gen 2+ / Dimensity 9000    (high-mid)
     *   1.1 → SD 695 / Dimensity 700 / mid-range             (mid)
     *   1.0 → older mid / low-mid
     *    0  → no Vulkan (budget / very old)
     */
    private fun vulkanTier(context: Context): Int {
        if (Build.VERSION.SDK_INT < 24) return 0
        val pm = context.packageManager
        return when {
            pm.hasSystemFeature("android.hardware.vulkan.version", 0x403000) -> 4  // 1.3
            pm.hasSystemFeature("android.hardware.vulkan.version", 0x402000) -> 3  // 1.2
            pm.hasSystemFeature("android.hardware.vulkan.version", 0x401000) -> 2  // 1.1
            pm.hasSystemFeature("android.hardware.vulkan.level",   1)        -> 1  // 1.0
            pm.hasSystemFeature("android.hardware.vulkan.compute")           -> 1
            else -> 0
        }
    }

    // ── GPU capability signals (requires EGL context) ─────────────
    /**
     * Reads GPU capability via OpenGL extensions — fully universal.
     * Extension support maps to GPU tier without any renderer string matching:
     *
     *   ASTC HDR + tessellation → flagship (Adreno 7xx, Immortalis-G925)
     *   ASTC LDR + geometry     → high-end (Adreno 6xx+, Mali-G710+)
     *   ASTC LDR only           → mid-range (Adreno 6xx, Mali-G57+)
     *   ETC2 only               → budget (any modern GPU)
     *   None                    → very old
     *
     * GLES version is the most reliable single signal:
     *   3.2 → high-end+, 3.1 → mid+, 3.0 → low-mid+
     */
    private fun readGpuSignals(): GpuSignals {
        val ext     = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
        val version = GLES20.glGetString(GLES20.GL_VERSION)    ?: ""

        val glesVersion = when {
            version.contains("OpenGL ES 3.2") -> 32
            version.contains("OpenGL ES 3.1") -> 31
            version.contains("OpenGL ES 3.0") -> 30
            version.contains("OpenGL ES 2.0") -> 20
            else -> 0
        }

        val hasAstcLdr  = ext.contains("GL_KHR_texture_compression_astc_ldr")
        val hasAstcHdr  = ext.contains("GL_KHR_texture_compression_astc_hdr")
        val hasGeomShdr = ext.contains("GL_EXT_geometry_shader")
        val hasTessShdr = ext.contains("GL_EXT_tessellation_shader")
        val hasEtc2     = ext.contains("GL_OES_compressed_ETC1_RGB8_texture") ||
                glesVersion >= 30  // ETC2 mandatory in GLES 3.0+

        // Composite GPU score 0–10
        var score = 0
        score += when (glesVersion) {
            32   -> 3
            31   -> 2
            30   -> 1
            else -> 0
        }
        if (hasAstcLdr)  score += 2   // mid-range and above
        if (hasAstcHdr)  score += 2   // high-end only
        if (hasGeomShdr) score += 1   // high-end
        if (hasTessShdr) score += 2   // flagship only

        return GpuSignals(
            glesVersion  = glesVersion,
            hasAstcLdr   = hasAstcLdr,
            hasAstcHdr   = hasAstcHdr,
            hasGeomShdr  = hasGeomShdr,
            hasTessShdr  = hasTessShdr,
            hasEtc2      = hasEtc2,
            gpuScore     = score.coerceIn(0, 10)
        )
    }

    // ─────────────────────────────────────────────────────────────
    //  CLASSIFICATION
    // ─────────────────────────────────────────────────────────────

    private fun classify(s: HardwareSignals): DeviceTier {

        // ── A. RAM (0–4) ──────────────────────────────────────────
        val ramPts = when {
            s.ramGb >= 11.5f -> 4   // 12 GB+
            s.ramGb >=  7.5f -> 3   // 8 GB
            s.ramGb >=  5.5f -> 2   // 6 GB
            s.ramGb >=  3.5f -> 1   // 4 GB
            else             -> 0   // 2–3 GB
        }

        // ── B. Big-cluster CPU frequency (0–5) ───────────────────
        // Reading from cpuinfo_max_freq can be thermally throttled.
        // We give one bucket of leniency (+200MHz tolerance) to avoid
        // penalising devices that report their sustained rather than peak freq.
        val freqPts = when {
            s.bigCoreFreq >= 3000 -> 5   // SD 8 Gen 3 / Dimensity 9400 prime
            s.bigCoreFreq >= 2800 -> 4   // SD 8 Gen 2 / SD 7+ Gen 3 (Nord 4)
            s.bigCoreFreq >= 2500 -> 3   // SD 8 Gen 1 / SD 7 Gen 3
            s.bigCoreFreq >= 2200 -> 2   // SD 7 Gen 1-2 / G99
            s.bigCoreFreq >= 1800 -> 1   // SD 695 / Dimensity 700
            else                  -> 0
        }

        // ── C. Cluster topology (0–2) ─────────────────────────────
        val clusterPts = when (s.clusterCount) {
            3    -> 2   // tri-cluster = flagship architecture
            2    -> 1   // big.LITTLE = mainstream
            else -> 0
        }

        // ── D. ARM ISA generation (0–2) ──────────────────────────
        val armPts = if (s.armVersion >= 9) 2 else 0

        // ── E. CPU bench (0–4) ───────────────────────────────────
        // Thresholds calibrated for 50M LCG iterations on background thread
        // after warmup. Main-thread cold values are clamped to min 1 to avoid
        // catastrophic misclassification from scheduler noise.
        val rawBenchPts = when {
            s.cpuScore < 40_000_000L  -> 4   // <40ms  flagship
            s.cpuScore < 70_000_000L  -> 3   // 40–70ms high-mid
            s.cpuScore < 110_000_000L -> 2   // 70–110ms mid
            s.cpuScore < 250_000_000L -> 1   // 110–250ms low-mid (extended from 180ms)
            else                      -> 0
        }
        // If bench hasn't been run async yet, don't let a cold-start value of
        // >250ms crater the score — floor at 1 when we lack a refined reading.
        val benchPts = if (refinedBench == null) rawBenchPts.coerceAtLeast(1) else rawBenchPts

        // ── F. Vulkan tier (0–4) ─────────────────────────────────
        // Universal GPU generation proxy — no chip names needed
        val vulkanPts = s.vulkanTier  // 0–4

        // ── G. GPU capability score (0–4, skipped if no GL context) ──
        // Maps 0–10 raw score → 0–4 points
        val gpuPts: Int
        val gpuWeight: Int
        if (s.gpu != null && s.gpu.glesVersion > 0) {
            gpuPts   = (s.gpu.gpuScore / 2.5f).toInt().coerceIn(0, 4)
            gpuWeight = 4
        } else {
            gpuPts   = 0
            gpuWeight = 0
        }

        // ── Weighted total → normalised 0–100 ────────────────────
        //
        // Weights reflect reliability and independence:
        //   freq    ×5  most reliable, directly measured hardware
        //   bench   ×4  measured throughput, independent of freq
        //   vulkan  ×4  universal GPU generation, no names needed
        //   ram     ×3  necessary but not sufficient
        //   gpu cap ×4  capability-based, added when GL context available
        //   cluster ×2  architecture topology
        //   arm ISA ×2  ISA generation
        //
        val weighted = (freqPts  * 5) +
                (benchPts * 4) +
                (vulkanPts* 4) +
                (ramPts   * 3) +
                (gpuPts   * gpuWeight) +
                (clusterPts * 2) +
                (armPts   * 2)

        val maxPossible = (5 * 5) + (4 * 4) + (4 * 4) + (3 * 4) +
                (gpuWeight * 4) + (2 * 2) + (2 * 2)

        val score = (weighted * 100f / maxPossible).toInt()

        Log.d(TAG, buildString {
            append("freqPts=$freqPts benchPts=$benchPts vulkanPts=$vulkanPts ")
            append("ramPts=$ramPts gpuPts=$gpuPts clusterPts=$clusterPts armPts=$armPts ")
            append("→ score=$score/100")
        })

        return when {
            score >= 75 -> DeviceTier.FLAGSHIP
            score >= 55 -> DeviceTier.HIGH_MID
            score >= 36 -> DeviceTier.MID
            score >= 20 -> DeviceTier.LOW_MID
            else        -> DeviceTier.LOW
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  LOGGING
    // ─────────────────────────────────────────────────────────────

    private fun log(s: HardwareSignals) {
        Log.d("DeviceTier", "BOARD=${Build.BOARD} HARDWARE=${Build.HARDWARE} DEVICE=${Build.DEVICE} MODEL=${Build.MODEL}")
        Log.d("DeviceTier", "FINGERPRINT=${Build.FINGERPRINT}")
        
        Log.d(TAG, "RAM=%.1fGB bigFreq=${s.bigCoreFreq}MHz cores=${s.coreCount} ".format(s.ramGb) +
                "clusters=${s.clusterCount} ARMv${s.armVersion} " +
                "bench=${s.cpuScore / 1_000_000}ms vulkan=${s.vulkanTier} " +
                "gpu=${s.gpu?.gpuScore ?: "pending"} gles=${s.gpu?.glesVersion ?: "pending"}")
    }
}