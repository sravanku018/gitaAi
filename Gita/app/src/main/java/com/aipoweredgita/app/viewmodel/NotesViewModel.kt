package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.VerseNote
import com.aipoweredgita.app.database.VerseNoteDao
import com.aipoweredgita.app.repository.NotesServerSync
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
        refreshFromServer()
    }

    /** Re-pull notes from server (call after login or when opening Notes). */
    fun refreshFromServer() {
        viewModelScope.launch {
            NotesServerSync.pullFromServer(context)
        }
    }

    fun addNote(chapter: Int, verse: Int, text: String, colorHex: String = "") {
        viewModelScope.launch {
            noteDao.insertNote(VerseNote(chapterNo = chapter, verseNo = verse, note = text, colorHex = colorHex))
            val uid = authPrefs.userId
            if (!uid.isNullOrEmpty()) {
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

    fun updateNote(noteId: Int, chapter: Int, verse: Int, text: String, colorHex: String) {
        viewModelScope.launch {
            noteDao.insertNote(VerseNote(id = noteId, chapterNo = chapter, verseNo = verse, note = text, colorHex = colorHex, updatedAt = System.currentTimeMillis()))
            val uid = authPrefs.userId
            if (!uid.isNullOrEmpty()) {
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
            if (!uid.isNullOrEmpty()) {
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

    fun restoreStreak() {
        viewModelScope.launch {
            try {
                val statsDao = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context).userStatsDao()
                val currentStats = statsDao.getUserStatsOnce()
                if (currentStats != null) {
                    val targetStreak = if (currentStats.longestStreak > 0) currentStats.longestStreak else 1
                    statsDao.updateCurrentStreak(targetStreak)
                    val todayStr = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toString()
                    statsDao.updateLastActive(System.currentTimeMillis(), todayStr)
                }
            } catch (_: Exception) {}
        }
    }
}
