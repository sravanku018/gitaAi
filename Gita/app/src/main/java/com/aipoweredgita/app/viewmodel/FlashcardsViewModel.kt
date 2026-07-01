package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.Flashcard
import com.aipoweredgita.app.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val topic: String = savedStateHandle.get<String>("topic") ?: ""

    val cards: StateFlow<List<Flashcard>> = flashcardRepository.getByTopic(topic)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCard(card: Flashcard) {
        viewModelScope.launch(Dispatchers.IO) {
            flashcardRepository.update(card)
        }
    }
}
