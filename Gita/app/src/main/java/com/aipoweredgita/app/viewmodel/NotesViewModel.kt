package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.VerseNote
import com.aipoweredgita.app.database.VerseNoteDao
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.NoteDeleteRequest
import com.aipoweredgita.app.network.NoteSyncItem
import com.aipoweredgita.app.network.NotesSyncRequest
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

    val notes: StateFlow<List<VerseNote>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncNotesFromServer()
    }

    private fun syncNotesFromServer() {
        viewModelScope.launch {
            if (!AuthPreferences.getInstance(context).isGuestUser && !AuthPreferences.getInstance(context).userId.isNullOrEmpty()) {
                try {
                    val serverNotes = CoinApi.retrofitService.getNotes(AuthPreferences.getInstance(context).userId!!)
                    for (sn in serverNotes) {
                        val existing = noteDao.getNote(sn.chapter_no, sn.verse_no)
                        if (existing == null) {
                            noteDao.insertNote(VerseNote(chapterNo = sn.chapter_no, verseNo = sn.verse_no, note = sn.note))
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun addNote(chapter: Int, verse: Int, text: String) {
        viewModelScope.launch {
            noteDao.insertNote(VerseNote(chapterNo = chapter, verseNo = verse, note = text))
            if (!AuthPreferences.getInstance(context).isGuestUser && !AuthPreferences.getInstance(context).userId.isNullOrEmpty()) {
                try {
                    CoinApi.retrofitService.syncNotes(
                        NotesSyncRequest(AuthPreferences.getInstance(context).userId!!, listOf(NoteSyncItem(chapter, verse, text)))
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteNote(noteId: Int, chapterNo: Int, verseNo: Int) {
        viewModelScope.launch {
            noteDao.deleteNote(noteId)
            if (!AuthPreferences.getInstance(context).isGuestUser && !AuthPreferences.getInstance(context).userId.isNullOrEmpty()) {
                try {
                    CoinApi.retrofitService.deleteNote(NoteDeleteRequest(AuthPreferences.getInstance(context).userId!!, chapterNo, verseNo))
                } catch (_: Exception) {}
            }
        }
    }
}
