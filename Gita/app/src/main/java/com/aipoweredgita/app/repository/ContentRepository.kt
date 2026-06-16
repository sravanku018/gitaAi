package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.database.RecommendationDataDao
import kotlinx.coroutines.flow.Flow

class ContentRepository(private val recommendationDataDao: RecommendationDataDao) {

    fun getActiveRecommendations(): Flow<List<RecommendationData>> = recommendationDataDao.getActiveRecommendations()
}
