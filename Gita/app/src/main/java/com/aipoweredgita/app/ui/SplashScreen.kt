package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.R
import kotlinx.coroutines.delay
import kotlin.math.*

// ─── Color Palette: Sacred Manuscript ────────────────────────────────────────
private object SacredColors {
    // Backgrounds
    val NightInk        = Color(0xFF0A0812)   // near-black deep indigo
    val DeepVoid        = Color(0xFF110E1A)
    val MidnightVeil    = Color(0xFF1C1628)

    // Gold hierarchy
    val GoldFlame       = Color(0xFFFFD050)   // primary gold — titles, mandala rings
    val GoldDusk        = Color(0xFFE8A825)   // medium gold — quote text
    val GoldAsh         = Color(0xFFBF8B2E)   // muted gold — secondary text
    val GoldGlow        = Color(0xFFFFE99A)   // near-white gold — accent highlight

    // Spiritual tints
    val SaffronAura     = Color(0xFFFF7B1C)   // lotus / aura orange
    val TilakRed        = Color(0xFFCC3311)   // danger / exit accent
    val MoonSilver      = Color(0xFFD4C9E8)   // body text on dark
    val StarDust        = Color(0xFF6B5F8A)   // muted decorative
}

// ─── Gita Quote Pool ──────────────────────────────────────────────────────────
private val gitaQuotes = listOf(
    "You have a right to perform your prescribed duty,\nbut you are not entitled to the fruits of action.",
    "Man is made by his belief.\nAs he believes, so he is.",
    "Thinking of objects, attachment to them is formed.\nFrom attachment longing arises,\nand from longing, anger.",
    "For one who has conquered the mind,\nthe mind is the best of friends.",
    "The soul is never born, nor does it ever die;\nnor, having once existed, does it cease to be.",
    "Set thy heart upon thy work,\nbut never on its reward.",
    "Change is the law of the universe.\nYou can be a millionaire, or a pauper, in an instant.",
    "A gift is pure when it is given\nfrom the heart to the right person\nat the right time and place."
)

// ─── Mandala Canvas ───────────────────────────────────────────────────────────
@Composable
private fun MandalaCanvas(
    rotationDeg: Float,
    auraAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.center.x
        val cy = size.center.y
        val r  = size.minDimension * 0.42f

        // Outer radial aura glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SacredColors.SaffronAura.copy(alpha = auraAlpha * 0.35f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.6f
            ),
            radius = r * 1.6f,
            center = Offset(cx, cy)
        )

        // Rotating outer petal ring (8 petals)
        rotate(rotationDeg, pivot = Offset(cx, cy)) {
            repeat(8) { i ->
                val angle  = (i * 45f) * (PI / 180f).toFloat()
                val px     = cx + r * cos(angle)
                val py     = cy + r * sin(angle)
                drawCircle(
                    color  = SacredColors.GoldFlame.copy(alpha = 0.18f),
                    radius = r * 0.14f,
                    center = Offset(px, py)
                )
            }
        }

        // Rotating inner petal ring (8 petals, counter)
        rotate(-rotationDeg * 0.6f, pivot = Offset(cx, cy)) {
            repeat(8) { i ->
                val angle  = (i * 45f + 22.5f) * (PI / 180f).toFloat()
                val px     = cx + r * 0.62f * cos(angle)
                val py     = cy + r * 0.62f * sin(angle)
                drawCircle(
                    color  = SacredColors.GoldDusk.copy(alpha = 0.22f),
                    radius = r * 0.09f,
                    center = Offset(px, py)
                )
            }
        }

        // Concentric circle halos
        listOf(0.28f, 0.50f, 0.72f, 0.95f).forEach { scale ->
            drawCircle(
                color  = SacredColors.GoldFlame.copy(alpha = 0.10f),
                radius = r * scale,
                center = Offset(cx, cy),
                style  = Stroke(width = 0.8f)
            )
        }

        // 12-spoke radial lines
        rotate(rotationDeg * 0.3f, pivot = Offset(cx, cy)) {
            repeat(12) { i ->
                val angle = (i * 30f) * (PI / 180f).toFloat()
                drawLine(
                    color       = SacredColors.GoldAsh.copy(alpha = 0.15f),
                    start       = Offset(cx + r * 0.3f * cos(angle), cy + r * 0.3f * sin(angle)),
                    end         = Offset(cx + r * 0.95f * cos(angle), cy + r * 0.95f * sin(angle)),
                    strokeWidth = 0.6f
                )
            }
        }
    }
}

