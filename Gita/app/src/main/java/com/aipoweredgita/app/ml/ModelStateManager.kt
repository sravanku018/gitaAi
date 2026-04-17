package com.aipoweredgita.app.ml

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
    }
}
