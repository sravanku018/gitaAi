package com.aipoweredgita.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.widget.GitaVerseWidget
import com.aipoweredgita.app.ui.components.GradientCardContainer
import com.aipoweredgita.app.ui.theme.UiDefaults
import com.aipoweredgita.app.ui.LocalUiConfig

@Composable
fun WidgetSettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var widgetCount by remember { mutableStateOf(0) }
    val uiCfg = LocalUiConfig.current

    LaunchedEffect(Unit) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, GitaVerseWidget::class.java)
        )
        widgetCount = widgetIds.size
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Widget Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Daily Verse Widget",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Widget Preview
        GradientCardContainer(
            brush = Brush.linearGradient(colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            cornerRadius = UiDefaults.CornerRadius,
            elevation = UiDefaults.ElevationEmphasis,
            contentPadding = 16.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Gita Verse",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2:47",
                        fontSize = 12.sp,
                        color = Color(0xFFE0E0E0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Do your duty without attachment to results",
                        fontSize = 12.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "↻",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Widget Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (widgetCount > 0)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (widgetCount > 0) "✓" else "○",
                    fontSize = 24.sp,
                    color = if (widgetCount > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (widgetCount > 0) "Widget Active" else "No Widget Added",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (widgetCount > 0) "$widgetCount widget(s) on home screen" else "Add widget from home screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How to Add Widget",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                InstructionStep(number = "1", text = "Long press on your home screen")
                InstructionStep(number = "2", text = "Tap \"Widgets\" button")
                InstructionStep(number = "3", text = "Find \"Bhagavad Gita\" widget")
                InstructionStep(number = "4", text = "Drag it to your home screen")
                InstructionStep(number = "5", text = "Widget updates daily at midnight!")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Refresh Widget Button
        if (widgetCount > 0) {
            Button(
                onClick = {
                    val intent = Intent(context, GitaVerseWidget::class.java).apply {
                        action = GitaVerseWidget.ACTION_WIDGET_REFRESH
                    }
                    context.sendBroadcast(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Widget Now")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ℹ️ Widget Info",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Shows a new inspirational verse each day\n" +
                            "• Rotates through 18 meaningful verses\n" +
                            "• Requires offline verses to be downloaded\n" +
                            "• Tap title to open app\n" +
                            "• Tap ↻ to refresh manually",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
