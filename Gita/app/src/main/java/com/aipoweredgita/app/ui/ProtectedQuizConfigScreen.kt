package com.aipoweredgita.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.aipoweredgita.app.ui.theme.*

// ── Palette constants moved to theme/Color.kt ───────────────────────────────

// ═══════════════════════════════════════════════════════════════════════════
//  PROTECTED WRAPPER
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun ProtectedQuizConfigScreen(
    onStartQuiz: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiCfg = LocalUiConfig.current
    if (uiCfg.isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SacredQuizConfigScreen(
                modifier = Modifier.weight(1f),
                onStartQuiz = { count: Int -> onStartQuiz(count, "tel") }
            )
            Spacer(Modifier.weight(1f))
        }
    } else {
        SacredQuizConfigScreen(
            modifier = Modifier.fillMaxSize(),
            onStartQuiz = { count: Int -> onStartQuiz(count, "tel") }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  QUIZ CONFIG SCREEN — Sacred battlefield entry
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SacredQuizConfigScreen(
    modifier: Modifier = Modifier,
    onStartQuiz: (Int) -> Unit
) {
    var questionCount by remember { mutableStateOf(15) }

    // Pulsing glow on start button
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow_alpha"
    )
    val btnScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.025f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "btn_scale"
    )

    Box(
        modifier = modifier
            .background(BgDark)
            .drawBehind {
                // Ambient saffron glow bottom-centre
                drawCircle(
                    brush  = Brush.radialGradient(
                        listOf(SaffronGlow, Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.85f),
                        radius = size.width * 0.75f
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width / 2f, size.height * 0.85f)
                )
                // Gold glow top-left
                drawCircle(
                    brush  = Brush.radialGradient(
                        listOf(GoldSpark.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(0f, 0f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Header ─────────────────────────────────────────────────────
            OrnamentalHeader()

            Spacer(Modifier.height(36.dp))

            // ── Question count section ─────────────────────────────────────
            SectionLabel(text = "Questions per battle")
            Spacer(Modifier.height(14.dp))
            QuestionCountRow(
                selected  = questionCount,
                onSelect  = { questionCount = it }
            )

            Spacer(Modifier.height(32.dp))

            // ── Language badge ─────────────────────────────────────────────
            SectionLabel(text = "Language")
            Spacer(Modifier.height(14.dp))
            LanguageBadgeCard()

            Spacer(Modifier.height(32.dp))

            // ── Feature highlights ─────────────────────────────────────────
            FeatureHighlightsCard()

            Spacer(Modifier.height(40.dp))

            // ── Start button ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(btnScale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(listOf(GoldSpark, Saffron, GoldSpark))
                    )
                    .drawBehind {
                        // Outer glow ring
                        drawCircle(
                            color  = Saffron.copy(alpha = glowAlpha * 0.3f),
                            radius = size.minDimension * 0.65f,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style  = Stroke(width = 24f)
                        )
                    }
                    .clickable { onStartQuiz(questionCount) }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "प्रारंभ करें",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text       = "Begin Quiz",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Text(
                        text     = "$questionCount questions · Telugu",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.65f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Ornamental header ───────────────────────────────────────────────────────
@Composable
private fun OrnamentalHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Krishna & Devotee Imagery
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(GoldSpark, Saffron)), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.krishna_icon),
                    contentDescription = "Krishna",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(-20.dp)) // Overlap effect
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, GoldSpark.copy(0.5f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.devotee),
                    contentDescription = "Devotee",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Om glyph
        val shimmer by rememberInfiniteTransition(label = "om").animateFloat(
            0.5f, 1f,
            infiniteRepeatable(tween(2500), RepeatMode.Reverse),
            label = "om_glow"
        )
        Text(
            text  = "ॐ",
            fontSize = 36.sp,
            color = GoldSpark.copy(alpha = shimmer),
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.height(6.dp))
        // Title
        Text(
            text  = "Gita Quiz",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = "Test your knowledge of the sacred scripture",
            fontSize  = 13.sp,
            color     = TextWhite.copy(alpha = 0.4f),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        // Gold rule
        OrnamentRule()
    }
}

// ── Question count selector ─────────────────────────────────────────────────
@Composable
private fun QuestionCountRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(15 to "Sprint", 25 to "Deep Dive").forEach { (count, label) ->
            val isActive = selected == count
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isActive)
                            Brush.linearGradient(listOf(Surface2, Surface2))
                        else
                            Brush.linearGradient(listOf(Surface1, Surface1))
                    )
                    .border(
                        width = if (isActive) 1.5.dp else 0.5.dp,
                        brush = if (isActive)
                            Brush.linearGradient(listOf(GoldSpark, Saffron))
                        else
                            Brush.linearGradient(listOf(GoldSpark.copy(0.2f), GoldSpark.copy(0.2f))),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(count) }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Big number with animated color
                    val numColor = if (isActive) GoldBright else TextWhite.copy(0.35f)
                    Text(
                        text       = "$count",
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color      = numColor,
                        lineHeight = 44.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = label,
                        fontSize = 11.sp,
                        color    = if (isActive) GoldPale.copy(0.8f) else TextWhite.copy(0.3f),
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isActive) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Saffron)
                        )
                    }
                }
            }
        }
    }
}

