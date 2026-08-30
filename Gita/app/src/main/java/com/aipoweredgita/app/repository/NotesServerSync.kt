package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.VerseNote
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.utils.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pull verse notes from the coin API (VPS HTTPS → localhost Postgres).
 * Requires a valid Bearer token — GET /notes is behind requireAuth.
 */
object NotesServerSync {
    private const val TAG = "NotesServerSync"

    suspend fun pullFromServer(context: Context): Int = withContext(Dispatchers.IO) {
        val authPrefs = AuthPreferences.getInstance(context)
        val uid = authPrefs.userId
        val token = authPrefs.token
        if (uid.isNullOrBlank() || token.isNullOrBlank()) {
            Log.w(TAG, "skip pull: missing userId or token (uid=${uid != null}, token=${!token.isNullOrBlank()})")
            return@withContext 0
        }

        try {
            val serverNotes = CoinApi.retrofitService.getNotes(
                userId = uid,
                token = "Bearer $token",
            )
            val dao = GitaDatabase.getDatabase(context).verseNoteDao()
            var applied = 0
            for (sn in serverNotes) {
                if (sn.chapter_no <= 0 || sn.verse_no <= 0) continue
                val text = sn.note.trim()
                if (text.isEmpty()) continue
                val existing = dao.getNote(sn.chapter_no, sn.verse_no)
                if (existing == null) {
                    dao.insertNote(
                        VerseNote(
                            chapterNo = sn.chapter_no,
                            verseNo = sn.verse_no,
                            note = text,
                        )
                    )
                    applied++
                } else if (existing.note != text) {
                    dao.updateNote(
                        existing.copy(
                            note = text,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    applied++
                }
            }
            Log.d(TAG, "pulled ${serverNotes.size} server notes, applied $applied for user=$uid")
            applied
        } catch (e: Exception) {
            Log.e(TAG, "pull failed for user=$uid: ${e.message}", e)
            0
        }
    }
}
