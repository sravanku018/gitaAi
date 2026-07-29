package com.aipoweredgita.app.ui.screens.quiz.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron

fun translateLandingText(text: String, language: String): String {
    if (language != "tel") return text
    return when (text) {
        "Quiz Section" -> "క్విజ్ విభాగం"
        "15 Questions" -> "15 ప్రశ్నలు"
        "25 Questions" -> "25 ప్రశ్నలు"
        "15 Question Marathon" -> "15 ప్రశ్నల మహోత్సవం"
        "25 Question Challenge" -> "25 ప్రశ్నల సవాలు"
        "A quick spiritual check-in to test your knowledge of the Bhagavad Gita's fundamental truths." -> "భగవద్గీత యొక్క ప్రాథమిక సత్యాలపై మీ జ్ఞానాన్ని పరీక్షించడానికి ఒక చిన్న ఆధ్యాత్మిక విశ్లేషణ."
        "An in-depth journey through the sacred verses. Ready to test your mastery of divine wisdom?" -> "పవిత్ర శ్లోకాల ద్వారా ఒక లోతైన ప్రయాణం. దైవిక జ్ఞానంపై మీ నైపుణ్యాన్ని పరీక్షించడానికి సిద్ధంగా ఉన్నారా?"
        "Start Quiz" -> "క్విజ్ ప్రారంభించండి"
        "Every question is a step closer to self-realization." -> "ప్రతి ప్రశ్నా మిమ్మల్ని ఆత్మసాక్షాత్కారానికి ఒక అడుగు దగ్గరగా తీసుకెళ్తుంది."
        else -> text
    }
}

@Composable
fun QuizStartLanding(
    title: String,
    description: String,
    onStart: () -> Unit,
    language: String = "tel"
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Color(0xFFD84315)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFFFF3E0), CircleShape)
                .border(2.dp, Brush.linearGradient(listOf(gold, Saffron)), CircleShape)
                .shadow(6.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                tint = gold
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = translateLandingText(title, language),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = gold
            )
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = translateLandingText(description, language),
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = textSecondary
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Saffron, gold)
                    ),
                    shape = MaterialTheme.shapes.large
                ),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            shape = MaterialTheme.shapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text(
                    translateLandingText("Start Quiz", language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = translateLandingText("Every question is a step closer to self-realization.", language),
            style = MaterialTheme.typography.labelSmall.copy(
                fontStyle = FontStyle.Italic,
                color = textSecondary.copy(alpha = 0.7f)
            )
        )
    }
}
