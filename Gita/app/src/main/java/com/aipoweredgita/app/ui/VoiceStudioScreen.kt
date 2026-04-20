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

// ── Shared Sacred Gold Palette (imported from theme) ──────────────────────────
private val Border        = Color(0x14FFFFFF)   // 8 % white
private val BorderHi      = Color(0x24FFFFFF)   // 14 % white
private val TextPrimary   = TextWhite
private val TextSecondary = TextDim
private val TextMuted     = Color(0x52E8E8E8)   // 32 %
private val ListenRed     = CrimsonDeep
private val SpeakGreen    = Forest
private val UserBubbleBg  = Forest.copy(alpha = 0.2f)
private val UserBubbleBdr = Forest.copy(alpha = 0.3f)
private val RevolvingYellow = GoldBright

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

    Box(modifier = modifier.fillMaxSize().background(BgDark)) {
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
                    .background(Surface1, CircleShape)
                    .border(0.5.dp, GoldSpark.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back",
                    tint = GoldSpark, modifier = Modifier.size(16.dp))
            }

            Text(
                text = "Sacred conversations",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary,
                    letterSpacing = 0.2.sp
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
        }

        // ── Language chips (EN + हि — తె and Auto move to bottom row) ──────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val topModes = com.aipoweredgita.app.utils.LanguageMode.entries
                .filter { it.displayShort != "తె" && it.displayShort != "A" }
            topModes.forEach { mode ->
                val selected = languageMode == mode
                Surface(
                    onClick = { onLanguageModeChange(mode) },
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) GoldSpark else Color.Transparent,
                    border = if (selected) null else
                        androidx.compose.foundation.BorderStroke(0.5.dp, Border)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = mode.displayShort,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) BgDark else GoldSpark
                        )
                    }
                }
            }
        }

        // ── Divider ───────────────────────────────────────────────────────────
        OrnamentRule()
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
                            .background(Surface1, CircleShape)
                            .border(0.5.dp, Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null,
                            tint = GoldSpark, modifier = Modifier.size(22.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "What wisdom\ndo you seek?",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Light,
                            fontStyle = FontStyle.Italic,
                            color = TextPrimary,
                            lineHeight = 36.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Speak or write your question — the Gita holds answers to every struggle of the human soul.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
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
                                        containerColor = Surface1,
                                        labelColor = TextSecondary,
                                        disabledContainerColor = Surface1,
                                        disabledLabelColor = TextMuted
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        borderColor = Border, enabled = true,
                                        disabledBorderColor = Border
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
                    Text(state.error ?: "", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2)))
                    Text(
                        text = "Retry",
                        modifier = Modifier.clickable { onClearError(); onRefreshModelStatus() },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GoldSpark, fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // ── Bottom panel ──────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgDark.copy(alpha = 0.96f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                // Input row
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
                            Text("Ask your question...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorderHi,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Surface2,
                            unfocusedContainerColor = Surface1,
                            disabledContainerColor = Surface1,
                            disabledBorderColor = Border
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
                                if (canSend) GoldSpark else Surface1,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) BgDark else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Mic row — clear | orb | theme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Clear
                    SideButton(
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Clear",
                            tint = Saffron, modifier = Modifier.size(17.dp)) },
                        label = "Clear",
                        onClick = { if (canInteract) onClearChat() }
                    )

                    // Central orb
                    val orbScale by animateFloatAsState(
                        targetValue = if (state.isListening || state.isSpeaking || state.isThinking) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "orb_scale"
                    )
                    Box(modifier = Modifier.scale(orbScale)) {
                        GeminiMicOrb(
                            state = state,
                            onMicClick = {
                                when {
                                    !canInteract        -> Unit
                                    state.isSpeaking    -> onStopSpeaking()
                                    state.isListening   -> onStopListening()
                                    !hasAudioPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    else                -> onStartListening()
                                }
                            }
                        )
                    }

                    // తె chip
                    val teMode = com.aipoweredgita.app.utils.LanguageMode.entries
                        .firstOrNull { it.displayShort == "తె" }
                    if (teMode != null) {
                        val teSelected = languageMode == teMode
                        SideButton(
                            icon = {
                                Text(
                                    text = "తె",
                                    fontSize = 14.sp,
                                    fontWeight = if (teSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (teSelected) BgDark else GoldSpark
                                )
                            },
                            label = "Telugu",
                            onClick = { onLanguageModeChange(teMode) },
                            isSelected = teSelected
                        )
                    }

                    // Auto chip
                    val autoMode = com.aipoweredgita.app.utils.LanguageMode.entries
                        .firstOrNull { it.displayShort == "A" }
                    if (autoMode != null) {
                        val autoSelected = languageMode == autoMode
                        SideButton(
                            icon = {
                                Text(
                                    text = "A",
                                    fontSize = 14.sp,
                                    fontWeight = if (autoSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (autoSelected) BgDark else GoldSpark
                                )
                            },
                            label = "Auto",
                            onClick = { onLanguageModeChange(autoMode) },
                            isSelected = autoSelected
                        )
                    }
                }

                // Status line
                Text(
                    text = when {
                        state.isListening -> "Listening..."
                        state.isThinking  -> "Contemplating..."
                        state.isSpeaking  -> "Speaking — tap to stop"
                        else              -> "Tap the orb to speak"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = when {
                            state.isListening -> ListenRed
                            state.isThinking  -> GoldSpark
                            state.isSpeaking  -> SpeakGreen
                            else              -> TextMuted
                        }
                    )
                )
            }
        }
    }
}

