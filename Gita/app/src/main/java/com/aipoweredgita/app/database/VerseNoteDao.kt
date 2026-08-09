package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "verse_notes")
data class VerseNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chapterNo: Int,
    val verseNo: Int,
    val note: String,
    val colorHex: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface VerseNoteDao {
    @Query("SELECT * FROM verse_notes WHERE chapterNo = :chapter AND verseNo = :verse LIMIT 1")
    suspend fun getNote(chapter: Int, verse: Int): VerseNote?

    @Query("SELECT * FROM verse_notes WHERE chapterNo = :chapter AND verseNo = :verse LIMIT 1")
    fun getNoteFlow(chapter: Int, verse: Int): Flow<VerseNote?>

    @Query("SELECT * FROM verse_notes ORDER BY chapterNo ASC, verseNo ASC")
    fun getAllNotes(): Flow<List<VerseNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VerseNote)

    @Update
    suspend fun updateNote(note: VerseNote)

    @Query("DELETE FROM verse_notes WHERE id = :id")
    suspend fun deleteNote(id: Int)

    @Query("SELECT COUNT(*) FROM verse_notes")
    suspend fun getNoteCount(): Int
}