// ── Language badge card ─────────────────────────────────────────────────────
@Composable
private fun LanguageBadgeCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(0.5.dp, GoldSpark.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text      = "LANGUAGE",
                fontSize  = 10.sp,
                color     = TextWhite.copy(0.35f),
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "తెలుగు  ·  Telugu",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = TextWhite
            )
        }
        // Flag badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Forest.copy(0.5f))
                .border(0.5.dp, ForestMid.copy(0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("🇮🇳", fontSize = 22.sp)
        }
    }
}

// ── Feature highlights card ─────────────────────────────────────────────────
@Composable
private fun FeatureHighlightsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(0.5.dp, GoldSpark.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text      = "WHAT TO EXPECT",
            fontSize  = 10.sp,
            color     = GoldSpark.copy(0.6f),
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Medium
        )
        listOf(
            "✦" to "Context-aware Gita questions",
            "✦" to "Intelligent difficulty scaling",
            "✦" to "Telugu language support",
            "✦" to "100% offline & private"
        ).forEach { (icon, text) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(icon, fontSize = 10.sp, color = GoldSpark)
                Text(
                    text     = text,
                    fontSize = 13.sp,
                    color    = TextWhite.copy(0.65f)
                )
            }
        }
    }
}

// ── Section label ───────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(
                    Brush.verticalGradient(listOf(GoldSpark, Saffron)),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = text.uppercase(),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = TextWhite.copy(0.55f),
            letterSpacing = 1.5.sp
        )
    }
}

// ── Gold ornament rule ──────────────────────────────────────────────────────
@Composable
private fun OrnamentRule() {
    Row(
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, GoldSpark.copy(0.45f)))
                )
        )
        Spacer(Modifier.width(10.dp))
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == 1) 5.dp else 3.dp)
                    .clip(CircleShape)
                    .background(GoldSpark.copy(if (i == 1) 0.8f else 0.4f))
            )
            if (i < 2) Spacer(Modifier.width(5.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(GoldSpark.copy(0.45f), Color.Transparent))
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  AI DOWNLOAD DIALOG — Sacred scroll style
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AIDownloadDialog(
    viewModel         : ModelDownloadViewModel,
    onConfirmDownload : (String) -> Unit,
    onCancel          : () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { ModelDownloadManager(context) }
    var modelStatuses by remember { mutableStateOf<List<ModelDownloadManager.ModelStatus>>(emptyList()) }
    var selectedModel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        modelStatuses = manager.getModelsStatus()
        selectedModel = modelStatuses.firstOrNull { !it.isDownloaded }?.name
    }

    AIDownloadDialogContent(
        modelStatuses = modelStatuses,
        selectedModel = selectedModel,
        onModelSelect = { selectedModel = it },
        onConfirmDownload = onConfirmDownload,
        onCancel = onCancel
    )
}

