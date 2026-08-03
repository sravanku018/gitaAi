package com.aipoweredgita.app.ui.screens.history.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AHTipsTab(averageAccuracy: Float) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AHTipCard("", "Study Regularly", "Read verses daily to improve retention. Consistency is key to understanding the Bhagavad Gita's teachings.", MaterialTheme.colorScheme.primary)
        }
        if (averageAccuracy < 60) {
            item {
                AHTipCard("", "Focus on Understanding", "Don't just memorize! Try to understand the meaning and context of each verse. Use Normal Mode to read explanations before taking quizzes.", MaterialTheme.colorScheme.error)
            }
        }
        if (averageAccuracy >= 60 && averageAccuracy < 80) {
            item {
                AHTipCard("", "Practice More", "You're doing well! Keep practicing with different question types to improve your accuracy further.", MaterialTheme.colorScheme.secondary)
            }
        }
        if (averageAccuracy >= 80) {
            item {
                AHTipCard("", "Excellent Work!", "You have a great understanding! Consider helping others learn and sharing your knowledge of the Gita.", MaterialTheme.colorScheme.primary)
            }
        }
        item {
            AHTipCard("", "Reflect on Teachings", "After each quiz, spend a moment reflecting on how the teachings apply to your life.", MaterialTheme.colorScheme.tertiary)
        }
        item {
            AHTipCard("", "Review Mistakes", "Go back to verses you got wrong in quizzes. Understanding your mistakes is the fastest way to improve.", MaterialTheme.colorScheme.secondary)
        }
        item {
            AHTipCard("", "Set a Goal", "Try to improve your accuracy by 5% each week. Small, consistent improvements lead to mastery!", MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun AHTipCard(icon: String, title: String, tip: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (icon.isNotBlank()) {
                Text(text = icon, fontSize = 36.sp, modifier = Modifier.padding(end = 14.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
