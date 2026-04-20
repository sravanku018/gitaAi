package com.aipoweredgita.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class ThemePreferences(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val ACCENT_KEY = stringPreferencesKey("accent")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences -> 
            val modeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeString)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }

    val isDynamicColor: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DYNAMIC_COLOR_KEY] ?: true }

    val accent: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[ACCENT_KEY] ?: "Sacred" }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun setAccent(name: String) {
        context.dataStore.edit { preferences -> preferences[ACCENT_KEY] = name }
    }
}
