package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.FavoriteVerse
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesState(
    val favorites: List<FavoriteVerse> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val favoriteCount: Int = 0
)

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    private val favoriteRepository: FavoriteRepository

    init {
        val database = GitaDatabase.getDatabase(application)
        favoriteRepository = FavoriteRepository(database.favoriteVerseDao())
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            favoriteRepository.allFavorites.collect { favorites ->
                _state.value = _state.value.copy(
                    favorites = favorites,
                    isLoading = false,
                    favoriteCount = favorites.size
                )
            }
        }
    }

    fun deleteFavorite(chapter: Int, verse: Int) {
        viewModelScope.launch {
            val result = favoriteRepository.removeFavorite(chapter, verse)
            result.onSuccess { message ->
                _state.value = _state.value.copy(message = message)
                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(message = null)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    message = error.message ?: "Failed to delete"
                )
                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(message = null)
            }
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            val result = favoriteRepository.clearAllFavorites()
            result.onSuccess { message ->
                _state.value = _state.value.copy(message = message)
                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(message = null)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    message = error.message ?: "Failed to clear favorites"
                )
                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(message = null)
            }
        }
    }
}
