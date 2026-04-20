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
    primary = Saffron,
    secondary = GoldSpark,
    tertiary = GoldBright,
    background = BgDark,
    surface = Surface1,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim
)

private val LightColorScheme = lightColorScheme(
    primary = Saffron,
    secondary = GoldSpark,
    tertiary = GoldPale,
    background = Color(0xFFFFFBF0),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun GitaLearningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    accentName: String? = "Sacred",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Determine the base color scheme
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply custom accent if it's not "Sacred" (default) or if dynamic color is off
    val scheme = if (accentName != null && accentName != "Sacred" && (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
        when (accentName) {
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
    } else {
        base
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
