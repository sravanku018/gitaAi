package com.aipoweredgita.app.ui.screens.voice.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.screens.voice.VoiceStudioColors

@Composable
fun BalanceLoadingOverlay(colors: VoiceStudioColors) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ॐ",
            fontSize = 48.sp,
            color = colors.RevolvingYellow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = colors.RevolvingYellow,
            strokeWidth = 3.dp,
            trackColor = colors.RevolvingYellow.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preparing your spiritual connection...",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = colors.TextPrimary,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please wait while we verify your balance.",
            style = MaterialTheme.typography.bodySmall.copy(color = colors.TextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InsufficientCoinsOverlay(
    coinBalance: Int,
    onExit: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToRead: () -> Unit,
    colors: VoiceStudioColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ॐ",
            fontSize = 48.sp,
            color = colors.RevolvingYellow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Insufficient Divine Energy",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.TextPrimary,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        // FIX 3: Show exact balance; handles 0 and negative correctly
        Text(
            text = "Sacred conversations require spiritual energy. You have $coinBalance Krishna Coin${if (coinBalance == 1) "" else "s"} remaining.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.TextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (colors.IsDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
            ),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.Border.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Perform spiritual acts to earn coins:",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.TextMuted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Button(
                    onClick = onNavigateToQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.RevolvingYellow,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take a Quiz (+5 to +15 Coins)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onNavigateToRead,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.RevolvingYellow),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(colors.RevolvingYellow.copy(alpha = 0.4f), colors.RevolvingYellow.copy(alpha = 0.4f))
                        )
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.RevolvingYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Read Gita Verses (+Coins)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onExit, modifier = Modifier.height(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.TextMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Back to Dashboard", color = colors.TextMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}
