package com.aipoweredgita.app.database
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface LearningInsightsDao {
    @Insert
    suspend fun insert(insight: LearningInsights)
    @Query("SELECT * FROM learning_insights WHERE isViewed = 0 ORDER BY createdAt DESC")
    fun getUnviewedInsights(): Flow<List<LearningInsights>>
    @Query("SELECT * FROM learning_insights WHERE isDismissed = 0 ORDER BY impactScore DESC")
    fun getActiveInsights(): Flow<List<LearningInsights>>
}
