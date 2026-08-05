package com.aipoweredgita.app.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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

enum class AmbientSoundMode(val label: String) {
    DRONE("432Hz Drone"),
    WATER_DROP("Water Drops 💧"),
    FLUTE("Bansuri Flute 🪈"),
    OFF("Music Off")
}

/**
 * Procedural background music drone & sound generator for meditation sessions.
 * Highly safe lifecycle management with try-finally resource cleanup.
 * 100% Royalty-Free & Copyright-Free synthesized code.
 */
class AmbientMusicPlayer {
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false
    @Volatile
    var mode: AmbientSoundMode = AmbientSoundMode.DRONE
    private var workerThread: Thread? = null

    fun start(soundMode: AmbientSoundMode = mode) {
        mode = soundMode
        if (mode == AmbientSoundMode.OFF) {
            stop()
            return
        }
        if (isPlaying) return
        isPlaying = true
        workerThread = Thread {
            var localTrack: AudioTrack? = null
            try {
                val sampleRate = 44100
                val minSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = Math.max(minSize, 2048)
                localTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
                audioTrack = localTrack
                if (localTrack.state == AudioTrack.STATE_INITIALIZED) {
                    localTrack.play()
                } else {
                    isPlaying = false
                    return@Thread
                }

                val samples = ShortArray(bufferSize)
                var angle1 = 0.0
                var angle2 = 0.0
                var sampleIndex = 0L

                val fluteScale = doubleArrayOf(329.63, 392.00, 440.00, 493.88, 587.33, 659.25)
                var currentFluteNoteIndex = 0
                var nextNoteChangeSample = 0L

                while (isPlaying && audioTrack != null) {
                    val currentMode = mode
                    if (currentMode == AmbientSoundMode.OFF) break

                    for (i in samples.indices) {
                        sampleIndex++
                        val t = sampleIndex.toDouble() / sampleRate

                        val sampleValue: Double = when (currentMode) {
                            AmbientSoundMode.DRONE -> {
                                val val1 = Math.sin(angle1) * 0.10
                                val val2 = Math.sin(angle2) * 0.05
                                angle1 += 2.0 * Math.PI * 432.0 / sampleRate
                                angle2 += 2.0 * Math.PI * 108.0 / sampleRate
                                if (angle1 > 2.0 * Math.PI) angle1 -= 2.0 * Math.PI
                                if (angle2 > 2.0 * Math.PI) angle2 -= 2.0 * Math.PI
                                val1 + val2
                            }
                            AmbientSoundMode.WATER_DROP -> {
                                val dropPeriodSamples = (1.2 * sampleRate).toLong()
                                val dropPhase = sampleIndex % dropPeriodSamples
                                val dropDurationSamples = (0.15 * sampleRate).toLong()

                                var dropVal = 0.0
                                if (dropPhase < dropDurationSamples) {
                                    val dropProgress = dropPhase.toDouble() / dropDurationSamples
                                    val freq = 900.0 - (550.0 * dropProgress)
                                    val envelope = Math.exp(-dropProgress * 6.0)
                                    dropVal = Math.sin(angle1) * 0.22 * envelope
                                    angle1 += 2.0 * Math.PI * freq / sampleRate
                                    if (angle1 > 2.0 * Math.PI) angle1 -= 2.0 * Math.PI
                                }
                                val trickle = (Math.random() - 0.5) * 0.015
                                dropVal + trickle
                            }
                            AmbientSoundMode.FLUTE -> {
                                if (sampleIndex >= nextNoteChangeSample) {
                                    currentFluteNoteIndex = (currentFluteNoteIndex + 1) % fluteScale.size
                                    nextNoteChangeSample = sampleIndex + (1.8 * sampleRate).toLong()
                                }
                                val baseFreq = fluteScale[currentFluteNoteIndex]
                                val vibrato = Math.sin(2.0 * Math.PI * 5.0 * t) * 3.5
                                val freq = baseFreq + vibrato

                                angle1 += 2.0 * Math.PI * freq / sampleRate
                                if (angle1 > 2.0 * Math.PI) angle1 -= 2.0 * Math.PI

                                val val1 = Math.sin(angle1) * 0.12
                                val val2 = Math.sin(angle1 * 2.0) * 0.03
                                val1 + val2
                            }
                            AmbientSoundMode.OFF -> 0.0
                        }

                        samples[i] = (sampleValue * Short.MAX_VALUE)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                    localTrack.write(samples, 0, samples.size)
                }
            } catch (_: Exception) {
            } finally {
                isPlaying = false
                try {
                    localTrack?.stop()
                    localTrack?.release()
                } catch (_: Exception) {}
            }
        }
        workerThread?.start()
    }

    fun stop() {
        isPlaying = false
        val trackToRelease = audioTrack
        audioTrack = null
        try {
            if (trackToRelease?.state == AudioTrack.STATE_INITIALIZED) {
                trackToRelease.stop()
            }
            trackToRelease?.release()
        } catch (_: Exception) { }
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
    var selectedSoundMode by remember { mutableStateOf(AmbientSoundMode.DRONE) }

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
            musicPlayer.stop()
            voiceManager.destroy()
        }
    }

    LaunchedEffect(uiState.isRunning, uiState.isPaused, uiState.breathingPhase, uiState.breathingTimer, uiState.timeLeftSeconds, selectedSoundMode) {
        if (uiState.isRunning) {
            MeditationNotificationController.showOrUpdateNotification(
                context = context,
                phase = uiState.breathingPhase,
                timerVal = uiState.breathingTimer,
                timeLeftSeconds = uiState.timeLeftSeconds,
                isPaused = uiState.isPaused
            )

            if (!uiState.isPaused) {
                if (selectedSoundMode != AmbientSoundMode.OFF) {
                    musicPlayer.start(selectedSoundMode)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                FilterChip(
                    selected = isVoiceEnabled,
                    onClick = {
                        isVoiceEnabled = !isVoiceEnabled
                        if (!isVoiceEnabled) voiceManager.stopSpeaking()
                    },
                    leadingIcon = {
                        Icon(
                            if (isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null
                        )
                    },
                    label = { Text(if (isVoiceEnabled) "Voice: ON" else "Voice: OFF") }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Sound Mode Options (Drone, Water Drops, Flute, Off)
            Text("Ambient Sound", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                AmbientSoundMode.entries.forEach { modeOption ->
                    FilterChip(
                        selected = selectedSoundMode == modeOption,
                        onClick = {
                            selectedSoundMode = modeOption
                            if (modeOption == AmbientSoundMode.OFF) {
                                musicPlayer.stop()
                            } else if (uiState.isRunning && !uiState.isPaused) {
                                musicPlayer.stop()
                                musicPlayer.start(modeOption)
                            }
                        },
                        label = { Text(modeOption.label) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (!uiState.isRunning) {
                // Meditation Reward Banner
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261D0C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🧘 Meditate & Earn Krishna Coins 🪙",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "5m = +10 🪙  |  10m = +20 🪙  |  15m = +30 🪙  |  20m = +40 🪙",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Duration selection
                Text("Choose Duration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeditationDuration.entries.forEach { duration ->
                        val coins = duration.minutes * 2
                        FilterChip(
                            selected = uiState.selectedDuration == duration,
                            onClick = { viewModel.selectDuration(duration) },
                            label = { Text("${duration.label} (+${coins}🪙)") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Reward: +${uiState.selectedDuration.minutes * 2} Krishna Coins 🪙 upon completion",
                    color = Color(0xFFF59E0B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(32.dp))
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
