package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert
    suspend fun insert(card: Flashcard)

    @Update
    suspend fun update(card: Flashcard)

    @Query("SELECT * FROM flashcards WHERE topic = :topic ORDER BY createdAt DESC")
    fun getByTopic(topic: String): Flow<List<Flashcard>>
}

