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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.ui.theme.GitaLearningTheme

@Composable
fun QuizSectionScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("15 Questions", "25 Questions")

    // Hoist ViewModels to avoid re-creation on tab switches
    val quizViewModel: QuizViewModel = viewModel()
    val quizState by quizViewModel.quizState.collectAsState()
    val language = quizState.language

    var isStarted by remember(selectedTab) { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Remove auto-start logic from LaunchedEffect to allow manual start
    LaunchedEffect(selectedTab) {
        // Pre-configure but don't start
        when (selectedTab) {
            0 -> quizViewModel.setQuizLimit(15)
            1 -> quizViewModel.setQuizLimit(25)
        }
    }

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
                text = translateLandingText("Quiz Section", language),
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
                            color = MaterialTheme.colorScheme.primary
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
                                    tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = translateLandingText(title, language),
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
            label = "tab_content"
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
            }
        }

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
    }
}

@Composable
private fun QuizStartLanding(
    title: String,
    description: String,
    onStart: () -> Unit,
    language: String = "tel"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = translateLandingText(title, language),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = translateLandingText(description, language),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(translateLandingText("Start Quiz", language), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = translateLandingText("Every question is a step closer to self-realization.", language),
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
