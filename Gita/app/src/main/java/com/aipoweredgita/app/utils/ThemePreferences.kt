package com.aipoweredgita.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val ACCENT_KEY = androidx.datastore.preferences.core.stringPreferencesKey("accent")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_THEME_KEY] ?: false }

    val isDynamicColor: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DYNAMIC_COLOR_KEY] ?: true }

    val accent: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[ACCENT_KEY] ?: "Saffron" }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences -> preferences[DARK_THEME_KEY] = isDark }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun setAccent(name: String) {
        context.dataStore.edit { preferences -> preferences[ACCENT_KEY] = name }
    }
}
