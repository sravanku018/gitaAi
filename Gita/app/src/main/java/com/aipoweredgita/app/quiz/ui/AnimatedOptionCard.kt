package com.aipoweredgita.app.quiz.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.ui.theme.*
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
        OptionVisualState.Idle -> Surface1
        OptionVisualState.Selected -> Saffron.copy(alpha = 0.2f)
        OptionVisualState.Correct -> Forest.copy(alpha = 0.3f)
        OptionVisualState.Wrong -> CrimsonDeep.copy(alpha = 0.3f)
    }
    
    val borderColor = when (state) {
        OptionVisualState.Idle -> Surface2
        OptionVisualState.Selected -> Saffron
        OptionVisualState.Correct -> GoldSpark
        OptionVisualState.Wrong -> Color.Red
    }

    val contentColor = when (state) {
        OptionVisualState.Idle -> TextWhite
        OptionVisualState.Selected -> GoldSpark
        OptionVisualState.Correct -> GoldSpark
        OptionVisualState.Wrong -> Color.Red
    }

    val animatedColor = animateColorAsState(targetValue = containerColor, label = "cardColor")
    val animatedBorderColor = animateColorAsState(targetValue = borderColor, label = "borderColor")

    val scale = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(state) {
        when (state) {
            OptionVisualState.Selected, OptionVisualState.Correct, OptionVisualState.Wrong -> {
                scale.snapTo(1f)
                offsetY.snapTo(0f)
                scale.animateTo(1.04f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                offsetY.animateTo(-8f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                
                if (state == OptionVisualState.Wrong) {
                    offsetX.snapTo(0f)
                    offsetX.animateTo(0f, keyframes {
                        durationMillis = 300
                        0f at 0
                        8f at 50
                        -8f at 100
                        5f at 150
                        -5f at 200
                        0f at 300
                    })
                }
            }
            else -> {
                scale.animateTo(1f)
                offsetX.animateTo(0f)
                offsetY.animateTo(0f)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale.value)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .padding(vertical = 4.dp)
            .let {
                if (enabled) {
                    it.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
                } else it
            },
        colors = CardDefaults.cardColors(containerColor = animatedColor.value),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, animatedBorderColor.value),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.padding(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                fontWeight = if (state != OptionVisualState.Idle) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
