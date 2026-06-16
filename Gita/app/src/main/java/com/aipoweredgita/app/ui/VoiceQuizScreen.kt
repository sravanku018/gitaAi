package com.aipoweredgita.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.VoiceQuizViewModel

@Composable
fun VoiceQuizScreen(
    onExit: () -> Unit,
    viewModel: VoiceQuizViewModel = viewModel(),
    languageMode: com.aipoweredgita.app.utils.LanguageMode = com.aipoweredgita.app.utils.LanguageMode.ENG_TO_TEL
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Propagate language mode to VoiceQuizViewModel
    LaunchedEffect(languageMode) {
        viewModel.setLanguageMode(languageMode)
    }

    // Permission state
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasAudioPermission = isGranted
            if (isGranted) {
                viewModel.startListening()
            }
        }
    )

    // Pulse animation for the microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (state.isListening) 0.6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1), // Light cream
                        Color(0xFFFFECB3), // Soft saffron
                        Color(0xFFFFCCBC)  // Warm peach
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color(0xFF5D4037))
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Gita Quiz",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5D4037)
                    )
                    Text(
                        "Voice Interactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8D6E63)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFB300),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${state.score}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn(tween(500)) + scaleIn(initialScale = 0.9f) togetherWith
                        fadeOut(tween(500)) + scaleOut(targetScale = 1.1f)
                    },
                    label = "content_transition"
                ) { targetState ->
                    when {
                        targetState.isLoading -> LoadingWisdom()
                        targetState.isGameOver -> GameOverDisplay(targetState.score, targetState.totalQuestions) { viewModel.startQuiz() }
                        targetState.currentQuestion != null -> QuestionDisplay(targetState.currentQuestion!!, targetState.questionIndex + 1)
                    }
                }
            }

            // Feedback / Transcript Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayText = state.partialTranscript.ifEmpty { state.transcript }
                    AnimatedVisibility(
                        visible = displayText.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = displayText,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF4E342E)
                            )
                        }
                    }
                    
                    if (state.feedback.isNotEmpty()) {
                        val isCorrectText = state.feedback.trim().let { 
                            it.startsWith("CORRECT", ignoreCase = true) || 
                            it.startsWith("సరియైనది", ignoreCase = true) || 
                            it.startsWith("సరైనది", ignoreCase = true) ||
                            it.contains("excellent", ignoreCase = true) ||
                            it.contains("శభాష్", ignoreCase = true)
                        }
                        Text(
                            text = state.feedback,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCorrectText) Color(0xFF2E7D32) else Color(0xFFC62828),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Interaction Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.isListening) {
                        Surface(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(pulseScale),
                            shape = CircleShape,
                            color = Color(0xFFFF5722).copy(alpha = pulseAlpha)
                        ) {}
                    }
                    
                    Surface(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (hasAudioPermission) {
                                    if (state.isListening) viewModel.stopListening()
                                    else viewModel.startListening()
                                } else {
                                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                        color = when {
                            state.isListening -> Color(0xFFFF5722)
                            state.isSpeaking -> Color(0xFFFFA000).copy(alpha = 0.6f)
                            else -> Color(0xFFFFA000)
                        },
                        shadowElevation = 8.dp,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isListening) Icons.Default.Warning else Icons.Default.Mic,
                                contentDescription = if (state.isListening) "Stop" else "Mic",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = when {
                        !hasAudioPermission -> "Grant Microphone Access"
                        state.isListening -> "Listening... Speak now"
                        state.isSpeaking -> "Teacher is speaking..."
                        state.isLoading -> "Contemplating..."
                        else -> "Tap to answer"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isListening) Color(0xFFFF5722) else Color(0xFF5D4037)
                )
            }
        }
    }
}

@Composable
fun QuestionDisplay(question: String, index: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
        color = Color.White.copy(alpha = 0.7f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Shloka Wisdom $index",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFF8F00),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF3E2723),
                lineHeight = 32.sp
            )
        }
    }
}

@Composable
fun LoadingWisdom() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Color(0xFFFFB300), strokeWidth = 6.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Fetching Guru's Wisdom...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5D4037)
        )
    }
}

@Composable
fun GameOverDisplay(score: Int, total: Int, onRestart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Wisdom Journey Complete",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF5D4037)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your Soul's Progress: $score / $total",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(56.dp).fillMaxWidth(0.6f)
        ) {
            Text("Restart Journey", fontWeight = FontWeight.Bold)
        }
    }
}
