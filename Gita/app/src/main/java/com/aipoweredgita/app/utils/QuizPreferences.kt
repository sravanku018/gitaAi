package com.aipoweredgita.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.quizDataStore: DataStore<Preferences> by preferencesDataStore(name = "quiz_preferences")

class QuizPreferences(private val context: Context) {

    companion object {
        private val QUIZ_SCORE_KEY = intPreferencesKey("quiz_score")
        private val QUIZ_TOTAL_QUESTIONS_KEY = intPreferencesKey("quiz_total_questions")
        private val QUIZ_MAX_QUESTIONS_KEY = intPreferencesKey("quiz_max_questions")
        private val QUIZ_START_TIME_KEY = longPreferencesKey("quiz_start_time")
        private val USED_QUESTIONS_KEY = stringPreferencesKey("used_questions")
    }

    val quizState: Flow<SavedQuizState?> = context.quizDataStore.data
        .map { preferences ->
            val totalQuestions = preferences[QUIZ_TOTAL_QUESTIONS_KEY] ?: 0
            if (totalQuestions > 0) {
                SavedQuizState(
                    score = preferences[QUIZ_SCORE_KEY] ?: 0,
                    totalQuestions = totalQuestions,
                    maxQuestions = preferences[QUIZ_MAX_QUESTIONS_KEY] ?: 10,
                    startTime = preferences[QUIZ_START_TIME_KEY] ?: 0L,
                    usedQuestions = preferences[USED_QUESTIONS_KEY]?.split(",")?.toSet() ?: emptySet()
                )
            } else {
                null
            }
        }

    suspend fun saveQuizState(
        score: Int,
        totalQuestions: Int,
        maxQuestions: Int,
        startTime: Long,
        usedQuestions: Set<String>
    ) {
        context.quizDataStore.edit { preferences ->
            preferences[QUIZ_SCORE_KEY] = score
            preferences[QUIZ_TOTAL_QUESTIONS_KEY] = totalQuestions
            preferences[QUIZ_MAX_QUESTIONS_KEY] = maxQuestions
            preferences[QUIZ_START_TIME_KEY] = startTime
            preferences[USED_QUESTIONS_KEY] = usedQuestions.joinToString(",")
        }
    }

    suspend fun clearQuizState() {
        context.quizDataStore.edit { preferences ->
            preferences.remove(QUIZ_SCORE_KEY)
            preferences.remove(QUIZ_TOTAL_QUESTIONS_KEY)
            preferences.remove(QUIZ_MAX_QUESTIONS_KEY)
            preferences.remove(QUIZ_START_TIME_KEY)
            preferences.remove(USED_QUESTIONS_KEY)
        }
    }
}

data class SavedQuizState(
    val score: Int,
    val totalQuestions: Int,
    val maxQuestions: Int,
    val startTime: Long,
    val usedQuestions: Set<String>
)
