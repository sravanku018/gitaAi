package com.aipoweredgita.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimeTracker(
    private val onTimeUpdate: suspend (seconds: Long) -> Unit
) {
    private var startTime: Long = 0
    private var totalSeconds: Long = 0
    private var trackingJob: Job? = null

    fun start() {
        if (trackingJob?.isActive == true) return

        startTime = System.currentTimeMillis()

        trackingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(10000) // Update every 10 seconds
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                if (elapsed >= 10) {
                    totalSeconds += elapsed
                    onTimeUpdate(elapsed)
                    startTime = System.currentTimeMillis()
                }
            }
        }
    }

    fun stop() {
        trackingJob?.cancel()
        trackingJob = null

        // Save any remaining time
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        if (elapsed > 0) {
            totalSeconds += elapsed
            CoroutineScope(Dispatchers.Default).launch {
                onTimeUpdate(elapsed)
            }
        }
    }

    fun getTotalSeconds(): Long = totalSeconds
}
