package com.aipoweredgita.app.ui

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
fun LoginScreen(
    onLoginSuccess: (userId: String) -> Unit,
    onGuestLogin: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authPrefs = remember { AuthPreferences.getInstance(context) }
    val authManager = remember { com.aipoweredgita.app.repository.AuthManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // Load saved credentials
    val (savedPhone, savedEmail, savedPassword) = remember { authPrefs.getSavedCredentials() }
    
    var selectedTab by remember { mutableStateOf(authPrefs.loginMethod ?: "email") }
    var userId by remember { mutableStateOf(savedEmail ?: "") }
    var email by remember { mutableStateOf(savedEmail ?: "") }
    var password by remember { mutableStateOf(savedPassword ?: "") }
    var name by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rememberMe by remember { mutableStateOf(authPrefs.rememberMe) }
    
    val focusManager = LocalFocusManager.current
    
    val formatAuthError: (Throwable, Boolean) -> String = { error, isRegister ->
        val msg = error.message ?: ""
        when {
            msg.contains("500") -> {
                if (isRegister) "Server Error (500): The database server is currently experiencing issues. Please try again later or use Guest mode."
                else "Server Error (500): The login server is currently experiencing issues. Please try again later or use Guest mode."
            }
            msg.contains("404") -> {
                if (isRegister) "Server Error (404): The registration service was not found on the server."
                else "Login Failed (404): Incorrect User ID/Email or password. Please check your credentials or register."
            }
            msg.contains("400") -> "Invalid Input (400): Please check the format of your fields."
            msg.contains("timeout") || msg.contains("ConnectException") || msg.contains("UnknownHostException") -> 
                "Network Error: Unable to connect to the server. Please check your internet connection."
            else -> if (isRegister) "Registration failed: ${error.localizedMessage}" else "Login failed: ${error.localizedMessage}"
        }
    }
    
    // Auth handlers
    val handleLogin = {
        if (userId.isBlank() || password.isBlank()) {
            errorMessage = "User ID and password required"
        } else {
            isLoading = true
            errorMessage = null
            scope.launch {
                val result = authManager.login(userId, password)
                result.fold(
                    onSuccess = { authResult ->
                        if (rememberMe) {
                            authPrefs.email = userId
                            authPrefs.loginMethod = "email"
                            authPrefs.rememberMe = true
                        }
                        onLoginSuccess(authResult.userId)
                    },
                    onFailure = { error ->
                        errorMessage = formatAuthError(error, false)
                    }
                )
                isLoading = false
            }
        }
    }
    
    val handleRegister = {
        if (userId.isBlank() || password.isBlank()) {
            errorMessage = "User ID and password required"
        } else {
            isLoading = true
            errorMessage = null
            scope.launch {
                val registerEmail = if (userId.contains("@")) userId else email.ifEmpty { "${userId}@gita.com" }
                val result = authManager.register(userId, password, name, registerEmail)
                result.fold(
                    onSuccess = { authResult ->
                        if (rememberMe) {
                            authPrefs.email = userId
                            authPrefs.loginMethod = "email"
                            authPrefs.rememberMe = true
                        }
                        onLoginSuccess(authResult.userId)
                    },
                    onFailure = { error ->
                        errorMessage = formatAuthError(error, true)
                    }
                )
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBrown)
    ) {
        // Back button
        Text(
            text = "✕",
            fontSize = 22.sp,
            color = GoldPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .clickable(onClick = onBack)
                .size(40.dp)
                .wrapContentSize(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top golden band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GoldDark, GoldPrimary, GoldLight, GoldPrimary, GoldDark)
                        )
                    )
            )

            // Mandala background (subtle)
            MandalaDecorative(
                modifier = Modifier
                    .size(340.dp)
                    .offset(y = (-60).dp)
                    .alpha(0.07f)
            )

            // Lamp row
            Row(
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                LampIcon()
                LotusIcon()
                LampIcon()
            }

            // App title
            Text(
                text = "AI Powered Gita",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )

            // Sanskrit subtitle
            Text(
                text = "॥ ज्ञान · भक्ति · मोक्ष ॥",
                fontSize = 10.sp,
                color = GoldMuted,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, GoldPrimary, Color.Transparent)
                        )
                    )
                    .padding(vertical = 14.dp)
            )

            // Ornament
            Text(
                text = "✦ ✦ ✦",
                fontSize = 18.sp,
                color = GoldPrimary,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )

            // Shloka
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"योगस्थः कुरु कर्माणि\"",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Perform your actions, established in yoga",
                    fontSize = 9.sp,
                    color = GoldAccent,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }

            // Login card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.04f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(GoldPrimary.copy(alpha = 0.3f), GoldPrimary.copy(alpha = 0.3f))
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp, 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Card title
                    Text(
                        text = "— BEGIN YOUR JOURNEY —",
                        fontSize = 10.sp,
                        color = GoldMuted,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )

                    // Mode toggle (Login / Register)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(0.5.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        TabButton(
                            text = "✦ Login",
                            isSelected = !isRegisterMode,
                            onClick = { isRegisterMode = false },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "✦ Register",
                            isSelected = isRegisterMode,
                            onClick = { isRegisterMode = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Error message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Name field (register only)
                    if (isRegisterMode) {
                        SacredInput(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Your Name",
                            keyboardType = KeyboardType.Text,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoldSubtle
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // User ID / Email field
                    SacredInput(
                        value = userId,
                        onValueChange = { userId = it },
                        placeholder = "User ID or Email",
                        keyboardType = KeyboardType.Email,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoldSubtle
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password field
                    SacredInput(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoldSubtle
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit button
                    Button(
                        onClick = { if (isRegisterMode) handleRegister() else handleLogin() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = DeepBrown
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DeepBrown,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "✦ Create Account ✦" else "✦ Enter the Gita ✦",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Remember me checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { 
                                rememberMe = it
                                authPrefs.rememberMe = it
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GoldPrimary,
                                uncheckedColor = GoldSubtle,
                                checkmarkColor = DeepBrown
                            )
                        )
                        Text(
                            text = "Remember me",
                            fontSize = 12.sp,
                            color = GoldSubtle,
                            modifier = Modifier.clickable { 
                                rememberMe = !rememberMe
                                authPrefs.rememberMe = rememberMe
                            }
                        )
                    }

                    // OR divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .background(GoldPrimary.copy(alpha = 0.2f))
                        )
                        Text(
                            text = "OR",
                            fontSize = 10.sp,
                            color = GoldSubtle,
                            letterSpacing = 2.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .background(GoldPrimary.copy(alpha = 0.2f))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Guest button
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                // Guest login is instant and performed locally without server round-trip latency
                                val guestId = "guest_${java.util.UUID.randomUUID()}"
                                authPrefs.saveGuestState(guestId)

                                // Update Room DB with guest user info so user stats and viewmodels work correctly
                                try {
                                    val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                                    db.userStatsDao().updateUserId(guestId)
                                    db.userStatsDao().updateProfile(name = "Guest User", dob = "")
                                } catch (e: Exception) {
                                    android.util.Log.e("LoginScreen", "Failed to update Room database for guest", e)
                                }

                                // Award 50 coin welcome bonus to new guests (once only)
                                if (!authPrefs.guestWelcomeAwarded) {
                                    authPrefs.localCoins = 50
                                    authPrefs.guestWelcomeAwarded = true
                                    com.aipoweredgita.app.coin.CoinTransactionLogger.log(context, 50, "Welcome bonus (guest)")
                                }
                                isLoading = false
                                onGuestLogin()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GoldAccent
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(GoldPrimary.copy(alpha = 0.35f), GoldPrimary.copy(alpha = 0.35f))
                            )
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter as Guest Seeker",
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Footer
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "By continuing, you accept our",
                    fontSize = 10.sp,
                    color = GoldDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Terms of Dharma",
                        fontSize = 10.sp,
                        color = GoldAccent,
                        modifier = Modifier.clickable { }
                    )
                    Text(
                        text = "·",
                        fontSize = 10.sp,
                        color = GoldDim
                    )
                    Text(
                        text = "Privacy Vow",
                        fontSize = 10.sp,
                        color = GoldAccent,
                        modifier = Modifier.clickable { }
                    )
                }
            }

            // Bottom band
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, GoldPrimary, Color.Transparent)
                        )
                    )
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun TabButton(
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
private fun SacredInput(
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
private fun MandalaDecorative(modifier: Modifier = Modifier) {
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
private fun LampIcon() {
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
private fun LotusIcon() {
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
