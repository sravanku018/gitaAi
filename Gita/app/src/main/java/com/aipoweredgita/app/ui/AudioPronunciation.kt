package com.aipoweredgita.app.ui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object SanskritAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null

    fun play(url: String, onCompletion: () -> Unit = {}) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    onCompletion()
                    currentUrl = null
                }
                setOnErrorListener { _, _, _ ->
                    currentUrl = null
                    true
                }
                prepareAsync()
            }
            currentUrl = url
        } catch (e: Exception) {
            currentUrl = null
        }
    }

    fun stop() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentUrl = null
    }

    fun isPlaying(url: String): Boolean = currentUrl == url && mediaPlayer?.isPlaying == true
}

@Composable
fun AudioPronunciationButton(
    chapterNo: Int,
    verseNo: Int,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val audioUrl = "https://audio.example.com/gita/${chapterNo}_${verseNo}.mp3" // Placeholder URL

    DisposableEffect(Unit) {
        onDispose { SanskritAudioPlayer.stop() }
    }

    IconButton(
        onClick = {
            if (isPlaying) {
                SanskritAudioPlayer.stop()
                isPlaying = false
            } else {
                isLoading = true
                SanskritAudioPlayer.play(audioUrl) {
                    isPlaying = false
                    isLoading = false
                }
                isPlaying = true
                isLoading = false
            }
        },
        modifier = modifier,
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play pronunciation",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AudioSpeedControl(
    currentSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Speed:", style = MaterialTheme.typography.labelSmall)
        listOf(0.75f to "0.75x", 1.0f to "1x", 1.5f to "1.5x").forEach { (speed, label) ->
            FilterChip(
                selected = currentSpeed == speed,
                onClick = { onSpeedChange(speed) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
