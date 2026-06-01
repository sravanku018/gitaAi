package com.aipoweredgita.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
<<<<<<< HEAD
import androidx.compose.foundation.isSystemInDarkTheme
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
=======
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
<<<<<<< HEAD
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.ui.theme.GitaLearningTheme
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
=======
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.QuizViewModel

// Premium color palette
private val GradientSaffron = Color(0xFFFF8F00)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

@Composable
fun QuizSectionScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("15 Questions", "25 Questions")

    // Hoist ViewModels to avoid re-creation on tab switches
    val quizViewModel: QuizViewModel = viewModel()
<<<<<<< HEAD
    val quizState by quizViewModel.quizState.collectAsState()
    val language = quizState.language

    var isStarted by remember(selectedTab) { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Auto-download questions on first open
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(ctx)
                val count = db.quizQuestionBankDao().getTotalCount()
                if (count < 50) {
                    android.util.Log.d("QuizSection", "Auto-downloading questions...")
                    val importer = com.aipoweredgita.app.ml.BhagavadGitaQAImporter(ctx, db.quizQuestionBankDao())
                    importer.importDataset(language = "english", batchSize = 500)
                    android.util.Log.d("QuizSection", "Auto-download complete")
                }
            } catch (e: Exception) {
                android.util.Log.w("QuizSection", "Auto-download failed: ${e.message}")
            }
        }
    }

=======

    var isStarted by remember(selectedTab) { mutableStateOf(false) }

    // Remove auto-start logic from LaunchedEffect to allow manual start
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    LaunchedEffect(selectedTab) {
        // Pre-configure but don't start
        when (selectedTab) {
            0 -> quizViewModel.setQuizLimit(15)
            1 -> quizViewModel.setQuizLimit(25)
        }
    }

<<<<<<< HEAD
    val isDark = isSystemInDarkTheme()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        if (isDark) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier.fillMaxSize()
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Back",
                        tint = textPrimary.copy(alpha = 0.9f)
                    )
                }

                Text(
                    text = translateLandingText("Quiz Section", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = gold
                    )
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            // Tab Row
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                cornerRadius = 24.dp,
                elevation = 4.dp,
                tint = cardBg,
                border = cardBorder
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = textPrimary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = gold
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
                                        imageVector = Icons.Filled.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedTab == index) gold else textPrimary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = translateLandingText(title, language),
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) textPrimary else textPrimary.copy(alpha = 0.5f)
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
                targetState = selectedTab to isStarted,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(300))
                },
                label = "tab_content",
                modifier = Modifier.weight(1f)
            ) { (tab, started) ->
                if (!started) {
                    QuizStartLanding(
                        title = if (tab == 0) "15 Question Marathon" else "25 Question Challenge",
                        description = if (tab == 0) 
                            "A quick spiritual check-in to test your knowledge of the Bhagavad Gita's fundamental truths."
                            else "An in-depth journey through the sacred verses. Ready to test your mastery of divine wisdom?",
                        onStart = {
                            showLanguageDialog = true
                        },
                        language = language
                    )
                } else {
                    QuizTabContent(quizViewModel = quizViewModel, onExit = onExit)
=======
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }

            Text(
                "Quiz Section",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.size(40.dp))
        }

        // Tab Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shadowElevation = 2.dp
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
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
                                    tint = if (selectedTab == index) GradientSaffron else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                }
            }
        }

<<<<<<< HEAD
        if (showLanguageDialog) {
            QuizLanguageDialog(
                onLanguageSelected = { lang ->
                    quizViewModel.setLanguage(lang)
                    quizViewModel.resetQuiz()
                    isStarted = true
                    showLanguageDialog = false
                },
                onCancel = { showLanguageDialog = false }
            )
        }
    }
}

