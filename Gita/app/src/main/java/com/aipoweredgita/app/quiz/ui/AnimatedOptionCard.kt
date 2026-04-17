package com.aipoweredgita.app.quiz.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

sealed class OptionVisualState {
    object Idle : OptionVisualState()
    object Selected : OptionVisualState()
    object Correct : OptionVisualState()
    object Wrong : OptionVisualState()
}

@Composable
fun AnimatedOptionCard(
    text: String,
    state: OptionVisualState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (state) {
        OptionVisualState.Idle -> MaterialTheme.colorScheme.surface
        OptionVisualState.Selected -> MaterialTheme.colorScheme.secondaryContainer
        OptionVisualState.Correct -> MaterialTheme.colorScheme.primaryContainer
        OptionVisualState.Wrong -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (state) {
        OptionVisualState.Idle -> MaterialTheme.colorScheme.onSurface
        OptionVisualState.Selected -> MaterialTheme.colorScheme.onSecondaryContainer
        OptionVisualState.Correct -> MaterialTheme.colorScheme.onPrimaryContainer
        OptionVisualState.Wrong -> MaterialTheme.colorScheme.onErrorContainer
    }

    val animatedColor = animateColorAsState(targetValue = containerColor, label = "cardColor")

    val scale = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(state) {
        when (state) {
            OptionVisualState.Selected, OptionVisualState.Correct, OptionVisualState.Wrong -> {
                // Bounce + scale
                scale.snapTo(1f)
                offsetY.snapTo(0f)
                scale.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                offsetY.animateTo(-12f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                if (state == OptionVisualState.Correct) {
                    scale.animateTo(1.08f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                }
                if (state == OptionVisualState.Wrong) {
                    // Wiggle X keyframes
                    offsetX.snapTo(0f)
                    offsetX.animateTo(0f, keyframes {
                        durationMillis = 360
                        0f at 0
                        12f at 60
                        -12f at 120
                        8f at 180
                        -8f at 240
                        0f at 360
                    })
                    // Flash overlay
                    flashAlpha.snapTo(0f)
                    flashAlpha.animateTo(0.25f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    flashAlpha.animateTo(0f)
                }
            }
            else -> {
                scale.animateTo(1f)
                offsetX.animateTo(0f)
                offsetY.animateTo(0f)
                flashAlpha.animateTo(0f)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .scale(scale.value)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .padding(vertical = 6.dp)
            .let {
                if (enabled) {
                    it.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
                } else it
            },
        colors = CardDefaults.cardColors(containerColor = animatedColor.value),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.padding(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            if (flashAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = flashAlpha.value))
                )
            }
        }
    }
}
