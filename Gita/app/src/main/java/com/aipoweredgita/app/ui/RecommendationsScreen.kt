package com.aipoweredgita.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.database.StudyPlan
import com.aipoweredgita.app.viewmodel.StudyPlanViewModel
import com.aipoweredgita.app.viewmodel.RecommendationsViewModel
import com.aipoweredgita.app.domain.model.UiConfigUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    initialTab: Int = 0,
    onOpenChapter: (Int) -> Unit,
    onStartTopicQuiz: () -> Unit,
    onOpenFlashcards: (String?) -> Unit,
    onBack: () -> Unit,
    studyPlanViewModel: StudyPlanViewModel = hiltViewModel(),
    recommendationsViewModel: RecommendationsViewModel = hiltViewModel()
) {
    val recs by recommendationsViewModel.activeRecommendations.collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("For You", "Study Plans")

    val uiCfg = LocalUiConfig.current
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Recommendations") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                val activePlan by studyPlanViewModel.activePlan.collectAsState(initial = null)
                if (activePlan == null) {
                    var showCreateDialog by remember { mutableStateOf(false) }
                    FloatingActionButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Create Plan")
                    }
                    if (showCreateDialog) {
                        CreatePlanDialog(onDismiss = { showCreateDialog = false }) { title, desc, days, chapters ->
                            studyPlanViewModel.createPlan(title, desc, days, "custom", chapters)
                            showCreateDialog = false
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> RecommendationsTab(recs, onOpenChapter, onStartTopicQuiz, onOpenFlashcards, recommendationsViewModel, uiCfg)
                1 -> StudyPlansTab(studyPlanViewModel, onOpenChapter)
            }
        }
    }
}

@Composable
private fun RecommendationsTab(
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
private fun StudyPlansTab(viewModel: StudyPlanViewModel, onOpenChapter: (Int) -> Unit) {
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
private fun ActivePlanCard(plan: StudyPlan, viewModel: StudyPlanViewModel, onOpenChapter: (Int) -> Unit) {
    val progress by viewModel.getPlanProgress(plan.id).collectAsState(initial = emptyList())
    val completedDays = progress.count { it.isCompleted }
    val totalDays = plan.durationDays
    val currentDayProgress = progress.find { it.day == plan.currentDay }

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(plan.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(plan.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { completedDays.toFloat() / totalDays }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            if (currentDayProgress != null && !currentDayProgress.isCompleted) {
                Text("Day ${plan.currentDay}: Chapter ${currentDayProgress.chapterNo}")
                Button(onClick = { onOpenChapter(currentDayProgress.chapterNo) }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("Start Reading")
                }
            }
        }
    }
}

@Composable
private fun PlanTemplateCard(title: String, description: String, onClick: () -> Unit) {
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
private fun CreatePlanDialog(onDismiss: () -> Unit, onCreate: (String, String, Int, String) -> Unit) {
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
