package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.theme.GoldSpark

@Composable
fun QuizLanguageDialog(
    onLanguageSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "Select Quiz Language",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = GoldSpark
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Choose the language for quiz questions and options:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // English Option
                LanguageOptionCard(
                    flag = "🇺🇸",
                    language = "English",
                    description = "Quiz in English",
                    onClick = { onLanguageSelected("en") }
                )

                // Telugu Option
                LanguageOptionCard(
                    flag = "🇮🇳",
                    language = "Telugu",
                    description = "క్విజ్ తెలుగులో",
                    onClick = { onLanguageSelected("tel") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color(0xFFE57373))
            }
        },
        containerColor = Color(0xFF140F0A),
        shape = MaterialTheme.shapes.large
    )
}

@Composable
fun LanguageOptionCard(
    flag: String,
    language: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 12.dp,
            tint = Color.White.copy(alpha = 0.05f),
            border = Color.White.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = flag,
                    fontSize = 28.sp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = language,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Select",
                    tint = GoldSpark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