// ── Gemini orb ────────────────────────────────────────────────────────────────

@Composable
private fun GeminiMicOrb(state: VoiceChatState, onMicClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring_rot"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(88.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onMicClick
            )
    ) {
        // Revolving animated border
        Canvas(modifier = Modifier.size(88.dp)) {
            val strokeWidth = 3.dp.toPx()
            rotate(ringRotation) {
                if (state.isListening) {
                    // Red and Yellow for listening (disciple icon state)
                    drawArc(
                        color = Color(0xFFEA4335), // Red
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(strokeWidth)
                    )
                    drawArc(
                        color = RevolvingYellow, // Yellow
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(strokeWidth)
                    )
                } else {
                    // Green and Yellow for speaking/idle (Krishna icon state)
                    drawArc(
                        color = Color(0xFF34A853), // Green
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(strokeWidth)
                    )
                    drawArc(
                        color = RevolvingYellow, // Yellow
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(strokeWidth)
                    )
                }
            }
        }

        // Core orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(Surface1, CircleShape)
                .border(1.5.dp, BorderHi, CircleShape)
                .clip(CircleShape)
        ) {
            Crossfade(targetState = state.isListening, label = "orb_image") { isListening ->
                if (isListening) {
                    Image(
                        painter = painterResource(id = com.aipoweredgita.app.R.drawable.devotee),
                        contentDescription = "Devotee",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = com.aipoweredgita.app.R.drawable.krishna),
                        contentDescription = "Krishna",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "waves")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(22.dp)
    ) {
        val delays = listOf(0, 120, 240, 60, 180)
        delays.forEach { delay ->
            val height by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(900, delayMillis = delay, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "bar_$delay"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .background(ListenRed, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun ThinkingDots() {
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
                    .background(GoldSpark.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

// ── Side button ───────────────────────────────────────────────────────────────

@Composable
private fun SideButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(if (isSelected) GoldSpark else Surface1, CircleShape)
                .border(0.5.dp, if (isSelected) GoldSpark else Border, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) GoldSpark else TextMuted,
                letterSpacing = 0.08.sp,
                fontSize = 10.sp
            )
        )
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean = false,
    onEdit: (String) -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isUser) Modifier.clickable { onEdit(message.text) } else Modifier)
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            val glowDp by animateDpAsState(
                targetValue = if (isSpeaking) 10.dp else 0.dp,
                animationSpec = if (isSpeaking)
                    infiniteRepeatable(tween(800), RepeatMode.Reverse)
                else tween(300),
                label = "speak_glow"
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(30.dp)
                    .shadow(glowDp, CircleShape,
                        spotColor = GoldSpark, ambientColor = GoldSpark)
                    .background(Surface2, CircleShape)
                    .border(0.5.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null,
                    tint = GoldSpark, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 265.dp),
            shape = if (isUser)
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            else
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = if (isUser) UserBubbleBg else Surface1,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (isUser) UserBubbleBdr else Border
            )
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    lineHeight = 22.sp,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(30.dp)
                    .background(Color(0xFF1A2E1A), CircleShape)
                    .border(0.5.dp, UserBubbleBdr, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null,
                    tint = SpeakGreen, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Thinking bubble ───────────────────────────────────────────────────────────

@Composable
private fun ThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(30.dp)
                .background(Surface2, CircleShape)
                .border(0.5.dp, Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, contentDescription = null,
                tint = GoldSpark, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = GoldSpark.copy(alpha = 0.10f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldSpark.copy(alpha = 0.22f))
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
