package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.YogaLevel
import com.aipoweredgita.app.network.YogaSubStage
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.components.LotusBadge
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlin.math.*

// ─── Colour palette (mirrors the HTML/CSS tokens) ───────────────────────────

private val BgDeep         = Color(0xFF0A0600)
private val BgCard         = Color(0xFF0E0900)
private val BgCardActive   = Color(0xFF160900)
private val BgCardDone     = Color(0xFF0D0800)
private val BgCardLocked   = Color(0xFF09070A)
private val BorderBase     = Color(0xFF2A1800)
private val BorderActive   = Color(0xFF7A3800)
private val BorderDone     = Color(0xFF2A1800)
private val BorderLocked   = Color(0xFF141008)
private val TextActive     = Color(0xFFE09030)
private val TextDone       = Color(0xFFA07030)
private val TextLocked     = Color(0xFF2A2015)
private val RangeBgActive  = Color(0xFF2A1200)
private val RangeBgDone    = Color(0xFF1A1000)
private val RangeBgLocked  = Color(0xFF0D0A00)
private val RangeBrActive  = Color(0xFF6A3000)
private val RangeBrDone    = Color(0xFF2A1A00)
private val RangeBrLocked  = Color(0xFF161200)
private val RangeTxtActive = Color(0xFFC07020)
private val RangeTxtDone   = Color(0xFF6A4010)
private val RangeTxtLocked = Color(0xFF1E1A10)
private val DotActive      = Color(0xFFF0B000)
private val DotDone        = Color(0xFFC07020)
private val DotLocked      = Color(0xFF2A2010)
private val SubTagActiveBg = Color(0xFF200E00)
private val SubTagActiveBr = Color(0xFF8A4000)
private val SubTagActiveTx = Color(0xFFD07020)
private val SubTagDoneBg   = Color(0xFF0D0900)
private val SubTagDoneBr   = Color(0xFF2A1800)
private val SubTagDoneTx   = Color(0xFF7A5020)
private val SubTagLockBg   = Color(0xFF080600)
private val SubTagLockBr   = Color(0xFF100D00)
private val SubTagLockTx   = Color(0xFF1C1A10)

// ─── Main composable ────────────────────────────────────────────────────────

