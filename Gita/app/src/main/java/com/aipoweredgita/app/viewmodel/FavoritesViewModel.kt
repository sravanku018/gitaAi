package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.domain.model.FavoritesEvent
import com.aipoweredgita.app.domain.model.FavoritesSideEffect
import com.aipoweredgita.app.domain.model.FavoritesUiState
import com.aipoweredgita.app.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<FavoritesSideEffect>()
    val sideEffect: SharedFlow<FavoritesSideEffect> = _sideEffect.asSharedFlow()

    // Keep legacy state alias for partial compatibility before UI migration
    val state: StateFlow<FavoritesUiState> = uiState

    init {
        loadFavorites()
    }

    fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.DeleteFavorite -> deleteFavorite(event.chapter, event.verse)
            is FavoritesEvent.ClearAllFavorites -> clearAllFavorites()
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            favoriteRepository.allFavorites.collect { favorites ->
                _uiState.update {
                    it.copy(
                        favorites = favorites,
                        isLoading = false,
                        favoriteCount = favorites.size
                    )
                }
            }
        }
    }

    private fun deleteFavorite(chapter: Int, verse: Int) {
        viewModelScope.launch {
            val result = favoriteRepository.removeFavorite(chapter, verse)
            result.onSuccess { message ->
                _sideEffect.emit(FavoritesSideEffect.ShowMessage(message))
            }.onFailure { error ->
                _sideEffect.emit(FavoritesSideEffect.ShowMessage(error.message ?: "Failed to delete"))
            }
        }
    }

    private fun clearAllFavorites() {
        viewModelScope.launch {
            val result = favoriteRepository.clearAllFavorites()
            result.onSuccess { message ->
                _sideEffect.emit(FavoritesSideEffect.ShowMessage(message))
            }.onFailure { error ->
                _sideEffect.emit(FavoritesSideEffect.ShowMessage(error.message ?: "Failed to clear favorites"))
            }
        }
    }
}
