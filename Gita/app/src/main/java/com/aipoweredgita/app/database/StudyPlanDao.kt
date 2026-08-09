package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_plans")
data class StudyPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val durationDays: Int,
    val planType: String, // "karma_yoga", "bhakti_yoga", "quiz_challenge", "full_gita"
    val chapters: String, // comma-separated chapter numbers
    val currentDay: Int = 1,
    val isActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val serverUpdatedAt: String = ""
)

@Entity(tableName = "study_plan_progress")
data class StudyPlanProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val planId: Int,
    val day: Int,
    val chapterNo: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<StudyPlan?>

    @Query("SELECT * FROM study_plans ORDER BY startDate DESC")
    fun getAllPlans(): Flow<List<StudyPlan>>

    @Query("SELECT * FROM study_plan_progress WHERE planId = :planId ORDER BY day")
    fun getPlanProgress(planId: Int): Flow<List<StudyPlanProgress>>

    @Insert
    suspend fun insertPlan(plan: StudyPlan): Long

    @Insert
    suspend fun insertProgress(progress: StudyPlanProgress)

    @Update
    suspend fun updatePlan(plan: StudyPlan)

    @Query("UPDATE study_plan_progress SET isCompleted = 1, completedAt = :time WHERE planId = :planId AND day = :day")
    suspend fun markDayComplete(planId: Int, day: Int, time: Long)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deletePlan(id: Int)

    @Query("SELECT * FROM study_plan_progress WHERE planId = :planId ORDER BY day")
    suspend fun getPlanProgressOnce(planId: Int): List<StudyPlanProgress>

    @Query("UPDATE study_plans SET isActive = 0, completedAt = :time WHERE id = :planId")
    suspend fun completePlan(planId: Int, time: Long)
}

object StudyPlanTemplates {
    private val verseCounts = intArrayOf(0, 47, 72, 43, 42, 29, 47, 30, 28, 34, 42, 55, 20, 35, 27, 20, 24, 28, 78)

    fun karmaYoga14Day(): List<StudyPlanProgress> {
        val chapters = listOf(2, 3, 4, 18)
        return (1..14).map { day ->
            val chapter = chapters[(day - 1) % chapters.size]
            val maxVerse = verseCounts[chapter]
            val start = ((day - 1) / chapters.size) * 10 + 1
            StudyPlanProgress(
                planId = 0,
                day = day,
                chapterNo = chapter,
                verseStart = start,
                verseEnd = minOf(start + 9, maxVerse)
            )
        }
    }

    fun quizChallenge7Day(): List<StudyPlanProgress> {
        return (1..7).map { day ->
            StudyPlanProgress(planId = 0, day = day, chapterNo = -1, verseStart = 0, verseEnd = 0)
        }
    }

    fun fullGita18Day(): List<StudyPlanProgress> {
        return (1..18).map { day ->
            StudyPlanProgress(
                planId = 0,
                day = day,
                chapterNo = day,
                verseStart = 1,
                verseEnd = verseCounts[day]
            )
        }
    }
}
