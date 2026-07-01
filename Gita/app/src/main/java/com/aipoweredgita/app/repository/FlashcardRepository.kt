package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.Flashcard
import com.aipoweredgita.app.database.FlashcardDao
import kotlinx.coroutines.flow.Flow

class FlashcardRepository(private val dao: FlashcardDao) {
    suspend fun insert(card: Flashcard) = dao.insert(card)
    suspend fun update(card: Flashcard) = dao.update(card)
    fun getByTopic(topic: String): Flow<List<Flashcard>> = dao.getByTopic(topic)
}
