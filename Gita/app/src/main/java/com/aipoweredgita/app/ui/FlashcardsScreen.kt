package com.aipoweredgita.app.ui

<<<<<<< HEAD
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.Flashcard
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.ui.components.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.aipoweredgita.app.ui.theme.GoldSpark
import androidx.compose.ui.graphics.luminance
import com.aipoweredgita.app.ui.theme.Saffron
=======
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.Flashcard
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import com.aipoweredgita.app.ui.LocalUiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(topic: String, onBack: () -> Unit) {
<<<<<<< HEAD
    val context = LocalContext.current
    val db = remember { GitaDatabase.getDatabase(context) }
    val cards by db.flashcardDao().getByTopic(topic).collectAsState(initial = emptyList())
    val uiCfg = LocalUiConfig.current
    val coroutineScope = rememberCoroutineScope()

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var deckCompleted by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var playCompletionConfetti by remember { mutableIntStateOf(0) }

    // Reset when topic changes
    LaunchedEffect(topic) {
        currentIndex = 0
        isFlipped = false
        deckCompleted = false
        correctCount = 0
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "card_flip"
    )

    val isDark = isSystemInDarkTheme()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val gold = if (isDark) GoldSpark else Saffron

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        if (isDark) {
            com.aipoweredgita.app.ui.components.AmbientOrbs(modifier = Modifier.fillMaxSize())
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (topic.isBlank()) "Flashcards" else "Flashcards: $topic", color = textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = gold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = textPrimary,
                        navigationIconContentColor = textPrimary
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
            if (cards.isEmpty()) {
                // Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("🎴", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No flashcards found for this topic.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (deckCompleted) {
                // Completed state with high-fidelity confetti
                ConfettiBurst(
                    playId = playCompletionConfetti,
                    count = 100,
                    onFinished = {}
                )

                GlassCard(
                    modifier = Modifier
                        .width(340.dp)
                        .wrapContentHeight()
                        .padding(24.dp),
                    cornerRadius = 32.dp,
                    elevation = 8.dp,
                    tint = cardBg,
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Deck Completed!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You got $correctCount correct out of ${cards.size}!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    currentIndex = 0
                                    isFlipped = false
                                    deckCompleted = false
                                    correctCount = 0
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Saffron)
                            ) {
                                Text("Study Again")
                            }
                        }
                    }
                }
            } else {
                // Main flashcard review flow
                val card = cards.getOrNull(currentIndex)
                if (card != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Progress bar at the top
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Card ${currentIndex + 1} of ${cards.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val progressValue = (currentIndex.toFloat() + 1) / cards.size
                            LinearProgressIndicator(
                                progress = { progressValue },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = gold,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // The 3D-flipping Card Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .weight(1f)
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(300.dp)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        rotationY = rotation
                                        cameraDistance = 8f * density
                                    }
                                    .shadow(8.dp, MaterialTheme.shapes.extraLarge)
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                gold,
                                                Saffron
                                            )
                                        ),
                                        shape = MaterialTheme.shapes.extraLarge
                                    )
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(cardBg)
                                    .clickable { isFlipped = !isFlipped }
                            ) {
                                if (rotation <= 90f) {
                                    // Front Side
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "QUESTION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) GoldSpark else Color(0xFFC2410C),
                                            letterSpacing = 1.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = card.frontText,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = FontFamily.Serif
                                            ),
                                            fontWeight = FontWeight.Medium,
                                            color = textPrimary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 28.sp
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "Tap to reveal translation",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary
                                        )
                                    }
                                } else {
                                    // Back Side (mirrored, so we apply correction)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationY = 180f }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "TRANSLATION",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF81C784) else Color(0xFF2D5016),
                                                letterSpacing = 1.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                Text(
                                                    text = card.backText,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = textPrimary,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 22.sp
                                                )
                                            }
                                            if (card.chapterNo > 0 && card.verseNo > 0) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Bhagavad Gita ${card.chapterNo}.${card.verseNo}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = gold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Tap to flip back",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom SRS voting buttons (fade in when card is flipped)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedVisibility(
                                visible = rotation > 90f,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Hard button
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                db.flashcardDao().update(
                                                    card.copy(
                                                        timesShown = card.timesShown + 1
                                                    )
                                                )
                                            }
                                            if (currentIndex < cards.size - 1) {
                                                currentIndex++
                                                isFlipped = false
                                            } else {
                                                playCompletionConfetti++
                                                deckCompleted = true
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Need Practice 🔄", fontWeight = FontWeight.Bold)
                                    }

                                    // Easy button
                                    Button(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                db.flashcardDao().update(
                                                    card.copy(
                                                        timesShown = card.timesShown + 1,
                                                        timesCorrect = card.timesCorrect + 1
                                                    )
                                                )
                                            }
                                            correctCount++
                                            if (currentIndex < cards.size - 1) {
                                                currentIndex++
                                                isFlipped = false
                                            } else {
                                                playCompletionConfetti++
                                                deckCompleted = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2D5016)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Got It! 🎯", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
=======
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = GitaDatabase.getDatabase(context)
    val cards by db.flashcardDao().getByTopic(topic).collectAsState(initial = emptyList())
    val uiCfg = LocalUiConfig.current
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (topic.isBlank()) "Flashcards" else "Flashcards: $topic") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(if (uiCfg.isLandscape) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                items = cards,
                key = { flashcard: Flashcard -> flashcard.id }
            ) { c ->
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(c.frontText, style = MaterialTheme.typography.titleMedium)
                    Text(c.backText, style = MaterialTheme.typography.bodySmall)
                } }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            }
        }
    }
}
<<<<<<< HEAD
}
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
