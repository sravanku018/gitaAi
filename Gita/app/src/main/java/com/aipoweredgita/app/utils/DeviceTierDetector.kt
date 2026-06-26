package com.aipoweredgita.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

enum class DeviceTier(val label: String) {
    FLAGSHIP("Flagship"),
    HIGH_MID("High-Mid"),
    MID("Mid"),
    LOW_MID("Low-Mid"),
    LOW("Low")
}

/**
 * Device tier detector using four independent axes:
 *
 *  **CPU**    — core count, peak frequency, ARM ISA version, cluster topology
 *  **GPU**    — Vulkan API level, OpenCL availability
 *  **RAM**    — total memory capacity
 *  **AnTuTu** — v10 benchmark (SoC name lookup or HW-based estimation)
 *
 *  No blocking micro-benchmarks or GL context creation.
 *  Every signal is available synchronously without blocking the calling thread.
 */
object DeviceTierDetector {

    private const val TAG = "DeviceTier"
    @Volatile private var cached: DeviceTier? = null
    @Volatile private var cachedArmVersion: Int? = null

    fun invalidate() { cached = null; cachedArmVersion = null }

    /**
     * Synchronous detection. Safe to call on any thread — no blocking I/O or
     * micro-benchmarks. Returns a cached result on subsequent calls.
     */
    fun detect(context: Context): DeviceTier {
        cached?.let { return it }
        val signals = collectSignals(context)
        return classify(signals).also { cached = it }
    }

    // ─── signal types ───────────────────────────────────────────────

    data class CpuSignals(
        val coreCount: Int,
        val bigCoreFreq: Int,    // MHz
        val armVersion: Int,     // 8 or 9
        val clusterCount: Int    // 1, 2, or 3
    )

    data class GpuSignals(
        val vulkanTier: Int,   // 0–4
        val hasOpenCL: Boolean
    )

    data class HardwareSignals(
        val ramGb: Float,
        val cpu: CpuSignals,
        val gpu: GpuSignals,
        val antutuScore: Int     // AnTuTu v10 benchmark score (0–2_000_000+)
    )

    // ─── collection ─────────────────────────────────────────────────

    private fun collectSignals(context: Context): HardwareSignals {
        val ram = totalRamGb(context)
        val cpu = collectCpuSignals()
        val gpu = collectGpuSignals(context)
        return HardwareSignals(
            ramGb = ram,
            cpu   = cpu,
            gpu   = gpu,
            antutuScore = resolveAntutuScore(cpu, gpu, ram)
        )
    }

    // ─── RAM ────────────────────────────────────────────────────────

    private fun totalRamGb(context: Context): Float {
        val mi = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getMemoryInfo(mi)
        return mi.totalMem / 1_073_741_824f
    }

    // ─── CPU axis ───────────────────────────────────────────────────

    private fun collectCpuSignals(): CpuSignals {
        val freqs = allCoreFreqsMhz()
        val cores = Runtime.getRuntime().availableProcessors()
        return CpuSignals(
            coreCount    = cores,
            bigCoreFreq  = freqs.maxOrNull() ?: 0,
            armVersion   = armVersion(),
            clusterCount = detectClusterCount(freqs)
        )
    }

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

    private fun detectClusterCount(freqs: List<Int>): Int {
        val distinct = freqs.toSortedSet()
        if (distinct.size <= 1) return 1
        var clusters = 1
        var prev = distinct.first()
        for (f in distinct.drop(1)) {
            if (f - prev > 300) clusters++
            prev = f
        }
        return clusters
    }

    private fun armVersion(): Int = cachedArmVersion ?: runCatching {
        val info = java.io.File("/proc/cpuinfo").readText()
        if (info.contains("ARMv9", ignoreCase = true) ||
            info.contains("Cortex-X4", ignoreCase = true) ||
            info.contains("Cortex-X3", ignoreCase = true) ||
            info.contains("Cortex-A720", ignoreCase = true) ||
            info.contains("Cortex-A520", ignoreCase = true) ||
            Build.VERSION.SDK_INT >= 34) 9 else 8
    }.getOrDefault(8).also { cachedArmVersion = it }

