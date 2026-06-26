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

enum class MeditationDuration(val minutes: Int, val label: String) {
    FIVE(5, "5 min"),
    TEN(10, "10 min"),
    FIFTEEN(15, "15 min"),
    TWENTY(20, "20 min")
}

enum class BreathingPhase(val label: String, val seconds: Int) {
    INHALE("Inhale", 4),
    HOLD("Hold", 4),
    EXHALE("Exhale", 4)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationTimerScreen(
    onBack: () -> Unit = {},
    onComplete: (minutes: Int) -> Unit = {}
) {
    var selectedDuration by remember { mutableStateOf(MeditationDuration.TEN) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableIntStateOf(0) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var breathingPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var breathingTimer by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning, isPaused) {
        if (isRunning && !isPaused) {
            while (timeLeftSeconds > 0) {
                delay(1000)
                timeLeftSeconds--

                // Breathing cycle
                breathingTimer++
                val currentPhaseSeconds = breathingPhase.seconds
                if (breathingTimer >= currentPhaseSeconds) {
                    breathingTimer = 0
                    breathingPhase = when (breathingPhase) {
                        BreathingPhase.INHALE -> BreathingPhase.HOLD
                        BreathingPhase.HOLD -> BreathingPhase.EXHALE
                        BreathingPhase.EXHALE -> BreathingPhase.INHALE
                    }
                }
            }
            if (timeLeftSeconds <= 0) {
                isRunning = false
                onComplete(selectedDuration.minutes)
            }
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
            if (!isRunning) {
                // Duration selection
                Text("Choose Duration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MeditationDuration.entries.forEach { duration ->
                        FilterChip(
                            selected = selectedDuration == duration,
                            onClick = { selectedDuration = duration },
                            label = { Text(duration.label) }
                        )
                    }
                }
                Spacer(Modifier.height(48.dp))
                Button(
                    onClick = {
                        totalSeconds = selectedDuration.minutes * 60
                        timeLeftSeconds = totalSeconds
                        breathingPhase = BreathingPhase.INHALE
                        breathingTimer = 0
                        isRunning = true
                        isPaused = false
                    },
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, "Start", modifier = Modifier.size(48.dp))
                }
            } else {
                // Timer running
                val progress = if (totalSeconds > 0) timeLeftSeconds.toFloat() / totalSeconds else 0f
                val minutes = timeLeftSeconds / 60
                val seconds = timeLeftSeconds % 60

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
                            breathingPhase.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${breathingTimer + 1} / ${breathingPhase.seconds}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                // Controls
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    FilledIconButton(
                        onClick = { isRunning = false; isPaused = false },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(Icons.Default.Stop, "Stop", modifier = Modifier.size(32.dp))
                    }
                    FilledIconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.size(80.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            if (isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}
