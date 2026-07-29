package com.aipoweredgita.app.ui.screens.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.QuizLanguageDialog
import com.aipoweredgita.app.ui.screens.quiz.components.QuizStartLanding
import com.aipoweredgita.app.ui.screens.quiz.components.translateLandingText
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.viewmodel.QuizViewModel

@Composable
fun QuizSectionScreen(
    onExit: () -> Unit,
    onNavigateToQuizBattle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("15 Questions", "25 Questions", "⚔️ Battle Quiz")

    // Hoist ViewModels to avoid re-creation on tab switches
    val quizViewModel: QuizViewModel = hiltViewModel()
    val quizState by quizViewModel.quizState.collectAsState()
    val language = quizState.language

    var isStarted by remember(selectedTab) { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Auto-download questions on first open
    LaunchedEffect(Unit) {
        quizViewModel.checkAndDownloadQuestions()
    }

    LaunchedEffect(selectedTab) {
        quizViewModel.resetQuiz()
        // Pre-configure but don't start
        when (selectedTab) {
            0 -> quizViewModel.setQuizLimit(15)
            1 -> quizViewModel.setQuizLimit(25)
        }
    }

    val isDark = isSystemInDarkTheme()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val gold = if (isDark) GoldSpark else Color(0xFFD84315)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 4.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val pillBg = if (isSelected) {
                            if (isDark) gold.copy(alpha = 0.2f) else gold.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        }
                        val pillBorder = if (isSelected) {
                            gold.copy(alpha = 0.4f)
                        } else {
                            Color.Transparent
                        }
                        val contentColor = if (isSelected) gold else textPrimary.copy(alpha = 0.6f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(pillBg)
                                .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (index == 2) Icons.Filled.SportsMma else Icons.Filled.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = translateLandingText(title, language),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1
                                )
                            }
                        }
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
                if (tab == 2) {
                    QuizStartLanding(
                        title = "⚔️ Mahabharata Battle Quiz",
                        description = "10,091 sequence MCQs (5,000 medium + 5,091 hard) in English & Telugu! Test your epic battle timeline knowledge in rapid-fire rounds.",
                        onStart = {
                            onNavigateToQuizBattle()
                        },
                        language = language
                    )
                } else if (!started) {
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

@Composable
private fun QuizTabContent(
    quizViewModel: QuizViewModel,
    onExit: () -> Unit
) {
    QuizScreen(
        onExitQuiz = onExit,
        viewModel = quizViewModel
    )
}
