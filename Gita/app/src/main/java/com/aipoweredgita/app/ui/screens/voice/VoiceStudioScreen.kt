package com.aipoweredgita.app.ui.screens.voice

import android.Manifest
import com.aipoweredgita.app.ui.screens.voice.components.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.domain.model.VoiceChatEvent
import com.aipoweredgita.app.domain.model.ChatMessage
import com.aipoweredgita.app.domain.model.VoiceChatUiState
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.viewmodel.VoiceChatViewModel
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.GlassCard
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.luminance

class VoiceStudioColors(
    val IsDark: Boolean,
    val AppBg: Color,
    val Border: Color,
    val BorderHi: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val ListenRed: Color,
    val SpeakGreen: Color,
    val UserBubbleBg: Color,
    val UserBubbleBdr: Color,
    val RevolvingYellow: Color
)

@Composable
fun getVoiceStudioColors(): VoiceStudioColors {
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textMuted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val gold = if (isDark) GoldSpark else Saffron
    val border = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val borderHi = gold
    val userBubbleBg = Saffron.copy(alpha = if (isDark) 0.12f else 0.08f)
    val userBubbleBdr = gold.copy(alpha = 0.25f)
    val appBg = MaterialTheme.colorScheme.background

    return remember(isDark, textPrimary, textSecondary, textMuted, gold, border, borderHi, userBubbleBg, userBubbleBdr, appBg) {
        VoiceStudioColors(
            IsDark = isDark,
            AppBg = appBg,
            Border = border,
            BorderHi = borderHi,
            TextPrimary = textPrimary,
            TextSecondary = textSecondary,
            TextMuted = textMuted,
            ListenRed = Color(0xFFE57373),
            SpeakGreen = Color(0xFF81C784),
            UserBubbleBg = userBubbleBg,
            UserBubbleBdr = userBubbleBdr,
            RevolvingYellow = gold
        )
    }
}

@Composable
fun VoiceStudioScreen(
    onExit: () -> Unit,
    onNavigateToQuiz: () -> Unit = {},
    onNavigateToRead: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    voiceChatViewModel: VoiceChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("voice_studio_prefs", android.content.Context.MODE_PRIVATE)

    DisposableEffect(Unit) {
        voiceChatViewModel.onStartSession()
        voiceChatViewModel.checkAndRestoreCooldown()
        onDispose {
            voiceChatViewModel.stopAll()
            voiceChatViewModel.onStopSession()
        }
    }

    val colors = getVoiceStudioColors()
    val state by voiceChatViewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(colors.AppBg)) {
        if (colors.IsDark) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
        }

        // FIX 3: Guard order — balance must be loaded first, then check amount
        if (!state.isBalanceLoaded) {
            BalanceLoadingOverlay(colors = colors)
        } else if (state.coinBalance < com.aipoweredgita.app.coin.VoiceCoinPricing.MIN_COST) {
            InsufficientCoinsOverlay(
                coinBalance = state.coinBalance,
                onExit = onExit,
                onNavigateToQuiz = onNavigateToQuiz,
                onNavigateToRead = onNavigateToRead,
                colors = colors
            )
        } else {
            VoiceChatTab(
                voiceChatViewModel = voiceChatViewModel,
                onNavigateToSettings = onNavigateToSettings,
                onExit = onExit
            )
        }
    }
}

// ── Root tab wrapper ──────────────────────────────────────────────────────────

@Composable
private fun VoiceChatTab(
    voiceChatViewModel: VoiceChatViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val state by voiceChatViewModel.uiState.collectAsStateWithLifecycle()
    VoiceChatContent(
        state = state,
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
        onUpdateSelectedModel  = {
            com.aipoweredgita.app.ml.ModelAvailability.getInstance(context).updateSelectedModel(it)
            voiceChatViewModel.refreshModelStatus()
        },
        onConfirmSend          = { voiceChatViewModel.confirmAndSendMessage() },
        onDismissConfirmation  = { voiceChatViewModel.dismissCoinConfirmation() },
        onNavigateToSettings   = onNavigateToSettings,
        onExit                 = onExit
    )
}

