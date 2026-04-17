package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiConfigState(
    val isLandscape: Boolean = false,
    val gridColumns: Int = 7,
)

class UiConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UiConfigState())
    val state: StateFlow<UiConfigState> = _state.asStateFlow()

    fun updateFromSize(widthDp: Int, heightDp: Int) {
        val landscape = widthDp > heightDp
        val columns = if (landscape) 10 else 7
        _state.value = UiConfigState(isLandscape = landscape, gridColumns = columns)
    }
}

