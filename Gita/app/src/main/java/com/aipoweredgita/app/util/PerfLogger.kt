package com.aipoweredgita.app.util

import android.util.Log

object PerfLogger {
    fun start(): Long = System.nanoTime()

    fun endAndLog(tag: String, op: String, startNs: Long) {
        if (!FeatureFlags.ENABLE_PERF_METRICS) return
        val durMs = (System.nanoTime() - startNs) / 1_000_000.0
        Log.d(tag, "perf:$op ${String.format("%.2f", durMs)}ms")
    }
}