@Composable
fun AIDownloadDialogContent(
    modelStatuses     : List<ModelDownloadManager.ModelStatus>,
    selectedModel     : String?,
    onModelSelect     : (String) -> Unit,
    onConfirmDownload : (String) -> Unit,
    onCancel          : () -> Unit
) {
    val missingModels = modelStatuses.filter { !it.isDownloaded }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor   = Surface1,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    "ॐ",
                    fontSize = 22.sp,
                    color    = GoldSpark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Download AI Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = TextWhite,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OrnamentRule()
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (missingModels.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Forest.copy(0.3f))
                            .border(0.5.dp, ForestMid.copy(0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✦", color = ForestMid, fontSize = 14.sp)
                        Text(
                            "All models ready",
                            fontSize   = 14.sp,
                            color      = Color(0xFFC0DD97),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        "Select a model to download:",
                        fontSize = 13.sp,
                        color    = TextWhite.copy(0.55f)
                    )
                    missingModels.forEach { model ->
                        val isSelected = selectedModel == model.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Surface2 else BgDark)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    brush = if (isSelected)
                                        Brush.linearGradient(listOf(GoldSpark, Saffron))
                                    else
                                        Brush.linearGradient(listOf(GoldSpark.copy(0.15f), GoldSpark.copy(0.15f))),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onModelSelect(model.name) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick  = { onModelSelect(model.name) },
                                colors   = RadioButtonDefaults.colors(
                                    selectedColor   = GoldSpark,
                                    unselectedColor = TextWhite.copy(0.25f)
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    model.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.sp,
                                    color      = if (isSelected) GoldPale else TextWhite
                                )
                                if (model.size.isNotEmpty()) {
                                    Text(
                                        "Size: ${model.size}",
                                        fontSize = 12.sp,
                                        color    = TextWhite.copy(0.4f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Features unlocked panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgDark)
                        .border(0.5.dp, GoldSpark.copy(0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "UNLOCKS",
                        fontSize      = 10.sp,
                        color         = GoldSpark.copy(0.6f),
                        letterSpacing = 2.sp,
                        fontWeight    = FontWeight.Bold
                    )
                    listOf(
                        "Smart context-aware questions",
                        "Telugu language support",
                        "Intelligent difficulty scaling",
                        "Offline & private"
                    ).forEach { feat ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(ForestMid)
                            )
                            Text(feat, fontSize = 12.sp, color = TextWhite.copy(0.6f))
                        }
                    }
                }

                Text(
                    "Download once, quiz anytime — fully offline.",
                    fontSize  = 12.sp,
                    color     = GoldPale.copy(0.55f),
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedModel != null)
                            Brush.horizontalGradient(listOf(GoldSpark, Saffron))
                        else
                            Brush.horizontalGradient(listOf(GoldSpark.copy(0.25f), GoldSpark.copy(0.25f)))
                    )
                    .clickable(enabled = selectedModel != null) {
                        selectedModel?.let { onConfirmDownload(it) }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Download  →",
                    color      = if (selectedModel != null) Color.White else Color.White.copy(0.3f),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Not now", color = TextWhite.copy(0.4f), fontSize = 13.sp)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  QUIZ NOT READY — Diya loading screen with radial pulse rings
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun QuizNotReadyScreen(
    onBackClick: () -> Unit,
    viewModel  : ModelDownloadViewModel = viewModel()
) {
    val context = LocalContext.current
    val perFileProgress by viewModel.fileProgressList.collectAsState()
    val filesRemaining  by viewModel.filesRemaining.collectAsState()

    val mgr = remember { ModelDownloadManager(context) }
    var remMb by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                remMb = (mgr.getRemainingDownloadSizeBytes() / (1024 * 1024)).toInt()
            } catch (e: Exception) {
                android.util.Log.w("QuizNotReadyScreen", "size error", e)
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    QuizNotReadyScreenContent(
        onBackClick     = onBackClick,
        perFileProgress = perFileProgress,
        filesRemaining  = filesRemaining,
        remainingMb     = remMb
    )
}

@Composable
fun QuizNotReadyScreenContent(
    onBackClick     : () -> Unit,
    perFileProgress : List<ModelDownloadProgress>,
    filesRemaining  : Int,
    remainingMb     : Int?
) {
    // Pulsing diya rings
    val infiniteTransition = rememberInfiniteTransition(label = "diya")
    val ring1 by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), initialStartOffset = StartOffset(730)),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), initialStartOffset = StartOffset(1460)),
        label = "ring3"
    )
    val flameAlpha by infiniteTransition.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flame"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .drawBehind {
                // Draw pulsing rings from center
                val cx = size.width / 2f
                val cy = size.height * 0.32f
                listOf(ring1, ring2, ring3).forEach { progress ->
                    val radius = 60.dp.toPx() + progress * 200.dp.toPx()
                    val alpha  = (1f - progress) * 0.35f
                    drawCircle(
                        color  = Saffron.copy(alpha = alpha),
                        center = Offset(cx, cy),
                        radius = radius,
                        style  = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                // Background ambient glow
                drawCircle(
                    brush  = Brush.radialGradient(
                        listOf(Saffron.copy(0.12f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = 180.dp.toPx()
                    ),
                    radius = 180.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment  = Alignment.CenterHorizontally,
            verticalArrangement  = Arrangement.spacedBy(0.dp)
        ) {

            // Diya flame
            Text(
                "🪔",
                fontSize = 52.sp,
                modifier = Modifier.drawBehind {
                    drawCircle(
                        color  = Saffron.copy(alpha = flameAlpha * 0.4f),
                        radius = 60.dp.toPx()
                    )
                }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "कुरुक्षेत्र तैयार हो रहा है",
                fontSize   = 13.sp,
                color      = GoldSpark.copy(0.6f),
                fontStyle  = FontStyle.Italic,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "The Arena Prepares",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = TextWhite,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "AI models are downloading to power your quiz experience. This happens only once.",
                fontSize   = 13.sp,
                color      = TextWhite.copy(0.4f),
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(28.dp))
            OrnamentRule()
            Spacer(Modifier.height(28.dp))

            // Download progress card
            DownloadProgressCardContent(
                remainingMb = remainingMb,
                perFileProgress = perFileProgress,
                filesRemaining = filesRemaining
            )

            Spacer(Modifier.height(28.dp))

            // Feature list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1)
                    .border(0.5.dp, GoldSpark.copy(0.18f), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "AWAITING YOU",
                    fontSize = 10.sp,
                    color    = GoldSpark.copy(0.55f),
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold
                )
                listOf(
                    "✦" to "Intelligent Questions",
                    "✦" to "Theme-Based Learning",
                    "✦" to "Fully Offline",
                    "✦" to "Context Aware"
                ).forEach { (icon, text) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(icon, color = Saffron, fontSize = 11.sp)
                        Text(text, fontSize = 13.sp, color = TextWhite.copy(0.6f))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(GoldSpark.copy(0.4f), Saffron.copy(0.4f))),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(Color.Transparent)
                    .clickable(onClick = onBackClick)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "← Return to Home",
                    color      = GoldPale.copy(0.7f),
                    fontWeight = FontWeight.Medium,
                    fontSize   = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Please wait until models finish downloading.",
                fontSize  = 11.sp,
                color     = TextWhite.copy(0.25f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ── Download progress card ──────────────────────────────────────────────────
@Composable
private fun DownloadProgressCard(
    viewModel: ModelDownloadViewModel,
    context  : android.content.Context
) {
    val mgr = remember { ModelDownloadManager(context) }
    var remMb by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                remMb = (mgr.getRemainingDownloadSizeBytes() / (1024 * 1024)).toInt()
            } catch (e: Exception) {
                android.util.Log.w("QuizNotReadyScreen", "size error", e)
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val perFileProgress by viewModel.fileProgressList.collectAsState()
    val filesRemaining  by viewModel.filesRemaining.collectAsState()

    DownloadProgressCardContent(
        remainingMb = remMb,
        perFileProgress = perFileProgress,
        filesRemaining = filesRemaining
    )
}

@Composable
fun DownloadProgressCardContent(
    remainingMb     : Int?,
    perFileProgress : List<ModelDownloadProgress>,
    filesRemaining  : Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(0.5.dp, GoldSpark.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "DOWNLOAD PROGRESS",
                fontSize = 10.sp,
                color    = GoldSpark.copy(0.55f),
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Saffron.copy(0.15f))
                    .border(0.5.dp, Saffron.copy(0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${remainingMb ?: 0} MB left",
                    fontSize   = 11.sp,
                    color      = GoldPale.copy(0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (perFileProgress.isNotEmpty()) {
            Text(
                "$filesRemaining file(s) remaining",
                fontSize = 13.sp,
                color    = TextWhite.copy(0.5f)
            )
            perFileProgress.forEachIndexed { idx, p ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "File ${idx + 1}",
                            fontSize = 12.sp,
                            color    = TextWhite.copy(0.45f)
                        )
                        Text(
                            "${p.percentage}%",
                            fontSize   = 12.sp,
                            color      = GoldPale,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Progress bar — gold fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(BgDark)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (p.percentage / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(GoldSpark, Saffron))
                                )
                        )
                    }
                }
            }
        } else {
            // Indeterminate shimmer bar
            val infiniteTransition = rememberInfiniteTransition(label = "bar")
            val sweep by infiniteTransition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(1400, easing = LinearEasing)),
                label = "sweep"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BgDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .fillMaxHeight()
                        .offset(x = (sweep * 300).dp - 120.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldSpark, Saffron, Color.Transparent)
                            )
                        )
                )
            }
            Text(
                "Preparing download…",
                fontSize  = 12.sp,
                color     = TextWhite.copy(0.3f),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LEGACY COMPOSABLES (kept for call-site compatibility)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextWhite.copy(0.55f))
        Text(value, fontSize = 12.sp, color = GoldPale, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FeatureRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(GoldSpark)
        )
        Text(text, fontSize = 13.sp, color = TextWhite.copy(0.6f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
fun ProtectedQuizConfigScreenPreview() {
    GitaLearningTheme {
        ProtectedQuizConfigScreen(
            onStartQuiz = { _, _ -> },
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
fun SacredQuizConfigScreenPreview() {
    GitaLearningTheme {
        SacredQuizConfigScreen(
            onStartQuiz = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OrnamentalHeaderPreview() {
    GitaLearningTheme {
        Box(modifier = Modifier.background(BgDark).padding(20.dp)) {
            OrnamentalHeader()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuestionCountRowPreview() {
    var selected by remember { mutableStateOf(15) }
    GitaLearningTheme {
        Box(modifier = Modifier.background(BgDark).padding(20.dp)) {
            QuestionCountRow(selected = selected, onSelect = { selected = it })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageBadgeCardPreview() {
    GitaLearningTheme {
        Box(modifier = Modifier.background(BgDark).padding(20.dp)) {
            LanguageBadgeCard()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeatureHighlightsCardPreview() {
    GitaLearningTheme {
        Box(modifier = Modifier.background(BgDark).padding(20.dp)) {
            FeatureHighlightsCard()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AIDownloadDialogPreview() {
    val sampleStatuses = listOf(
        ModelDownloadManager.ModelStatus("Qwen3 0.6B", "1.2 GB", true, 1200000000L, "/path/1"),
        ModelDownloadManager.ModelStatus("Gemma 4 2B", "2.1 GB", false, 0L, "/path/2")
    )
    GitaLearningTheme {
        AIDownloadDialogContent(
            modelStatuses = sampleStatuses,
            selectedModel = "Gemma 4 2B",
            onModelSelect = {},
            onConfirmDownload = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
fun QuizNotReadyScreenPreview() {
    val sampleProgress = listOf(
        ModelDownloadProgress("Qwen3 0.6B", 45, "Downloading...", null, 450_000_000L, 1_000_000_000L),
        ModelDownloadProgress("Gemma 4 2B", 10, "Pending...", null, 200_000_000L, 2_000_000_000L)
    )
    GitaLearningTheme {
        QuizNotReadyScreenContent(
            onBackClick = {},
            perFileProgress = sampleProgress,
            filesRemaining = 1,
            remainingMb = 1450
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadProgressCardPreview() {
    val sampleProgress = listOf(
        ModelDownloadProgress("Qwen3 0.6B", 65, "Downloading...", null, 650_000_000L, 1_000_000_000L)
    )
    GitaLearningTheme {
        Box(modifier = Modifier.background(BgDark).padding(20.dp)) {
            DownloadProgressCardContent(
                remainingMb = 350,
                perFileProgress = sampleProgress,
                filesRemaining = 1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SectionLabelPreview() {
    GitaLearningTheme {
        Box(modifier = Modifier.background(BgDark).padding(20.dp)) {
            SectionLabel(text = "Sample Section")
        }
    }
}
