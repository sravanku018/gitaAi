package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.Note
import com.aipoweredgita.app.database.NoteDao
import kotlinx.coroutines.flow.Flow

class NotesRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes()
    }

    fun getNotesForVerse(chapter: Int, verse: Int): Flow<List<Note>> {
        return noteDao.getNotesForVerse(chapter, verse)
    }

    fun getNotesByTag(tag: String): Flow<List<Note>> {
        return noteDao.getNotesByTag(tag)
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query)
    }

    fun getNotesByMood(mood: String): Flow<List<Note>> {
        return noteDao.getNotesByMood(mood)
    }

    suspend fun addNote(
        chapter: Int,
        verse: Int,
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        mood: String? = null,
        isPrivate: Boolean = true
    ): Long {
        val tagsString = tags.joinToString(",")
        val note = Note(
            chapter = chapter,
            verse = verse,
            noteTitle = title,
            noteContent = content,
            tags = tagsString,
            mood = mood,
            isPrivate = isPrivate
        )
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        noteDao.updateNote(updatedNote)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        noteDao.deleteNoteById(id)
    }

    suspend fun clearAllNotes() {
        noteDao.deleteAllNotes()
    }

    suspend fun getNoteCount(): Int {
        return noteDao.getNoteCount()
    }

    suspend fun hasNoteForVerse(chapter: Int, verse: Int): Boolean {
        return noteDao.getNotesForVerse(chapter, verse).let { flow ->
            var hasNote = false
            flow.collect { notes ->
                hasNote = notes.isNotEmpty()
            }
            hasNote
        }
    }
}
