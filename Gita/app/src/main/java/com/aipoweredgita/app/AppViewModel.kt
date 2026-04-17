package com.aipoweredgita.app

import android.content.res.Configuration
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped ViewModel to track device orientation across the whole app.
 * Share this via Activity (e.g., activityViewModels) or CompositionLocal in Compose screens.
 */
class AppViewModel : ViewModel() {
    private val _orientation = MutableStateFlow(Configuration.ORIENTATION_UNDEFINED)
    val orientation: StateFlow<Int> = _orientation.asStateFlow()

    fun setOrientation(orientation: Int) {
        _orientation.value = orientation
    }
}

