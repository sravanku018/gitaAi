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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

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
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.GlassCard
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

private class VoiceStudioColors(
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
private fun getVoiceStudioColors(): VoiceStudioColors {
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
    modifier: Modifier = Modifier,
    voiceChatViewModel: VoiceChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("voice_studio_prefs", android.content.Context.MODE_PRIVATE)

    DisposableEffect(Unit) {
        voiceChatViewModel.onStartSession()
        onDispose { voiceChatViewModel.onStopSession() }
    }

    val colors = getVoiceStudioColors()
    val state by voiceChatViewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(colors.AppBg)) {
        if (colors.IsDark) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
        }
        
        if (!state.isBalanceLoaded) {
            // Block access until coin balance is confirmed
            BalanceLoadingOverlay(colors = colors)
        } else if (state.coinBalance <= 0) {
            InsufficientCoinsOverlay(
                onExit = onExit,
                onNavigateToQuiz = onNavigateToQuiz,
                onNavigateToRead = onNavigateToRead,
                colors = colors
            )
        } else {
            VoiceChatTab(
                voiceChatViewModel = voiceChatViewModel,
                onExit = onExit
            )
        }
    }
}

// ── Root tab wrapper ──────────────────────────────────────────────────────────

@Composable
private fun VoiceChatTab(
    voiceChatViewModel: VoiceChatViewModel = viewModel(),
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val state by voiceChatViewModel.state.collectAsState()
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
        onUpdateSelectedModel  = { com.aipoweredgita.app.ml.ModelAvailability.getInstance(context).updateSelectedModel(it) },
        onConfirmSend          = { voiceChatViewModel.confirmAndSendMessage() },
        onDismissConfirmation  = { voiceChatViewModel.dismissCoinConfirmation() },
        onExit                 = onExit
    )
}

