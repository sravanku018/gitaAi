package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.ui.components.MandalaBackground
import com.aipoweredgita.app.ui.components.PremiumDashboardCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

enum class TimelineStatus {
    LOCKED,
    CURRENT,
    COMPLETED
}

@Composable
fun ProgressionHeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        // Background Pattern
        MandalaBackground(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(280.dp)
                .offset(x = 40.dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
        )
        
        MandalaBackground(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(120.dp)
                .offset(x = (-20).dp, y = (-20).dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Your Journey",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Ascending the steps of Yoga",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun BadgesScreen(modifier: Modifier = Modifier, viewModel: ProfileViewModel = viewModel()) {
    val stats by viewModel.stats.collectAsState()
    val uiCfg = LocalUiConfig.current

    @Composable
    fun AnimatedItem(
        index: Int,
        content: @Composable () -> Unit
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(index * 50L)
            visible = true
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)) + 
                    slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(500)
                    )
        ) {
            content()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = if (uiCfg.isLandscape) 24.dp else 16.dp,
                     end = if (uiCfg.isLandscape) 24.dp else 16.dp,
                     top = if (uiCfg.isLandscape) 24.dp else 16.dp,
                     bottom = if (uiCfg.isLandscape) 96.dp else 80.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedItem(index = 0) {
            ProgressionHeroSection()
        }
        
        // Interleaved Timeline Content
        // Get yoga progression
        val context = androidx.compose.ui.platform.LocalContext.current
        val database = androidx.compose.runtime.remember { com.aipoweredgita.app.database.GitaDatabase.getDatabase(context) }
        val progression by database.yogaProgressionDao().getProgressionFlow().collectAsState(initial = null)

        // Determine status helper
        // Assuming:
        // Level 1 = Karma Yoga (progression.yogaLevel 0-based index?)
        // Let's assume progression.yogaLevel is 0-based.
        // So Level 1 card corresponds to yogaLevel 0.
        // If user is at yogaLevel 0: Level 1 is CURRENT. Level 2 is LOCKED.
        // If user is at yogaLevel 1: Level 1 is COMPLETED. Level 2 is CURRENT.
        
        val currentLevelIndex = progression?.yogaLevel ?: 0 // 0-based
        val getStatus = { levelIndex: Int -> // levelIndex is 1-based passed from UI
             if (levelIndex - 1 < currentLevelIndex) TimelineStatus.COMPLETED
             else if (levelIndex - 1 == currentLevelIndex) TimelineStatus.CURRENT
             else TimelineStatus.LOCKED
        }

        // 1. Karma Yoga (Green Theme)
        val karmaStart = Color(0xFF10B981)
        val karmaEnd = Color(0xFF059669)
        val status1 = getStatus(1)

        AnimatedItem(index = 1) {
            TimelineLevelItem(1, "Karma Yoga", "Path of Action", "🌿", karmaStart, karmaEnd, status1, isFirst = true)
        }
        AnimatedItem(index = 2) { TimelineStepItem(1, "Swadharma", "Do your duty honestly", "①", karmaEnd, status1) }
        AnimatedItem(index = 3) { TimelineStepItem(2, "Nishkama Karma", "Action without desire", "②", karmaEnd, status1) }
        AnimatedItem(index = 4) { TimelineStepItem(3, "Ishwara Arpanam", "Offer work to God", "③", karmaEnd, status1) }

        // 2. Bhakti Yoga (Pink/Red Theme)
        val bhaktiStart = Color(0xFFEC4899)
        val bhaktiEnd = Color(0xFFF43F5E)
        val status2 = getStatus(2)

        AnimatedItem(index = 5) {
            TimelineLevelItem(2, "Bhakti Yoga", "Path of Devotion", "🔥", bhaktiStart, bhaktiEnd, status2)
        }
        AnimatedItem(index = 6) { TimelineStepItem(4, "Bhakti Drida", "Firm Devotion", "④", bhaktiEnd, status2) }
        AnimatedItem(index = 7) { TimelineStepItem(5, "Surrender", "Saranagati", "⑤", bhaktiEnd, status2) }
        AnimatedItem(index = 8) { TimelineStepItem(6, "Prem Bhakti", "Divine Love", "⑥", bhaktiEnd, status2) }
        AnimatedItem(index = 9) { TimelineStepItem(7, "Ananya Bhakti", "Exclusive Devotion", "⑦", bhaktiEnd, status2) }

        // 3. Jnana Yoga (Purple Theme)
        val jnanaStart = Color(0xFF8B5CF6)
        val jnanaEnd = Color(0xFF6366F1)
        val status3 = getStatus(3)
        
        AnimatedItem(index = 10) {
            TimelineLevelItem(3, "Jnana Yoga", "Path of Knowledge", "🧠", jnanaStart, jnanaEnd, status3)
        }
        AnimatedItem(index = 11) { TimelineStepItem(8, "Self-Inquiry", "Who am I?", "⑧", jnanaEnd, status3) }
        AnimatedItem(index = 12) { TimelineStepItem(9, "Discrimination", "Real vs Unreal", "⑨", jnanaEnd, status3) }
        AnimatedItem(index = 13) { TimelineStepItem(10, "Viveka", "Wisdom", "⑩", jnanaEnd, status3) }
        AnimatedItem(index = 14) { TimelineStepItem(11, "Brahma Jnana", "Ultimate Truth", "⑪", jnanaEnd, status3) }

        // 4. Dhyana Yoga (Blue Theme)
        val dhyanaStart = Color(0xFF3B82F6)
        val dhyanaEnd = Color(0xFF2563EB)
        val status4 = getStatus(4)

        AnimatedItem(index = 15) {
            TimelineLevelItem(4, "Dhyana Yoga", "Path of Meditation", "🌬️", dhyanaStart, dhyanaEnd, status4)
        }
        AnimatedItem(index = 16) { TimelineStepItem(12, "Dharana", "Concentration", "⑫", dhyanaEnd, status4) }
        AnimatedItem(index = 17) { TimelineStepItem(13, "Dhyana", "Meditation", "⑬", dhyanaEnd, status4) }
        AnimatedItem(index = 18) { TimelineStepItem(14, "Pratyahara", "Withdrawal", "⑭", dhyanaEnd, status4) }
        AnimatedItem(index = 19) { TimelineStepItem(15, "Samadhi", "Absorption", "⑮", dhyanaEnd, status4) }

        // 5. Moksha (Gold/Orange Theme)
        val mokshaStart = Color(0xFFF59E0B)
        val mokshaEnd = Color(0xFFD97706)
        val status5 = getStatus(5)

        AnimatedItem(index = 20) {
            TimelineLevelItem(5, "Moksha", "Liberation", "🌺", mokshaStart, mokshaEnd, status5)
        }
        AnimatedItem(index = 21) { TimelineStepItem(16, "Vairagya", "Detachment", "⑯", mokshaEnd, status5) }
        AnimatedItem(index = 22) { TimelineStepItem(17, "Sanyasa", "Renunciation", "⑰", mokshaEnd, status5) }
        AnimatedItem(index = 23) { TimelineStepItem(18, "Kaivalya", "Solitude", "⑱", mokshaEnd, status5) }
        AnimatedItem(index = 24) { TimelineStepItem(19, "Moksha", "Freedom", "⑲", mokshaEnd, status5, isLast = true) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Journey Summary
        Text(
            text = "Your Journey",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Your Spiritual Journey:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Karma → Bhakti → Jnana → Dhyana → Moksha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Each level guides you deeper into the spiritual path described in the Bhagavad Gita.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Stats Section
        if (stats != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox("Verses Read", "${stats?.versesRead ?: 0}")
                StatBox("Quizzes Taken", "${stats?.totalQuizzesTaken ?: 0}")
                StatBox("Favorite Verses", "${stats?.totalFavorites ?: 0}")
            }
        }
    }
}

@Composable
fun TimelineNode(
    isLast: Boolean,
    isFirst: Boolean,
    color: Color,
    isLevel: Boolean,
    status: TimelineStatus,
    content: @Composable () -> Unit
) {
    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_node")
    val pulseScale by if (status == TimelineStatus.CURRENT && isLevel) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val alpha = if (status == TimelineStatus.LOCKED) 0.4f else 1f
    // If locked, override color to gray, or just use alpha? Let's use surfaceVariant/onSurfaceVariant to be theme-aware.
    val displayColor = if (status == TimelineStatus.LOCKED) MaterialTheme.colorScheme.outline else color

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline Line Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Top Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .weight(1f)
                    .background(
                        if (isFirst) Color.Transparent else displayColor.copy(alpha = if(status == TimelineStatus.LOCKED) 0.3f else 0.5f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            // Node
            Box(
                modifier = Modifier
                    .size(if (isLevel) 24.dp else 16.dp)
                    .graphicsLayer {
                         scaleX = pulseScale
                         scaleY = pulseScale
                    }
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(displayColor)
                    .padding(if (isLevel) 4.dp else 0.dp) // Ring effect for Levels only
            ) {
                 if (isLevel) {
                     Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                     )
                 }
            }

            // Bottom Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .weight(1f)
                    .background(
                        if (isLast) Color.Transparent else displayColor.copy(alpha = if(status == TimelineStatus.LOCKED) 0.3f else 0.5f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp)
                .graphicsLayer { this.alpha = alpha }
        ) {
            content()
        }
    }
}

@Composable
fun TimelineLevelItem(
    level: Int,
    name: String,
    description: String,
    emoji: String,
    colorStart: Color,
    colorEnd: Color,
    status: TimelineStatus,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    TimelineNode(
        isLast = isLast,
        isFirst = isFirst,
        color = colorEnd,
        isLevel = true,
        status = status
    ) {
        PremiumDashboardCard(
            title = "$level. $name",
            description = if(status == TimelineStatus.LOCKED) "Locked" else description,
            icon = { Text(text = emoji, fontSize = 28.sp) },
            gradient = if (status == TimelineStatus.LOCKED) 
                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline) 
                else listOf(colorStart, colorEnd),
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TimelineStepItem(
    step: Int,
    name: String,
    description: String,
    icon: String,
    color: Color,
    status: TimelineStatus,
    isLast: Boolean = false
) {
    TimelineNode(
        isLast = isLast,
        isFirst = false,
        color = color,
        isLevel = false,
        status = status
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if(status == TimelineStatus.LOCKED) 0.dp else 2.dp),
            shape = RoundedCornerShape(12.dp)
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
                         .size(32.dp)
                         .clip(androidx.compose.foundation.shape.CircleShape)
                         .background(
                             if(status == TimelineStatus.LOCKED) MaterialTheme.colorScheme.surfaceVariant 
                             else MaterialTheme.colorScheme.secondaryContainer
                         ),
                     contentAlignment = Alignment.Center
                ) {
                     Text(
                         text = step.toString(),
                         style = MaterialTheme.typography.titleSmall,
                         fontWeight = FontWeight.Bold,
                         color = if(status == TimelineStatus.LOCKED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSecondaryContainer
                     )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
