package com.aipoweredgita.app.ui.login

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.utils.AuthPreferences
import com.aipoweredgita.app.viewmodel.LoginViewModel
import androidx.hilt.navigation.compose.hiltViewModel

// Sacred color palette
private val DeepBrown = Color(0xFF1A0F00)
private val GoldPrimary = Color(0xFFD4A017)
private val GoldLight = Color(0xFFF5C842)
private val GoldDark = Color(0xFF8B4513)
private val GoldMuted = Color(0xFFC4922A)
private val GoldSubtle = Color(0xFF7A5A20)
private val GoldDim = Color(0xFF5A3E10)
private val GoldAccent = Color(0xFFA07840)

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200),
        label = "tab_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) GoldLight else GoldSubtle,
        animationSpec = tween(200),
        label = "tab_text"
    )

    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SacredInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GoldPrimary.copy(alpha = 0.07f))
            .border(0.5.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = GoldLight,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(GoldLight),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { /* Handle done */ }
                ),
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = GoldDim,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (isPassword) {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        modifier = Modifier.size(16.dp),
                        tint = GoldSubtle
                    )
                }
            }
        }
    }
}

@Composable
fun MandalaDecorative(modifier: Modifier = Modifier) {
    // Simplified mandala pattern
    Box(
        modifier = modifier.drawBehind {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f
            
            // Draw concentric circles
            for (i in 1..5) {
                val radius = maxRadius * i / 5f
                drawCircle(
                    color = GoldPrimary,
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
                )
            }
            
            // Draw radial lines
            for (angle in 0 until 360 step 30) {
                val radians = Math.toRadians(angle.toDouble())
                val endX = center.x + (maxRadius * Math.cos(radians)).toFloat()
                val endY = center.y + (maxRadius * Math.sin(radians)).toFloat()
                drawLine(
                    color = GoldPrimary,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
        }
    )
}

@Composable
fun LampIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "lamp")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Flame
        Box(
            modifier = Modifier
                .size(10.dp, 16.dp)
                .scale(scale)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    Brush.radialGradient(
                        colors = listOf(GoldLight, Color(0xFFFF8C00), Color.Transparent),
                        center = Offset(0.5f, 0.9f),
                        radius = 50f
                    ),
                    shape = RoundedCornerShape(50, 50, 40, 40)
                )
        )
        // Wick
        Box(
            modifier = Modifier
                .size(1.5.dp, 6.dp)
                .background(Color(0xFF888888))
        )
        // Lamp body
        Box(
            modifier = Modifier
                .size(22.dp, 10.dp)
                .background(
                    Brush.verticalGradient(listOf(GoldPrimary, GoldDark)),
                    shape = RoundedCornerShape(0, 0, 12, 12)
                )
        )
    }
}

@Composable
fun LotusIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "lotus")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lotus_float"
    )

    Box(
        modifier = Modifier
            .size(48.dp, 42.dp)
            .offset(y = offsetY.dp),
        contentAlignment = Alignment.Center
    ) {
        // Simplified lotus using Canvas or just a symbol
        Text(
            text = "🪷",
            fontSize = 32.sp
        )
    }
}
