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
        android.util.Log.d("WelcomeDialog", "=== WELCOME DIALOG COMPOSING (v2.11.0) ===")
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
                    text = "🌸",
                    style = MaterialTheme.typography.headlineLarge
                )
                
                // Title
                Text(
                    text = "Welcome to Śrīmad Bhagavad Gītā",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
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
                        icon = "⚡",
                        title = "Cloud Groq AI Guidance",
                        description = "Fast, intelligent AI Krishna guidance with zero local model downloads required."
                    )
                    
                    FeatureItem(
                        icon = "⚔️",
                        title = "Mahabharata Battle Quiz",
                        description = "10,091 sequence MCQs in English & Telugu with live language toggle!"
                    )
                    
                    FeatureItem(
                        icon = "📚",
                        title = "Bundled Offline Datasets",
                        description = "3,501 pre-translated English & Telugu Q&A pairs embedded offline — zero network downloads needed."
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
