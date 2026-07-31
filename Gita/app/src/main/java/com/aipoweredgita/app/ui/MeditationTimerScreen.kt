package com.aipoweredgita.app.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.notifications.MeditationNotificationController
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.viewmodel.BreathingPhase
import com.aipoweredgita.app.viewmodel.MeditationDuration
import com.aipoweredgita.app.viewmodel.MeditationViewModel
import kotlinx.coroutines.delay

/**
 * Ambient background music drone generator for meditation sessions (432Hz warm harmonic tone).
 */
class AmbientMusicPlayer {
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false
    private var workerThread: Thread? = null

    fun start() {
        if (isPlaying) return
        isPlaying = true
        workerThread = Thread {
            try {
                val sampleRate = 44100
                val minSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = Math.max(minSize, 2048)
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
                audioTrack?.play()

                val samples = ShortArray(bufferSize)
                var angle1 = 0.0
                var angle2 = 0.0

                while (isPlaying) {
                    for (i in samples.indices) {
                        val val1 = Math.sin(angle1) * 0.10
                        val val2 = Math.sin(angle2) * 0.05
                        samples[i] = ((val1 + val2) * Short.MAX_VALUE).toInt().toShort()

                        angle1 += 2.0 * Math.PI * 432.0 / sampleRate
                        angle2 += 2.0 * Math.PI * 108.0 / sampleRate
                        if (angle1 > 2.0 * Math.PI) angle1 -= 2.0 * Math.PI
                        if (angle2 > 2.0 * Math.PI) angle2 -= 2.0 * Math.PI
                    }
                    audioTrack?.write(samples, 0, samples.size)
                }
            } catch (e: Exception) {
                // Ignore audio generation interruption
            }
        }
        workerThread?.start()
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) { }
        audioTrack = null
        workerThread = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationTimerScreen(
    onBack: () -> Unit = {},
    onComplete: (minutes: Int) -> Unit = {},
    viewModel: MeditationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val voiceManager = remember { VoiceManager(context) }
    val musicPlayer = remember { AmbientMusicPlayer() }

    var isVoiceEnabled by remember { mutableStateOf(true) }
    var isMusicEnabled by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        MeditationNotificationController.onActionReceived = { action ->
            when (action) {
                MeditationNotificationController.ACTION_PAUSE -> viewModel.pauseTimer()
                MeditationNotificationController.ACTION_RESUME -> viewModel.startTimer()
                MeditationNotificationController.ACTION_STOP -> viewModel.stopTimer()
            }
        }
        onDispose {
            MeditationNotificationController.onActionReceived = null
            MeditationNotificationController.dismissNotification(context)
            voiceManager.destroy()
            musicPlayer.stop()
        }
    }

    LaunchedEffect(uiState.isRunning, uiState.isPaused, uiState.breathingPhase, uiState.breathingTimer, uiState.timeLeftSeconds) {
        if (uiState.isRunning) {
            MeditationNotificationController.showOrUpdateNotification(
                context = context,
                phase = uiState.breathingPhase,
                timerVal = uiState.breathingTimer,
                timeLeftSeconds = uiState.timeLeftSeconds,
                isPaused = uiState.isPaused
            )

            if (!uiState.isPaused) {
                if (isMusicEnabled) {
                    musicPlayer.start()
                } else {
                    musicPlayer.stop()
                }

                if (uiState.breathingTimer == 0 && isVoiceEnabled) {
                    val phrase = when (uiState.breathingPhase) {
                        BreathingPhase.INHALE -> "Inhale"
                        BreathingPhase.HOLD -> "Hold"
                        BreathingPhase.EXHALE -> "Exhale"
                    }
                    voiceManager.speak(phrase, flush = true)
                }
            } else {
                musicPlayer.stop()
                voiceManager.stopSpeaking()
            }
        } else {
            MeditationNotificationController.dismissNotification(context)
            musicPlayer.stop()
            voiceManager.stopSpeaking()
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            MeditationNotificationController.dismissNotification(context)
            if (isVoiceEnabled) {
                voiceManager.speak("Meditation complete. Well done.", flush = true)
            }
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
            // Audio & Voice Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isVoiceEnabled,
                    onClick = {
                        isVoiceEnabled = !isVoiceEnabled
                        if (!isVoiceEnabled) voiceManager.stopSpeaking()
                    },
                    leadingIcon = {
                        Icon(
                            if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null
                        )
                    },
                    label = { Text(if (isVoiceEnabled) "Voice: ON" else "Voice: OFF") }
                )
                FilterChip(
                    selected = isMusicEnabled,
                    onClick = {
                        isMusicEnabled = !isMusicEnabled
                        if (!isMusicEnabled) musicPlayer.stop()
                    },
                    leadingIcon = {
                        Icon(
                            if (isMusicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = null
                        )
                    },
                    label = { Text(if (isMusicEnabled) "Music: ON" else "Music: OFF") }
                )
            }

            Spacer(Modifier.height(24.dp))

            if (!uiState.isRunning) {
                // Duration selection
                Text("Choose Duration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
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
