package com.aipoweredgita.app.ui

import com.aipoweredgita.app.ui.components.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
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
import com.aipoweredgita.app.services.ModelDownloadProgress
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
    onBackClick: () -> Unit,
    language: String = "tel"
) {
    val uiCfg = LocalUiConfig.current
    if (uiCfg.isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SacredQuizConfigScreen(
                modifier = Modifier.weight(1f),
                onStartQuiz = { count: Int -> onStartQuiz(count, language) },
                language = language
            )
            Spacer(Modifier.weight(1f))
        }
    } else {
        SacredQuizConfigScreen(
            modifier = Modifier.fillMaxSize(),
            onStartQuiz = { count: Int -> onStartQuiz(count, language) },
            language = language
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  QUIZ CONFIG SCREEN — Sacred battlefield entry
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SacredQuizConfigScreen(
    modifier: Modifier = Modifier,
    onStartQuiz: (Int) -> Unit,
    language: String = "tel"
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
            .background(MaterialTheme.colorScheme.background)
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
            OrnamentalHeader(language = language)

            Spacer(Modifier.height(36.dp))

            // ── Question count section ─────────────────────────────────────
            SectionLabel(text = "Questions per battle", language = language)
            Spacer(Modifier.height(14.dp))
            QuestionCountRow(
                selected  = questionCount,
                onSelect  = { questionCount = it },
                language = language
            )

            Spacer(Modifier.height(32.dp))

            // ── Language badge ─────────────────────────────────────────────
            SectionLabel(text = "Language", language = language)
            Spacer(Modifier.height(14.dp))
            LanguageBadgeCard(language = language)

            Spacer(Modifier.height(32.dp))

            // ── Feature highlights ─────────────────────────────────────────
            FeatureHighlightsCard(language = language)

            Spacer(Modifier.height(40.dp))

            // ── Start button ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(btnScale)
                    .clip(MaterialTheme.shapes.large)
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
                        text       = when (language) {
                            "tel" -> "ప్రారంభించండి"
                            "hin" -> "प्रारंभ करें"
                            else -> "START"
                        },
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text       = if (language == "tel") "క్విజ్ ప్రారంభించండి" else "Begin Quiz",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    val subtext = if (language == "tel") {
                        "$questionCount ప్రశ్నలు · తెలుగు"
                    } else {
                        "$questionCount questions · Telugu"
                    }
                    Text(
                        text     = subtext,
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


