package com.aipoweredgita.app.services

import com.aipoweredgita.app.database.SpacedRepetitionItem
import com.aipoweredgita.app.database.SpacedRepetitionDao
import kotlinx.coroutines.flow.Flow

class SpacedRepetitionManager(private val dao: SpacedRepetitionDao) {

    fun getDueItems(): Flow<List<SpacedRepetitionItem>> {
        return dao.getDueItems()
    }

    fun getAllActiveItems(): Flow<List<SpacedRepetitionItem>> {
        return dao.getAllActiveItems()
    }

    suspend fun addItem(
        chapter: Int,
        verse: Int,
        easeFactor: Float = 2.5f
    ): Long {
        val now = System.currentTimeMillis()
        val item = SpacedRepetitionItem(
            chapter = chapter,
            verse = verse,
            easeFactor = easeFactor,
            lastReviewedAt = now,
            nextReviewAt = now
        )
        return dao.insertItem(item)
    }

    suspend fun reviewItem(
        itemId: Int,
        quality: Int // 0-5 (0=forgot, 5=perfect)
    ) {
        val now = System.currentTimeMillis()

        // Get current item
        val item = dao.getAllActiveItems().let { flow ->
            var currentItem: SpacedRepetitionItem? = null
            flow.collect { items ->
                currentItem = items.find { it.id == itemId }
            }
            currentItem
        } ?: return

        // Update using SM-2 algorithm
        val newEaseFactor = calculateNewEaseFactor(item.easeFactor, quality)
        val newInterval = calculateNewInterval(item, quality)
        val newRepetition = if (quality >= 3) item.repetition + 1 else 0
        val nextReview = now + (newInterval * 24 * 60 * 60 * 1000L) // Convert days to milliseconds

        dao.reviewItem(
            id = itemId,
            timestamp = now,
            nextReview = nextReview,
            interval = newInterval,
            repetition = newRepetition,
            quality = quality,
            easeFactor = newEaseFactor
        )
    }

    suspend fun getItem(chapter: Int, verse: Int): SpacedRepetitionItem? {
        return dao.getItem(chapter, verse)
    }

    suspend fun deactivateItem(itemId: Int) {
        dao.deactivateItem(itemId)
    }

    suspend fun deleteItem(itemId: Int) {
        dao.deleteItem(itemId)
    }

    suspend fun getActiveItemCount(): Int {
        return dao.getActiveItemCount()
    }

    suspend fun getDueItemCount(): Int {
        return dao.getDueItemCount(System.currentTimeMillis())
    }

    suspend fun cleanOldItems() {
        dao.deleteInactiveItems()
    }

    private fun calculateNewEaseFactor(easeFactor: Float, quality: Int): Float {
        // SM-2 algorithm for ease factor
        val newEase = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        return if (newEase < 1.3f) 1.3f else newEase as Float
    }

    private fun calculateNewInterval(item: SpacedRepetitionItem, quality: Int): Int {
        // SM-2 algorithm for interval
        return when {
            quality < 3 -> 1 // Reset if quality is low
            item.repetition == 0 -> 1
            item.repetition == 1 -> 6
            else -> (item.interval * easeFactorToMultiplier(item.easeFactor)).toInt()
        }
    }

    private fun easeFactorToMultiplier(easeFactor: Float): Float {
        // Convert ease factor to interval multiplier
        return when {
            easeFactor < 2.0 -> 1.2f
            easeFactor < 2.5 -> 1.5f
            easeFactor < 3.0 -> 2.0f
            else -> 2.5f
        }
    }

    companion object {
        fun getNextReviewDate(days: Int): Long {
            return System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L)
        }

        fun getReviewQualityLabel(quality: Int): String {
            return when (quality) {
                0 -> "Complete Blackout"
                1 -> "Incorrect; easy reminder"
                2 -> "Incorrect; difficult reminder"
                3 -> "Correct; difficult"
                4 -> "Correct; hesitant"
                5 -> "Correct; easy"
                else -> "Unknown"
            }
        }
    }
}
