package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.StudyPlan
import com.aipoweredgita.app.database.StudyPlanDao
import com.aipoweredgita.app.database.StudyPlanProgress
import com.aipoweredgita.app.database.StudyPlanTemplates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudyPlanViewModel @Inject constructor(
    private val planDao: StudyPlanDao
) : ViewModel() {

    val activePlan: Flow<StudyPlan?> = planDao.getActivePlan()
    val allPlans: Flow<List<StudyPlan>> = planDao.getAllPlans()

    fun createPlan(title: String, desc: String, days: Int, type: String, chapters: String) {
        viewModelScope.launch {
            val plan = StudyPlan(title = title, description = desc, durationDays = days, planType = type, chapters = chapters)
            val planId = planDao.insertPlan(plan)
            val templates = when (type) {
                "karma_yoga" -> StudyPlanTemplates.karmaYoga14Day()
                "quiz_challenge" -> StudyPlanTemplates.quizChallenge7Day()
                "full_gita" -> StudyPlanTemplates.fullGita18Day()
                else -> StudyPlanTemplates.fullGita18Day()
            }
            templates.take(days).forEach { progress ->
                planDao.insertProgress(progress.copy(planId = planId.toInt()))
            }
        }
    }

    fun getPlanProgress(planId: Int): Flow<List<StudyPlanProgress>> =
        planDao.getPlanProgress(planId)

    fun markDayComplete(planId: Int, day: Int) {
        viewModelScope.launch {
            planDao.markDayComplete(planId, day, System.currentTimeMillis())
            // Check if all days complete → mark plan done
            val progress = planDao.getPlanProgressOnce(planId)
            if (progress.all { it.isCompleted }) {
                planDao.completePlan(planId, System.currentTimeMillis())
            }
        }
    }

    fun completePlan(planId: Int) {
        viewModelScope.launch {
            planDao.completePlan(planId, System.currentTimeMillis())
        }
    }
}
