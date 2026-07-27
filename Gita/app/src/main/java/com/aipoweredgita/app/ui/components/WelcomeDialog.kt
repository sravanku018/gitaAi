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
        android.util.Log.d("WelcomeDialog", "=== WELCOME DIALOG COMPOSING (v2.0.4) ===")
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayMedium
                )
                
                // Title
                Text(
                    text = "✨ What's New in v2.0.4",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                
                // Introduction
                Text(
                    text = "Enhanced coin history deduplication, distinct reward logs, and scrollable updates dialog!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Features
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureItem(
                        icon = "⚡",
                        title = "Groq AI — Now Default",
                        description = "No model downloads needed! Groq & NVIDIA power all AI features instantly from the cloud."
                    )
                    
                    FeatureItem(
                        icon = "⚔️",
                        title = "Mahabharata Battle Quiz",
                        description = "10,091 Mahabharata sequence MCQs (5,000 medium + 5,091 hard) in English & Telugu with live language toggle!"
                    )
                    
                    FeatureItem(
                        icon = "📚",
                        title = "Bundled Offline Datasets",
                        description = "3,501 pre-translated English & Telugu Q&A pairs embedded offline — zero network downloads needed."
                    )
                    
                    FeatureItem(
                        icon = "🪙",
                        title = "Krishna Coins & Daily Rewards",
                        description = "Fixed daily share & daily check-in coin rewards with automatic duplicate purging."
                    )
                    
                    FeatureItem(
                        icon = "🧘",
                        title = "Yoga Levels & Multipliers",
                        description = "Progress through Seeker, Yogi, and Sage levels to unlock high coin reward multipliers!"
                    )
                    
                    FeatureItem(
                        icon = "⚡",
                        title = "Instant Offline Ingestion",
                        description = "Rapid response time with local database loading across all study and battle quiz modes."
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(end = 12.dp)
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
