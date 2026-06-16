package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.Note
import com.aipoweredgita.app.database.NoteDao
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    suspend fun insertNote(note: Note): Long = dao.insertNote(note)
    suspend fun updateNote(note: Note) = dao.updateNote(note)
    suspend fun deleteNote(note: Note) = dao.deleteNote(note)
    fun getNotesForVerse(chapter: Int, verse: Int): Flow<List<Note>> = dao.getNotesForVerse(chapter, verse)
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()
}
