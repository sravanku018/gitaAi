package com.aipoweredgita.app.ui

<<<<<<< HEAD
import androidx.compose.foundation.isSystemInDarkTheme
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
import androidx.compose.ui.draw.shadow
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
    onBackClick: () -> Unit,
    language: String = "tel"
=======
    onBackClick: () -> Unit
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
) {
    val uiCfg = LocalUiConfig.current
    if (uiCfg.isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
<<<<<<< HEAD
                .background(MaterialTheme.colorScheme.background)
=======
                .background(BgDark)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SacredQuizConfigScreen(
                modifier = Modifier.weight(1f),
<<<<<<< HEAD
                onStartQuiz = { count: Int -> onStartQuiz(count, language) },
                language = language
=======
                onStartQuiz = { count: Int -> onStartQuiz(count, "tel") }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )
            Spacer(Modifier.weight(1f))
        }
    } else {
        SacredQuizConfigScreen(
            modifier = Modifier.fillMaxSize(),
<<<<<<< HEAD
            onStartQuiz = { count: Int -> onStartQuiz(count, language) },
            language = language
=======
            onStartQuiz = { count: Int -> onStartQuiz(count, "tel") }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  QUIZ CONFIG SCREEN — Sacred battlefield entry
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SacredQuizConfigScreen(
    modifier: Modifier = Modifier,
<<<<<<< HEAD
    onStartQuiz: (Int) -> Unit,
    language: String = "tel"
=======
    onStartQuiz: (Int) -> Unit
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
            .background(MaterialTheme.colorScheme.background)
=======
            .background(BgDark)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
            OrnamentalHeader(language = language)
=======
            OrnamentalHeader()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

            Spacer(Modifier.height(36.dp))

            // ── Question count section ─────────────────────────────────────
<<<<<<< HEAD
            SectionLabel(text = "Questions per battle", language = language)
            Spacer(Modifier.height(14.dp))
            QuestionCountRow(
                selected  = questionCount,
                onSelect  = { questionCount = it },
                language = language
=======
            SectionLabel(text = "Questions per battle")
            Spacer(Modifier.height(14.dp))
            QuestionCountRow(
                selected  = questionCount,
                onSelect  = { questionCount = it }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )

            Spacer(Modifier.height(32.dp))

            // ── Language badge ─────────────────────────────────────────────
<<<<<<< HEAD
            SectionLabel(text = "Language", language = language)
            Spacer(Modifier.height(14.dp))
            LanguageBadgeCard(language = language)
=======
            SectionLabel(text = "Language")
            Spacer(Modifier.height(14.dp))
            LanguageBadgeCard()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

            Spacer(Modifier.height(32.dp))

            // ── Feature highlights ─────────────────────────────────────────
<<<<<<< HEAD
            FeatureHighlightsCard(language = language)
=======
            FeatureHighlightsCard()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

            Spacer(Modifier.height(40.dp))

            // ── Start button ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(btnScale)
<<<<<<< HEAD
                    .clip(MaterialTheme.shapes.large)
=======
                    .clip(RoundedCornerShape(18.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                        text       = if (language == "tel") "ప్రారంభించండి" else "प्रारंभ करें",
=======
                        text       = "प्रारंभ करें",
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    Text(
<<<<<<< HEAD
                        text       = if (language == "tel") "క్విజ్ ప్రారంభించండి" else "Begin Quiz",
=======
                        text       = "Begin Quiz",
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
<<<<<<< HEAD
                    val subtext = if (language == "tel") {
                        "$questionCount ప్రశ్నలు · తెలుగు"
                    } else {
                        "$questionCount questions · Telugu"
                    }
                    Text(
                        text     = subtext,
=======
                    Text(
                        text     = "$questionCount questions · Telugu",
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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

<<<<<<< HEAD
// ── Translation Helper ──────────────────────────────────────────────────────
private fun translateConfigText(text: String, language: String): String {
    if (language != "tel") return text
    return when (text) {
        "Questions per battle" -> "సమరానికి ప్రశ్నలు"
        "Language" -> "భాష"
        "WHAT TO EXPECT" -> "ఏమి ఆశించాలి"
        "Context-aware Gita questions" -> "సందర్భోచిత గీతా ప్రశ్నలు"
        "Intelligent difficulty scaling" -> "కఠినత్వ స్థాయిల క్రమబద్ధీకరణ"
        "Telugu language support" -> "తెలుగు భాషా మద్దతు"
        "100% offline & private" -> "100% ఆఫ్‌లైన్ & వ్యక్తిగతం"
        "Gita Quiz" -> "గీతా క్విజ్"
        "Test your knowledge of the sacred scripture" -> "పవిత్ర గ్రంథంపై మీ జ్ఞానాన్ని పరీక్షించుకోండి"
        "Sprint" -> "లఘు ప్రశ్నలు"
        "Deep Dive" -> "లోతైన విశ్లేషణ"
        "LANGUAGE" -> "భాష"
        "Download AI Engine" -> "AI ఇంజిన్‌ను డౌన్‌లోడ్ చేయండి"
        "Select a model to download:" -> "డౌన్‌లోడ్ చేయడానికి ఒక నమూనాను ఎంచుకోండి:"
        "UNLOCKS" -> "అన్‌లాక్ అవుతాయి"
        "Download once, quiz anytime — fully offline." -> "ఒక్కసారి డౌన్‌లోడ్ చేయండి, ఎప్పుడైనా క్విజ్ ఆడండి — పూర్తిగా ఆఫ్‌లైన్."
        "Not now" -> "ఇప్పుడు వద్దు"
        "Download  →" -> "డౌన్‌లోడ్  →"
        "All models ready" -> "అన్ని మోడల్స్ సిద్ధంగా ఉన్నాయి"
        "The Arena Prepares" -> "యుద్ధరంగం సిద్ధమవుతోంది"
        "AI models are downloading to power your quiz experience. This happens only once." -> "మీ క్విజ్ అనుభవాన్ని మెరుగుపరచడానికి AI మోడల్స్ డౌన్‌లోడ్ అవుతున్నాయి. ఇది ఒక్కసారి మాత్రమే జరుగుతుంది."
        "AWAITING YOU" -> "మీ కొరకు సిద్ధంగా ఉన్నవి"
        "Intelligent Questions" -> "మేధోపరమైన ప్రశ్నలు"
        "Theme-Based Learning" -> "విషయ-ఆధారిత అభ్యాసం"
        "Fully Offline" -> "పూర్తిగా ఆఫ్‌లైన్"
        "Context Aware" -> "సందర్భోచితం"
        "← Return to Home" -> "← హోమ్‌కి తిరిగి వెళ్ళండి"
        "Please wait until models finish downloading." -> "దయచేసి మోడల్స్ డౌన్‌లోడ్ పూర్తయ్యే వరకు వేచి ఉండండి."
        "DOWNLOAD PROGRESS" -> "డౌన్‌లోడ్ పురోగతి"
        "Preparing download…" -> "డౌన్‌లోడ్ సిద్ధమవుతోంది…"
        "file(s) remaining" -> "ఫైల్(లు) మిగిలి ఉన్నాయి"
        "MB left" -> "MB మిగిలి ఉంది"
        "Smart context-aware questions" -> "సందర్భోచిత మేధో ప్రశ్నలు"
        "Offline & private" -> "ఆఫ్‌లైన్ & వ్యక్తిగతం"
        else -> text
    }
}

// ── Ornamental header ───────────────────────────────────────────────────────
@Composable
private fun OrnamentalHeader(language: String = "tel") {
=======
// ── Ornamental header ───────────────────────────────────────────────────────
@Composable
private fun OrnamentalHeader() {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
            text  = translateConfigText("Gita Quiz", language),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
=======
            text  = "Gita Quiz",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
<<<<<<< HEAD
            text      = translateConfigText("Test your knowledge of the sacred scripture", language),
            fontSize  = 13.sp,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
=======
            text      = "Test your knowledge of the sacred scripture",
            fontSize  = 13.sp,
            color     = TextWhite.copy(alpha = 0.4f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
private fun QuestionCountRow(selected: Int, onSelect: (Int) -> Unit, language: String = "tel") {
=======
private fun QuestionCountRow(selected: Int, onSelect: (Int) -> Unit) {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(15 to "Sprint", 25 to "Deep Dive").forEach { (count, label) ->
            val isActive = selected == count
            Box(
                modifier = Modifier
                    .weight(1f)
<<<<<<< HEAD
                    .clip(MaterialTheme.shapes.extraLarge)
=======
                    .clip(RoundedCornerShape(16.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .shadow(if (isActive) 6.dp else 0.dp, MaterialTheme.shapes.extraLarge)
=======
                        shape = RoundedCornerShape(16.dp)
                    )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    .clickable { onSelect(count) }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Big number with animated color
<<<<<<< HEAD
                    val numColor = if (isActive) GoldBright else MaterialTheme.colorScheme.onSurface.copy(0.35f)
=======
                    val numColor = if (isActive) GoldBright else TextWhite.copy(0.35f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    Text(
                        text       = "$count",
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color      = numColor,
                        lineHeight = 44.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
<<<<<<< HEAD
                        text     = translateConfigText(label, language),
                        fontSize = 11.sp,
                        color    = if (isActive) GoldPale.copy(0.8f) else MaterialTheme.colorScheme.onSurface.copy(0.3f),
=======
                        text     = label,
                        fontSize = 11.sp,
                        color    = if (isActive) GoldPale.copy(0.8f) else TextWhite.copy(0.3f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
private fun LanguageBadgeCard(language: String = "tel") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, GoldSpark.copy(0.25f), MaterialTheme.shapes.extraLarge)
            .shadow(4.dp, MaterialTheme.shapes.extraLarge)
=======
private fun LanguageBadgeCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(0.5.dp, GoldSpark.copy(0.25f), RoundedCornerShape(14.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
<<<<<<< HEAD
                text      = translateConfigText("LANGUAGE", language),
                fontSize  = 10.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(0.45f),
=======
                text      = "LANGUAGE",
                fontSize  = 10.sp,
                color     = TextWhite.copy(0.35f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "తెలుగు  ·  Telugu",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
<<<<<<< HEAD
                color      = MaterialTheme.colorScheme.onSurface
=======
                color      = TextWhite
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )
        }
        // Flag badge
        Box(
            modifier = Modifier
<<<<<<< HEAD
                .clip(MaterialTheme.shapes.small)
                .background(Forest.copy(0.5f))
                .border(0.5.dp, ForestMid.copy(0.5f), MaterialTheme.shapes.small)
=======
                .clip(RoundedCornerShape(8.dp))
                .background(Forest.copy(0.5f))
                .border(0.5.dp, ForestMid.copy(0.5f), RoundedCornerShape(8.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("🇮🇳", fontSize = 22.sp)
        }
    }
}

// ── Feature highlights card ─────────────────────────────────────────────────
@Composable
<<<<<<< HEAD
private fun FeatureHighlightsCard(language: String = "tel") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, GoldSpark.copy(0.2f), MaterialTheme.shapes.extraLarge)
            .shadow(3.dp, MaterialTheme.shapes.extraLarge)
=======
private fun FeatureHighlightsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(0.5.dp, GoldSpark.copy(0.2f), RoundedCornerShape(16.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
<<<<<<< HEAD
            text      = translateConfigText("WHAT TO EXPECT", language),
            fontSize  = 10.sp,
            color     = GoldSpark.copy(0.8f),
=======
            text      = "WHAT TO EXPECT",
            fontSize  = 10.sp,
            color     = GoldSpark.copy(0.6f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                    text     = translateConfigText(text, language),
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(0.85f)
=======
                    text     = text,
                    fontSize = 13.sp,
                    color    = TextWhite.copy(0.65f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                )
            }
        }
    }
}

// ── Section label ───────────────────────────────────────────────────────────
@Composable
<<<<<<< HEAD
private fun SectionLabel(text: String, language: String = "tel") {
=======
private fun SectionLabel(text: String) {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
            text       = translateConfigText(text, language).uppercase(),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground.copy(0.6f),
=======
            text       = text.uppercase(),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = TextWhite.copy(0.55f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
    onCancel          : () -> Unit,
    language          : String = "tel"
=======
    onCancel          : () -> Unit
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
        onCancel = onCancel,
        language = language
=======
        onCancel = onCancel
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    )
}

@Composable
fun AIDownloadDialogContent(
    modelStatuses     : List<ModelDownloadManager.ModelStatus>,
    selectedModel     : String?,
    onModelSelect     : (String) -> Unit,
    onConfirmDownload : (String) -> Unit,
<<<<<<< HEAD
    onCancel          : () -> Unit,
    language          : String = "tel"
=======
    onCancel          : () -> Unit
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
) {
    val missingModels = modelStatuses.filter { !it.isDownloaded }

    AlertDialog(
        onDismissRequest = onCancel,
<<<<<<< HEAD
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
=======
        containerColor   = Surface1,
        shape            = RoundedCornerShape(24.dp),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                    text = translateConfigText("Download AI Engine", language),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
=======
                    "Download AI Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = TextWhite,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                            .clip(MaterialTheme.shapes.medium)
                            .background(Forest.copy(0.2f))
                            .border(0.5.dp, ForestMid.copy(0.5f), MaterialTheme.shapes.medium)
=======
                            .clip(RoundedCornerShape(12.dp))
                            .background(Forest.copy(0.3f))
                            .border(0.5.dp, ForestMid.copy(0.5f), RoundedCornerShape(12.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✦", color = ForestMid, fontSize = 14.sp)
                        Text(
<<<<<<< HEAD
                            text = translateConfigText("All models ready", language),
                            fontSize   = 14.sp,
                            color      = if (rememberThemeIsDark()) Color(0xFFC0DD97) else Forest,
=======
                            "All models ready",
                            fontSize   = 14.sp,
                            color      = Color(0xFFC0DD97),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
<<<<<<< HEAD
                        text = translateConfigText("Select a model to download:", language),
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(0.6f)
=======
                        "Select a model to download:",
                        fontSize = 13.sp,
                        color    = TextWhite.copy(0.55f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    )
                    missingModels.forEach { model ->
                        val isSelected = selectedModel == model.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
<<<<<<< HEAD
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
=======
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Surface2 else BgDark)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    brush = if (isSelected)
                                        Brush.linearGradient(listOf(GoldSpark, Saffron))
                                    else
                                        Brush.linearGradient(listOf(GoldSpark.copy(0.15f), GoldSpark.copy(0.15f))),
<<<<<<< HEAD
                                    shape = MaterialTheme.shapes.medium
=======
                                    shape = RoundedCornerShape(14.dp)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(0.3f)
=======
                                    unselectedColor = TextWhite.copy(0.25f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    model.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.sp,
<<<<<<< HEAD
                                    color      = if (isSelected) GoldSpark else MaterialTheme.colorScheme.onSurface
=======
                                    color      = if (isSelected) GoldPale else TextWhite
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                                )
                                if (model.size.isNotEmpty()) {
                                    Text(
                                        "Size: ${model.size}",
                                        fontSize = 12.sp,
<<<<<<< HEAD
                                        color    = MaterialTheme.colorScheme.onSurface.copy(0.5f)
=======
                                        color    = TextWhite.copy(0.4f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                                    )
                                }
                            }
                        }
                    }
                }

                // Features unlocked panel
                Column(
<<<<<<< HEAD
                     modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.background)
                        .border(0.5.dp, GoldSpark.copy(0.2f), MaterialTheme.shapes.medium)
=======
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgDark)
                        .border(0.5.dp, GoldSpark.copy(0.2f), RoundedCornerShape(12.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
<<<<<<< HEAD
                        text = translateConfigText("UNLOCKS", language),
                        fontSize      = 10.sp,
                        color         = GoldSpark.copy(0.8f),
=======
                        "UNLOCKS",
                        fontSize      = 10.sp,
                        color         = GoldSpark.copy(0.6f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                            Text(translateConfigText(feat, language), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
=======
                            Text(feat, fontSize = 12.sp, color = TextWhite.copy(0.6f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        }
                    }
                }

                Text(
<<<<<<< HEAD
                    text = translateConfigText("Download once, quiz anytime — fully offline.", language),
                    fontSize  = 12.sp,
                    color     = GoldPale.copy(0.75f),
=======
                    "Download once, quiz anytime — fully offline.",
                    fontSize  = 12.sp,
                    color     = GoldPale.copy(0.55f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
<<<<<<< HEAD
                    .clip(MaterialTheme.shapes.medium)
=======
                    .clip(RoundedCornerShape(12.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                    text = translateConfigText("Download  →", language),
=======
                    "Download  →",
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    color      = if (selectedModel != null) Color.White else Color.White.copy(0.3f),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
<<<<<<< HEAD
                Text(translateConfigText("Not now", language), color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 13.sp)
=======
                Text("Not now", color = TextWhite.copy(0.4f), fontSize = 13.sp)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
    viewModel  : ModelDownloadViewModel = viewModel(),
    language   : String = "tel"
=======
    viewModel  : ModelDownloadViewModel = viewModel()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
        remainingMb     = remMb,
        language        = language
=======
        remainingMb     = remMb
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    )
}

@Composable
fun QuizNotReadyScreenContent(
    onBackClick     : () -> Unit,
    perFileProgress : List<ModelDownloadProgress>,
    filesRemaining  : Int,
<<<<<<< HEAD
    remainingMb     : Int?,
    language        : String = "tel"
=======
    remainingMb     : Int?
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
            .background(MaterialTheme.colorScheme.background)
=======
            .background(BgDark)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                text = if (language == "tel") "ధర్మక్షేత్రం సిద్ధమవుతోంది" else "कुरुक्षेत्र तैयार हो रहा है",
=======
                "कुरुक्षेत्र तैयार हो रहा है",
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                fontSize   = 13.sp,
                color      = GoldSpark.copy(0.6f),
                fontStyle  = FontStyle.Italic,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
<<<<<<< HEAD
                text = translateConfigText("The Arena Prepares", language),
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
=======
                "The Arena Prepares",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = TextWhite,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
            Text(
<<<<<<< HEAD
                text = translateConfigText("AI models are downloading to power your quiz experience. This happens only once.", language),
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onBackground.copy(0.6f),
=======
                "AI models are downloading to power your quiz experience. This happens only once.",
                fontSize   = 13.sp,
                color      = TextWhite.copy(0.4f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                filesRemaining = filesRemaining,
                language = language
=======
                filesRemaining = filesRemaining
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )

            Spacer(Modifier.height(28.dp))

            // Feature list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
<<<<<<< HEAD
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, GoldSpark.copy(0.18f), MaterialTheme.shapes.large)
=======
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1)
                    .border(0.5.dp, GoldSpark.copy(0.18f), RoundedCornerShape(16.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
<<<<<<< HEAD
                    text = translateConfigText("AWAITING YOU", language),
                    fontSize = 10.sp,
                    color    = GoldSpark.copy(0.8f),
=======
                    "AWAITING YOU",
                    fontSize = 10.sp,
                    color    = GoldSpark.copy(0.55f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                        Text(translateConfigText(text, language), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
=======
                        Text(text, fontSize = 13.sp, color = TextWhite.copy(0.6f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
<<<<<<< HEAD
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(GoldSpark.copy(0.4f), Saffron.copy(0.4f))),
                        shape = MaterialTheme.shapes.medium
=======
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(GoldSpark.copy(0.4f), Saffron.copy(0.4f))),
                        shape = RoundedCornerShape(14.dp)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    )
                    .background(Color.Transparent)
                    .clickable(onClick = onBackClick)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
<<<<<<< HEAD
                    text = translateConfigText("← Return to Home", language),
=======
                    "← Return to Home",
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    color      = GoldPale.copy(0.7f),
                    fontWeight = FontWeight.Medium,
                    fontSize   = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
<<<<<<< HEAD
                text = translateConfigText("Please wait until models finish downloading.", language),
                fontSize  = 11.sp,
                color     = MaterialTheme.colorScheme.onBackground.copy(0.4f),
=======
                "Please wait until models finish downloading.",
                fontSize  = 11.sp,
                color     = TextWhite.copy(0.25f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

<<<<<<< HEAD
=======
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

>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
@Composable
fun DownloadProgressCardContent(
    remainingMb     : Int?,
    perFileProgress : List<ModelDownloadProgress>,
<<<<<<< HEAD
    filesRemaining  : Int,
    language        : String = "tel"
=======
    filesRemaining  : Int
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
<<<<<<< HEAD
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, GoldSpark.copy(0.25f), MaterialTheme.shapes.large)
=======
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(0.5.dp, GoldSpark.copy(0.25f), RoundedCornerShape(16.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
<<<<<<< HEAD
                text = translateConfigText("DOWNLOAD PROGRESS", language),
                fontSize = 10.sp,
                color    = GoldSpark.copy(0.8f),
=======
                "DOWNLOAD PROGRESS",
                fontSize = 10.sp,
                color    = GoldSpark.copy(0.55f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
<<<<<<< HEAD
                    .clip(MaterialTheme.shapes.large)
                    .background(Saffron.copy(0.15f))
                    .border(0.5.dp, Saffron.copy(0.35f), MaterialTheme.shapes.large)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                val mbLeftText = if (language == "tel") {
                    "${remainingMb ?: 0} MB మిగిలి ఉంది"
                } else {
                    "${remainingMb ?: 0} MB left"
                }
                Text(
                    text = mbLeftText,
                    fontSize   = 11.sp,
                    color      = if (rememberThemeIsDark()) GoldPale else Saffron,
=======
                    .clip(RoundedCornerShape(20.dp))
                    .background(Saffron.copy(0.15f))
                    .border(0.5.dp, Saffron.copy(0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${remainingMb ?: 0} MB left",
                    fontSize   = 11.sp,
                    color      = GoldPale.copy(0.8f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (perFileProgress.isNotEmpty()) {
<<<<<<< HEAD
            val filesRemainingText = if (language == "tel") {
                "$filesRemaining ఫైల్(లు) మిగిలి ఉన్నాయి"
            } else {
                "$filesRemaining file(s) remaining"
            }
            Text(
                text = filesRemainingText,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(0.6f)
=======
            Text(
                "$filesRemaining file(s) remaining",
                fontSize = 13.sp,
                color    = TextWhite.copy(0.5f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )
            perFileProgress.forEachIndexed { idx, p ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
<<<<<<< HEAD
                        val fileLabel = if (language == "tel") "ఫైల్ ${idx + 1}" else "File ${idx + 1}"
                        Text(
                            text = fileLabel,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                        )
                        Text(
                            text = "${p.percentage}%",
                            fontSize   = 12.sp,
                            color      = if (rememberThemeIsDark()) GoldPale else Saffron,
=======
                        Text(
                            "File ${idx + 1}",
                            fontSize = 12.sp,
                            color    = TextWhite.copy(0.45f)
                        )
                        Text(
                            "${p.percentage}%",
                            fontSize   = 12.sp,
                            color      = GoldPale,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Progress bar — gold fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
<<<<<<< HEAD
                            .background(MaterialTheme.colorScheme.surface)
=======
                            .background(BgDark)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                    .background(MaterialTheme.colorScheme.surface)
=======
                    .background(BgDark)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                text = translateConfigText("Preparing download…", language),
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(0.4f),
=======
                "Preparing download…",
                fontSize  = 12.sp,
                color     = TextWhite.copy(0.3f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
=======
        Text(text, fontSize = 13.sp, color = TextWhite.copy(0.6f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