@Composable
fun AwakeningPage(modifier: Modifier = Modifier) {
    val uiCfg = LocalUiConfig.current
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val stats       by profileViewModel.stats.collectAsState()
    val totalCoins  by profileViewModel.coinBalance.collectAsState()
    val level        = YogaLevelManager.levelFor(stats)
    val intensity    = YogaLevelManager.compositeScore(stats)

    var yogaLevels    by remember { mutableStateOf<List<YogaLevel>>(emptyList()) }
    var yogaSubStages by remember { mutableStateOf<List<YogaSubStage>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val res = CoinApi.retrofitService.getYogaStages()
            yogaLevels    = res.levels
            yogaSubStages = res.sub_stages
        } catch (_: Exception) {}
    }
    LaunchedEffect(stats) {
        if (stats?.userId?.isNotEmpty() == true) profileViewModel.refreshCoinBalance()
    }

    val levelEmojis = mapOf(1 to "🌿", 2 to "🔥", 3 to "🧠", 4 to "📘", 5 to "🌸")
    val currentLevel = if (yogaLevels.isNotEmpty()) {
        yogaLevels.indexOfFirst { totalCoins in it.min_coins until it.max_coins }
            .let { if (it < 0) yogaLevels.indexOfLast { l -> totalCoins >= l.max_coins }.coerceAtLeast(0) else it }
    } else 0

    com.aipoweredgita.app.ui.background.AppBackground(
        pattern    = com.aipoweredgita.app.ui.background.BgPattern.ORBS_MANDALA,
        intensity  = 0.35f,
        isDark     = isSystemInDarkTheme()
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (uiCfg.isLandscape) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            // ── Title ──────────────────────────────────────────────────────
            Text(
                text      = "Awakening Consciousness",
                style     = MaterialTheme.typography.headlineMedium.copy(
                    color      = Color(0xFFF0C840),
                    fontWeight = FontWeight.Bold,
                    shadow     = Shadow(
                        color      = Color(0xFFC07020).copy(alpha = 0.6f),
                        offset     = Offset(0f, 2f),
                        blurRadius = 12f
                    )
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text      = "· YOGA MARG · PATH TO LIBERATION ·",
                fontSize  = 8.sp,
                color     = Color(0xFF4A3010),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // ── Sacred Flame Hero ──────────────────────────────────────────
            Box(
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment  = Alignment.BottomCenter
            ) {
                // Flame particle canvas
                SacredFlame(modifier = Modifier.fillMaxSize())

                // Dark gradient overlay (top + bottom fade)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color(0xFF0A0600).copy(alpha = 0.30f),
                                0.40f to Color.Transparent,
                                0.80f to Color(0xFF0A0600).copy(alpha = 0.80f),
                                1.00f to Color(0xFF0A0600)
                            )
                        )
                )

                // Hero overlay content
                Column(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    // Rotating mandala + badge
                    Box(
                        modifier         = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MandalaBadge(intensity = intensity)
                        LotusBadge(level = level, size = 64.dp)
                    }

                    val currentLevelName = yogaLevels.getOrNull(currentLevel)?.name ?: "Yoga Path"
                    Text(
                        text       = currentLevelName,
                        fontSize   = 18.sp,
                        color      = Color(0xFFF0C840),
                        fontWeight = FontWeight.Bold,
                        fontStyle  = FontStyle.Normal
                    )
                    Text(
                        text     = "🪙 $totalCoins",
                        fontSize = 13.sp,
                        color    = GoldSpark
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Sub-stage progress bar ─────────────────────────────────────
            val activeSub = yogaSubStages.lastOrNull { totalCoins >= it.min_coins }
            if (activeSub != null) {
                val nextSub = yogaSubStages.firstOrNull { it.min_coins > totalCoins }
                val subProgress = if (nextSub != null)
                    (totalCoins - activeSub.min_coins).toFloat() /
                            (nextSub.min_coins - activeSub.min_coins).toFloat()
                else 1f

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "· ${activeSub.sub_name.uppercase()} STAGE ·",
                        fontSize      = 9.sp,
                        color         = GoldSpark.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "$totalCoins / ${nextSub?.min_coins ?: activeSub.max_coins}",
                        fontSize   = 10.sp,
                        color      = GoldSpark.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(5.dp))

                // Progress track
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1A1000))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(subProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8A4000), Color(0xFFE08000), GoldSpark)
                                )
                            )
                    )
                    // Glow dot at fill end
                    Box(
                        Modifier
                            .size(8.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFF0C000))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── ॐ divider ──────────────────────────────────────────────────
            OmDivider()

            Spacer(Modifier.height(8.dp))

            Text(
                text          = "· THE FIVE PATHS OF LIBERATION ·",
                fontSize      = 8.sp,
                color         = Color(0xFF2A2015),
                letterSpacing = 2.5.sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // ── Vertical stage path ────────────────────────────────────────
            yogaLevels.forEachIndexed { index, yl ->
                val subs   = yogaSubStages.filter { it.level == yl.level }.sortedBy { it.sub_level }
                val done   = totalCoins >= yl.max_coins
                val active = index == currentLevel
                val locked = !done && !active
                val emoji  = levelEmojis[yl.level] ?: "✨"

                YogaMargStage(
                    emoji        = emoji,
                    name         = yl.name,
                    range        = "${yl.min_coins} – ${yl.max_coins}",
                    subs         = subs,
                    currentCoins = totalCoins,
                    done         = done,
                    active       = active,
                    locked       = locked,
                    isLast       = index == yogaLevels.lastIndex
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── ॐ divider ──────────────────────────────────────────────────
            OmDivider()

            Spacer(Modifier.height(12.dp))

            // ── Sacred Rewards card ────────────────────────────────────────
            val multiplier = YogaLevelManager.getCoinMultiplier(stats)
            SacredRewardsCard(multiplier = multiplier)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── OM Divider ─────────────────────────────────────────────────────────────

@Composable
private fun OmDivider() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF3A2000), Color.Transparent)
                    )
                )
        )
        Text("  ॐ  ", fontSize = 16.sp, color = Color(0xFF8A5010))
        Box(
            Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF3A2000), Color.Transparent)
                    )
                )
        )
    }
}

