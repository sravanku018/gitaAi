package com.aipoweredgita.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimeTracker(
    private val scope: CoroutineScope,
    private val onTimeUpdate: suspend (seconds: Long) -> Unit
) {
    private var startTime: Long = 0
    private var totalSeconds: Long = 0
    private var trackingJob: Job? = null

    @Synchronized
    fun start() {
        if (trackingJob?.isActive == true) return

        startTime = System.currentTimeMillis()

        trackingJob = scope.launch {
            while (isActive) {
                delay(10000) // Update every 10 seconds
                val elapsedToReport = synchronized(this@TimeTracker) {
                    if (startTime == 0L) return@synchronized 0L
                    val now = System.currentTimeMillis()
                    val diff = (now - startTime) / 1000
                    if (diff >= 10) {
                        totalSeconds += diff
                        startTime = now
                        diff
                    } else 0L
                }
                if (elapsedToReport >= 10) {
                    onTimeUpdate(elapsedToReport)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        trackingJob?.cancel()
        trackingJob = null

        // Guard: if start() was never called, startTime is 0 — don't write epoch-scale garbage
        if (startTime == 0L) return

        // Save any remaining time since last periodic update
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        startTime = 0L // Reset so repeated stop() calls don't double-count
        if (elapsed > 0) {
            totalSeconds += elapsed
            scope.launch {
                onTimeUpdate(elapsed)
            }
        }
    }

    @Synchronized
    fun getTotalSeconds(): Long = totalSeconds
}
