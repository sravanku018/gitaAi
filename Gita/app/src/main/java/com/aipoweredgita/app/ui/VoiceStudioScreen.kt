package com.aipoweredgita.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.VoiceChatViewModel
import com.aipoweredgita.app.viewmodel.ChatMessage

private val GradientSaffron = Color(0xFFFF8F00)
private val UserBubbleStart = Color(0xFFFF6B35)
private val UserBubbleEnd = Color(0xFFFF9F43)
private val ListeningRed = Color(0xFFFF5722)
private val ThinkingPurple = Color(0xFF7C4DFF)

@Composable
fun VoiceStudioScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    voiceChatViewModel: VoiceChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("voice_studio_prefs", android.content.Context.MODE_PRIVATE)

    DisposableEffect(Unit) {
        voiceChatViewModel.onStartSession()
        onDispose {
            voiceChatViewModel.onStopSession()
        }
    }

    var selectedLanguageMode by remember {
        mutableStateOf(
            com.aipoweredgita.app.utils.LanguageMode.fromString(
                prefs.getString("language_mode", com.aipoweredgita.app.utils.LanguageMode.AUTO.name) ?: "AUTO"
            )
        )
    }

    // FIX: declare BEFORE LaunchedEffect that references it
    var showTryNowDialog by remember { mutableStateOf(false) }

    val isDownloading = com.aipoweredgita.app.ml.ModelDownloadStateManager.isGemmaDownloading(context)

    // Check if Gemma 4 (voice model) is available or downloading
    val gemmaDownloaded = remember(context, isDownloading) {
        com.aipoweredgita.app.ml.ModelAvailability.getInstance(context).areVoiceFeaturesAvailable() || isDownloading
    }

    LaunchedEffect(gemmaDownloaded) {
        if (!gemmaDownloaded && !isDownloading) showTryNowDialog = true
    }

    LaunchedEffect(selectedLanguageMode) {
        voiceChatViewModel.setLanguageMode(selectedLanguageMode)
    }

    fun saveLanguageMode(mode: com.aipoweredgita.app.utils.LanguageMode) {
        selectedLanguageMode = mode
        prefs.edit().putString("language_mode", mode.name).apply()
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape).size(40.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Voice Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.size(40.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.aipoweredgita.app.utils.LanguageMode.entries.forEach { mode ->
                val isSelected = selectedLanguageMode == mode
                Surface(
                    onClick = { saveLanguageMode(mode) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) GradientSaffron else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GradientSaffron else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(mode.displayShort, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        VoiceChatTab(languageMode = selectedLanguageMode, voiceChatViewModel = voiceChatViewModel)
    }

    if (showTryNowDialog) {
        TryNowDialog(onDismiss = { showTryNowDialog = false }, onDownloadStarted = { showTryNowDialog = false })
    }
}

@Composable
private fun VoiceChatTab(
    languageMode: com.aipoweredgita.app.utils.LanguageMode,
    voiceChatViewModel: VoiceChatViewModel = viewModel()
) {
    val state by voiceChatViewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val canInteractWithGemma = state.isLlmReady && !state.isThinking

    LaunchedEffect(languageMode) { voiceChatViewModel.setLanguageMode(languageMode) }

    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) voiceChatViewModel.startListening()
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "chat_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (state.isListening) 1.3f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val thinkingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "thinking_alpha"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.messages.isEmpty() && !state.isThinking) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🙏", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gita Wisdom Chat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Speak or type to ask questions about the\nBhagavad Gita, karma, dharma, and life.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), textAlign = TextAlign.Center, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    if (!state.isLlmReady) {
                        val downloadViewModel: com.aipoweredgita.app.viewmodel.ModelDownloadViewModel = viewModel()
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Download the Gemma 2B model for AI-powered responses.",
                                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer, textAlign = TextAlign.Center)
                                var showTryNow by remember { mutableStateOf(false) }
                                Button(onClick = { showTryNow = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GradientSaffron),
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Try Now - Download Now", fontWeight = FontWeight.Bold)
                                }
                                if (showTryNow) {
                                    TryNowDialog(onDismiss = { showTryNow = false }, onDownloadStarted = { showTryNow = false })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf("What is karma?", "Explain dharma simply", "How to find inner peace?").forEach { suggestion ->
                            val isBusy = state.isThinking || state.isSpeaking || state.isListening || !canInteractWithGemma
                            SuggestionChip(
                                onClick = { if (!isBusy) voiceChatViewModel.sendMessage(suggestion) },
                                enabled = !isBusy,
                                label = { Text(suggestion, fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, labelColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        ChatBubble(message = message, onEdit = { voiceChatViewModel.updateUserInput(it) })
                    }
                    if (state.isThinking) { item { ThinkingBubble(alpha = thinkingAlpha) } }
                }
            }
        }

        AnimatedVisibility(visible = state.error != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            val errorBgColor = when (state.errorType) {
                com.aipoweredgita.app.viewmodel.VoiceChatErrorType.CRASH_RECOVERY -> MaterialTheme.colorScheme.errorContainer
                com.aipoweredgita.app.viewmodel.VoiceChatErrorType.NETWORK -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
            val errorBorderColor = when (state.errorType) {
                com.aipoweredgita.app.viewmodel.VoiceChatErrorType.CRASH_RECOVERY -> MaterialTheme.colorScheme.error
                com.aipoweredgita.app.viewmodel.VoiceChatErrorType.NETWORK -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            val errorTint = when (state.errorType) {
                com.aipoweredgita.app.viewmodel.VoiceChatErrorType.CRASH_RECOVERY -> MaterialTheme.colorScheme.onErrorContainer
                com.aipoweredgita.app.viewmodel.VoiceChatErrorType.NETWORK -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onErrorContainer
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = errorBgColor, shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, errorBorderColor)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = errorTint, modifier = Modifier.size(20.dp))
                    Text(state.error ?: "", modifier = Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall, color = errorTint, fontWeight = FontWeight.Medium)
                    val gemmaExists = com.aipoweredgita.app.ml.ModelDownloadStateManager.isGemmaDownloaded(context)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.errorType == com.aipoweredgita.app.viewmodel.VoiceChatErrorType.CRASH_RECOVERY) {
                            TextButton(onClick = { voiceChatViewModel.clearError() }) {
                                Text("Dismiss", color = errorTint.copy(alpha = 0.6f))
                            }
                        }
                        TextButton(onClick = {
                            voiceChatViewModel.clearError()
                            if (!gemmaExists) com.aipoweredgita.app.ml.ModelDownloadStateManager.startDownload(context)
                            else voiceChatViewModel.refreshModelStatus()
                        }) {
                            Text(if (!gemmaExists) "Download" else "Retry", color = if (!gemmaExists) GradientSaffron else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.liveTranscript.isNotEmpty(), enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp)) {
                Text("\"${state.liveTranscript}\"", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 8.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = state.userInput, onValueChange = { voiceChatViewModel.updateUserInput(it) },
                    enabled = canInteractWithGemma,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Ask your question...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GradientSaffron, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), focusedContainerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp), maxLines = 3
                )
                Text(
                    text = when {
                        !state.isLlmReady -> "Starting Gemma model. Please wait..."
                        !hasAudioPermission -> "Grant microphone access to chat by voice"
                        state.isListening -> "🎙 Listening... Speak now"
                        state.isSpeaking -> "🔊 Teacher is speaking..."
                        state.isThinking -> "✨ Contemplating wisdom..."
                        state.userInput.isNotBlank() -> "Ready to send your message"
                        else -> "Tap the mic to speak, or type above"
                    },
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                    color = when {
                        !state.isLlmReady -> GradientSaffron; state.isListening -> ListeningRed
                        state.isSpeaking -> GradientSaffron; state.isThinking -> ThinkingPurple
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { if (canInteractWithGemma) voiceChatViewModel.clearChat() }, enabled = canInteractWithGemma,
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isListening) {
                            Surface(modifier = Modifier.size(96.dp).scale(pulseScale), shape = CircleShape, color = ListeningRed.copy(alpha = 0.2f)) {}
                            Surface(modifier = Modifier.size(80.dp).scale(pulseScale * 0.9f), shape = CircleShape, color = ListeningRed.copy(alpha = 0.15f)) {}
                        }
                        Surface(
                            modifier = Modifier.size(64.dp).shadow(8.dp, CircleShape).clip(CircleShape).clickable {
                                when {
                                    !canInteractWithGemma -> Unit
                                    state.isSpeaking -> voiceChatViewModel.stopSpeaking()
                                    state.isListening -> voiceChatViewModel.stopListening()
                                    !hasAudioPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    else -> voiceChatViewModel.startListening()
                                }
                            },
                            shape = CircleShape,
                            color = when { state.isListening -> ListeningRed; state.isSpeaking -> GradientSaffron.copy(alpha = 0.6f); state.isThinking -> ThinkingPurple.copy(alpha = 0.6f); else -> GradientSaffron }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = when { state.isListening -> Icons.Default.Stop; state.isSpeaking -> Icons.Default.Stop; else -> Icons.Default.Mic },
                                    contentDescription = "Mic", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    val isBusy = state.isThinking || state.isSpeaking || state.isListening || !canInteractWithGemma
                    IconButton(
                        onClick = { if (state.userInput.isNotBlank() && !isBusy) voiceChatViewModel.sendMessage() },
                        enabled = !isBusy && state.userInput.isNotBlank(),
                        modifier = Modifier.size(48.dp).background(if (state.userInput.isNotBlank()) GradientSaffron.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = if (state.userInput.isNotBlank()) GradientSaffron else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onEdit: (String) -> Unit) {
    val isUser = message.isUser
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = isUser) { onEdit(message.text) },
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = GradientSaffron) { Box(contentAlignment = Alignment.Center) { Text("🙏", fontSize = 18.sp) } }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Surface(modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(topStart = if (isUser) 20.dp else 4.dp, topEnd = if (isUser) 4.dp else 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant, shadowElevation = if (isUser) 0.dp else 2.dp) {
            Box(modifier = if (isUser) Modifier.background(Brush.linearGradient(listOf(UserBubbleStart, UserBubbleEnd))).padding(14.dp) else Modifier.padding(14.dp)) {
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
            }
        }
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = UserBubbleStart) { Box(contentAlignment = Alignment.Center) { Text("🧘", fontSize = 18.sp) } }
        }
    }
}

@Composable
private fun ThinkingBubble(alpha: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = GradientSaffron) { Box(contentAlignment = Alignment.Center) { Text("🙏", fontSize = 18.sp) } }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 2.dp) {
            Row(modifier = Modifier.padding(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    val dotAlpha by animateFloatAsState(targetValue = alpha, animationSpec = tween(300, delayMillis = i * 200), label = "dot_$i")
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = ThinkingPurple.copy(alpha = dotAlpha)) {}
                }
            }
        }
    }
}