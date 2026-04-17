package com.aipoweredgita.app.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnswerOptionItem(
    option: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showAnswer: Boolean,
    onClick: () -> Unit
) {
    val optionColors = remember {
        listOf(
            Color(0xFF6366F1),
            Color(0xFFEC4899),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFF8B5CF6),
            Color(0xFF06B6D4)
        )
    }
    val baseColor = remember(index) { optionColors[index % optionColors.size] }
    val backgroundColor = when {
        showAnswer && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.3f)
        showAnswer && isSelected && !isCorrect -> Color(0xFFFF5252).copy(alpha = 0.3f)
        isSelected -> baseColor.copy(alpha = 0.3f)
        else -> baseColor.copy(alpha = 0.1f)
    }
    val borderColor = when {
        showAnswer && isCorrect -> Color(0xFF4CAF50)
        showAnswer && isSelected && !isCorrect -> Color(0xFFFF5252)
        isSelected -> baseColor
        else -> baseColor.copy(alpha = 0.3f)
    }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(showAnswer, isSelected) {
        if (showAnswer && isSelected) {
            scale.animateTo(1.1f)
            scale.animateTo(1f)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = !showAnswer) { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${('A' + index)}. ", color = MaterialTheme.colorScheme.onSurface)
                Text(text = option, color = MaterialTheme.colorScheme.onSurface)
            }
            AnimatedVisibility(visible = showAnswer, enter = scaleIn(), exit = scaleOut()) {
                val face = when {
                    isCorrect -> "\uD83D\uDE0A" // 😊 happy
                    isSelected && !isCorrect -> "\u2639\uFE0F" // ☹️ sad
                    else -> ""
                }
                Text(text = face, fontSize = 24.sp)
            }
        }
    }
}