    // ─── GPU axis ───────────────────────────────────────────────────

    private fun collectGpuSignals(context: Context) = GpuSignals(
        vulkanTier = vulkanTier(context),
        hasOpenCL  = openCLAvailable()
    )

    /**
     * Vulkan version is the best single GPU generation proxy available
     * without requiring an EGL/GL context:
     *   1.3 → Adreno 7xx / Immortalis-G925  (flagship)
     *   1.2 → Adreno 6xx / Mali-G710+       (high-mid)
     *   1.1 → Adreno 5xx / Mali-G57+         (mid)
     *   1.0 → older GPUs                     (low-mid)
     *   0   → no Vulkan                      (budget)
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

    /** OpenCL availability — critical for LiteRT GPU delegate on many SoCs. */
    private fun openCLAvailable(): Boolean = try {
        java.io.File("/system/lib64/libOpenCL.so").exists() ||
        java.io.File("/vendor/lib64/libOpenCL.so").exists() ||
        java.io.File("/system/lib/libOpenCL.so").exists() ||
        java.io.File("/vendor/lib/libOpenCL.so").exists()
    } catch (_: Exception) { false }

    // ─── AnTuTu v10 score ──────────────────────────────────────────

    /**
     * Resolves AnTuTu v10 score in two ways:
     *  1. SoC name lookup (Build.SOC_MODEL on API 31+ or sysfs fallback)
     *  2. CPU/GPU characteristic estimation if SoC is unknown
     *
     * Every device gets a score — no device left at zero unless truly entry-level.
     */
    private fun resolveAntutuScore(cpu: CpuSignals, gpu: GpuSignals, ramGb: Float): Int {
        val soc = detectSocName()
        if (soc != null) {
            // Exact match first
            ANTUTU_DATABASE[soc]?.let { return it }
            // Then partial match (e.g. "Snapdragon 7+ Gen 3" matches "Snapdragon 7+ Gen 3")
            ANTUTU_DATABASE.entries.firstOrNull { (key) ->
                soc.contains(key, ignoreCase = true)
            }?.let { return it.value }
        }
        return estimateScore(cpu, gpu, ramGb)
    }

    /** Detect SoC model name from system properties or sysfs. */
    private fun detectSocName(): String? {
        if (Build.VERSION.SDK_INT >= 31) {
            @Suppress("InlinedApi")
            val soc = Build.SOC_MODEL
            if (!soc.isNullOrBlank()) return soc.trim()
        }
        return readSocFromSys()
    }

    /** Read SoC identifier from sysfs (common on Qualcomm, MediaTek, Exynos). */
    private fun readSocFromSys(): String? {
        val paths = listOf(
            "/sys/devices/soc0/machine",
            "/sys/devices/soc0/family",
            "/sys/devices/system/soc/soc0/machine"
        )
        for (path in paths) {
            try {
                val name = java.io.File(path).readText().trim()
                if (name.isNotBlank()) return name
            } catch (_: Exception) { /* try next path */ }
        }
        return null
    }

    /**
     * Estimate AnTuTu v10 score from CPU/GPU capability when SoC is unknown.
     * Used for devices not in the lookup database or running on older Android.
     */
    private fun estimateScore(cpu: CpuSignals, gpu: GpuSignals, ramGb: Float): Int {
        var score = 0
        // ARM v9 ISA → modern cores (Cortex-X4/A720/A520)
        score += if (cpu.armVersion >= 9) 250_000 else 100_000
        // Peak CPU frequency → higher = faster single-thread
        score += (cpu.bigCoreFreq * 200).coerceAtMost(400_000)
        // Core count → more = better multi-thread
        score += (cpu.coreCount * 50_000).coerceAtMost(400_000)
        // Cluster topology → 3 clusters = prime+mid+LITTLE
        score += when (cpu.clusterCount) {
            3    -> 200_000
            2    -> 100_000
            else -> 0
        }
        // GPU capability
        score += when (gpu.vulkanTier) {
            4    -> 300_000  // Vulkan 1.3 = modern GPU
            3    -> 200_000  // Vulkan 1.2 = capable
            2    -> 100_000  // Vulkan 1.1 = moderate
            else -> 50_000
        }
        if (gpu.hasOpenCL) score += 100_000
        // RAM capacity proxy
        score += (ramGb * 30_000).toInt()
        // Clamp to realistic range
        return score.coerceIn(0, 2_000_000)
    }

