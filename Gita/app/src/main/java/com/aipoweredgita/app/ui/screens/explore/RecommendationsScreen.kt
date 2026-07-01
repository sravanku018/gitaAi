package com.aipoweredgita.app.ui.screens.explore

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.ui.screens.explore.components.*
import com.aipoweredgita.app.viewmodel.RecommendationsViewModel
import com.aipoweredgita.app.viewmodel.StudyPlanViewModel

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
    
    var selectedTab by remember { mutableIntStateOf(initialTab) }
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
