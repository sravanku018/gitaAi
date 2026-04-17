package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.Flashcard
import com.aipoweredgita.app.ui.LocalUiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(topic: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = GitaDatabase.getDatabase(context)
    val cards by db.flashcardDao().getByTopic(topic).collectAsState(initial = emptyList())
    val uiCfg = LocalUiConfig.current
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (topic.isBlank()) "Flashcards" else "Flashcards: $topic") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(if (uiCfg.isLandscape) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                items = cards,
                key = { flashcard: Flashcard -> flashcard.id }
            ) { c ->
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(c.frontText, style = MaterialTheme.typography.titleMedium)
                    Text(c.backText, style = MaterialTheme.typography.bodySmall)
                } }
            }
        }
    }
}
