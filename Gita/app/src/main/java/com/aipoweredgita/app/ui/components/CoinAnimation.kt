package com.aipoweredgita.app.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.aipoweredgita.app.ui.theme.GoldSpark

enum class CoinEventType {
    EARNED, BURNED, LEVEL_UP
}

data class CoinEvent(
    val type: CoinEventType,
    val amount: Int = 0,
    val message: String? = null
)

object CoinAnimationManager {
    private val _events = MutableSharedFlow<CoinEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: CoinEvent) {
        _events.emit(event)
    }
}

@Composable
fun CoinOverlay() {
    var activeEvent by remember { mutableStateOf<CoinEvent?>(null) }
    
    LaunchedEffect(Unit) {
        CoinAnimationManager.events.collect { event ->
            activeEvent = event
            delay(2500) // Duration of animation
            activeEvent = null
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = activeEvent != null,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 1.5f)
        ) {
            activeEvent?.let { event ->
                when (event.type) {
                    CoinEventType.EARNED -> CoinEarnedAnimation(event.amount)
                    CoinEventType.BURNED -> CoinBurnedAnimation(event.amount)
                    CoinEventType.LEVEL_UP -> LevelUpAnimation(event.message ?: "New Level Reached!")
                }
            }
        }
    }
}

@Composable
private fun CoinEarnedAnimation(amount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "coin")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .background(GoldSpark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🪙", fontSize = 40.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "+$amount Krishna Coins",
            color = GoldSpark,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun CoinBurnedAnimation(amount: Int) {
    val transition = rememberInfiniteTransition(label = "burn")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart), label = "alpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .alpha(alpha)
                .background(Color(0xFFE57373), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🔥", fontSize = 40.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "-$amount Coins Consumed",
            color = Color(0xFFE57373),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LevelUpAnimation(message: String) {
    val transition = rememberInfiniteTransition(label = "level")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rotate"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { rotationZ = rotation },
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 80.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "LEVEL UP!",
            color = GoldSpark,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            message,
            color = Color.White,
            fontSize = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

