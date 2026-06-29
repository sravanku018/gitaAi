package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.SpacedRepetitionDao
import com.aipoweredgita.app.database.SpacedRepetitionItem
import kotlinx.coroutines.flow.Flow

class SpacedRepetitionRepository(private val dao: SpacedRepetitionDao) {
    suspend fun insertItem(item: SpacedRepetitionItem): Long = dao.insertItem(item)
    suspend fun updateItem(item: SpacedRepetitionItem) = dao.updateItem(item)
    suspend fun deleteItem(id: Int) = dao.deleteItem(id)
    fun getDueItems(): Flow<List<SpacedRepetitionItem>> = dao.getDueItems()
    fun getAllActiveItems(): Flow<List<SpacedRepetitionItem>> = dao.getAllActiveItems()

    suspend fun reviewCard(item: SpacedRepetitionItem, quality: Int) {
        // SM-2 Algorithm
        // quality: 0-5 (0=complete blackout, 5=perfect response)
        var easeFactor = item.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        if (easeFactor < 1.3f) easeFactor = 1.3f

        var repetition = item.repetition
        var interval = item.interval

        if (quality < 3) {
            repetition = 0
            interval = 1
        } else {
            repetition += 1
            interval = when (repetition) {
                1 -> 1
                2 -> 6
                else -> (interval * easeFactor).toInt()
            }
        }

        val now = System.currentTimeMillis()
        val nextReviewAt = now + (interval * 24L * 60L * 60L * 1000L)

        val updatedItem = item.copy(
            easeFactor = easeFactor,
            interval = interval,
            repetition = repetition,
            lastReviewedAt = now,
            nextReviewAt = nextReviewAt,
            quality = quality
        )
        dao.updateItem(updatedItem)
    }
}
