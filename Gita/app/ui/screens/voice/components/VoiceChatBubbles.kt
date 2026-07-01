package com.aipoweredgita.app.ui.screens.voice.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.screens.voice.VoiceStudioColors
import com.aipoweredgita.app.ui.screens.voice.getVoiceStudioColors
import com.aipoweredgita.app.domain.model.ChatMessage

@Composable
fun ThinkingDots() {
    val colors = getVoiceStudioColors()
    val infiniteTransition = rememberInfiniteTransition(label = "think_dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(1400, delayMillis = i * 220),
                    RepeatMode.Reverse
                ),
                label = "dot_alpha_$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.RevolvingYellow.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean = false,
    onEdit: (String) -> Unit
) {
    val colors = getVoiceStudioColors()
    val isUser = message.isUser
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.padding(top = 2.dp).size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    rotate(rotation) {
                        drawArc(
                            color = colors.RevolvingYellow,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, colors.Border, CircleShape)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = com.aipoweredgita.app.R.drawable.krishna),
                        contentDescription = "Krishna",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        }

        val bubbleShape = if (isUser)
            RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
        else
            RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)

        val bubbleBg = if (isUser) colors.UserBubbleBg
        else (if (colors.IsDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f))
        val bubbleBdrColor = if (isUser) colors.UserBubbleBdr
        else (if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))

        var showMenu by remember { mutableStateOf(false) }
        val clipboardManager = LocalClipboardManager.current

        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(0.5.dp, bubbleBdrColor, bubbleShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf((if (colors.IsDark) Color.White else Color.Black).copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true }
                    )
                }
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.TextPrimary,
                    lineHeight = 22.sp,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(if (colors.IsDark) Color(0xFF140F0A) else Color(0xFFFFFDF8))
            ) {
                if (isUser) {
                    DropdownMenuItem(
                        text = { Text("Edit & Resend", color = colors.TextPrimary) },
                        onClick = {
                            onEdit(message.text)
                            showMenu = false
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Copy", color = colors.TextPrimary) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            showMenu = false
                        }
                    )
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.padding(top = 2.dp).size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    rotate(-rotation) {
                        drawArc(
                            color = colors.UserBubbleBdr,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, colors.Border, CircleShape)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = com.aipoweredgita.app.R.drawable.devotee),
                        contentDescription = "Devotee",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingBubble() {
    val colors = getVoiceStudioColors()
    val infiniteTransition = rememberInfiniteTransition(label = "think_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.padding(top = 2.dp).size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(38.dp)) {
                rotate(rotation) {
                    drawArc(
                        color = colors.RevolvingYellow,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.dp, colors.Border, CircleShape)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = com.aipoweredgita.app.R.drawable.krishna),
                    contentDescription = "Krishna",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        val bubbleShape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
        val bubbleBg = if (colors.IsDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)
        val bubbleBdrColor = if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)

        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(0.5.dp, bubbleBdrColor, bubbleShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf((if (colors.IsDark) Color.White else Color.Black).copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThinkingDots()
            }
        }
    }
}

@Composable
fun ListeningBubble(liveTranscript: String) {
    val colors = getVoiceStudioColors()
    val infiniteTransition = rememberInfiniteTransition(label = "listen_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        val bubbleShape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
        val bubbleBg = if (colors.IsDark) Color(0xFFFF6400).copy(alpha = 0.12f) else Color(0xFFFF6400).copy(alpha = 0.08f)
        val bubbleBdrColor = colors.ListenRed.copy(alpha = 0.5f)

        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 260.dp)
                .scale(scale)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(1.dp, bubbleBdrColor, bubbleShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf((if (colors.IsDark) Color.White else Color.Black).copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = colors.ListenRed, modifier = Modifier.size(16.dp))
                if (liveTranscript.isNotBlank()) {
                    Text(
                        text = liveTranscript,
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextPrimary, fontStyle = FontStyle.Italic)
                    )
                } else {
                    Text(
                        text = "Listening...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted, fontStyle = FontStyle.Italic)
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, colors.ListenRed.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(id = com.aipoweredgita.app.R.drawable.devotee),
                contentDescription = "Devotee",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
