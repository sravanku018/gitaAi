package com.aipoweredgita.app.utils

import kotlinx.coroutines.sync.Mutex

/**
 * Global turn manager for AI and Voice operations.
 * Ensures that only one LLM inference or TTS operation is active at a time
 * across the entire application, preventing thermal spikes and engine conflicts.
 */
object AiTurnManager {
    val mutex = Mutex()
}