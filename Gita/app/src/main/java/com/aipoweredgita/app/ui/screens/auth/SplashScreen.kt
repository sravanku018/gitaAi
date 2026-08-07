package com.aipoweredgita.app.ui.screens.auth

import androidx.compose.animation.core.*
import com.aipoweredgita.app.ui.LocalUiConfig
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.theme.NightInk
import com.aipoweredgita.app.ui.theme.DeepVoid
import com.aipoweredgita.app.ui.theme.MoonSilver
import kotlinx.coroutines.delay
import kotlin.math.*

// ─── Splash-specific color constants ──────────────────────────────────────────
private val MidnightVeil = Color(0xFF1C1628)
private val GoldFlame    = Color(0xFFFFD050)   // primary gold — titles, mandala rings
private val GoldDusk     = Color(0xFFE8A825)   // medium gold — quote text
private val GoldAsh      = Color(0xFFBF8B2E)   // muted gold — secondary text
private val GoldGlow     = Color(0xFFFFE99A)   // near-white gold — accent highlight
private val SaffronAura  = Color(0xFFFF7B1C)   // lotus / aura orange
private val TilakRed     = Color(0xFFCC3311)   // danger / exit accent
private val StarDust     = Color(0xFF6B5F8A)   // muted decorative

/**
 * Brand logo for splash/exit: circular mask with soft edge.
 * [splash_logo] is square with light corners — Crop + slight zoom hides the square edge.
 */