// ── Main content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceChatContent(
    state: VoiceChatUiState,
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
    onUpdateSelectedModel: (String) -> Unit,
    onConfirmSend: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val canInteract = state.isLlmReady && !state.isThinking && state.isBalanceLoaded
    val isBusy = state.isThinking || state.isSpeaking || state.isListening || !canInteract
    val colors = getVoiceStudioColors()
    var showModelMenu by remember { mutableStateOf(false) }
    val modelOptions = listOf("Auto (Recommended)", "Gemma 4 2B (Advanced)", "NVIDIA 70B (Cloud)", "Groq (Cloud)")

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
        modifier = Modifier.fillMaxSize()
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
                    .background(if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f), CircleShape)
                    .border(1.dp, if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Back",
                    tint = colors.TextPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sacred conversations",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.TextPrimary,
                            letterSpacing = 0.2.sp
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        color = GoldSpark.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldSpark.copy(alpha = 0.7f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "🪙", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${state.coinBalance}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = GoldSpark
                            )
                        }
                    }
                }
                Text(
                    text = "Model: ${state.currentModelName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (state.currentModelName.contains("NVIDIA", ignoreCase = true) || state.currentModelName.contains("Groq", ignoreCase = true))
                            colors.RevolvingYellow
                        else
                            colors.TextMuted
                    )
                )
            }

            // Language Toggle
            Row(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (colors.IsDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                    .border(0.5.dp, colors.Border, MaterialTheme.shapes.medium)
            ) {
                com.aipoweredgita.app.utils.LanguageMode.entries.forEach { mode ->
                    val isSelected = state.currentLanguageMode == mode
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isSelected) colors.RevolvingYellow.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onSetLanguageMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.displayShort,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.RevolvingYellow else colors.TextSecondary
                        )
                    }
                }
            }

            // Model Selection
            Box {
                IconButton(
                    onClick = { showModelMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f), CircleShape)
                        .border(1.dp, if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = "Select Model",
                        tint = colors.RevolvingYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                    modifier = Modifier.background(if (colors.IsDark) Color(0xFF140F0A) else Color(0xFFFFFDF8))
                ) {
                    modelOptions.forEach { option ->
                        val ma = com.aipoweredgita.app.ml.ModelAvailability.getInstance(context)
                        val isAvailable = when {
                            option.contains("Gemma 4")  -> ma.isGemma4Available()
                            else -> true // Auto / cloud models always "available"
                        }

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(option, style = MaterialTheme.typography.bodyMedium, color = colors.TextPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isActive = when {
                                            option.contains("NVIDIA") && state.currentModelName.contains("NVIDIA", ignoreCase = true) -> true
                                            option.contains("Groq")   && state.currentModelName.contains("Groq",  ignoreCase = true) -> true
                                            option.contains("Gemma")  && state.currentModelName.contains("Gemma", ignoreCase = true) -> true
                                            option.contains("Auto")
                                                    && !state.currentModelName.contains("NVIDIA", ignoreCase = true)
                                                    && !state.currentModelName.contains("Groq",  ignoreCase = true)
                                                    && !state.currentModelName.contains("Gemma", ignoreCase = true) -> true
                                            else -> false
                                        }
                                        if (isActive) {
                                            Text("Active", style = MaterialTheme.typography.labelSmall, color = colors.SpeakGreen)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        if (!isAvailable && !option.contains("Auto")) {
                                            Text("Not Downloaded", style = MaterialTheme.typography.labelSmall, color = colors.ListenRed)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Download",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = colors.RevolvingYellow),
                                                modifier = Modifier.clickable {
                                                    showModelMenu = false
                                                    onNavigateToSettings()
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onUpdateSelectedModel(option)
                                showModelMenu = false
                            }
                        )
                    }

                    HorizontalDivider(color = colors.Border)

                    val tier = com.aipoweredgita.app.utils.DeviceTierDetector.detect(context)
                    Text(
                        text = "Device: ${tier.label}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(12.dp),
                        color = colors.TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Chat area ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

            if (state.messages.isEmpty() && !state.isThinking) {
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

                    // FIX 4: Replaced broken glyph with standard bullet •
                    Text(
                        text = "Speak or write your question \u2022 costs 4 / 6 / 10 coins (short / medium / long). The Gita holds answers to every struggle of the human soul.",
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
                        state.suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { if (!isBusy) onSendMessage(suggestion) },
                                    enabled = !isBusy,
                                    label = { Text(suggestion, fontSize = 12.sp, color = colors.TextPrimary) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (colors.IsDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                                        labelColor = colors.TextPrimary,
                                        disabledContainerColor = if (colors.IsDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.01f),
                                        disabledLabelColor = colors.TextPrimary.copy(alpha = 0.3f)
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        borderColor = if (colors.IsDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                        enabled = true,
                                        disabledBorderColor = if (colors.IsDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                                    ),
                                    shape = MaterialTheme.shapes.large,
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
                    items(state.messages.filter { it.text.isNotEmpty() }, key = { it.id }) { message ->
                        val isLastAi = !message.isUser &&
                                message == state.messages.lastOrNull { !it.isUser }
                        ChatBubble(
                            message = message,
                            isSpeaking = isLastAi && state.isSpeaking,
                            onEdit = { onUpdateUserInput(it) }
                        )
                    }
                    if (state.isThinking) { item(key = "thinking_bubble") {
                        Box(modifier = Modifier.animateItem()) { ThinkingBubble() }
                    } }
                    if (state.isListening) { item(key = "listening_bubble") {
                        Box(modifier = Modifier.animateItem()) { ListeningBubble(state.liveTranscript) }
                    } }
                }
            }
        }

        // ── Error bar ─────────────────────────────────────────────────────────
        AnimatedVisibility(visible = state.error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                color = if (colors.IsDark) Color(0x1AEA4335) else Color(0x0DEA4335),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (colors.IsDark) Color(0x4DEA4335) else Color(0x28EA4335))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null,
                        tint = Color(0xFFE57373), modifier = Modifier.size(15.dp))
                    Text(state.error ?: "An error occurred", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(color = if (colors.IsDark) Color(0xFFFFCDD2) else Color(0xFFC62828)))
                    Text(
                        text = "Retry",
                        modifier = Modifier.clickable { onClearError() },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.RevolvingYellow, fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // ── Bottom panel ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.AppBg.copy(alpha = 0.85f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.cooldownSeconds > 0) {
                        val mins = state.cooldownSeconds / 60
                        val secs = state.cooldownSeconds % 60
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.RevolvingYellow.copy(alpha = 0.15f))
                                .border(1.dp, colors.RevolvingYellow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Cooldown Timer",
                                tint = colors.RevolvingYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Cooldown: ${mins}m ${secs}s",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = colors.RevolvingYellow,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Daily Questions Counter Pill (5 per day limit)
                    val pillColor = if (state.dailyQuestionsAsked >= 5) colors.ListenRed else colors.RevolvingYellow
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillColor.copy(alpha = 0.15f))
                            .border(1.dp, pillColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Daily Limit: ${state.dailyQuestionsAsked}/${state.maxDailyQuestions}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = pillColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Google Voice Assistant Animated Waveform Banner
                AnimatedVisibility(
                    visible = state.isListening,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GoogleVoiceWaveform(
                        audioLevel = state.audioLevel,
                        liveText = state.liveTranscript,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state.userInput,
                        onValueChange = { onUpdateUserInput(it) },
                        enabled = canInteract && state.cooldownSeconds == 0,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        placeholder = {
                            Text(
                                if (state.cooldownSeconds > 0) "Cooldown active (${state.cooldownSeconds / 60}m ${state.cooldownSeconds % 60}s)..."
                                else if (state.isBalanceLoaded) "Ask Krishna..." else "Loading spiritual balance...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextSecondary.copy(alpha = 0.7f))
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.RevolvingYellow,
                            unfocusedBorderColor = if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                            focusedContainerColor = if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                            unfocusedContainerColor = if (colors.IsDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f),
                            disabledContainerColor = if (colors.IsDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.01f),
                            disabledBorderColor = if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                            focusedTextColor = colors.TextPrimary,
                            unfocusedTextColor = colors.TextPrimary,
                            disabledTextColor = colors.TextMuted
                        ),
                        shape = RoundedCornerShape(21.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    // Mic / Stop button
                    if (state.isSpeaking) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                            label = "scale"
                        )
                        IconButton(
                            onClick = { onStopSpeaking() },
                            modifier = Modifier
                                .size(42.dp)
                                .scale(scale)
                                .background(colors.ListenRed.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, colors.ListenRed, CircleShape)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Speaking", tint = colors.ListenRed)
                        }
                    }

                    // Mic button (ALWAYS accessible)
                    val isBusyForMic = state.isThinking || state.isSpeaking || !canInteract
                    val googleColors = listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853), Color(0xFF4285F4))
                    val micScale by animateFloatAsState(
                        targetValue = if (state.isListening) 1f + (state.audioLevel * 0.3f) else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "micScale"
                    )
                    IconButton(
                        onClick = {
                            if (hasAudioPermission) {
                                if (state.isListening) onStopListening() else onStartListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = !isBusyForMic && state.cooldownSeconds == 0,
                        modifier = Modifier
                            .size(44.dp)
                            .scale(micScale)
                            .background(
                                brush = if (state.isListening) Brush.sweepGradient(googleColors)
                                        else Brush.linearGradient(listOf(if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f), if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))),
                                shape = CircleShape
                            )
                            .border(
                                width = if (state.isListening) 2.dp else 1.dp,
                                brush = if (state.isListening) Brush.sweepGradient(googleColors)
                                        else SolidColor(if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (state.isListening) Color.White else if (state.cooldownSeconds > 0) colors.TextMuted else colors.RevolvingYellow,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Send button
                    val canSend = !isBusy && state.userInput.isNotBlank() && state.cooldownSeconds == 0
                    IconButton(
                        onClick = { if (canSend) onSendCurrentMessage() },
                        enabled = canSend,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (canSend) colors.RevolvingYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (canSend) colors.RevolvingYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) MaterialTheme.colorScheme.surface else colors.TextPrimary.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Clear button
                    val canClear = canInteract && state.messages.isNotEmpty()
                    AnimatedVisibility(
                        visible = state.messages.isNotEmpty(),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        IconButton(
                            onClick = { if (canClear) onClearChat() },
                            enabled = canClear,
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (canClear) (if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                                    else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (canClear) (if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))
                                    else colors.Border.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = if (canClear) colors.RevolvingYellow else colors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (state.showCoinConfirmation) {
            AlertDialog(
                onDismissRequest = onDismissConfirmation,
                containerColor   = colors.AppBg,
                shape            = MaterialTheme.shapes.extraLarge,
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("ॐ", fontSize = 24.sp, color = colors.RevolvingYellow)
                        Text("Sacred Inquiry", fontWeight = FontWeight.Bold, color = colors.TextPrimary)
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Guided spiritual inquiries require divine energy. This search will consume your accumulated Krishna Coins.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Caution: Guidance is limited by your current mastery.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.ListenRed,
                            fontStyle = FontStyle.Italic
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmSend,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.RevolvingYellow)
                    ) {
                        Text("Spend ${state.pendingCost} coin(s)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissConfirmation) {
                        Text("Cancel", color = colors.TextMuted)
                    }
                }
            )
        }
    }
}

// ── Thinking dots ─────────────────────────────────────────────────────────────

// ── Overlays ──────────────────────────────────────────────────────────────────


// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioIdle() {
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatUiState(isLlmReady = true),
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onUpdateSelectedModel = {},
                onConfirmSend = {}, onDismissConfirmation = {}, onNavigateToSettings = {}, onExit = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioChat() {
    val msgs = listOf(
        ChatMessage(text = "What is karma?", isUser = true),
        ChatMessage(text = "Karma is the law of cause and effect \u2022 every action you take shapes your future. Act rightly, without attachment to the fruits.", isUser = false)
    )
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatUiState(messages = msgs, isLlmReady = true),
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onUpdateSelectedModel = {},
                onConfirmSend = {}, onDismissConfirmation = {}, onNavigateToSettings = {}, onExit = {}
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
                state = VoiceChatUiState(isLlmReady = true, isListening = true),
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onUpdateSelectedModel = {},
                onConfirmSend = {}, onDismissConfirmation = {}, onNavigateToSettings = {}, onExit = {}
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
                state = VoiceChatUiState(
                    messages = listOf(ChatMessage(text = "What is dharma?", isUser = true)),
                    isLlmReady = true, isThinking = true
                ),
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {}, onUpdateSelectedModel = {},
                onConfirmSend = {}, onDismissConfirmation = {}, onNavigateToSettings = {}, onExit = {}
            )
        }
    }
}

@Composable
fun GoogleVoiceWaveform(
    audioLevel: Float,
    liveText: String,
    modifier: Modifier = Modifier
) {
    val googleBlue = Color(0xFF4285F4)
    val googleRed = Color(0xFFEA4335)
    val googleYellow = Color(0xFFFBBC05)
    val googleGreen = Color(0xFF34A853)
    val colorsList = listOf(googleBlue, googleRed, googleYellow, googleGreen)

    val transition = rememberInfiniteTransition(label = "googleWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "phase"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Brush.horizontalGradient(colorsList.map { it.copy(alpha = 0.5f) })),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 4 Google Brand Color Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(36.dp)
            ) {
                colorsList.forEachIndexed { index, color ->
                    val offset = index * 0.7f
                    val waveFactor = kotlin.math.sin((phase + offset).toDouble()).toFloat()
                    val rawTarget = 12.dp + (24.dp * (audioLevel * 0.8f + 0.2f * waveFactor).coerceIn(0.15f, 1f))
                    val animatedHeight by animateDpAsState(
                        targetValue = rawTarget,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "barHeight"
                    )

                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(animatedHeight)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Real-time streaming transcript text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (liveText.isNotBlank()) liveText else "Listening...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (liveText.isNotBlank()) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (liveText.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}