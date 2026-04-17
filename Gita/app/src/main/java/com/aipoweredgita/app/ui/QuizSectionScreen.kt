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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.viewmodel.VoiceQuizViewModel

// Premium color palette
private val GradientSaffron = Color(0xFFFF8F00)
private val GradientAmber = Color(0xFFFFC107)
private val DeepBrown = Color(0xFF3E2723)
private val WarmCream = Color(0xFFFFF8E1)
private val SoftPeach = Color(0xFFFFECB3)

@Composable
fun QuizSectionScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("15 Questions", "25 Questions", "Voice Quiz")

    // Hoist ViewModels to avoid re-creation on tab switches
    val quizViewModel: QuizViewModel = viewModel()
    val voiceQuizViewModel: VoiceQuizViewModel = viewModel()

    // Auto-start normal quiz when switching to a question-count tab
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> {
                quizViewModel.setQuizLimit(15)
                quizViewModel.resetQuiz()
            }
            1 -> {
                quizViewModel.setQuizLimit(25)
                quizViewModel.resetQuiz()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WarmCream, SoftPeach, Color(0xFFFFCCBC))
                )
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = DeepBrown)
            }

            Text(
                "Quiz Section",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DeepBrown
            )

            Spacer(modifier = Modifier.size(40.dp))
        }

        // Tab Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.6f),
            shadowElevation = 2.dp
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = DeepBrown,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = GradientSaffron
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (index == 2) Icons.Filled.Mic else Icons.Filled.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == index) GradientSaffron else DeepBrown.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) DeepBrown else DeepBrown.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(300)) + slideInHorizontally(
                    initialOffsetX = { if (targetState > initialState) it / 4 else -it / 4 },
                    animationSpec = tween(300)
                ) togetherWith fadeOut(tween(200)) + slideOutHorizontally(
                    targetOffsetX = { if (targetState > initialState) -it / 4 else it / 4 },
                    animationSpec = tween(200)
                )
            },
            label = "tab_content"
        ) { tab ->
            when (tab) {
                0 -> QuizTabContent(quizViewModel = quizViewModel, onExit = onExit)
                1 -> QuizTabContent(quizViewModel = quizViewModel, onExit = onExit)
                2 -> VoiceQuizTabContent(viewModel = voiceQuizViewModel)
            }
        }
    }
}

@Composable
private fun QuizTabContent(
    quizViewModel: QuizViewModel,
    onExit: () -> Unit
) {
    // Reuse the existing QuizScreen but without re-creating ViewModel
    QuizScreen(
        onExitQuiz = onExit,
        viewModel = quizViewModel
    )
}

@Composable
private fun VoiceQuizTabContent(
    viewModel: VoiceQuizViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

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

    // Show start screen if quiz hasn't started yet
    if (!state.hasStarted) {
        VoiceQuizStartScreen(
            onStartQuiz = {
                if (hasAudioPermission) {
                    viewModel.startQuiz()
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            hasAudioPermission = hasAudioPermission
        )
        return
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        Spacer(modifier = Modifier.height(16.dp))

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

@Composable
private fun VoiceQuizStartScreen(
    onStartQuiz: () -> Unit,
    hasAudioPermission: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = GradientSaffron
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Voice Quiz",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = DeepBrown,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Answer Gita questions using your voice.\nKrishna will evaluate your wisdom!",
            style = MaterialTheme.typography.bodyLarge,
            color = DeepBrown.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "5 Questions",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = GradientSaffron
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartQuiz,
            colors = ButtonDefaults.buttonColors(containerColor = GradientSaffron),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(0.7f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Start Quiz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (!hasAudioPermission) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Microphone access is required for voice quiz",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC62828),
                textAlign = TextAlign.Center
            )
        }
    }
}
