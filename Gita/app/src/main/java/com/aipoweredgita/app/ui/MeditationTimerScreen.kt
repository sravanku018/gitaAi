package com.aipoweredgita.app.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MusicNote
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

enum class AmbientSoundMode(val label: String) {
    DRONE("432Hz Drone 🎵"),
    WATER_DROP("Water Drops 💧"),
    FLUTE("Bansuri Flute 🪈"),
    OFF("Music Off 🔇")
}

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
                                val val1 = Math.sin(angle1) * 0.15
                                angle1 += 2.0 * Math.PI * freq / sampleRate
                                if (angle1 > 2.0 * Math.PI) angle1 -= 2.0 * Math.PI
                                val1
                            }
                            AmbientSoundMode.OFF -> 0.0
                        }

                        val pcmValue = (sampleValue * 32767).toInt().coerceIn(-32768, 32767)
                        samples[i] = pcmValue.toShort()
                    }

                    if (isPlaying && localTrack.state == AudioTrack.STATE_INITIALIZED) {
                        localTrack.write(samples, 0, samples.size)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    localTrack?.stop()
                    localTrack?.release()
                } catch (_: Exception) {}
                audioTrack = null
                isPlaying = false
            }
        }
        workerThread?.start()
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        workerThread = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationTimerScreen(
    onBack: () -> Unit = {},
    onComplete: (Int) -> Unit = {},
    viewModel: MeditationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val voiceManager = remember { VoiceManager(context) }
    var isVoiceEnabled by remember { mutableStateOf(true) }
    var isMusicEnabled by remember { mutableStateOf(true) }
    var selectedSoundMode by remember { mutableStateOf(AmbientSoundMode.DRONE) }
    var showDurationDropdown by remember { mutableStateOf(false) }

    val musicPlayer = remember { AmbientMusicPlayer() }
    val goldColor = Color(0xFFF59E0B)

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopSpeaking()
            musicPlayer.stop()
            MeditationNotificationController.dismissNotification(context)
        }
    }

    LaunchedEffect(uiState.isRunning, uiState.isPaused, isMusicEnabled, selectedSoundMode) {
        if (uiState.isRunning && !uiState.isPaused && isMusicEnabled && selectedSoundMode != AmbientSoundMode.OFF) {
            musicPlayer.start(selectedSoundMode)
        } else {
            musicPlayer.stop()
        }
    }

    LaunchedEffect(uiState.breathingPhase, isVoiceEnabled) {
        if (uiState.isRunning && !uiState.isPaused && isVoiceEnabled) {
            val phrase = when (uiState.breathingPhase) {
                BreathingPhase.INHALE -> "Inhale"
                BreathingPhase.HOLD -> "Hold"
                BreathingPhase.EXHALE -> "Exhale"
            }
            voiceManager.speak(phrase, flush = true)
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
                title = { Text("Meditation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    val coinsToEarn = uiState.selectedDuration.minutes * 2
                    Surface(
                        color = goldColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "🪙 +${coinsToEarn} Coins",
                            color = goldColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Voice & Music Control Toggles Row with Symbols
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Voice Guide Toggle
                FilterChip(
                    selected = isVoiceEnabled,
                    onClick = {
                        isVoiceEnabled = !isVoiceEnabled
                        if (!isVoiceEnabled) voiceManager.stopSpeaking()
                    },
                    leadingIcon = {
                        Icon(
                            if (isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Voice"
                        )
                    },
                    label = { Text(if (isVoiceEnabled) "Voice: ON" else "Voice: OFF") }
                )

                // Music ON / OFF Toggle with Symbol
                FilterChip(
                    selected = isMusicEnabled,
                    onClick = {
                        isMusicEnabled = !isMusicEnabled
                        if (!isMusicEnabled) {
                            musicPlayer.stop()
                        } else if (uiState.isRunning && !uiState.isPaused) {
                            musicPlayer.start(selectedSoundMode)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            if (isMusicEnabled) Icons.Default.MusicNote else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Music"
                        )
                    },
                    label = { Text(if (isMusicEnabled) "Music: ON 🎵" else "Music: OFF 🔇") }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Ambient Sound Selection Chips (No Sound Dropdown as requested!)
            Text("Ambient Sound", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                AmbientSoundMode.entries.forEach { modeOption ->
                    val isSel = selectedSoundMode == modeOption
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            selectedSoundMode = modeOption
                            if (modeOption == AmbientSoundMode.OFF) {
                                musicPlayer.stop()
                            } else if (uiState.isRunning && !uiState.isPaused) {
                                musicPlayer.stop()
                                musicPlayer.start(modeOption)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = goldColor,
                            selectedLabelColor = Color.Black,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        label = {
                            Text(
                                text = modeOption.label,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // CIRCLE WITH TIME & DROPDOWN IN THE MIDDLE
            val progress = if (uiState.totalSeconds > 0) uiState.timeLeftSeconds.toFloat() / uiState.totalSeconds else 1f
            val minutes = if (uiState.isRunning) uiState.timeLeftSeconds / 60 else uiState.selectedDuration.minutes
            val seconds = if (uiState.isRunning) uiState.timeLeftSeconds % 60 else 0

            Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(260.dp)) {
                    // Outer Track
                    drawCircle(
                        color = goldColor.copy(alpha = 0.2f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    // Progress Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFFF6400), Color(0xFFF59E0B))
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                        size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx())
                    )
                }

                // Middle Content: Show Only Time with Dropdown in Middle with Number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!uiState.isRunning) {
                        // Time Number Dropdown Selector right in the middle of the circle
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                onClick = { showDurationDropdown = true },
                                shape = RoundedCornerShape(20.dp),
                                color = goldColor.copy(alpha = 0.15f),
                                border = BorderStroke(2.dp, goldColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "%02d:00".format(uiState.selectedDuration.minutes),
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 46.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Duration",
                                        tint = goldColor,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDurationDropdown,
                                onDismissRequest = { showDurationDropdown = false }
                            ) {
                                MeditationDuration.entries.forEach { duration ->
                                    val coins = duration.minutes * 2
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${duration.label} (+${coins} 🪙)",
                                                fontWeight = if (uiState.selectedDuration == duration) FontWeight.Bold else FontWeight.Normal,
                                                color = if (uiState.selectedDuration == duration) goldColor else Color.Unspecified
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectDuration(duration)
                                            showDurationDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Tap time to change duration",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        // Running Countdown Timer in Middle of Circle
                        Text(
                            text = "%02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 56.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = uiState.breathingPhase.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = goldColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.breathingTimer + 1} / ${uiState.breathingPhase.seconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            if (!uiState.isRunning) {
                // Big Start Button below Middle Circle
                Button(
                    onClick = { viewModel.startTimer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = goldColor)
                ) {
                    Icon(Icons.Default.PlayArrow, "Start", tint = Color.Black, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "START MEDITATION",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Pause / Stop Controls
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            if (uiState.isPaused) viewModel.startTimer() else viewModel.pauseTimer()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B2E))
                    ) {
                        Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = goldColor)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isPaused) "Resume" else "Pause", color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.stopTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Stop, null, tint = Color(0xFFEF4444))
                        Spacer(Modifier.width(8.dp))
                        Text("Stop", color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}