// ─── Particle Field ───────────────────────────────────────────────────────────
private data class Particle(val x: Float, val y: Float, val radius: Float, val phase: Float)

@Composable
private fun ParticleField(modifier: Modifier = Modifier) {
    val particles = remember {
        List(42) {
            Particle(
                x      = (0..1000).random() / 1000f,
                y      = (0..1000).random() / 1000f,
                radius = (2..6).random() / 10f + 0.5f,
                phase  = (0..628).random() / 100f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "tick"
    )

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val alpha = (sin(tick + p.phase) * 0.3f + 0.5f).coerceIn(0f, 1f)
            drawCircle(
                color  = SacredColors.GoldGlow.copy(alpha = alpha),
                radius = p.radius.dp.toPx(),
                center = Offset(size.width * p.x, size.height * p.y)
            )
        }
    }
}

// ─── Decorative Divider ───────────────────────────────────────────────────────
@Composable
private fun GoldDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(12.dp).fillMaxWidth()) {
        val cx = size.center.x
        val cy = size.center.y
        val halfW = size.width / 2f

        // Left arm
        drawLine(
            brush  = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, SacredColors.GoldAsh)
            ),
            start       = Offset(cx - halfW, cy),
            end         = Offset(cx - 12f, cy),
            strokeWidth = 0.8f
        )
        // Right arm
        drawLine(
            brush  = Brush.horizontalGradient(
                colors = listOf(SacredColors.GoldAsh, Color.Transparent)
            ),
            start       = Offset(cx + 12f, cy),
            end         = Offset(cx + halfW, cy),
            strokeWidth = 0.8f
        )
        // Centre diamond
        drawCircle(
            color  = SacredColors.GoldFlame,
            radius = 3.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}

// ─── Om Symbol Badge ──────────────────────────────────────────────────────────
@Composable
private fun OmBadge() {
    Surface(
        shape  = RoundedCornerShape(40.dp),
        color  = SacredColors.GoldFlame.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SacredColors.GoldFlame.copy(alpha = 0.40f))
    ) {
        Row(
            modifier               = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
            verticalAlignment      = Alignment.CenterVertically,
            horizontalArrangement  = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "ॐ", fontSize = 15.sp, color = SacredColors.GoldFlame)
            Text(
                text         = "Gemma 4 · Wisdom Engine",
                fontSize     = 12.sp,
                fontWeight   = FontWeight.Medium,
                color        = SacredColors.GoldDusk,
                letterSpacing = 0.6.sp
            )
        }
    }
}