    /**
     * AnTuTu v10 estimated scores for common mobile SoCs.
     * Keys match Build.SOC_MODEL output (API 31+) and common sysfs identifiers.
     * Values are realistic average AnTuTu v10 scores.
     */
    private val ANTUTU_DATABASE = mapOf(
            // ── Qualcomm Snapdragon 8-series (flagship) ──────────────
            "Snapdragon 8 Elite"            to 2_200_000,
            "Snapdragon 8 Gen 3"            to 1_900_000,
            "Snapdragon 8 Gen 2"            to 1_550_000,
            "Snapdragon 8+ Gen 1"           to 1_350_000,
            "Snapdragon 8 Gen 1"            to 1_050_000,
            "Snapdragon 888"                to   800_000,
            "Snapdragon 870"                to   700_000,
            "Snapdragon 865"                to   640_000,
            "Snapdragon 855"                to   540_000,
            "Snapdragon 845"                to   380_000,
            "Snapdragon 835"                to   240_000,

            // ── Qualcomm Snapdragon 7-series (upper-mid) ────────────
            "Snapdragon 7+ Gen 3"           to 1_150_000,  // OnePlus Nord 4
            "Snapdragon 7 Gen 3"            to   780_000,
            "Snapdragon 7+ Gen 2"           to 1_050_000,
            "Snapdragon 7 Gen 1"            to   650_000,
            "Snapdragon 778G"               to   560_000,
            "Snapdragon 782G"               to   600_000,
            "Snapdragon 7s Gen 2"           to   610_000,

            // ── Qualcomm Snapdragon 6-series (mid) ──────────────────
            "Snapdragon 6 Gen 1"            to   520_000,
            "Snapdragon 695"                to   410_000,
            "Snapdragon 680"                to   340_000,
            "Snapdragon 678"                to   330_000,
            "Snapdragon 675"                to   310_000,
            "Snapdragon 665"                to   280_000,

            // ── MediaTek Dimensity ───────────────────────────────────
            "Dimensity 9400"                to 2_400_000,
            "Dimensity 9300"                to 1_950_000,
            "Dimensity 9200"                to 1_250_000,
            "Dimensity 8300"                to 1_350_000,
            "Dimensity 8200"                to   900_000,
            "Dimensity 8100"                to   850_000,
            "Dimensity 8050"                to   750_000,
            "Dimensity 7300"                to   620_000,
            "Dimensity 7200"                to   600_000,
            "Dimensity 7000"                to   500_000,
            "Dimensity 1080"                to   550_000,
            "Dimensity 900"                 to   500_000,
            "Dimensity 810"                 to   420_000,
            "Dimensity 700"                 to   320_000,

            // ── MediaTek Helio ───────────────────────────────────────
            "Helio G99"                     to   410_000,
            "Helio G96"                     to   380_000,
            "Helio G95"                     to   370_000,
            "Helio G88"                     to   280_000,
            "Helio G85"                     to   270_000,
            "Helio G80"                     to   260_000,
            "Helio G70"                     to   250_000,
            "Helio P65"                     to   200_000,

            // ── Samsung Exynos ───────────────────────────────────────
            "Exynos 2400"                   to 1_720_000,
            "Exynos 2200"                   to 1_020_000,
            "Exynos 2100"                   to   780_000,
            "Exynos 1480"                   to   650_000,
            "Exynos 1380"                   to   550_000,
            "Exynos 1280"                   to   450_000,
            "Exynos 990"                    to   520_000,
            "Exynos 9820"                   to   450_000,
            "Exynos 9611"                   to   280_000,
            "Exynos 850"                    to   200_000,

            // ── Google Tensor ────────────────────────────────────────
            "Tensor G4"                     to 1_600_000,
            "Tensor G3"                     to 1_450_000,
            "Tensor G2"                     to 1_050_000,
            "Tensor"                        to   800_000,

            // ── Huawei Kirin ─────────────────────────────────────────
            "Kirin 9000"                    to   750_000,
            "Kirin 990"                     to   480_000,
            "Kirin 810"                     to   330_000,

            // ── UNISOC ───────────────────────────────────────────────
            "T760"                          to   420_000,
            "T616"                          to   260_000,
            "T612"                          to   240_000,
            "T610"                          to   230_000,
            "T606"                          to   220_000
        )

