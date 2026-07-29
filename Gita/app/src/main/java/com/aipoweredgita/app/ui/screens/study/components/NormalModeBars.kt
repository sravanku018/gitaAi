package com.aipoweredgita.app.ui.screens.study.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.CrimsonDeep
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.ui.theme.rememberThemeIsDark

@Composable
fun OfflineBanner(onReadOfflineClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrimsonDeep)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text       = "No internet connection",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = Color(0xFFFFCDD2)
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = onReadOfflineClick,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(0.12f))
        ) {
            Text(
                text  = "Read offline →",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BottomActionBar(
    isFavorite      : Boolean,
    favoriteMessage : String?,
    canGoPrev       : Boolean,
    canGoNext       : Boolean,
    onFavoriteToggle: () -> Unit,
    onShare         : () -> Unit,
    onBattleQuiz    : () -> Unit,
    onPrev          : () -> Unit,
    onNext          : () -> Unit
) {
    val isDark = rememberThemeIsDark()
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isFavorite)
                                Brush.horizontalGradient(listOf(CrimsonDeep, Color(0xFFC62828)))
                            else
                                Brush.horizontalGradient(listOf(GoldSpark, Saffron))
                        )
                        .clickable(onClick = onFavoriteToggle),
                    contentAlignment = Alignment.Center
                ) {
                    val buttonContentColor = if (isFavorite) Color.White else Color.Black
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector        = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint               = buttonContentColor,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text       = if (isFavorite) "Saved" else "Save",
                            color      = buttonContentColor,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onShare),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector        = Icons.Filled.Share,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurface,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text       = "Share",
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onBattleQuiz),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector        = Icons.Filled.SportsMma,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurface,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text       = "Battle",
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val msgColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
            AnimatedVisibility(
                visible = !favoriteMessage.isNullOrBlank(),
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut()
            ) {
                Text(
                    text      = favoriteMessage ?: "",
                    fontSize  = 12.sp,
                    color     = msgColor,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(top = 6.dp),
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NavArrowButton(
                    label     = "Previous",
                    enabled   = canGoPrev,
                    isForward = false,
                    modifier  = Modifier.weight(1f),
                    onClick   = onPrev
                )
                NavArrowButton(
                    label     = "Next",
                    enabled   = canGoNext,
                    isForward = true,
                    modifier  = Modifier.weight(1f),
                    onClick   = onNext
                )
            }
        }
    }
}

@Composable
fun NavArrowButton(
    label    : String,
    enabled  : Boolean,
    isForward: Boolean,
    modifier : Modifier = Modifier,
    onClick  : () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f * alpha))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f * alpha), MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!isForward) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier           = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        if (isForward) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}
