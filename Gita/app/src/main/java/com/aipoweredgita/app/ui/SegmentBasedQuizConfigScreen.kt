package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.data.LearningSegment
import com.aipoweredgita.app.data.SegmentWeightageSystem
import com.aipoweredgita.app.ui.components.GradientCardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentBasedQuizConfigScreen(
    segmentSystem: SegmentWeightageSystem,
    onStartQuiz: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedQuestionCount by remember { mutableStateOf(15) }
    val uiCfg = LocalUiConfig.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    "AI-Powered Quiz",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI Notice
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI will select questions based on your learning progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Question Count Selection
        Text(
            text = "How many questions do you want to practice?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(15, 25).forEach { count ->
                QuizOptionCard(
                    questionCount = count,
                    isSelected = selectedQuestionCount == count,
                    onClick = { selectedQuestionCount = count },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Focus Areas Section
        Text(
            text = "Focus Areas (AI-Selected)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val focusSegments = segmentSystem.getWeightedSegments()
            .sortedByDescending { it.second }
            .take(3)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(focusSegments) { (segment, weightage) ->
                val segmentProgress = segmentSystem.segments[segment]!!
                val color = when (segment) {
                    LearningSegment.KARMA_YOGA -> Color(0xFF4CAF50)
                    LearningSegment.BHAKTI_YOGA -> Color(0xFFE91E63)
                    LearningSegment.JNANA_YOGA -> Color(0xFF2196F3)
                    LearningSegment.DHYANA_YOGA -> Color(0xFF9C27B0)
                    LearningSegment.MOKSHA_YOGA -> Color(0xFFFF9800)
                    LearningSegment.WARRIOR_CODE -> Color(0xFFF44336)
                    LearningSegment.DIVINE_NATURE -> Color(0xFF00BCD4)
                    LearningSegment.SURRENDER -> Color(0xFF8BC34A)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = color.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = segment.displayName.split(" ").map { it.first() }.joinToString(""),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = segment.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Accuracy: ${segmentProgress.accuracy.toInt()}% • Priority: ${if (weightage > 1.5) "High" else if (weightage < 0.7) "Low" else "Normal"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (weightage > 1.5) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "High Priority",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Quiz Button
        Button(
            onClick = { onStartQuiz() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start AI-Guided Quiz",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selected: $selectedQuestionCount Questions • AI will focus on your learning gaps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuizOptionCard(
    questionCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        Color.Transparent

    val gradient = when (questionCount) {
        15 -> listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
        else -> listOf(Color(0xFFEC4899), Color(0xFFF43F5E))
    }

    GradientCardContainer(
        brush = Brush.horizontalGradient(gradient),
        modifier = modifier
            .height(80.dp)
            .border(3.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        elevation = if (isSelected) 8.dp else 4.dp,
        cornerRadius = 16.dp,
        contentPadding = 16.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$questionCount",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