    // ─── classification ─────────────────────────────────────────────

    /**
     * Four-axis scoring:
     *
     *  **CPU** (max 10):    core count (0-2) + peak freq (0-3) + ARM ISA (0-2) + clusters (0-3)
     *  **GPU** (max 6):     vulkan tier (0-4) + OpenCL (0-2)
     *  **RAM** (max 3):     capacity gate for on-device LLM
     *  **AnTuTu** (max 5):  v10 benchmark — lookup or HW-estimated score
     *
     *  Total max = 24.
     */
    private fun classify(s: HardwareSignals): DeviceTier {
        // ── CPU score ──────────────────────────────────────────────
        val corePts = when {
            s.cpu.coreCount >= 8 -> 2
            s.cpu.coreCount >= 6 -> 1
            else                 -> 0
        }
        val freqPts = when {
            s.cpu.bigCoreFreq >= 3000 -> 3
            s.cpu.bigCoreFreq >= 2500 -> 2
            s.cpu.bigCoreFreq >= 2000 -> 1
            else                      -> 0
        }
        val armPts     = if (s.cpu.armVersion >= 9) 2 else 0
        val clusterPts = when (s.cpu.clusterCount) {
            3    -> 3   // tri-cluster = flagship CPU topology
            2    -> 1   // big.LITTLE = mainstream
            else -> 0
        }
        val cpuScore = corePts + freqPts + armPts + clusterPts

        // ── GPU score ──────────────────────────────────────────────
        val gpuScore = s.gpu.vulkanTier + (if (s.gpu.hasOpenCL) 2 else 0)

        // ── RAM score ──────────────────────────────────────────────
        val ramPts = when {
            s.ramGb >= 11.5f -> 3   // 12 GB+
            s.ramGb >=  7.5f -> 2   // 8 GB
            s.ramGb >=  5.5f -> 1   // 6 GB
            else             -> 0
        }

        // ── AnTuTu score ───────────────────────────────────────────
        val antutuPts = when {
            s.antutuScore >= 1_500_000 -> 5   // true flagship (8 Gen 3, D9300+)
            s.antutuScore >= 1_000_000 -> 4   // upper-mid flagship (7+ Gen 3, D8300)
            s.antutuScore >=   600_000 -> 3   // solid mid-range
            s.antutuScore >=   350_000 -> 2   // lower-mid
            s.antutuScore >=   200_000 -> 1   // entry
            else                       -> 0
        }

        val total = cpuScore + gpuScore + ramPts + antutuPts

        Log.d(TAG, buildString {
            append("cores=${s.cpu.coreCount} freq=${s.cpu.bigCoreFreq}MHz ")
            append("ARMv${s.cpu.armVersion} clusters=${s.cpu.clusterCount} ")
            append("vulkan=${s.gpu.vulkanTier} openCL=${s.gpu.hasOpenCL} ")
            append("ram=%.1fGB ".format(s.ramGb))
            append("antutu=${s.antutuScore} ")
            append("→ CPU=$cpuScore GPU=$gpuScore RAM=$ramPts NTU=$antutuPts total=$total/24")
        })

        return when {
            total >= 19 -> DeviceTier.FLAGSHIP
            total >= 14 -> DeviceTier.HIGH_MID
            total >=  9 -> DeviceTier.MID
            total >=  5 -> DeviceTier.LOW_MID
            else        -> DeviceTier.LOW
        }
    }
}