// ─── YogaMargStage ───────────────────────────────────────────────────────────

@Composable
private fun YogaMargStage(
    emoji        : String,
    name         : String,
    range        : String,
    subs         : List<YogaSubStage>,
    currentCoins : Int,
    done         : Boolean,
    active       : Boolean,
    locked       : Boolean,
    isLast       : Boolean
) {
    val cardAlpha  = if (locked) 0.45f else 1f
    val bgColor    = if (active) BgCardActive  else if (done) BgCardDone  else BgCardLocked
    val borderClr  = if (active) BorderActive  else if (done) BorderDone  else BorderLocked
    val nameColor  = if (active) TextActive    else if (done) TextDone    else TextLocked
    val rangeBg    = if (active) RangeBgActive else if (done) RangeBgDone else RangeBgLocked
    val rangeBr    = if (active) RangeBrActive else if (done) RangeBrDone else RangeBrLocked
    val rangeTxt   = if (active) RangeTxtActive else if (done) RangeTxtDone else RangeTxtLocked
    val dotColor   = if (active) DotActive      else if (done) DotDone     else DotLocked

    // Active dot pulses
    val infiniteAnim = rememberInfiniteTransition(label = "dot-pulse-$name")
    val dotGlow by if (active) infiniteAnim.animateFloat(
        initialValue   = 0.3f,
        targetValue    = 0.8f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot-glow"
    ) else remember { mutableStateOf(0f) }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha },
        verticalAlignment = Alignment.Top
    ) {
        // ── Connector column ──
        Column(
            modifier              = Modifier.width(36.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            val dotSize = if (active) 22.dp else 18.dp
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(
                        when {
                            active -> Color(0xFF2A1200)
                            done   -> Color(0xFF1A0E00)
                            else   -> Color(0xFF0D0900)
                        }
                    )
                    .border(
                        width = if (active) 1.5.dp else 1.dp,
                        color = dotColor.copy(alpha = if (active) 0.6f + dotGlow * 0.4f else 1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    done   -> Text("✓",  fontSize = 9.sp,  color = dotColor, fontWeight = FontWeight.Bold)
                    active -> Text("✦",  fontSize = 10.sp, color = dotColor)
                    else   -> {
                        // Lock icon drawn with Canvas
                        Canvas(Modifier.size(10.dp)) {
                            val w = size.width; val h = size.height
                            val paint = androidx.compose.ui.graphics.Paint().apply {
                                this.color = dotColor; strokeWidth = 1f
                                style = PaintingStyle.Stroke
                            }
                            drawContext.canvas.drawRoundRect(
                                left   = w * 0.15f, top    = h * 0.44f,
                                right  = w * 0.85f, bottom = h * 0.95f,
                                radiusX = 1.5f, radiusY = 1.5f, paint = paint
                            )
                            val path = Path().apply {
                                moveTo(w * 0.3f, h * 0.44f)
                                lineTo(w * 0.3f, h * 0.28f)
                                quadraticBezierTo(w * 0.3f, h * 0.05f, w * 0.5f, h * 0.05f)
                                quadraticBezierTo(w * 0.7f, h * 0.05f, w * 0.7f, h * 0.28f)
                                lineTo(w * 0.7f, h * 0.44f)
                            }
                            drawContext.canvas.drawPath(path, paint)
                        }
                    }
                }
            }

            // Connecting line (not shown on last item)
            if (!isLast) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(
                            Brush.verticalGradient(
                                when {
                                    active -> listOf(Color(0xFFC07000), Color(0xFF4A2800))
                                    done   -> listOf(Color(0xFF4A2800), Color(0xFF2A1400))
                                    else   -> listOf(Color(0xFF141000), Color(0xFF141000))
                                }
                            )
                        )
                )
                Spacer(Modifier.height(3.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.width(6.dp))

        // ── Stage card ──
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(0.5.dp, borderClr, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Header row: emoji + name + range pill
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment         = Alignment.CenterVertically,
                horizontalArrangement     = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        name,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = nameColor
                    )
                }
                Text(
                    range,
                    fontSize      = 8.sp,
                    color         = rangeTxt,
                    letterSpacing = 0.3.sp,
                    modifier      = Modifier
                        .background(rangeBg, RoundedCornerShape(20.dp))
                        .border(0.5.dp, rangeBr, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Sub-stage tags
            SubStageTags(
                subs         = subs,
                currentCoins = currentCoins
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

// ─── Sacred Rewards Card ─────────────────────────────────────────────────────

@Composable
private fun SacredRewardsCard(multiplier: Float) {
    GlassCard(
        modifier     = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        tint         = Color(0xFF0E0900).copy(alpha = 0.9f),
        border       = Color(0xFF2A1800)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Sacred Rewards (Active: ${multiplier}x Bonus) 🪙",
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFF0C840),
                fontSize   = 12.sp
            )
            Spacer(Modifier.height(2.dp))
            RewardItem("📖 Chapter complete: +1")
            RewardItem("Daily check-in: 1–7 coins + 7 bonus on day 7")
            RewardItem("Weekly bonus: 10–20 coins")
            RewardItem("Share verse: 1–7 coins (7-day streak)")
            RewardItem("Voice inquiry: −2 to −5 coins")
            RewardItem("Level up: +10 🪙", highlight = true)
            Text(
                "Bonus: Base × $multiplier streak multiplier",
                fontSize  = 9.sp,
                color     = Color(0xFF6A4010),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun RewardItem(text: String, highlight: Boolean = false) {
    Text(
        text      = text,
        fontSize  = 10.sp,
        color     = if (highlight) GoldSpark else Color(0xFF6A4010),
        fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
        lineHeight = 18.sp
    )
}

// ─── MandalaBadge (rotating aura ring matching HTML mandala2 canvas) ─────────

@Composable
private fun MandalaBadge(
    intensity : Float,
    modifier  : Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "mandala")
    val rot by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "rot"
    )
    val pulse by infinite.animateFloat(
        initialValue  = 0.95f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx    = size.width  / 2f
        val cy    = size.height / 2f
        val minDim = min(size.width, size.height)

        // Three ring layers matching HTML: radii 36, 28, 20 (scaled to dp → px)
        val radii    = listOf(minDim * 0.40f, minDim * 0.31f, minDim * 0.22f)
        val segments = listOf(8,              12,              16)
        val alphas   = listOf(0.30f,          0.25f,           0.20f)

        radii.forEachIndexed { i, r ->
            val count  = segments[i]
            val dir    = if (i % 2 == 0) 1f else -1f
            val rotRad = Math.toRadians((rot * dir * (i + 1) * 0.4f).toDouble()).toFloat()
            val alpha  = alphas[i]
            val red    = ((180 + i * 20) / 255f).coerceIn(0f, 1f)
            val green  = ((80  + i * 20) / 255f).coerceIn(0f, 1f)
            val color  = Color(red = red, green = green, blue = 0f, alpha = alpha)

            repeat(count) { s ->
                val a   = rotRad + (s.toFloat() / count) * (2f * PI.toFloat())
                val a2  = a + PI.toFloat() / count
                val path = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + cos(a)  * r * pulse, cy + sin(a)  * r * pulse)
                    lineTo(cx + cos(a2) * r * 0.7f * pulse, cy + sin(a2) * r * 0.7f * pulse)
                    close()
                }
                drawPath(path, color)
            }
        }

        // Outer glow rings
        drawCircle(
            color  = Color(0xFFC07020).copy(alpha = 0.18f * intensity.coerceIn(0f, 1f)),
            radius = minDim * 0.46f * pulse,
            center = Offset(cx, cy)
        )
        drawCircle(
            color  = Color(0xFFF0C840).copy(alpha = 0.10f * intensity.coerceIn(0f, 1f)),
            radius = minDim * 0.50f * pulse,
            center = Offset(cx, cy)
        )
    }
}

// ─── SacredFlame ─────────────────────────────────────────────────────────────
//  Three layers for a powerful flame:
//   1. CORE  — 60 large slow particles, wide base, very bright
//   2. MIDDLE — 80 medium particles, main flame body
//   3. EMBER  — 40 tiny fast sparks that fly high
// Trail effect: background rect drawn with low alpha each frame so old particles
// fade slowly instead of vanishing instantly (same trick as the HTML canvas).

private data class FlameParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var life: Float, val maxLife: Float,
    val size: Float,
    val layer: Int          // 0=core, 1=middle, 2=ember
)

private fun rnd() = Math.random().toFloat()

private fun makeParticle(layer: Int): FlameParticle {
    val spread = when (layer) { 0 -> 0.30f; 1 -> 0.22f; else -> 0.15f }
    val speed  = when (layer) { 0 -> 1.8f;  1 -> 3.0f;  else -> 5.5f  }
    val minSpd = when (layer) { 0 -> 0.8f;  1 -> 1.5f;  else -> 3.5f  }
    val sz     = when (layer) { 0 -> rnd() * 22f + 14f; 1 -> rnd() * 12f + 6f; else -> rnd() * 4f + 2f }
    val ml     = when (layer) { 0 -> rnd() * 0.5f + 0.5f; 1 -> rnd() * 0.6f + 0.4f; else -> rnd() * 0.4f + 0.2f }
    return FlameParticle(
        x       = 0.5f + (rnd() - 0.5f) * spread,
        y       = 0.90f + rnd() * 0.06f,
        vx      = (rnd() - 0.5f) * 1.4f,
        vy      = -(rnd() * speed + minSpd),
        life    = rnd() * ml,
        maxLife = ml,
        size    = sz,
        layer   = layer
    )
}

@Composable
fun SacredFlame(modifier: Modifier = Modifier) {
    val particles = remember {
        (List(60) { makeParticle(0) } +
                List(80) { makeParticle(1) } +
                List(40) { makeParticle(2) }).toMutableList()
    }

    // Drive recomposition at ~60 fps
    val infinite = rememberInfiniteTransition(label = "flame-tick")
    @Suppress("UNUSED_VARIABLE")
    val tick by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(16, easing = LinearEasing)),
        label         = "tick"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // ── Background: semi-transparent fill creates the motion-blur trail ──
        drawRect(color = Color(0xFF0A0600).copy(alpha = 0.22f))

        // ── Radial glow at base — makes flame look grounded ──
        drawCircle(
            brush  = Brush.radialGradient(
                colors  = listOf(Color(0xFFFF6000).copy(alpha = 0.35f), Color.Transparent),
                center  = Offset(cx, h * 0.88f),
                radius  = w * 0.38f
            ),
            radius = w * 0.38f,
            center = Offset(cx, h * 0.88f)
        )

        // ── Particles ──
        val dt = 0.016f   // fixed ~60fps delta
        particles.forEachIndexed { i, p ->
            // Physics update
            p.x    += (p.vx + (rnd() - 0.5f) * 1.2f) * dt
            p.y    += p.vy * dt
            // Core particles sway more; embers rise straight
            p.vx   += (rnd() - 0.5f) * (if (p.layer == 0) 0.6f else 0.3f)
            p.vx    = p.vx.coerceIn(-2f, 2f)
            p.life -= when (p.layer) { 0 -> 0.008f; 1 -> 0.011f; else -> 0.018f }

            if (p.life <= 0f) {
                particles[i] = makeParticle(p.layer)
                return@forEachIndexed
            }

            val t      = (p.life / p.maxLife).coerceIn(0f, 1f)
            val radius = p.size * t
            val color  = when (p.layer) {
                0    -> flameColorCore(t)
                1    -> flameColorMid(t)
                else -> flameColorEmber(t)
            }
            // Draw glow halo then bright core for each particle
            if (p.layer != 2) {
                drawCircle(
                    color  = color.copy(alpha = color.alpha * 0.25f),
                    radius = radius * 2.2f,
                    center = Offset(p.x * w, p.y * h)
                )
            }
            drawCircle(color = color, radius = radius, center = Offset(p.x * w, p.y * h))
        }

        // ── Bright white-hot core pillar ──
        drawRect(
            brush = Brush.verticalGradient(
                colors     = listOf(Color.Transparent, Color(0xFFFFFFCC).copy(alpha = 0.18f), Color(0xFFFFAA00).copy(alpha = 0.28f)),
                startY     = h * 0.3f,
                endY       = h * 0.92f
            ),
            topLeft = Offset(cx - w * 0.04f, h * 0.3f),
            size    = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.62f)
        )
    }
}

