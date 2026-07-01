package com.aipoweredgita.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.offlineDataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_preferences")

class OfflinePreferences(private val context: Context) {

    companion object {
        private val ALL_DOWNLOADED_NOTIFIED = booleanPreferencesKey("all_downloaded_notified")
    }

    val isAllDownloadedNotified: Flow<Boolean> = context.offlineDataStore.data
        .map { prefs -> prefs[ALL_DOWNLOADED_NOTIFIED] ?: false }

    suspend fun setAllDownloadedNotified(value: Boolean) {
        context.offlineDataStore.edit { prefs ->
            prefs[ALL_DOWNLOADED_NOTIFIED] = value
        }
    }

    suspend fun resetAllDownloadedNotified() {
        setAllDownloadedNotified(false)
    }
}

