package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpacedRepetitionDao {
    @Query("SELECT * FROM spaced_repetition_items WHERE isActive = 1 ORDER BY nextReviewAt ASC")
    fun getDueItems(): Flow<List<SpacedRepetitionItem>>

    @Query("SELECT * FROM spaced_repetition_items WHERE chapter = :chapter AND verse = :verse AND isActive = 1")
    suspend fun getItem(chapter: Int, verse: Int): SpacedRepetitionItem?

    @Query("SELECT * FROM spaced_repetition_items WHERE isActive = 1 ORDER BY nextReviewAt ASC")
    fun getAllActiveItems(): Flow<List<SpacedRepetitionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SpacedRepetitionItem): Long

    @Update
    suspend fun updateItem(item: SpacedRepetitionItem)

    @Query("UPDATE spaced_repetition_items SET lastReviewedAt = :timestamp, nextReviewAt = :nextReview, interval = :interval, repetition = :repetition, quality = :quality, easeFactor = :easeFactor WHERE id = :id")
    suspend fun reviewItem(
        id: Int,
        timestamp: Long,
        nextReview: Long,
        interval: Int,
        repetition: Int,
        quality: Int,
        easeFactor: Float
    )

    @Query("DELETE FROM spaced_repetition_items WHERE id = :id")
    suspend fun deleteItem(id: Int)

    @Query("DELETE FROM spaced_repetition_items WHERE isActive = 0")
    suspend fun deleteInactiveItems()

    @Query("UPDATE spaced_repetition_items SET isActive = 0 WHERE id = :id")
    suspend fun deactivateItem(id: Int)

    @Query("SELECT COUNT(*) FROM spaced_repetition_items WHERE isActive = 1")
    suspend fun getActiveItemCount(): Int

    @Query("SELECT COUNT(*) FROM spaced_repetition_items WHERE nextReviewAt <= :timestamp AND isActive = 1")
    suspend fun getDueItemCount(timestamp: Long): Int
}
