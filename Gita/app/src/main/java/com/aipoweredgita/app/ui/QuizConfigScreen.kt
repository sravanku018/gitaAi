package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.components.GradientCardContainer

@Composable
fun QuizConfigScreen(
    onStartQuiz: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedQuestions by remember { mutableStateOf(15) }
    val uiCfg = LocalUiConfig.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quiz Configuration",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "How many questions do you want to answer?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Question count options
        QuizOptionCard(
            questionCount = 15,
            isSelected = selectedQuestions == 15,
            onClick = {
                try {
                    selectedQuestions = 15
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            gradient = listOf(
                Color(0xFF10B981),
                Color(0xFF059669)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        QuizOptionCard(
            questionCount = 25,
            isSelected = selectedQuestions == 25,
            onClick = {
                try {
                    selectedQuestions = 25
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            gradient = listOf(
                Color(0xFFEC4899),
                Color(0xFFF43F5E)
            )
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Start Quiz Button
        Button(
            onClick = {
                try {
                    onStartQuiz(selectedQuestions)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start Quiz",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selected: $selectedQuestions Questions",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuizOptionCard(
    questionCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    gradient: List<Color>
) {
    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        Color.Transparent

    GradientCardContainer(
        brush = Brush.horizontalGradient(gradient),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(3.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        elevation = if (isSelected) 8.dp else 4.dp,
        cornerRadius = 16.dp,
        contentPadding = 20.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$questionCount",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Questions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            if (isSelected) {
                // Checkmark indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradient.first()
                    )
                }
            }
        }
    }
}
