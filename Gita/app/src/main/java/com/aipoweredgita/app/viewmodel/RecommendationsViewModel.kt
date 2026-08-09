package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.database.RecommendationDataDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val recommendationDao: RecommendationDataDao
) : ViewModel() {

    val activeRecommendations: StateFlow<List<RecommendationData>> = recommendationDao
        .getActiveRecommendations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun dismissRecommendation(id: Int) {
        viewModelScope.launch {
            try {
                recommendationDao.dismiss(id)
            } catch (e: Exception) {
                android.util.Log.e("RecommendationsVM", "Failed to dismiss recommendation $id", e)
            }
        }
    }
}
