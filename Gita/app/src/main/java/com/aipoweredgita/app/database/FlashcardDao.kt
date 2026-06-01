package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
<<<<<<< HEAD
import androidx.room.Update
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert
    suspend fun insert(card: Flashcard)

<<<<<<< HEAD
    @Update
    suspend fun update(card: Flashcard)

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    @Query("SELECT * FROM flashcards WHERE topic = :topic ORDER BY createdAt DESC")
    fun getByTopic(topic: String): Flow<List<Flashcard>>
}

