package com.aipoweredgita.app.ml

<<<<<<< HEAD
/**
 * Singleton that provides shared download progress state across all screens.
 * Tracks whether ML models have been downloaded and are ready for use.
 */
object ModelStateManager {
    private var _modelsReady: Boolean = false

    val modelsReady: Boolean get() = _modelsReady

    fun setModelsReady(ready: Boolean) {
        _modelsReady = ready
=======
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

object ModelStateManager {
    private val _modelsReady = mutableStateOf(false)
    val modelsReady: State<Boolean> = _modelsReady

    fun setModelsReady(ready: Boolean) {
        _modelsReady.value = ready
    }

    fun areModelsReady(): Boolean {
        return _modelsReady.value
    }

    fun resetModelsStatus() {
        _modelsReady.value = false
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    }
}
