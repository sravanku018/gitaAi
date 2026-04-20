package com.aipoweredgita.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.viewmodel.VoiceChatViewModel
import com.aipoweredgita.app.viewmodel.ChatMessage
import com.aipoweredgita.app.viewmodel.VoiceChatState
import com.aipoweredgita.app.ui.theme.*

// ── Shared Sacred Gold Palette ──────────────────────────
@Composable
private fun getVoiceStudioColors() = object {
    val Border        = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val BorderHi      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val TextPrimary   = MaterialTheme.colorScheme.onSurface
    val TextSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val TextMuted     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val ListenRed     = CrimsonDeep
    val SpeakGreen    = Forest
    val UserBubbleBg  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    val UserBubbleBdr = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val RevolvingYellow = GoldBright
}

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
        onDispose { voiceChatViewModel.onStopSession() }
    }

    var selectedLanguageMode by remember {
        mutableStateOf(
            com.aipoweredgita.app.utils.LanguageMode.fromString(
                prefs.getString("language_mode", com.aipoweredgita.app.utils.LanguageMode.AUTO.name) ?: "AUTO"
            )
        )
    }

    fun saveLanguageMode(mode: com.aipoweredgita.app.utils.LanguageMode) {
        selectedLanguageMode = mode
        prefs.edit().putString("language_mode", mode.name).apply()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        VoiceChatTab(
            languageMode = selectedLanguageMode,
            voiceChatViewModel = voiceChatViewModel,
            onExit = onExit,
            onLanguageModeChange = { saveLanguageMode(it) }
        )
    }
}

// ── Root tab wrapper ──────────────────────────────────────────────────────────

@Composable
private fun VoiceChatTab(
    languageMode: com.aipoweredgita.app.utils.LanguageMode,
    voiceChatViewModel: VoiceChatViewModel = viewModel(),
    onExit: () -> Unit,
    onLanguageModeChange: (com.aipoweredgita.app.utils.LanguageMode) -> Unit
) {
    val state by voiceChatViewModel.state.collectAsState()
    VoiceChatContent(
        state = state,
        languageMode = languageMode,
        onSendMessage          = { voiceChatViewModel.sendMessage(it) },
        onSendCurrentMessage   = { voiceChatViewModel.sendMessage() },
        onUpdateUserInput      = { voiceChatViewModel.updateUserInput(it) },
        onClearChat            = { voiceChatViewModel.clearChat() },
        onStartListening       = { voiceChatViewModel.startListening() },
        onStopListening        = { voiceChatViewModel.stopListening() },
        onStopSpeaking         = { voiceChatViewModel.stopSpeaking() },
        onClearError           = { voiceChatViewModel.clearError() },
        onRefreshModelStatus   = { voiceChatViewModel.refreshModelStatus() },
        onSetLanguageMode      = { voiceChatViewModel.setLanguageMode(it) },
        onExit                 = onExit,
        onLanguageModeChange   = onLanguageModeChange
    )
}

// ── Main content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceChatContent(
    state: VoiceChatState,
    languageMode: com.aipoweredgita.app.utils.LanguageMode,
    onSendMessage: (String) -> Unit,
    onSendCurrentMessage: () -> Unit,
    onUpdateUserInput: (String) -> Unit,
    onClearChat: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClearError: () -> Unit,
    onRefreshModelStatus: () -> Unit,
    onSetLanguageMode: (com.aipoweredgita.app.utils.LanguageMode) -> Unit,
    onExit: () -> Unit,
    onLanguageModeChange: (com.aipoweredgita.app.utils.LanguageMode) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val canInteract = state.isLlmReady && !state.isThinking
    val isBusy = state.isThinking || state.isSpeaking || state.isListening || !canInteract
    val colors = getVoiceStudioColors()

    LaunchedEffect(languageMode) { onSetLanguageMode(languageMode) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
        if (granted) onStartListening()
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                // Spark-like gold line around borders
                val strokeWidth = 2.dp.toPx()
                // Outer glow effect
                drawRect(
                    color = GoldSpark.copy(alpha = 0.3f),
                    style = Stroke(width = strokeWidth * 2f)
                )
                // Main solid gold line
                drawRect(
                    color = GoldSpark,
                    style = Stroke(width = strokeWidth)
                )
            }
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(0.5.dp, colors.RevolvingYellow.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back",
                    tint = colors.RevolvingYellow, modifier = Modifier.size(16.dp))
            }

            Text(
                text = "Sacred conversations",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = colors.TextPrimary,
                    letterSpacing = 0.2.sp
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Chat area ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

            if (state.messages.isEmpty() && !state.isThinking) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "icon_float")
                    val iconOffsetY by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = -7f,
                        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "float"
                    )

                    Box(
                        modifier = Modifier
                            .offset(y = iconOffsetY.dp)
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .border(0.5.dp, colors.Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null,
                            tint = colors.RevolvingYellow, modifier = Modifier.size(22.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "What wisdom\ndo you seek?",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Light,
                            fontStyle = FontStyle.Italic,
                            color = colors.TextPrimary,
                            lineHeight = 36.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Speak or write your question — the Gita holds answers to every struggle of the human soul.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.TextSecondary,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        maxItemsInEachRow = 2
                    ) {
                        listOf("What is karma?", "Explain dharma", "How to find peace?", "What is Atman?")
                            .forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { if (!isBusy) onSendMessage(suggestion) },
                                    enabled = !isBusy,
                                    label = { Text(suggestion, fontSize = 12.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = colors.TextSecondary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledLabelColor = colors.TextMuted
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        borderColor = colors.Border, enabled = true,
                                        disabledBorderColor = colors.Border
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        val isLastAi = !message.isUser &&
                                message == state.messages.lastOrNull { !it.isUser }
                        ChatBubble(
                            message = message,
                            isSpeaking = isLastAi && state.isSpeaking,
                            onEdit = { onUpdateUserInput(it) }
                        )
                    }
                // ── Thinking bubble ───────────────────────────────────────────────────────────
                if (state.isThinking) { item { ThinkingBubble() } }
                }
            }
        }

        // ── Error bar ─────────────────────────────────────────────────────────
        AnimatedVisibility(visible = state.error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                color = Color(0x1AEA4335),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x4DEA4335))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null,
                        tint = Color(0xFFE57373), modifier = Modifier.size(15.dp))
                    Text(state.error ?: "An error occurred", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2)))
                    Text(
                        text = "Retry",
                        modifier = Modifier.clickable { onClearError(); onRefreshModelStatus() },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.RevolvingYellow, fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // ── Bottom panel ──────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 16.dp, bottom = 32.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Text Input Field (Restored as per request)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state.userInput,
                        onValueChange = { onUpdateUserInput(it) },
                        enabled = canInteract,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        placeholder = {
                            Text("Share your search for truth...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.RevolvingYellow,
                            unfocusedBorderColor = colors.Border,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledBorderColor = colors.Border
                        ),
                        shape = RoundedCornerShape(21.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    val canSend = !isBusy && state.userInput.isNotBlank()
                    IconButton(
                        onClick = { if (canSend) onSendCurrentMessage() },
                        enabled = canSend,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (canSend) colors.RevolvingYellow else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) MaterialTheme.colorScheme.background else colors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Clear button moved here next to input
                    IconButton(
                        onClick = { if (canInteract) onClearChat() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear",
                            tint = colors.RevolvingYellow, modifier = Modifier.size(18.dp))
                    }
                }

                // Centered Language Selector
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    com.aipoweredgita.app.utils.LanguageMode.entries.forEachIndexed { index, mode ->
                        val selected = languageMode == mode
                        SegmentedButton(
                            selected = selected,
                            onClick = { onLanguageModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = com.aipoweredgita.app.utils.LanguageMode.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = colors.RevolvingYellow,
                                activeContentColor = MaterialTheme.colorScheme.background,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = colors.RevolvingYellow
                            ),
                            border = SegmentedButtonDefaults.borderStroke(color = colors.Border),
                            label = {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = {}
                        )
                    }
                }
            }
        }
    }
}

