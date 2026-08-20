package com.aipoweredgita.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.services.ModelDownloadProgress
import com.aipoweredgita.app.viewmodel.ModelDownloadViewModel

// ═══════════════════════════════════════════════════════════════════════════
//  QUIZ NOT READY — Diya loading screen with radial pulse rings
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun QuizNotReadyScreen(
    onBackClick: () -> Unit,
    viewModel  : ModelDownloadViewModel = viewModel(),
    language   : String = "tel"
) {
    val context = LocalContext.current
    val perFileProgress by viewModel.fileProgressList.collectAsState()
    val filesRemaining  by viewModel.filesRemaining.collectAsState()

    val mgr = remember { ModelDownloadManager(context) }
    var remMb by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val remaining = mgr.getRemainingDownloadSizeBytes()
                remMb = (remaining / (1024 * 1024)).toInt()
                if (remaining <= 0) break
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
        remainingMb     = remMb,
        language        = language
    )
}

@Composable
fun QuizNotReadyScreenContent(
    onBackClick     : () -> Unit,
    perFileProgress : List<ModelDownloadProgress>,
    filesRemaining  : Int,
    remainingMb     : Int?,
    language        : String = "tel"
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
            .background(MaterialTheme.colorScheme.background)
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
                text = if (language == "tel") "ధర్మక్షేత్రం సిద్ధమవుతోంది" else "कुरुक्षेत्र तैयार हो रहा है",
                fontSize   = 13.sp,
                color      = GoldSpark.copy(0.6f),
                fontStyle  = FontStyle.Italic,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = translateConfigText("The Arena Prepares", language),
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = translateConfigText("AI models are downloading to power your quiz experience. This happens only once.", language),
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onBackground.copy(0.6f),
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
                filesRemaining = filesRemaining,
                language = language
            )

            Spacer(Modifier.height(28.dp))

            // Feature list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, GoldSpark.copy(0.18f), MaterialTheme.shapes.large)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = translateConfigText("AWAITING YOU", language),
                    fontSize = 10.sp,
                    color    = GoldSpark.copy(0.8f),
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
                        Text(translateConfigText(text, language), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(GoldSpark.copy(0.4f), Saffron.copy(0.4f))),
                        shape = MaterialTheme.shapes.medium
                    )
                    .background(Color.Transparent)
                    .clickable(onClick = onBackClick)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = translateConfigText("← Return to Home", language),
                    color      = GoldPale.copy(0.7f),
                    fontWeight = FontWeight.Medium,
                    fontSize   = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = translateConfigText("Please wait until models finish downloading.", language),
                fontSize  = 11.sp,
                color     = MaterialTheme.colorScheme.onBackground.copy(0.4f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun DownloadProgressCardContent(
    remainingMb     : Int?,
    perFileProgress : List<ModelDownloadProgress>,
    filesRemaining  : Int,
    language        : String = "tel"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, GoldSpark.copy(0.25f), MaterialTheme.shapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text = translateConfigText("DOWNLOAD PROGRESS", language),
                fontSize = 10.sp,
                color    = GoldSpark.copy(0.8f),
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
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
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (perFileProgress.isNotEmpty()) {
            val filesRemainingText = if (language == "tel") {
                "$filesRemaining ఫైల్(లు) మిగిలి ఉన్నాయి"
            } else {
                "$filesRemaining file(s) remaining"
            }
            Text(
                text = filesRemainingText,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
            perFileProgress.forEachIndexed { idx, p ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val fileLabel = if (language == "tel") "ఫైల్ ${idx + 1}" else "File ${idx + 1}"
                        Text(
                            text = fileLabel,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                        )
                        Text(
                            text = "${p.percentage}%",
                            fontSize   = 12.sp,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Progress bar — gold fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surface)
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
                    .background(MaterialTheme.colorScheme.surface)
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
                text = translateConfigText("Preparing download…", language),
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

