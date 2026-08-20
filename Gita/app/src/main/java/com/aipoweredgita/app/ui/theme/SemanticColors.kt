package com.aipoweredgita.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Semantic UI tokens derived from [MaterialTheme.colorScheme].
 *
 * Prefer these over raw hex / ad-hoc `if (isDark)` branches so Lotus/Ocean
 * accents (and future themes) actually paint the screens.
 */
@Immutable
data class GitaSemanticColors(
    val isDark: Boolean,
    /** Brand accent — follows accent theme (Sacred / Lotus / Ocean). */
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDim: Color,
    val surface: Color,
    val cardTint: Color,
    val cardBorder: Color,
    val heroTint: Color,
    val heroBorder: Color,
    val heroTitle: Color,
    val heroSubtitle: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val pillBg: Color,
    val pillText: Color,
    val subtleBg: Color,
    val subtleBorder: Color,
    val divider: Color,
    val chipBg: Color,
    val chipBorder: Color,
    val chipText: Color,
    val buttonPrimary: Color,
    val buttonOutline: Color,
    val buttonOutlineText: Color,
)

@Composable
fun rememberGitaColors(): GitaSemanticColors {
    val scheme = MaterialTheme.colorScheme
    val isDark = remember(scheme.background) { scheme.background.luminance() < 0.5f }
    return remember(scheme, isDark) {
        val accent = scheme.primary
        val accentSoft = scheme.secondary
        GitaSemanticColors(
            isDark = isDark,
            accent = accent,
            accentSoft = accentSoft,
            onAccent = scheme.onPrimary,
            textPrimary = scheme.onBackground,
            textSecondary = scheme.onSurfaceVariant,
            textDim = scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.55f else 0.5f),
            surface = scheme.surface,
            cardTint = if (isDark) {
                Color.White.copy(alpha = 0.03f)
            } else {
                accent.copy(alpha = 0.05f)
            },
            cardBorder = scheme.onSurface.copy(alpha = if (isDark) 0.12f else 0.1f),
            heroTint = if (isDark) accent.copy(alpha = 0.22f) else scheme.surface,
            heroBorder = if (isDark) accent.copy(alpha = 0.28f) else accentSoft.copy(alpha = 0.35f),
            heroTitle = if (isDark) Color.White.copy(alpha = 0.97f) else accent,
            heroSubtitle = if (isDark) accentSoft.copy(alpha = 0.75f) else scheme.onSurfaceVariant,
            heroGradientStart = if (isDark) accent.copy(alpha = 0.18f) else accent.copy(alpha = 0.08f),
            heroGradientEnd = if (isDark) accent.copy(alpha = 0.08f) else accentSoft.copy(alpha = 0.04f),
            pillBg = if (isDark) accent.copy(alpha = 0.25f) else accent.copy(alpha = 0.12f),
            pillText = if (isDark) accentSoft.copy(alpha = 0.9f) else accent,
            subtleBg = if (isDark) Color.White.copy(alpha = 0.1f) else accent.copy(alpha = 0.08f),
            subtleBorder = if (isDark) Color.White.copy(alpha = 0.15f) else accentSoft.copy(alpha = 0.4f),
            divider = if (isDark) Color.White.copy(alpha = 0.1f) else accentSoft.copy(alpha = 0.25f),
            chipBg = accent.copy(alpha = if (isDark) 0.15f else 0.1f),
            chipBorder = accent.copy(alpha = if (isDark) 0.25f else 0.2f),
            chipText = if (isDark) accentSoft else accent,
            buttonPrimary = if (isDark) Color.White.copy(alpha = 0.15f) else accent,
            buttonOutline = if (isDark) accentSoft.copy(alpha = 0.2f) else accent.copy(alpha = 0.5f),
            buttonOutlineText = if (isDark) accentSoft.copy(alpha = 0.7f) else accent,
        )
    }
}
