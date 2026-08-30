package com.aipoweredgita.app.ui.screens.settings

import com.aipoweredgita.app.ui.LocalUiConfig
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.aipoweredgita.app.utils.ThemePreferences
import androidx.compose.ui.platform.LocalContext
import com.aipoweredgita.app.utils.DeviceUtils
import com.aipoweredgita.app.utils.DeviceConfigCategory
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.GlassCard
import androidx.compose.ui.graphics.luminance

import com.aipoweredgita.app.ui.theme.*

@Composable
fun WidgetSettingsSection(context: android.content.Context) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    
    var widgetCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, com.aipoweredgita.app.widget.GitaVerseWidget::class.java)
        )
        widgetCount = widgetIds.size
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 32.dp,
        elevation = 4.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Daily Verse Widget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (widgetCount > 0) "✓ Widget Active ($widgetCount)" else "No Widget Added",
                        color = if (widgetCount > 0) gold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "Shows a new inspirational verse each day on your home screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (widgetCount > 0) {
                    IconButton(onClick = {
                        val intent = android.content.Intent(context, com.aipoweredgita.app.widget.GitaVerseWidget::class.java).apply {
                            action = "com.aipoweredgita.app.widget.ACTION_WIDGET_REFRESH"
                        }
                        context.sendBroadcast(intent)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = gold)
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareSpecsCard(context: android.content.Context) {
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Device Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            SpecRow("Model", DeviceUtils.getModelName())
            SpecRow("RAM", DeviceUtils.getFormattedRAM(context))
            SpecRow("OS", DeviceUtils.getAndroidVersion())
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textPrimary.copy(alpha = 0.9f))
    }
}

@Composable
fun ModelRecommendationCard(context: android.content.Context) {
    val tier = com.aipoweredgita.app.utils.DeviceTierDetector.detect(context)
    val recommendedModel = when(tier) {
        com.aipoweredgita.app.utils.DeviceTier.FLAGSHIP -> "Gemma 4 2B (Advanced Insight)"
        else -> "Qwen3 0.6B (Fast Wisdom)"
    }
    
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        border = gold.copy(alpha = 0.25f),
        tint = gold.copy(alpha = if (isDark) 0.05f else 0.03f),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recommended for You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Based on your device hardware, we suggest using:", style = MaterialTheme.typography.bodySmall, color = textSecondary)
            Text(recommendedModel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = gold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("You can still manually select other models in the Download section if you have a stable connection.", style = MaterialTheme.typography.bodySmall, color = textSecondary.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun AboutSectionCard(context: android.content.Context) {
    val versionText = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = packageInfo.versionName ?: "2.19.1"
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "Version: v$name - Build $code"
        } catch (e: Exception) {
            "Version: v2.19.1 - Build 52"
        }
    }

    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("About AI-Powered Gita", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary.copy(alpha = 0.9f))
            }
            OrnamentRule()
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(versionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textPrimary.copy(alpha = 0.9f))
                Text(
                    "AI-Powered Gita is a modern, premium spiritual companion that brings the eternal wisdom of the Bhagavad Gita to life through on-device AI and a serene user experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
            
            HorizontalDivider(color = cardBorder)
            
            Text(
                "(c) 2026 Bhagavad Gita Dev Team",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