// ── Gemini orb ────────────────────────────────────────────────────────────────

@Composable
private fun ThinkingDots() {
    val colors = getVoiceStudioColors()
    val infiniteTransition = rememberInfiniteTransition(label = "think_dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(1400, delayMillis = i * 220),
                    RepeatMode.Reverse
                ),
                label = "dot_alpha_$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.RevolvingYellow.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

// ── Backing components ────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean = false,
    onEdit: (String) -> Unit
) {
    val colors = getVoiceStudioColors()
    val isUser = message.isUser
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Krishna Avatar with Revolving Line
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    rotate(rotation) {
                        drawArc(
                            color = colors.RevolvingYellow,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, colors.Border, CircleShape)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = com.aipoweredgita.app.R.drawable.krishna),
                        contentDescription = "Krishna",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 260.dp),
            shape = if (isUser)
                RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
            else
                RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            color = if (isUser) colors.UserBubbleBg else MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (isUser) colors.UserBubbleBdr else colors.Border
            ),
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.TextPrimary,
                    lineHeight = 22.sp,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        if (isUser) {
            Spacer(Modifier.width(10.dp))
            // Devotee Avatar with Revolving Line
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    rotate(-rotation)
                    {
                        drawArc(
                            color = colors.UserBubbleBdr,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, colors.Border, CircleShape)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = com.aipoweredgita.app.R.drawable.devotee),
                        contentDescription = "Devotee",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// ── Thinking bubble ───────────────────────────────────────────────────────────

@Composable
private fun ThinkingBubble() {
    val colors = getVoiceStudioColors()
    val infiniteTransition = rememberInfiniteTransition(label = "think_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Krishna Avatar (Thinking) with Revolving Line
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(38.dp)) {
                rotate(rotation) {
                    drawArc(
                        color = colors.RevolvingYellow,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.dp, colors.Border, CircleShape)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = com.aipoweredgita.app.R.drawable.krishna),
                    contentDescription = "Krishna",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            color = colors.RevolvingYellow.copy(alpha = 0.10f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.RevolvingYellow.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThinkingDots()
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioIdle() {
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatState(isLlmReady = true),
                languageMode = com.aipoweredgita.app.utils.LanguageMode.AUTO,
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onExit = {}, onLanguageModeChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioChat() {
    val msgs = listOf(
        ChatMessage(text = "What is karma?", isUser = true),
        ChatMessage(text = "Karma is the law of cause and effect — every action you take shapes your future. Act rightly, without attachment to the fruits.", isUser = false)
    )
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatState(messages = msgs, isLlmReady = true),
                languageMode = com.aipoweredgita.app.utils.LanguageMode.AUTO,
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onExit = {}, onLanguageModeChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioListening() {
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatState(isLlmReady = true, isListening = true),
                languageMode = com.aipoweredgita.app.utils.LanguageMode.AUTO,
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onExit = {}, onLanguageModeChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioThinking() {
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatState(
                    messages = listOf(ChatMessage(text = "What is dharma?", isUser = true)),
                    isLlmReady = true, isThinking = true
                ),
                languageMode = com.aipoweredgita.app.utils.LanguageMode.AUTO,
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onExit = {}, onLanguageModeChange = {}
            )
        }
    }
}
