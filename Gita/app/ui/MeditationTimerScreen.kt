package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.viewmodel.MeditationViewModel
import com.aipoweredgita.app.viewmodel.MeditationDuration
import com.aipoweredgita.app.viewmodel.BreathingPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationTimerScreen(
    onBack: () -> Unit = {},
    onComplete: (minutes: Int) -> Unit = {},
    viewModel: MeditationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onComplete(uiState.selectedDuration.minutes)
            viewModel.onCompletedAcknowledged()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meditation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!uiState.isRunning) {
                // Duration selection
                Text("Choose Duration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MeditationDuration.entries.forEach { duration ->
                        FilterChip(
                            selected = uiState.selectedDuration == duration,
                            onClick = { viewModel.selectDuration(duration) },
                            label = { Text(duration.label) }
                        )
                    }
                }
                Spacer(Modifier.height(48.dp))
                Button(
                    onClick = {
                        viewModel.startTimer()
                    },
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, "Start", modifier = Modifier.size(48.dp))
                }
            } else {
                // Timer running
                val progress = if (uiState.totalSeconds > 0) uiState.timeLeftSeconds.toFloat() / uiState.totalSeconds else 0f
                val minutes = uiState.timeLeftSeconds / 60
                val seconds = uiState.timeLeftSeconds % 60

                // Circular timer
                Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(260.dp)) {
                        // Background circle
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.2f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        // Progress arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFF6B48FF), Color(0xFF00D4FF), Color(0xFF6B48FF))
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 64.sp
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            uiState.breathingPhase.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${uiState.breathingTimer + 1} / ${uiState.breathingPhase.seconds}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                // Controls
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    FilledIconButton(
                        onClick = { viewModel.stopTimer() },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(Icons.Default.Stop, "Stop", modifier = Modifier.size(32.dp))
                    }
                    FilledIconButton(
                        onClick = { if (uiState.isPaused) viewModel.startTimer() else viewModel.pauseTimer() },
                        modifier = Modifier.size(80.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            if (uiState.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}
