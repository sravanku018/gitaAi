package com.aipoweredgita.app.ui.screens.profile.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.network.YogaSubStage
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.components.YogaLevelManager
import androidx.compose.ui.tooling.preview.Preview
import com.aipoweredgita.app.ui.theme.GitaLearningTheme
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.ui.theme.rememberThemeIsDark
import kotlin.math.cos
import kotlin.math.sin

data class ProfileInfo(val name: String, val age: Int)
data class SmallStatData(val label: String, val value: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun ProfileHeader(
    profile: ProfileInfo,
    yogaInfo: YogaLevelManager.YogaLevelInfo,
    levelProgress: Float,
    totalCoins: Int
) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawArc(
                    color = gold.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawArc(
                    color = gold,
                    startAngle = -90f,
                    sweepAngle = 360f * levelProgress,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            Text(text = yogaInfo.emoji, fontSize = 40.sp)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${yogaInfo.yogaName} · Step ${yogaInfo.step}",
                style = MaterialTheme.typography.labelLarge,
                color = gold,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "🪙 $totalCoins", style = MaterialTheme.typography.labelLarge, color = gold)
        }
    }
}

@Composable
fun GuestLoginBanner(onLoginClick: () -> Unit) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = gold.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = gold, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Guest Mode", fontWeight = FontWeight.Bold, color = gold)
                Text("Login to save progress", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onLoginClick, colors = ButtonDefaults.buttonColors(containerColor = gold)) {
                Text("Login", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreativeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).let { if (onClick != null) it.clickable { onClick() } else it },
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SmallStatItem(stat: SmallStatData, modifier: Modifier = Modifier) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    GlassCard(modifier = modifier.height(80.dp), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(stat.icon, contentDescription = null, tint = gold.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Column {
                Text(stat.value, fontWeight = FontWeight.Bold)
                Text(stat.label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun ProfileDisplayInfo(name: String, dob: String, age: Int, onEdit: () -> Unit) {
    Column {
        ProfileInfoRow("Name", name.ifEmpty { "Arjuna" })
        ProfileInfoRow("Birthday", dob.ifEmpty { "Not set" })
        Spacer(Modifier.height(12.dp))
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Saffron)) {
            Text("Edit Profile", color = Color.White)
        }
    }
}

@Composable
fun ProfileEditForm(
    name: String, onNameChange: (String) -> Unit,
    dob: String, onDobChange: (String) -> Unit,
    onSave: () -> Unit, onCancel: () -> Unit
) {
    Column {
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Spiritual Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = dob, onValueChange = onDobChange, label = { Text("DOB (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Saffron)) {
                Text("Save", color = Color.White)
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProgressionHeroSection() {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(GoldSpark, Saffron)))) {
        Column(modifier = Modifier.padding(24.dp).align(Alignment.CenterStart)) {
            Text("Your Journey", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Ascending the steps of Yoga", color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun BadgeItem(badge: UserBadge, goldColor: Color) {
    val textPrimary = MaterialTheme.colorScheme.onBackground
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(goldColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.5.dp, goldColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.icon,
                    fontSize = 28.sp
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textPrimary.copy(alpha = 0.7f)
                )
                
                Spacer(Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(goldColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = goldColor
                        )
                    }
                    
                    Text(
                        text = "Unlocked: ${badge.unlockedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textPrimary.copy(alpha = 0.5f)
                    )
                }
            }
            
            if (badge.level > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Lv. ${badge.level}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = goldColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Row {
                        repeat(badge.level.coerceAtMost(5)) {
                            Text("⭐", fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YogaMargStage(
    emoji: String,
    name: String,
    range: String,
    subs: List<YogaSubStage>,
    currentCoins: Int,
    done: Boolean,
    active: Boolean,
    locked: Boolean,
    isLast: Boolean
) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) gold.copy(alpha = 0.08f) else Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) gold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (locked) Color.Gray.copy(alpha = 0.1f) else gold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (done) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = Color.Green)
                } else if (active) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (subs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    subs.forEach { sub ->
                        val subDone = currentCoins > sub.max_coins
                        val subActive = currentCoins >= sub.min_coins && currentCoins <= sub.max_coins
                        val subLocked = !subDone && !subActive
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        color = when {
                                            subDone -> Color.Green.copy(alpha = 0.15f)
                                            subActive -> gold.copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.1f)
                                        },
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            subDone -> Color.Green
                                            subActive -> gold
                                            else -> Color.Gray.copy(alpha = 0.4f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (subDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Green,
                                        modifier = Modifier.size(10.dp)
                                    )
                                } else if (subActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(gold, CircleShape)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Step ${sub.level}.${sub.sub_level} · ${sub.sub_name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (subActive) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        subLocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        subActive -> gold
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = "${sub.min_coins} – ${sub.max_coins} 🪙",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (subLocked) 0.3f else 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SacredFlame(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlameEffects")
    
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleY"
    )

    val scaleX by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleX"
    )

    val translationX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationX"
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Saffron.copy(alpha = 0.05f),
                    Saffron.copy(alpha = 0.25f)
                )
            )
        ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = "🔥",
            fontSize = 80.sp,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .graphicsLayer(
                    scaleX = scaleX,
                    scaleY = scaleY,
                    translationX = translationX
                )
        )
    }
}

@Composable
fun MandalaBadge(intensity: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MandalaEffects")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = rotation)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2.3f

            drawCircle(
                color = GoldSpark.copy(alpha = glowAlpha),
                radius = baseRadius * 1.2f
            )

            drawCircle(
                color = GoldSpark.copy(alpha = 0.08f),
                radius = baseRadius
            )
            drawCircle(
                color = GoldSpark,
                radius = baseRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            val petalCount = 12
            for (i in 0 until petalCount) {
                val angleRad = (i * 2 * Math.PI / petalCount)
                val petalX = center.x + cos(angleRad).toFloat() * (baseRadius * 0.85f)
                val petalY = center.y + sin(angleRad).toFloat() * (baseRadius * 0.85f)
                
                drawLine(
                    color = GoldSpark.copy(alpha = 0.4f),
                    start = center,
                    end = Offset(petalX, petalY),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = GoldSpark.copy(alpha = 0.8f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(petalX, petalY)
                )
            }
        }

        Text(
            text = "🕉️",
            fontSize = 28.sp
        )
    }
}

@Preview(showBackground = true, name = "Profile Header - Light Mode")
@Composable
fun ProfileHeaderPreviewLight() {
    GitaLearningTheme(darkTheme = false) {
        ProfileHeader(
            profile = ProfileInfo("Arjuna Kumar", 28),
            yogaInfo = YogaLevelManager.YogaLevelInfo(
                level = 1,
                step = 2,
                yogaName = "Karma Yoga",
                yogaDescription = "Path of Unselfish Action",
                emoji = "🌱"
            ),
            levelProgress = 0.4f,
            totalCoins = 150
        )
    }
}

@Preview(showBackground = true, name = "Profile Header - Dark Mode", backgroundColor = 0xFF0F0F0F)
@Composable
fun ProfileHeaderPreviewDark() {
    GitaLearningTheme(darkTheme = true) {
        ProfileHeader(
            profile = ProfileInfo("Arjuna Kumar", 28),
            yogaInfo = YogaLevelManager.YogaLevelInfo(
                level = 1,
                step = 2,
                yogaName = "Karma Yoga",
                yogaDescription = "Path of Unselfish Action",
                emoji = "🌱"
            ),
            levelProgress = 0.4f,
            totalCoins = 150
        )
    }
}
