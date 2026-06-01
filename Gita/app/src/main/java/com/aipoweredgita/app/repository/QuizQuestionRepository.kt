package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.QuestionPerformance
import com.aipoweredgita.app.database.QuestionPerformanceDao
import com.aipoweredgita.app.database.QuizQuestionBank
import com.aipoweredgita.app.database.QuizQuestionBankDao
import kotlinx.coroutines.flow.Flow

class QuizQuestionRepository(
    private val questionBankDao: QuizQuestionBankDao,
    private val questionPerformanceDao: QuestionPerformanceDao
) {
    suspend fun insert(q: QuizQuestionBank) = questionBankDao.insert(q)
    suspend fun countByHash(hash: String): Int = questionBankDao.countByHash(hash)
    suspend fun getTotalAvailableQuestions(): Int = questionBankDao.getTotalAvailableQuestions()
    suspend fun getTotalCount(): Int = questionBankDao.getTotalCount()
    suspend fun getNextQuestions(minDiff: Int, maxDiff: Int, limit: Int): List<QuizQuestionBank> =
        questionBankDao.getNextQuestions(minDiff, maxDiff, limit)
    suspend fun insertPerformance(p: QuestionPerformance) = questionPerformanceDao.insert(p)
    suspend fun getPerformanceByQuestion(id: String): QuestionPerformance? = questionPerformanceDao.getPerformanceByQuestion(id)
    fun getWeakTopics(topic: String): Flow<List<QuestionPerformance>> = questionPerformanceDao.getWeakTopics(topic)
}
