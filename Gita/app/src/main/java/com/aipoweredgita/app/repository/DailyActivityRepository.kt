package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.DailyActivityDao
import kotlinx.coroutines.flow.Flow

class DailyActivityRepository(private val dao: DailyActivityDao) {

    fun getAllActivity(): Flow<List<DailyActivity>> = dao.getAllActivity()

    suspend fun getByDate(date: String): DailyActivity? = dao.getByDate(date)

    suspend fun getRecentActivity(limit: Int): List<DailyActivity> = dao.getRecentActivity(limit)

    suspend fun insertIfAbsent(activity: DailyActivity) = dao.insertIfAbsent(activity)

    suspend fun addNormalSeconds(date: String, seconds: Long) = dao.addNormalSeconds(date, seconds)

    suspend fun addQuizSeconds(date: String, seconds: Long) = dao.addQuizSeconds(date, seconds)

    suspend fun addVoiceStudioSeconds(date: String, seconds: Long) = dao.addVoiceStudioSeconds(date, seconds)

    suspend fun addVerses(date: String, count: Int) = dao.addVerses(date, count)
}
