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

// Sacred gold accents (shared); surfaces come from LoginPalette for light/dark
private val GoldPrimary = Color(0xFFD4A017)
private val GoldLight = Color(0xFFF5C842)
private val GoldDark = Color(0xFF8B4513)

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
    val loginViewModel: LoginViewModel = hiltViewModel()
    val palette = rememberLoginPalette()
    
    // Load saved credentials
    val (savedPhone, savedEmail, savedPassword) = remember { authPrefs.getSavedCredentials() }
    
    var selectedTab by remember { mutableStateOf(authPrefs.loginMethod ?: "email") }
    var userId by remember { mutableStateOf(savedEmail ?: "") }
    var email by remember { mutableStateOf(savedEmail ?: "") }
    var password by remember { mutableStateOf("") }
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
                        loginViewModel.handleLoginSuccess(authResult.userId)
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
                        loginViewModel.handleLoginSuccess(authResult.userId)
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
            .background(palette.background)
    ) {
        // Back button
        Text(
            text = "✕",
            fontSize = 22.sp,
            color = palette.gold,
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
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GoldDark, palette.gold, GoldLight, palette.gold, GoldDark)
                        )
                    )
            )

            // Mandala background (subtle)
            MandalaDecorative(
                modifier = Modifier
                    .size(180.dp)
                    .offset(y = (-20).dp)
                    .alpha(if (palette.isDark) 0.07f else 0.12f)
            )

            // Lamp row
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                LampIcon()
                LotusIcon()
                LampIcon()
            }

            // App title
            Text(
                text = "Śrīmad Bhagavad Gītā",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.title,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            // Sanskrit subtitle
            Text(
                text = "॥ ज्ञान · भक्ति · मोक्ष ॥",
                fontSize = 10.sp,
                color = palette.muted,
                letterSpacing = 2.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Shloka
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"योगस्थः कुरु कर्माणि\"",
                    fontSize = 11.sp,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 16.sp
                )
                Text(
                    text = "Perform your actions, established in yoga",
                    fontSize = 9.sp,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }

            // Login card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = palette.cardBg
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(palette.cardBorder, palette.cardBorder)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp, 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Card title
                    Text(
                        text = "— BEGIN YOUR JOURNEY —",
                        fontSize = 10.sp,
                        color = palette.muted,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Mode toggle (Login / Register)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.toggleBg)
                            .border(0.5.dp, palette.gold.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        TabButton(
                            text = "✦ Login",
                            isSelected = !isRegisterMode,
                            onClick = { isRegisterMode = false },
                            modifier = Modifier.weight(1f),
                            palette = palette,
                        )
                        TabButton(
                            text = "✦ Register",
                            isSelected = isRegisterMode,
                            onClick = { isRegisterMode = true },
                            modifier = Modifier.weight(1f),
                            palette = palette,
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Error message
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
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
                            palette = palette,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = palette.iconTint
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
                        palette = palette,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = palette.iconTint
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
                        palette = palette,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = palette.iconTint
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
                            containerColor = palette.gold,
                            contentColor = palette.onGold
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = palette.onGold,
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
                                checkedColor = palette.gold,
                                uncheckedColor = palette.subtle,
                                checkmarkColor = palette.onGold
                            )
                        )
                        Text(
                            text = "Remember me",
                            fontSize = 12.sp,
                            color = palette.subtle,
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
                                .background(palette.gold.copy(alpha = 0.2f))
                        )
                        Text(
                            text = "OR",
                            fontSize = 10.sp,
                            color = palette.subtle,
                            letterSpacing = 2.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .background(palette.gold.copy(alpha = 0.2f))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Guest button
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            loginViewModel.handleGuestLogin()
                            isLoading = false
                            onGuestLogin()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.accent
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(palette.gold.copy(alpha = 0.35f), palette.gold.copy(alpha = 0.35f))
                            )
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = palette.accent
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
                    color = palette.dim,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Terms of Dharma",
                        fontSize = 10.sp,
                        color = palette.accent,
                        modifier = Modifier.clickable { }
                    )
                    Text(
                        text = "·",
                        fontSize = 10.sp,
                        color = palette.dim
                    )
                    Text(
                        text = "Privacy Vow",
                        fontSize = 10.sp,
                        color = palette.accent,
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
                            colors = listOf(Color.Transparent, palette.gold, Color.Transparent)
                        )
                    )
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

