package com.aipoweredgita.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.theme.*
// ── Ornamental header ───────────────────────────────────────────────────────
@Composable
fun OrnamentalHeader(language: String = "tel") {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Krishna & Devotee Imagery
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(GoldSpark, Saffron)), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.krishna_icon),
                    contentDescription = "Krishna",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(-20.dp)) // Overlap effect
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, GoldSpark.copy(0.5f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.devotee),
                    contentDescription = "Devotee",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Om glyph
        val shimmer by rememberInfiniteTransition(label = "om").animateFloat(
            0.5f, 1f,
            infiniteRepeatable(tween(2500), RepeatMode.Reverse),
            label = "om_glow"
        )
        Text(
            text  = "ॐ",
            fontSize = 36.sp,
            color = GoldSpark.copy(alpha = shimmer),
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.height(6.dp))
        // Title
        Text(
            text  = translateConfigText("Gita Quiz", language),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = translateConfigText("Test your knowledge of the sacred scripture", language),
            fontSize  = 13.sp,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        // Gold rule
        OrnamentRule()
    }
}

// ── Question count selector ─────────────────────────────────────────────────
@Composable
fun QuestionCountRow(selected: Int, onSelect: (Int) -> Unit, language: String = "tel") {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(15 to "Sprint", 25 to "Deep Dive").forEach { (count, label) ->
            val isActive = selected == count
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        if (isActive)
                            Brush.linearGradient(listOf(Surface2, Surface2))
                        else
                            Brush.linearGradient(listOf(Surface1, Surface1))
                    )
                    .border(
                        width = if (isActive) 1.5.dp else 0.5.dp,
                        brush = if (isActive)
                            Brush.linearGradient(listOf(GoldSpark, Saffron))
                        else
                            Brush.linearGradient(listOf(GoldSpark.copy(0.2f), GoldSpark.copy(0.2f))),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .shadow(if (isActive) 6.dp else 0.dp, MaterialTheme.shapes.extraLarge)
                    .clickable { onSelect(count) }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Big number with animated color
                    val numColor = if (isActive) GoldBright else MaterialTheme.colorScheme.onSurface.copy(0.35f)
                    Text(
                        text       = "$count",
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color      = numColor,
                        lineHeight = 44.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = translateConfigText(label, language),
                        fontSize = 11.sp,
                        color    = if (isActive) GoldPale.copy(0.8f) else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isActive) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Saffron)
                        )
                    }
                }
            }
        }
    }
}

// ── Language badge card ─────────────────────────────────────────────────────
@Composable
fun LanguageBadgeCard(language: String = "tel") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, GoldSpark.copy(0.25f), MaterialTheme.shapes.extraLarge)
            .shadow(4.dp, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text      = translateConfigText("LANGUAGE", language),
                fontSize  = 10.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(0.45f),
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "తెలుగు  ·  Telugu",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
        // Flag badge
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(Forest.copy(0.5f))
                .border(0.5.dp, ForestMid.copy(0.5f), MaterialTheme.shapes.small)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("🇮🇳", fontSize = 22.sp)
        }
    }
}

// ── Feature highlights card ─────────────────────────────────────────────────
@Composable
fun FeatureHighlightsCard(language: String = "tel") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, GoldSpark.copy(0.2f), MaterialTheme.shapes.extraLarge)
            .shadow(3.dp, MaterialTheme.shapes.extraLarge)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text      = translateConfigText("WHAT TO EXPECT", language),
            fontSize  = 10.sp,
            color     = GoldSpark.copy(0.8f),
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Medium
        )
        listOf(
            "✦" to "Context-aware Gita questions",
            "✦" to "Intelligent difficulty scaling",
            "✦" to "Telugu language support",
            "✦" to "100% offline & private"
        ).forEach { (icon, text) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(icon, fontSize = 10.sp, color = GoldSpark)
                Text(
                    text     = translateConfigText(text, language),
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(0.85f)
                )
            }
        }
    }
}

// ── Section label ───────────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, language: String = "tel") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(
                    Brush.verticalGradient(listOf(GoldSpark, Saffron)),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = translateConfigText(text, language).uppercase(),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground.copy(0.6f),
            letterSpacing = 1.5.sp
        )
    }
}

// ── Gold ornament rule ──────────────────────────────────────────────────────
@Composable
fun OrnamentRule() {
    Row(
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, GoldSpark.copy(0.45f)))
                )
        )
        Spacer(Modifier.width(10.dp))
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == 1) 5.dp else 3.dp)
                    .clip(CircleShape)
                    .background(GoldSpark.copy(if (i == 1) 0.8f else 0.4f))
            )
            if (i < 2) Spacer(Modifier.width(5.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(GoldSpark.copy(0.45f), Color.Transparent))
                )
        )
    }
}