// ── Main content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceChatContent(
    state: VoiceChatState,
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
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val canInteract = state.isLlmReady && !state.isThinking && state.isBalanceLoaded
    val isBusy = state.isThinking || state.isSpeaking || state.isListening || !canInteract
    val colors = getVoiceStudioColors()
    var showModelMenu by remember { mutableStateOf(false) }
    val modelOptions = listOf("Auto (Recommended)", "Gemma 4 2B (Advanced)", "Cloud Proxy (Groq)")



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
                            fontWeight = FontWeight.Normal,
                            color = colors.TextPrimary,
                            letterSpacing = 0.2.sp
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = GoldSpark.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldSpark.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Coins ${state.coinBalance}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldSpark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Active Model: ${state.currentModelName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (state.currentModelName.contains("Groq", ignoreCase = true))
                            colors.RevolvingYellow
                        else
                            colors.TextMuted
                    )
                )
            }

            // Model Selection Option
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
                            option.contains("Gemma 4") -> ma.isGemma4Available()
                            else -> true // Auto/Groq are always "available"
                        }
                        
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(option, style = MaterialTheme.typography.bodyMedium, color = colors.TextPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (state.currentModelName != "Unknown" && option.contains(state.currentModelName.split("-")[0], ignoreCase = true)) {
                                            Text("Active", style = MaterialTheme.typography.labelSmall, color = colors.SpeakGreen)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        if (!isAvailable && !option.contains("Auto")) {
                                            Text("Not Downloaded", style = MaterialTheme.typography.labelSmall, color = colors.ListenRed)
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
                        text = "Speak or write your question � each inquiry costs 1 Krishna Coin. The Gita holds answers to every struggle of the human soul.",
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
                // ── Listening bubble ───────────────────────────────────────────────────────────
                if (state.isListening) { item { ListeningBubble(state.liveTranscript) } }
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
                        modifier = Modifier.clickable { onClearError(); onRefreshModelStatus() },
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
                            Text(if (state.isBalanceLoaded) "Ask Krishna (Costs 1 coin)..." else "Loading spiritual balance...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextSecondary.copy(alpha = 0.7f)))
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

                    // Mic button for voice input
                    IconButton(
                        onClick = {
                            if (hasAudioPermission) onStartListening()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (state.isListening) colors.ListenRed.copy(alpha = 0.25f) else (if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)),
                                CircleShape
                            )
                            .border(1.dp, if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input",
                            tint = if (state.isListening) colors.ListenRed else colors.RevolvingYellow,
                            modifier = Modifier.size(18.dp))
                    }

                    val canSend = !isBusy && state.userInput.isNotBlank()
                    IconButton(
                        onClick = { if (canSend) onSendCurrentMessage() },
                        enabled = canSend,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (canSend) colors.RevolvingYellow else (MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (canSend) colors.RevolvingYellow else (MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
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

                    // Clear button moved here next to input
                    IconButton(
                        onClick = { if (canInteract) onClearChat() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (colors.IsDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f), CircleShape)
                            .border(1.dp, if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear",
                            tint = colors.RevolvingYellow, modifier = Modifier.size(18.dp))
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

        val bubbleShape = if (isUser)
            RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
        else
            RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
        
        val bubbleBg = if (isUser) colors.UserBubbleBg else (if (colors.IsDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f))
        val bubbleBdrColor = if (isUser) colors.UserBubbleBdr else (if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))

        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(0.5.dp, bubbleBdrColor, bubbleShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf((if (colors.IsDark) Color.White else Color.Black).copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
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
        val bubbleShape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
        val bubbleBg = if (colors.IsDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)
        val bubbleBdrColor = if (colors.IsDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)

        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(0.5.dp, bubbleBdrColor, bubbleShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf((if (colors.IsDark) Color.White else Color.Black).copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
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

// ── Listening bubble ───────────────────────────────────────────────────────────

@Composable
private fun ListeningBubble(liveTranscript: String) {
    val colors = getVoiceStudioColors()
    val infiniteTransition = rememberInfiniteTransition(label = "listen_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        val bubbleShape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
        val bubbleBg = if (colors.IsDark) Color(0xFFFF6400).copy(alpha = 0.12f) else Color(0xFFFF6400).copy(alpha = 0.08f)
        val bubbleBdrColor = colors.ListenRed.copy(alpha = 0.5f)

        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 260.dp)
                .scale(scale)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(1.dp, bubbleBdrColor, bubbleShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf((if (colors.IsDark) Color.White else Color.Black).copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = colors.ListenRed, modifier = Modifier.size(16.dp))
                if (liveTranscript.isNotBlank()) {
                    Text(
                        text = liveTranscript,
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextPrimary, fontStyle = FontStyle.Italic)
                    )
                } else {
                    Text(
                        text = "Listening...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted, fontStyle = FontStyle.Italic)
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        // Devotee Avatar
        Box(
            modifier = Modifier.padding(top = 2.dp).size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, colors.ListenRed.copy(alpha=0.5f), CircleShape).clip(CircleShape)
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

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioIdle() {
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatState(isLlmReady = true),
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {},
                onUpdateSelectedModel = {},
                onConfirmSend = {},
                onDismissConfirmation = {},
                onExit = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVoiceStudioChat() {
    val msgs = listOf(
        ChatMessage(text = "What is karma?", isUser = true),
        ChatMessage(text = "Karma is the law of cause and effect � every action you take shapes your future. Act rightly, without attachment to the fruits.", isUser = false)
    )
    GitaLearningTheme {
        Box(Modifier.background(BgDark)) {
            VoiceChatContent(
                state = VoiceChatState(messages = msgs, isLlmReady = true),
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {},
                onUpdateSelectedModel = {},
                onConfirmSend = {},
                onDismissConfirmation = {},
                onExit = {}
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
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {},
                onUpdateSelectedModel = {},
                onConfirmSend = {},
                onDismissConfirmation = {},
                onExit = {}
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
                onSendMessage = {}, onSendCurrentMessage = {}, onUpdateUserInput = {},
                onClearChat = {}, onStartListening = {}, onStopListening = {},
                onStopSpeaking = {}, onClearError = {}, onRefreshModelStatus = {},
                onSetLanguageMode = {},
                onUpdateSelectedModel = {},
                onConfirmSend = {},
                onDismissConfirmation = {},
                onExit = {}
            )
        }
    }
}

@Composable
private fun BalanceLoadingOverlay(colors: VoiceStudioColors) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ॐ",
            fontSize = 48.sp,
            color = colors.RevolvingYellow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = colors.RevolvingYellow,
            strokeWidth = 3.dp,
            trackColor = colors.RevolvingYellow.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Preparing your spiritual connection...",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = colors.TextPrimary,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please wait while we verify your balance.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.TextSecondary
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InsufficientCoinsOverlay(
    onExit: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToRead: () -> Unit,
    colors: VoiceStudioColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Om ornament / Icon
        Text(
            text = "ॐ",
            fontSize = 48.sp,
            color = colors.RevolvingYellow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Insufficient Divine Energy",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.TextPrimary,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Sacred conversations require spiritual energy. You have 0 Krishna Coins remaining.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.TextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (colors.IsDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                colors.Border.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Perform spiritual acts to earn coins:",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.TextMuted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Earn via Quiz Button
                Button(
                    onClick = onNavigateToQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.RevolvingYellow,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Take a Quiz (+5 to +15 Coins)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Earn via Reading Button
                OutlinedButton(
                    onClick = onNavigateToRead,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.RevolvingYellow
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(colors.RevolvingYellow.copy(alpha = 0.4f), colors.RevolvingYellow.copy(alpha = 0.4f))
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.RevolvingYellow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Read Gita Verses (+Coins)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Back / Exit Button
        TextButton(
            onClick = onExit,
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colors.TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Back to Dashboard",
                color = colors.TextMuted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

