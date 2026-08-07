package com.aipoweredgita.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * First-open welcome popup with scrollable notes about features and coin rewards.
 * Header + CTA stay fixed; body scrolls.
 */
@Composable
fun WelcomeDialog(
    onDismiss: () -> Unit,
    onNavigateToBattleQuiz: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val gold = Color(0xFFC9A227)
    val deep = Color(0xFF1A1208)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Fixed header ─────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌸", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Welcome to Bhagavad Gita AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Your path of wisdom — and how coins reward your practice",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ── Scrollable notes ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle("🎁 Welcome gift")
                    RewardNoteCard(
                        emoji = "🪙",
                        title = "+50 Krishna Coins",
                        body = "New seekers (including guests) receive 50 coins as a welcome bonus. Check Coin History anytime to see +earns and spends."
                    )

                    SectionTitle("✨ How you earn coins")
                    RewardNoteCard(
                        emoji = "☀️",
                        title = "Daily check-in",
                        body = "Claim once per day. Streak days pay more; completing a 7-day week unlocks a bonus."
                    )
                    RewardNoteCard(
                        emoji = "📚",
                        title = "Quiz completion",
                        body = "Base 5 coins + accuracy bonus (up to 6), capped at 15 before yoga level multiplier. Higher yoga levels multiply rewards."
                    )
                    RewardNoteCard(
                        emoji = "⚔️",
                        title = "Battle quiz",
                        body = "Fibonacci coins for correct answers (1, 1, 2, 3, 5…), then yoga multiplier. Great for fast practice."
                    )
                    RewardNoteCard(
                        emoji = "📖",
                        title = "Share a sloka",
                        body = "Daily share reward; day 7 of the share streak includes a weekly bonus."
                    )
                    RewardNoteCard(
                        emoji = "📕",
                        title = "Chapter completion",
                        body = "Finish a chapter for 15 coins × your yoga multiplier."
                    )
                    RewardNoteCard(
                        emoji = "⬆",
                        title = "Level up",
                        body = "Crossing yoga thresholds (1k / 3k / 6k / 9k coins) grants a +10 level-up bonus."
                    )

                    SectionTitle("🎙 Voice chat costs")
                    RewardNoteCard(
                        emoji = "💬",
                        title = "Short · Medium · Long",
                        body = "Each question costs 4 / 6 / 10 coins by length (≤50 / ≤150 / longer characters). Balance is checked before the answer."
                    )

                    SectionTitle("🌟 App highlights")
                    FeatureItem(
                        icon = "🌐",
                        title = "Live Telugu & English",
                        description = "Toggle languages in Read Mode for translations, word meanings, and purports."
                    )
                    FeatureItem(
                        icon = "⚡",
                        title = "Cloud AI guidance",
                        description = "Fast Krishna guidance via cloud — no large local model required to start."
                    )
                    FeatureItem(
                        icon = "⚔️",
                        title = "Mahabharata Battle Quiz",
                        description = "Thousands of sequence MCQs in English & Telugu with live language toggle."
                    )
                    FeatureItem(
                        icon = "📚",
                        title = "Offline-ready content",
                        description = "Bundled Q&A datasets and verses so you can practice without waiting on downloads."
                    )

                    SectionTitle("📌 Tips")
                    RewardNoteCard(
                        emoji = "💡",
                        title = "Coin History",
                        body = "Open the drawer → Coin History to see every +earn and spend. Guests keep local history; signed-in users sync with the server."
                    )
                    RewardNoteCard(
                        emoji = "🔐",
                        title = "Sign in to keep progress",
                        body = "Create an account to sync coins, streaks, and notes across devices. Guest progress stays on this phone only."
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Scroll for more · ॐ",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // ── Fixed footer CTAs ────────────────────────────────────
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToBattleQuiz()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            "⚔️ Play Battle Quiz Now",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Explore Gita App")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun RewardNoteCard(
    emoji: String,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 10.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
