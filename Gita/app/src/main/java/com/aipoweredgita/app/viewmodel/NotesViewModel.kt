package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.VerseNote
import com.aipoweredgita.app.database.VerseNoteDao
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.utils.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteDao: VerseNoteDao,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val authPrefs = AuthPreferences.getInstance(context)

    val notes: StateFlow<List<VerseNote>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncNotesFromServer()
    }

    private fun syncNotesFromServer() {
        viewModelScope.launch {
            val uid = authPrefs.userId
            if (!authPrefs.isGuestUser && !uid.isNullOrEmpty()) {
                try {
                    val serverNotes = CoinApi.retrofitService.getNotes(uid)
                    for (sn in serverNotes) {
                        val existing = noteDao.getNote(sn.chapter_no, sn.verse_no)
                        if (existing == null) {
                            noteDao.insertNote(VerseNote(chapterNo = sn.chapter_no, verseNo = sn.verse_no, note = sn.note))
                        } else if (existing.note != sn.note) {
                            noteDao.updateNote(existing.copy(note = sn.note, updatedAt = System.currentTimeMillis()))
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun addNote(chapter: Int, verse: Int, text: String) {
        viewModelScope.launch {
            noteDao.insertNote(VerseNote(chapterNo = chapter, verseNo = verse, note = text))
            val uid = authPrefs.userId
            if (!authPrefs.isGuestUser && !uid.isNullOrEmpty()) {
                try {
                    val pendingDao = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context).pendingSyncEventDao()
                    val payloadStr = com.google.gson.Gson().toJson(mapOf("chapter" to chapter, "verse" to verse, "note" to text))
                    pendingDao.insert(
                        com.aipoweredgita.app.database.PendingSyncEvent(
                            userId = uid,
                            eventType = "ADD_NOTE",
                            payload = payloadStr,
                            coinsToAdjust = 0,
                            idempotencyKey = "note_${chapter}_${verse}_${uid}_${System.currentTimeMillis()}"
                        )
                    )
                    com.aipoweredgita.app.services.SyncWorker.schedule(context)
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteNote(noteId: Int, chapterNo: Int, verseNo: Int) {
        viewModelScope.launch {
            noteDao.deleteNote(noteId)
            val uid = authPrefs.userId
            if (!authPrefs.isGuestUser && !uid.isNullOrEmpty()) {
                try {
                    val pendingDao = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context).pendingSyncEventDao()
                    val payloadStr = com.google.gson.Gson().toJson(mapOf("chapter" to chapterNo, "verse" to verseNo))
                    pendingDao.insert(
                        com.aipoweredgita.app.database.PendingSyncEvent(
                            userId = uid,
                            eventType = "DELETE_NOTE",
                            payload = payloadStr,
                            coinsToAdjust = 0,
                            idempotencyKey = "delnote_${chapterNo}_${verseNo}_${uid}_${System.currentTimeMillis()}"
                        )
                    )
                    com.aipoweredgita.app.services.SyncWorker.schedule(context)
                } catch (_: Exception) {}
            }
        }
    }
}
