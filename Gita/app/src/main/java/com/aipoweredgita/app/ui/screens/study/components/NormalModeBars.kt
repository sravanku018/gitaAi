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

private val PillShape = RoundedCornerShape(50)

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
    val barBg = if (isDark) Color(0xFF1A1A1A).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.96f)
    val barBorder = if (isDark) Color.White.copy(0.10f) else Color(0xFFFFE0B2).copy(0.8f)

    // Floating pill tray above the home indicator
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(barBg)
                .border(1.dp, barBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val secondaryBtnBg = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFFFF8F2)
                val secondaryBtnBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFFFE0B2)

                // Save — gradient pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(PillShape)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = buttonContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFavorite) "Saved" else "Save",
                            color = buttonContentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Share pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(PillShape)
                        .border(1.dp, secondaryBtnBorder, PillShape)
                        .background(secondaryBtnBg)
                        .clickable(onClick = onShare),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Share",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Battle pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(PillShape)
                        .border(1.dp, secondaryBtnBorder, PillShape)
                        .background(secondaryBtnBg)
                        .clickable(onClick = onBattleQuiz),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsMma,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Battle",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val msgColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
            AnimatedVisibility(
                visible = !favoriteMessage.isNullOrBlank(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Text(
                    text = favoriteMessage ?: "",
                    fontSize = 12.sp,
                    color = msgColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavArrowButton(
                    label = "Prev",
                    enabled = canGoPrev,
                    isForward = false,
                    modifier = Modifier.weight(1f),
                    onClick = onPrev
                )
                NavArrowButton(
                    label = "Next",
                    enabled = canGoNext,
                    isForward = true,
                    modifier = Modifier.weight(1f),
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
fun NavArrowButton(
    label: String,
    enabled: Boolean,
    isForward: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    val isDark = rememberThemeIsDark()
    val bg = if (isDark) Color.White.copy(alpha = 0.06f * alpha) else Color(0xFFFFF8F2).copy(alpha = alpha)
    val border = if (isDark) Color.White.copy(alpha = 0.14f * alpha) else Color(0xFFFFE0B2).copy(alpha = alpha)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(PillShape)
            .background(bg)
            .border(1.dp, border, PillShape)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!isForward) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        if (isForward) {
            Spacer(Modifier.width(5.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
