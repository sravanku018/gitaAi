package com.aipoweredgita.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.aipoweredgita.app.utils.ThemePreferences
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DeepSaffron,
    secondary = DarkOrange,
    tertiary = DarkGold,
    background = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
    surface = androidx.compose.ui.graphics.Color(0xFF2A2A2A),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF3A3A3A),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE8D6)
)

private val LightColorScheme = lightColorScheme(
    primary = Saffron,
    secondary = OrangeLight,
    tertiary = GoldLight,
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE8D6),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF4A2C2A)
)

@Composable
fun GitaLearningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentName: String? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Base scheme: system dynamic or app palettes
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Accent override: map preferred accent to primary/secondary/tertiary
    val scheme = when (accentName) {
        "Lotus" -> base.copy(
            primary = LotusPrimary,
            secondary = LotusSecondary,
            tertiary = LotusTertiary,
            onPrimary = Color.White,
            onSecondary = Color.White
        )
        "Ocean" -> base.copy(
            primary = OceanPrimary,
            secondary = OceanSecondary,
            tertiary = OceanTertiary,
            onPrimary = Color(0xFF002022),
            onSecondary = Color(0xFF00221A)
        )
        else -> base
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
