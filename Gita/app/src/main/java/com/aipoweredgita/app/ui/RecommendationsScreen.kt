package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.ui.LocalUiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    onOpenChapter: (Int) -> Unit,
    onStartTopicQuiz: () -> Unit,
    onOpenFlashcards: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = GitaDatabase.getDatabase(context)
    val recs by db.recommendationDataDao().getActiveRecommendations().collectAsState(initial = emptyList())

    val uiCfg = LocalUiConfig.current
    Scaffold(topBar = {
        TopAppBar(title = { Text("Recommendations") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(if (uiCfg.isLandscape) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                items = recs,
                key = { rec: RecommendationData -> rec.id }
            ) { r ->
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        val scope = rememberCoroutineScope()
                        Button(onClick = {
                            scope.launch {
                                try { db.recommendationDataDao().dismiss(r.id) } catch (e: Exception) {
                                    android.util.Log.w("RecommendationsScreen", "Failed to dismiss ${r.id}", e)
                                }
                            }
                        }) { Text("Dismiss") }
                    }
                } }
            }
        }
    }
}
