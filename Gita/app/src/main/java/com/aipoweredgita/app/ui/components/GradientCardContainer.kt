package com.aipoweredgita.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.ui.theme.UiDefaults
import com.aipoweredgita.app.ui.theme.UiMotion

@Composable
fun GradientCardContainer(
    brush: Brush,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = UiDefaults.CornerRadius,
    elevation: Dp = UiDefaults.ElevationNone,
    contentPadding: Dp = UiDefaults.CardPadding,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier.scale(
            animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(UiMotion.Medium, easing = UiMotion.StandardEasing),
                label = "container-scale"
            ).value
        ),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush)
                .padding(contentPadding),
            content = content
        )
    }
}
