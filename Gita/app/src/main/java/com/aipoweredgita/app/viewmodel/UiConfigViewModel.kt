package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import com.aipoweredgita.app.domain.model.UiConfigEvent
import com.aipoweredgita.app.domain.model.UiConfigUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class UiConfigViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(UiConfigUiState())
    val uiState: StateFlow<UiConfigUiState> = _uiState.asStateFlow()

    // Keep legacy state alias
    val state: StateFlow<UiConfigUiState> = uiState

    fun onEvent(event: UiConfigEvent) {
        when (event) {
            is UiConfigEvent.UpdateFromSize -> updateFromSize(event.widthDp, event.heightDp)
        }
    }

    private fun updateFromSize(widthDp: Int, heightDp: Int) {
        val landscape = widthDp > heightDp
        val columns = if (landscape) 10 else 7
        _uiState.update { it.copy(isLandscape = landscape, gridColumns = columns) }
    }
}

