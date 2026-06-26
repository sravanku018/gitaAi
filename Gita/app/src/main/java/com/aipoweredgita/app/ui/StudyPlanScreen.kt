package com.aipoweredgita.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.StudyPlan
import com.aipoweredgita.app.database.StudyPlanProgress
import com.aipoweredgita.app.database.StudyPlanTemplates
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanScreen(
    onBack: () -> Unit = {},
    onStartReading: (chapter: Int, verse: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val db = remember { GitaDatabase.getDatabase(context) }
    val planDao = remember { db.studyPlanDao() }
    val activePlan by planDao.getActivePlan().collectAsState(initial = null)
    val allPlans by planDao.getAllPlans().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (activePlan == null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "Create Plan")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (activePlan != null) {
                item {
                    ActivePlanCard(activePlan!!, planDao, onStartReading)
                }
            }

            if (allPlans.isEmpty() && activePlan == null) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp)) {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\uD83D\uDCDA", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(16.dp))
                            Text("No study plans yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap + to create a structured reading plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            if (activePlan == null) {
                item {
                    Text(
                        "Quick Start Plans",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                val templates = listOf(
                    "14-Day Karma Yoga" to "Deep dive into action and duty" to { createPlan(scope, planDao, "14-Day Karma Yoga", "Deep dive into Karma Yoga teachings", 14, "karma_yoga", "2,3,4,18") },
                    "7-Day Quiz Challenge" to "Test your Gita knowledge" to { createPlan(scope, planDao, "7-Day Quiz Challenge", "Daily quiz challenges", 7, "quiz_challenge", "") },
                    "18-Day Complete Gita" to "Journey through all chapters" to { createPlan(scope, planDao, "18-Day Complete Gita", "Read key verses from each chapter", 18, "full_gita", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18") }
                )
                items(templates) { (titleDesc, createFn) ->
                    val (title, desc) = titleDesc
                    PlanTemplateCard(title, desc) { createFn() }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlanDialog(onDismiss = { showCreateDialog = false }) { title, desc, days, chapters ->
            createPlan(scope, planDao, title, desc, days, "custom", chapters)
            showCreateDialog = false
        }
    }
}

@Composable
private fun ActivePlanCard(plan: StudyPlan, planDao: com.aipoweredgita.app.database.StudyPlanDao, onStartReading: (Int, Int) -> Unit) {
    val progress by planDao.getPlanProgress(plan.id).collectAsState(initial = emptyList())
    val completedDays = progress.count { it.isCompleted }
    val totalDays = plan.durationDays
    val currentDayProgress = progress.find { it.day == plan.currentDay }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(plan.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(plan.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { completedDays.toFloat() / totalDays },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text("$completedDays / $totalDays days completed", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(12.dp))
            if (currentDayProgress != null && !currentDayProgress.isCompleted) {
                Text("Day ${plan.currentDay}: Chapter ${currentDayProgress.chapterNo}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onStartReading(currentDayProgress.chapterNo, currentDayProgress.verseStart) }) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start Reading")
                }
            }
        }
    }
}

@Composable
private fun PlanTemplateCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Icon(Icons.Default.PlayArrow, "Start", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CreatePlanDialog(onDismiss: () -> Unit, onCreate: (String, String, Int, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("14") }
    var chapters by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Plan") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Plan Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(days, { days = it }, label = { Text("Duration (days)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(chapters, { chapters = it }, label = { Text("Chapters (comma-separated)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onCreate(title, title, days.toIntOrNull() ?: 14, chapters)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun createPlan(
    scope: kotlinx.coroutines.CoroutineScope,
    planDao: com.aipoweredgita.app.database.StudyPlanDao,
    title: String, desc: String, days: Int, type: String, chapters: String
) {
    scope.launch {
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
