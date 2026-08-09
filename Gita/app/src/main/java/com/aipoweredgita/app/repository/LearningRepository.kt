package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.LearningInsights
import com.aipoweredgita.app.database.LearningInsightsDao
import com.aipoweredgita.app.database.LearningPattern
import com.aipoweredgita.app.database.LearningPatternDao
import com.aipoweredgita.app.database.LearningStyle
import com.aipoweredgita.app.database.LearningStyleDao
import kotlinx.coroutines.flow.Flow

class LearningRepository(
    private val insightsDao: LearningInsightsDao,
    private val patternDao: LearningPatternDao,
    private val styleDao: LearningStyleDao
) {
    suspend fun insertInsight(i: LearningInsights) = insightsDao.insert(i)
    fun getActiveInsights(): Flow<List<LearningInsights>> = insightsDao.getActiveInsights()
    suspend fun insertPattern(p: LearningPattern) = patternDao.insert(p)
    fun getAllPatterns(): Flow<List<LearningPattern>> = patternDao.getAllPatterns()
    fun getLearningStyle(): Flow<LearningStyle> = styleDao.getLearningStyle()
    suspend fun insertLearningStyle(s: LearningStyle) = styleDao.insertLearningStyle(s)
}
