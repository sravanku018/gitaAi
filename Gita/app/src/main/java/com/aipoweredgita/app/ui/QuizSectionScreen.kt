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

// Premium color palette
private val GradientSaffron = Color(0xFFFF8F00)

@Composable
fun QuizSectionScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("15 Questions", "25 Questions")

    // Hoist ViewModels to avoid re-creation on tab switches
    val quizViewModel: QuizViewModel = viewModel()

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