@Composable
private fun BrandLogoMark(
    size: Dp,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(elevation = 12.dp, shape = CircleShape, clip = false, ambientColor = GoldFlame.copy(alpha = 0.35f))
    ) {
        // Soft outer halo (hides hard clip)
        Box(
            Modifier
                .matchParentSize()
                .background(GoldFlame.copy(alpha = 0.10f), CircleShape)
        )
        // Dark disc under the art so white PNG corners never show
        Box(
            Modifier
                .size(size * 0.92f)
                .background(MidnightVeil, CircleShape)
                .border(1.2.dp, GoldFlame.copy(alpha = 0.40f), CircleShape)
        )
        Image(
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = stringResource(id = R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size * 0.88f)
                .clip(CircleShape)
                // Zoom past light/white margins of the square asset
                .scale(1.14f)
        )
        // Thin inner highlight ring for a clean edge
        Box(
            Modifier
                .size(size * 0.88f)
                .border(0.8.dp, GoldGlow.copy(alpha = 0.22f), CircleShape)
        )
    }
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
    val outerCos = remember { floatArrayOf(1f, 0.7071f, 0f, -0.7071f, -1f, -0.7071f, 0f, 0.7071f) }
    val outerSin = remember { floatArrayOf(0f, 0.7071f, 1f, 0.7071f, 0f, -0.7071f, -1f, -0.7071f) }
    val innerCos = remember {
        FloatArray(8) { i ->
            cos((i * 45f + 22.5f) * (Math.PI / 180f).toFloat())
        }
    }
    val innerSin = remember {
        FloatArray(8) { i ->
            sin((i * 45f + 22.5f) * (Math.PI / 180f).toFloat())
        }
    }
    val spokeCos = remember {
        FloatArray(12) { i ->
            cos((i * 30f) * (Math.PI / 180f).toFloat())
        }
    }
    val spokeSin = remember {
        FloatArray(12) { i ->
            sin((i * 30f) * (Math.PI / 180f).toFloat())
        }
    }
    val radialGlowBrush = remember { Brush.radialGradient(listOf(SaffronAura, Color.Transparent)) }
    val haloScales = remember { floatArrayOf(0.28f, 0.50f, 0.72f, 0.95f) }
    val strokeCached = remember { Stroke(width = 0.8f) }

    Canvas(modifier = modifier) {
        val cx = size.center.x
        val cy = size.center.y
        val r  = size.minDimension * 0.42f

        // Outer radial aura glow
        drawCircle(
            brush = radialGlowBrush,
            radius = r * 1.6f,
            center = Offset(cx, cy),
            alpha = auraAlpha * 0.35f
        )

        // Rotating outer petal ring (8 petals)
        rotate(rotationDeg, pivot = Offset(cx, cy)) {
            for (i in 0 until 8) {
                val px     = cx + r * outerCos[i]
                val py     = cy + r * outerSin[i]
                drawCircle(
                    color  = GoldFlame.copy(alpha = 0.18f),
                    radius = r * 0.14f,
                    center = Offset(px, py)
                )
            }
        }

        // Rotating inner petal ring (8 petals, counter)
        rotate(-rotationDeg * 0.6f, pivot = Offset(cx, cy)) {
            for (i in 0 until 8) {
                val px     = cx + r * 0.62f * innerCos[i]
                val py     = cy + r * 0.62f * innerSin[i]
                drawCircle(
                    color  = GoldDusk.copy(alpha = 0.22f),
                    radius = r * 0.09f,
                    center = Offset(px, py)
                )
            }
        }

        // Concentric circle halos
        for (i in 0 until 4) {
            drawCircle(
                color  = GoldFlame.copy(alpha = 0.10f),
                radius = r * haloScales[i],
                center = Offset(cx, cy),
                style  = strokeCached
            )
        }

        // 12-spoke radial lines
        rotate(rotationDeg * 0.3f, pivot = Offset(cx, cy)) {
            for (i in 0 until 12) {
                drawLine(
                    color       = GoldAsh.copy(alpha = 0.15f),
                    start       = Offset(cx + r * 0.3f * spokeCos[i], cy + r * 0.3f * spokeSin[i]),
                    end         = Offset(cx + r * 0.95f * spokeCos[i], cy + r * 0.95f * spokeSin[i]),
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val particles = remember {
        List(42) {
            val radiusDp = (2..6).random() / 10f + 0.5f
            val radiusPx = with(density) { radiusDp.dp.toPx() }
            Particle(
                x      = (0..1000).random() / 1000f,
                y      = (0..1000).random() / 1000f,
                radius = radiusPx,
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
                color  = GoldGlow.copy(alpha = alpha),
                radius = p.radius,
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
                colors = listOf(Color.Transparent, GoldAsh)
            ),
            start       = Offset(cx - halfW, cy),
            end         = Offset(cx - 12f, cy),
            strokeWidth = 0.8f
        )
        // Right arm
        drawLine(
            brush  = Brush.horizontalGradient(
                colors = listOf(GoldAsh, Color.Transparent)
            ),
            start       = Offset(cx + 12f, cy),
            end         = Offset(cx + halfW, cy),
            strokeWidth = 0.8f
        )
        // Centre diamond
        drawCircle(
            color  = GoldFlame,
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
        color  = GoldFlame.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldFlame.copy(alpha = 0.40f))
    ) {
        Row(
            modifier               = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
            verticalAlignment      = Alignment.CenterVertically,
            horizontalArrangement  = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "ॐ", style = MaterialTheme.typography.titleMedium, color = GoldFlame)
            Text(
                text         = "Gemma 4 · Wisdom Engine",
                style        = MaterialTheme.typography.bodySmall,
                fontWeight   = FontWeight.Medium,
                color        = GoldDusk,
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val versionText = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = packageInfo.versionName ?: "2.11.0"
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "v$name · Build $code"
        } catch (e: java.lang.Exception) {
            "v2.11.0 · Build 31"
        }
    }

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
                        NightInk,
                        DeepVoid,
                        MidnightVeil
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

            // Logo — soft circular edge (no square/white PNG corners)
            BrandLogoMark(
                size = 128.dp,
                scale = logoScale,
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Sanskrit header ornament
            Text(
                text          = "॥ श्रीमद्भगवद्गीता ॥",
                style         = MaterialTheme.typography.bodySmall,
                fontWeight    = FontWeight.Normal,
                color         = GoldAsh,
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // App title — Bhagavad Gita AI (from strings.xml)
            Text(
                text          = stringResource(id = R.string.app_name),
                style         = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color         = GoldFlame,
                textAlign     = TextAlign.Center
            )
            Text(
                text          = "Learn · Quiz · AI Insights",
                style         = MaterialTheme.typography.labelMedium,
                color         = GoldAsh,
                letterSpacing = 1.2.sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            GoldDivider(modifier = Modifier.fillMaxWidth(0.72f))

            Spacer(modifier = Modifier.height(20.dp))

            // Quote
            Text(
                text       = "\u201C$quote\u201D",
                style      = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 24.sp
                ),
                fontWeight = FontWeight.Normal,
                color      = MoonSilver.copy(alpha = 0.88f),
                textAlign  = TextAlign.Center,
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
                    text          = versionText,
                    fontSize      = 10.sp,
                    color         = StarDust,
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
                        NightInk,
                        DeepVoid,
                        MidnightVeil
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
            shape  = MaterialTheme.shapes.extraLarge,
            color  = NightInk.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(
                0.8.dp, GoldFlame.copy(alpha = 0.30f)
            )
        ) {
            Column(
                modifier               = Modifier.padding(32.dp),
                horizontalAlignment    = Alignment.CenterHorizontally,
                verticalArrangement    = Arrangement.spacedBy(0.dp)
            ) {
                BrandLogoMark(size = 76.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text          = "॥ धन्यवाद ॥",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = GoldAsh,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text       = "Thank You",
                    style      = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color      = GoldFlame
                )

                Spacer(modifier = Modifier.height(10.dp))

                GoldDivider(modifier = Modifier.fillMaxWidth(0.60f))

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text      = "The Gita travels with you.\nCome back whenever you seek wisdom.",
                    style     = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    color     = MoonSilver.copy(alpha = 0.80f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text      = "Are you sure you want to exit?",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = StarDust,
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
                        shape  = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp, GoldFlame.copy(alpha = 0.50f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GoldFlame
                        )
                    ) {
                        Text(
                            text       = "Stay",
                            style      = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    // Exit button
                    androidx.compose.material3.Button(
                        onClick  = onConfirmExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape  = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TilakRed,
                            contentColor   = GoldGlow
                        )
                    ) {
                        Text(
                            text          = "Exit",
                            style         = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}