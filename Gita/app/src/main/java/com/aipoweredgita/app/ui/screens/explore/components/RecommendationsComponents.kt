package com.aipoweredgita.app.ui.screens.explore.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.database.StudyPlan
import com.aipoweredgita.app.domain.model.UiConfigUiState
import com.aipoweredgita.app.viewmodel.RecommendationsViewModel
import com.aipoweredgita.app.viewmodel.StudyPlanViewModel

@Composable
fun RecommendationsTab(
    recs: List<RecommendationData>,
    onOpenChapter: (Int) -> Unit,
    onStartTopicQuiz: () -> Unit,
    onOpenFlashcards: (String?) -> Unit,
    viewModel: RecommendationsViewModel,
    uiCfg: UiConfigUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(if (uiCfg.isLandscape) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recs, key = { it.id }) { r ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.recommendationTitle, style = MaterialTheme.typography.titleMedium)
                    Text(r.reason, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (r.recommendationType) {
                            "chapter" -> Button(onClick = { onOpenChapter(r.recommendationId.toIntOrNull() ?: 1) }) { Text("Open Chapter") }
                            "topic" -> Button(onClick = onStartTopicQuiz) { Text("Start Topic Quiz") }
                            "yogalevel" -> Button(onClick = onStartTopicQuiz) { Text("Focus Level") }
                            "study_mode" -> Button(onClick = onStartTopicQuiz) { Text("Continue") }
                            else -> Button(onClick = { onOpenFlashcards(r.recommendationId) }) { Text("View Flashcards") }
                        }
                        Button(onClick = {
                            viewModel.dismissRecommendation(r.id)
                        }) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}

@Composable
fun StudyPlansTab(viewModel: StudyPlanViewModel, onOpenChapter: (Int) -> Unit) {
    val activePlan by viewModel.activePlan.collectAsState(initial = null)
    val allPlans by viewModel.allPlans.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (activePlan != null) {
            item {
                ActivePlanCard(activePlan!!, viewModel, onOpenChapter)
            }
        } else {
            item {
                Text("Quick Start Plans", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            val templates = listOf(
                "14-Day Karma Yoga" to "Deep dive into action and duty" to { viewModel.createPlan("14-Day Karma Yoga", "Deep dive into Karma Yoga teachings", 14, "karma_yoga", "2,3,4,18") },
                "7-Day Quiz Challenge" to "Test your Gita knowledge" to { viewModel.createPlan("7-Day Quiz Challenge", "Daily quiz challenges", 7, "quiz_challenge", "") },
                "18-Day Complete Gita" to "Journey through all chapters" to { viewModel.createPlan("18-Day Complete Gita", "Read key verses from each chapter", 18, "full_gita", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18") }
            )
            items(templates) { (titleDesc, createFn) ->
                val (title, desc) = titleDesc
                PlanTemplateCard(title, desc) { createFn() }
            }
        }
    }
}

@Composable
fun ActivePlanCard(plan: StudyPlan, viewModel: StudyPlanViewModel, onOpenChapter: (Int) -> Unit) {
    val progress by viewModel.getPlanProgress(plan.id).collectAsState(initial = emptyList())
    val completedDays = progress.count { it.isCompleted }
    val totalDays = plan.durationDays
    val currentDayProgress = progress.find { it.day == plan.currentDay }
    val percent = if (totalDays > 0) ((completedDays.toFloat() / totalDays) * 100).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🎯 " + plan.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Badge(containerColor = Color(0xFFF59E0B)) {
                    Text("$percent% Complete", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(plan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$completedDays of $totalDays Days Completed",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Day ${plan.currentDay}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { completedDays.toFloat() / totalDays },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = Color(0xFFF59E0B),
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )

            Spacer(Modifier.height(16.dp))

            if (currentDayProgress != null && !currentDayProgress.isCompleted) {
                Text(
                    text = "Today's Study Goal: Chapter ${currentDayProgress.chapterNo}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onOpenChapter(currentDayProgress.chapterNo) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Start Chapter ${currentDayProgress.chapterNo} Reading", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlanTemplateCard(title: String, description: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.PlayArrow, null)
        }
    }
}

@Composable
fun CreatePlanDialog(onDismiss: () -> Unit, onCreate: (String, String, Int, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("14") }
    var chapters by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Plan") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Plan Name") })
                OutlinedTextField(days, { days = it }, label = { Text("Duration (days)") })
                OutlinedTextField(chapters, { chapters = it }, label = { Text("Chapters (comma-separated)") })
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onCreate(title, title, days.toIntOrNull() ?: 14, chapters) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
