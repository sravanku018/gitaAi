package com.aipoweredgita.app.ml

/**
 * Singleton that provides shared download progress state across all screens.
 * Tracks whether ML models have been downloaded and are ready for use.
 */
object ModelStateManager {
    private var _modelsReady: Boolean = false

    val modelsReady: Boolean get() = _modelsReady

    fun setModelsReady(ready: Boolean) {
        _modelsReady = ready
    }
}
