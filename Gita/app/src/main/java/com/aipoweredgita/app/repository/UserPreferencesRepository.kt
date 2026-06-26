package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.UserPreferences
import com.aipoweredgita.app.database.UserPreferencesDao
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepository(private val dao: UserPreferencesDao) {
    suspend fun insert(prefs: UserPreferences) = dao.insert(prefs)
    suspend fun update(prefs: UserPreferences) = dao.update(prefs)
    fun getPreferences(userId: Int): Flow<UserPreferences?> = dao.getPreferences(userId)
}
