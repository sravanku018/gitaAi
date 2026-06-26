package com.aipoweredgita.app.database
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface RecommendationDataDao {
    @Insert
    suspend fun insert(recommendation: RecommendationData)
    @Query("SELECT * FROM recommendation_data WHERE status = 'pending' ORDER BY priority DESC")
    fun getPendingRecommendations(): Flow<List<RecommendationData>>
    @Query("SELECT * FROM recommendation_data WHERE isActive = 1 AND status != 'dismissed'")
    fun getActiveRecommendations(): Flow<List<RecommendationData>>
    @Query("UPDATE recommendation_data SET status = 'viewed' WHERE id = :id")
    suspend fun markAsViewed(id: Int)

    @Query("UPDATE recommendation_data SET status = 'dismissed' WHERE id = :id")
    suspend fun dismiss(id: Int)
}