private fun translateLandingText(text: String, language: String): String {
    if (language != "tel") return text
    return when (text) {
        "Quiz Section" -> "క్విజ్ విభాగం"
        "15 Questions" -> "15 ప్రశ్నలు"
        "25 Questions" -> "25 ప్రశ్నలు"
        "15 Question Marathon" -> "15 ప్రశ్నల మహోత్సవం"
        "25 Question Challenge" -> "25 ప్రశ్నల సవాలు"
        "A quick spiritual check-in to test your knowledge of the Bhagavad Gita's fundamental truths." -> "భగవద్గీత యొక్క ప్రాథమిక సత్యాలపై మీ జ్ఞానాన్ని పరీక్షించడానికి ఒక చిన్న ఆధ్యాత్మిక విశ్లేషణ."
        "An in-depth journey through the sacred verses. Ready to test your mastery of divine wisdom?" -> "పవిత్ర శ్లోకాల ద్వారా ఒక లోతైన ప్రయాణం. దైవిక జ్ఞానంపై మీ నైపుణ్యాన్ని పరీక్షించడానికి సిద్ధంగా ఉన్నారా?"
        "Start Quiz" -> "క్విజ్ ప్రారంభించండి"
        "Every question is a step closer to self-realization." -> "ప్రతి ప్రశ్నా మిమ్మల్ని ఆత్మసాక్షాత్కారానికి ఒక అడుగు దగ్గరగా తీసుకెళ్తుంది."
        else -> text
=======
        Spacer(modifier = Modifier.height(8.dp))

        // Content
        AnimatedContent(
            targetState = selectedTab to isStarted,
            transitionSpec = {
                fadeIn(tween(400)) togetherWith fadeOut(tween(300))
            },
            label = "tab_content"
        ) { (tab, started) ->
            if (!started) {
                QuizStartLanding(
                    title = if (tab == 0) "15 Question Marathon" else "25 Question Challenge",
                    description = if (tab == 0) 
                        "A quick spiritual check-in to test your knowledge of the Bhagavad Gita's fundamental truths."
                        else "An in-depth journey through the sacred verses. Ready to test your mastery of divine wisdom?",
                    onStart = {
                        quizViewModel.resetQuiz()
                        isStarted = true
                    }
                )
            } else {
                QuizTabContent(quizViewModel = quizViewModel, onExit = onExit)
            }
        }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    }
}

@Composable
private fun QuizStartLanding(
    title: String,
    description: String,
<<<<<<< HEAD
    onStart: () -> Unit,
    language: String = "tel"
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron

=======
    onStart: () -> Unit
) {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
<<<<<<< HEAD
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                .border(2.dp, Brush.linearGradient(listOf(gold, Saffron)), CircleShape)
                .shadow(8.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = gold
            )
=======
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = GradientSaffron.copy(alpha = 0.1f),

            border = BorderStroke(2.dp, GradientSaffron.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = GradientSaffron
                )
            }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        }

        Spacer(Modifier.height(32.dp))

        Text(
<<<<<<< HEAD
            text = translateLandingText(title, language),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = gold
            )
=======
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        )

        Spacer(Modifier.height(12.dp))

        Text(
<<<<<<< HEAD
            text = translateLandingText(description, language),
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = textSecondary
            ),
=======
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
<<<<<<< HEAD
                .height(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Saffron, gold)
                    ),
                    shape = MaterialTheme.shapes.large
                ),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            shape = MaterialTheme.shapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    translateLandingText("Start Quiz", language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
=======
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GradientSaffron),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Quiz", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
<<<<<<< HEAD
            text = translateLandingText("Every question is a step closer to self-realization.", language),
            style = MaterialTheme.typography.labelSmall.copy(
                fontStyle = FontStyle.Italic,
                color = textSecondary.copy(alpha = 0.7f)
            )
=======
            text = "Every question is a step closer to self-realization.",
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        )
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
<<<<<<< HEAD

@Preview(showBackground = true, name = "Quiz Landing Light")
@Composable
fun PreviewQuizStartLandingLight() {
    GitaLearningTheme(darkTheme = false) {
        QuizStartLanding(
            title = "15 Question Marathon",
            description = "A quick spiritual check-in to test your knowledge.",
            onStart = {}
        )
    }
}

@Preview(showBackground = true, name = "Quiz Landing Dark", backgroundColor = 0xFF0F0F0F)
@Composable
fun PreviewQuizStartLandingDark() {
    GitaLearningTheme(darkTheme = true) {
        QuizStartLanding(
            title = "15 Question Marathon",
            description = "A quick spiritual check-in to test your knowledge.",
            onStart = {}
        )
    }
}
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