// ─── SPLASH SCREEN ────────────────────────────────────────────────────────────
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }
    val uiCfg = LocalUiConfig.current
    val quote  = remember { gitaQuotes.random() }

    // Entry animations
    val alpha by animateFloatAsState(
        targetValue    = if (startAnimation) 1f else 0f,
        animationSpec  = tween(1200, easing = FastOutSlowInEasing),
        label          = "alpha"
    )
    val logoScale by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0.55f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "logoScale"
    )
    val titleSlide by animateFloatAsState(
        targetValue   = if (startAnimation) 0f else 40f,
        animationSpec = tween(900, delayMillis = 300, easing = FastOutSlowInEasing),
        label         = "titleSlide"
    )

    // Continuous mandala rotation
    val infiniteTransition = rememberInfiniteTransition(label = "mandala")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label         = "rotation"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.30f,
        targetValue   = 0.65f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label         = "aura"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000)
        onSplashFinished()
    }

    Box(
        modifier          = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SacredColors.NightInk,
                        SacredColors.DeepVoid,
                        SacredColors.MidnightVeil
                    )
                )
            ),
        contentAlignment  = Alignment.Center
    ) {

        // Particle field (background layer)
        ParticleField(modifier = Modifier.fillMaxSize())

        // Mandala backdrop centred
        MandalaCanvas(
            rotationDeg = rotation,
            auraAlpha   = auraAlpha,
            modifier    = Modifier
                .size(320.dp)
                .align(Alignment.Center)
        )

        // Main content column
        Column(
            modifier               = Modifier
                .alpha(alpha)
                .padding(
                    horizontal = if (uiCfg.isLandscape) 48.dp else 36.dp
                ),
            horizontalAlignment    = Alignment.CenterHorizontally,
            verticalArrangement    = Arrangement.Center
        ) {

            // Logo in glassy circle
            Box(contentAlignment = Alignment.Center) {
                // Outer frosted ring
                Surface(
                    modifier = Modifier.size(148.dp),
                    shape    = CircleShape,
                    color    = SacredColors.GoldFlame.copy(alpha = 0.06f),
                    border   = androidx.compose.foundation.BorderStroke(
                        0.8.dp, SacredColors.GoldFlame.copy(alpha = 0.30f)
                    )
                ) {}
                // Inner glassy disc
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape    = CircleShape,
                    color    = SacredColors.GoldFlame.copy(alpha = 0.04f),
                    border   = androidx.compose.foundation.BorderStroke(
                        0.5.dp, SacredColors.GoldGlow.copy(alpha = 0.20f)
                    )
                ) {}
                // Logo image
                Image(
                    painter            = painterResource(id = R.drawable.krishna_icon),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier           = Modifier
                        .size(88.dp)
                        .scale(logoScale)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Sanskrit header ornament
            Text(
                text          = "॥ श्रीमद्भगवद्गीता ॥",
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Normal,
                color         = SacredColors.GoldAsh,
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // App title
            Text(
                text          = stringResource(id = R.string.app_name),
                fontSize      = 30.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = SacredColors.GoldFlame,
                textAlign     = TextAlign.Center,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            GoldDivider(modifier = Modifier.fillMaxWidth(0.72f))

            Spacer(modifier = Modifier.height(20.dp))

            // Quote
            Text(
                text       = "\u201C$quote\u201D",
                fontSize   = 15.sp,
                fontStyle  = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                color      = SacredColors.MoonSilver.copy(alpha = 0.88f),
                textAlign  = TextAlign.Center,
                lineHeight = 24.sp,
                modifier   = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Footer
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(bottom = 44.dp)
                .alpha(alpha),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment  = Alignment.CenterHorizontally,
                verticalArrangement  = Arrangement.spacedBy(10.dp)
            ) {
                OmBadge()
                Text(
                    text          = "v1.6.0 · Build 4",
                    fontSize      = 10.sp,
                    color         = SacredColors.StarDust,
                    letterSpacing = 1.8.sp,
                    fontWeight    = FontWeight.Light
                )
            }
        }
    }
}

// ─── EXIT SCREEN ──────────────────────────────────────────────────────────────
@Composable
fun ExitScreen(
    onConfirmExit: () -> Unit,
    onCancelExit:  () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "alpha"
    )
    val cardScale by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "cardScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "exitAura")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.50f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label         = "exitAura"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)),
        label         = "exitRot"
    )

    LaunchedEffect(Unit) { startAnimation = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SacredColors.NightInk,
                        SacredColors.DeepVoid,
                        SacredColors.MidnightVeil
                    )
                )
            )
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        ParticleField(modifier = Modifier.fillMaxSize())

        MandalaCanvas(
            rotationDeg = rotation,
            auraAlpha   = auraAlpha,
            modifier    = Modifier
                .size(280.dp)
                .align(Alignment.Center)
        )

        // Dialogue card
        Surface(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .scale(cardScale),
            shape  = RoundedCornerShape(24.dp),
            color  = SacredColors.NightInk.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(
                0.8.dp, SacredColors.GoldFlame.copy(alpha = 0.30f)
            )
        ) {
            Column(
                modifier               = Modifier.padding(32.dp),
                horizontalAlignment    = Alignment.CenterHorizontally,
                verticalArrangement    = Arrangement.spacedBy(0.dp)
            ) {
                // Logo
                Image(
                    painter            = painterResource(id = R.drawable.krishna_icon),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier           = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text          = "॥ धन्यवाद ॥",
                    fontSize      = 11.sp,
                    color         = SacredColors.GoldAsh,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text       = "Thank You",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SacredColors.GoldFlame
                )

                Spacer(modifier = Modifier.height(10.dp))

                GoldDivider(modifier = Modifier.fillMaxWidth(0.60f))

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text      = "The Gita travels with you.\nCome back whenever you seek wisdom.",
                    fontSize  = 14.sp,
                    color     = SacredColors.MoonSilver.copy(alpha = 0.80f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text      = "Are you sure you want to exit?",
                    fontSize  = 13.sp,
                    color     = SacredColors.StarDust,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stay button
                    OutlinedButton(
                        onClick = onCancelExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp, SacredColors.GoldFlame.copy(alpha = 0.50f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SacredColors.GoldFlame
                        )
                    ) {
                        Text(
                            text       = "Stay",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Exit button
                    androidx.compose.material3.Button(
                        onClick  = onConfirmExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredColors.TilakRed,
                            contentColor   = SacredColors.GoldGlow
                        )
                    ) {
                        Text(
                            text          = "Exit",
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}