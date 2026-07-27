package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.data.QuizQuestion
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class MahabharataSequenceQuestion(
    @SerializedName("question") val question: String = "",
    @SerializedName("options") val options: List<String> = emptyList(),
    @SerializedName("answer_index") val answerIndex: Int = 0,
    @SerializedName("category") val category: String = "",
    @SerializedName("difficulty") val difficulty: String = "medium"
)

@Singleton
class MahabharataSequenceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val TAG = "MahabharataSeqRepo"
    private var englishQuestions: List<MahabharataSequenceQuestion>? = null
    private var teluguQuestions: List<MahabharataSequenceQuestion>? = null
    private val gson = Gson()

    suspend fun getQuestions(language: String = "english"): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val isTelugu = language.equals("telugu", ignoreCase = true) || language.equals("te", ignoreCase = true)
        val rawList = if (isTelugu) {
            if (teluguQuestions == null) {
                teluguQuestions = loadFromAsset("mahabharata_sequence_mcq_telugu.json")
            }
            teluguQuestions ?: emptyList()
        } else {
            if (englishQuestions == null) {
                englishQuestions = loadFromAsset("mahabharata_sequence_mcq.json")
            }
            englishQuestions ?: emptyList()
        }

        rawList.mapIndexed { index, item ->
            QuizQuestion(
                verse = GitaVerse(chapterNo = 0, verseNo = index + 1),
                question = item.question,
                options = item.options,
                correctAnswerIndex = item.answerIndex.coerceIn(0, (item.options.size - 1).coerceAtLeast(0)),
                explanation = if (isTelugu) "మహాభారత కాలక్రమ వరుస శ్లోక ప్రశ్న" else "Mahabharata chronological sequence question"
            )
        }
    }

    private fun loadFromAsset(filename: String): List<MahabharataSequenceQuestion> {
        return try {
            val jsonString = context.assets.open(filename).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val type = object : TypeToken<List<MahabharataSequenceQuestion>>() {}.type
            gson.fromJson<List<MahabharataSequenceQuestion>>(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $filename from assets: ${e.message}")
            emptyList()
        }
    }
}
