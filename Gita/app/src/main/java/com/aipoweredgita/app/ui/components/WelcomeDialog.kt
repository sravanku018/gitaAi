package com.aipoweredgita.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aipoweredgita.app.ui.theme.*

@Composable
fun WelcomeDialog(
    onDismiss: () -> Unit,
    onNavigateToBattleQuiz: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Debug logging
    androidx.compose.runtime.LaunchedEffect(Unit) {
        android.util.Log.d("WelcomeDialog", "=== WELCOME DIALOG COMPOSING (v2.2.0) ===")
        android.util.Log.d("WelcomeDialog", "Dialog is being rendered on screen")
    }
    
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Icon
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.headlineLarge
                )
                
                // Title
                Text(
                    text = "✨ What's New in v2.2.0",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                
                // Introduction
                Text(
                    text = "Added Śrīmad Bhagavad Gītā header branding, live English/Telugu translation toggle, single-tone card themes, and robust concurrency & network fixes!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Features
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureItem(
                        icon = "🌐",
                        title = "Live Telugu & English Toggle",
                        description = "Instant dynamic switching between English and Telugu translations, word-by-word meanings, and purports in Read Mode!"
                    )

                    FeatureItem(
                        icon = "🌸",
                        title = "Single-Tone Sacred Aesthetics",
                        description = "Pure white Light Mode cards with zero cement shadows, and warm soft Amber highlights in Dark Mode."
                    )

                    FeatureItem(
                        icon = "⚡",
                        title = "Groq AI & Cloud Acceleration",
                        description = "Instant cloud AI responses with zero local model downloads required."
                    )
                    
                    FeatureItem(
                        icon = "⚔️",
                        title = "Mahabharata Battle Quiz",
                        description = "10,091 sequence MCQs in English & Telugu with live language toggle!"
                    )
                    
                    FeatureItem(
                        icon = "🪙",
                        title = "Robust Sync & Concurrency",
                        description = "Thread-safe verse caching, atomic asset loading, and atomic database write queueing."
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToBattleQuiz()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("⚔️ Play Battle Quiz Now", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Explore Gita App")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 10.dp)
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
