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
import com.aipoweredgita.app.ui.theme.*

@Composable
fun WelcomeDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Debug logging
    androidx.compose.runtime.LaunchedEffect(Unit) {
        android.util.Log.d("WelcomeDialog", "=== WELCOME DIALOG COMPOSING ===")
        android.util.Log.d("WelcomeDialog", "Dialog is being rendered on screen")
    }
    
    Dialog(onDismissRequest = {
        android.util.Log.d("WelcomeDialog", "Dialog dismissed via onDismissRequest")
        onDismiss()
    }) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = Surface1
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldSpark.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GoldSpark
                        )
                    }
                }
                
                // Title
                Text(
                    text = "🙏 Welcome to Bhagavad Gita",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldSpark,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Introduction
                Text(
                    text = "Discover the timeless wisdom of the Bhagavad Gita in Telugu",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = TextWhite
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Features
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureItem(
                        icon = "📖",
                        title = "Read Verses",
                        description = "Explore all 700 verses with translations"
                    )
                    
                    FeatureItem(
                        icon = "🎯",
                        title = "Test Your Knowledge",
                        description = "AI-powered quizzes to deepen understanding"
                    )
                    
                    FeatureItem(
                        icon = "💬",
                        title = "Ask Questions",
                        description = "Get relevant verses for your spiritual queries"
                    )
                    
                    FeatureItem(
                        icon = "📊",
                        title = "Track Progress",
                        description = "Yoga progression system with 4 levels"
                    )
                    
                    FeatureItem(
                        icon = "🔔",
                        title = "Daily Reminders",
                        description = "Receive daily verses and reflections"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Get Started button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Saffron)
                ) {
                    Text("Get Started")
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
                color = GoldSpark
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim
            )
        }
    }
}