// Colour functions per layer
private fun flameColorCore(t: Float): Color = when {
    t > 0.75f -> Color(1f, 1f,              0.6f,           t * 0.95f)   // white-yellow
    t > 0.5f  -> Color(1f, 0.7f * t + 0.2f, 0f,            t * 0.90f)   // bright orange
    t > 0.25f -> Color(1f, 0.3f * t,        0f,             t * 0.80f)   // deep orange
    else      -> Color(0.8f, 0.1f,          0f,             t * 0.60f)   // red base
}

private fun flameColorMid(t: Float): Color = when {
    t > 0.7f -> Color(1f,          (200f + t * 55f) / 255f,  0f, t * 0.92f)
    t > 0.4f -> Color(1f,          (80f  + t * 220f) / 255f, 0f, t * 0.82f)
    t > 0.2f -> Color(200f / 255f, (40f  + t * 110f) / 255f, 0f, t * 0.65f)
    else     -> Color(100f / 255f, 20f / 255f,                0f, t * 0.45f)
}

private fun flameColorEmber(t: Float): Color = when {
    t > 0.6f -> Color(1f,          0.9f,  0.4f, t * 0.95f)   // white spark
    t > 0.3f -> Color(1f,          0.55f, 0f,   t * 0.80f)   // orange ember
    else     -> Color(0.7f,        0.15f, 0f,   t * 0.50f)   // fading red
}

// ─── SubStageTags — wrapping row without experimental FlowRow ────────────────

@Composable
private fun SubStageTags(subs: List<YogaSubStage>, currentCoins: Int) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content  = {
            subs.forEach { sub ->
                val subReached = currentCoins >= sub.min_coins
                val subActive  = currentCoins in sub.min_coins until sub.max_coins
                val tagBg  = if (subActive) SubTagActiveBg else if (subReached) SubTagDoneBg  else SubTagLockBg
                val tagBr  = if (subActive) SubTagActiveBr else if (subReached) SubTagDoneBr  else SubTagLockBr
                val tagTxt = if (subActive) SubTagActiveTx else if (subReached) SubTagDoneTx  else SubTagLockTx
                val tagAlpha = if (subReached || subActive) 1f else 0.5f
                Text(
                    text          = if (subActive) "✦ ${sub.sub_name}" else sub.sub_name,
                    fontSize      = 8.sp,
                    color         = tagTxt,
                    letterSpacing = 0.3.sp,
                    maxLines      = 1,
                    modifier      = Modifier
                        .graphicsLayer { alpha = tagAlpha }
                        .background(tagBg, RoundedCornerShape(20.dp))
                        .border(0.5.dp, tagBr, RoundedCornerShape(20.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    ) { measurables, constraints ->
        val hGap  = 3.dp.roundToPx()
        val vGap  = 3.dp.roundToPx()
        val items = measurables.map { it.measure(constraints.copy(minWidth = 0)) }

        // Wrap items into rows
        val rows   = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var rowBuf = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var rowW   = 0
        items.forEach { p ->
            val needed = if (rowBuf.isEmpty()) p.width else rowW + hGap + p.width
            if (needed > constraints.maxWidth && rowBuf.isNotEmpty()) {
                rows += rowBuf.toList()
                rowBuf = mutableListOf(p)
                rowW   = p.width
            } else {
                rowBuf += p
                rowW    = needed
            }
        }
        if (rowBuf.isNotEmpty()) rows += rowBuf.toList()

        val totalH = rows.sumOf { row -> row.maxOf { it.height } + vGap } - vGap
        layout(constraints.maxWidth, totalH.coerceAtLeast(0)) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowH = row.maxOf { it.height }
                row.forEach { p ->
                    p.placeRelative(x, y)
                    x += p.width + hGap
                }
                y += rowH + vGap
            }
        }
    }
}

// ─── SegmentCoinRow (unchanged, kept for compatibility) ──────────────────────

@Composable
fun SegmentCoinRow(name: String, count: Int) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = "$count",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = GoldSpark
            )
            Spacer(Modifier.width(4.dp))
            Text("🪙", fontSize = 14.sp)
        }
    }
}