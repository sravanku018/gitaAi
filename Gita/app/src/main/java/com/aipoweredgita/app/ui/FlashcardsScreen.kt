package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    onBack: () -> Unit,
    viewModel: com.aipoweredgita.app.viewmodel.FlashcardsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val gold = if (isDark) GoldSpark else Saffron

    val cards by viewModel.cards.collectAsState()
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        if (isDark) {
            com.aipoweredgita.app.ui.components.AmbientOrbs(modifier = Modifier.fillMaxSize())
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Flashcards", color = textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = gold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = textPrimary,
                        navigationIconContentColor = textPrimary
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (cards.isEmpty()) {
                    Text(
                        text = "No flashcards found for this topic.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (currentIndex >= cards.size) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "All cards completed!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text("Go Back", color = Color.White)
                        }
                    }
                } else {
                    val currentCard = cards[currentIndex]

                    val rotation by animateFloatAsState(
                        targetValue = if (isFlipped) 180f else 0f,
                        animationSpec = tween(durationMillis = 400),
                        label = "cardFlip"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "${currentIndex + 1} / ${cards.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 8 * density
                                }
                                .clickable { isFlipped = !isFlipped },
                            cornerRadius = 32.dp,
                            elevation = 8.dp,
                            tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (rotation <= 90f) {
                                    Text(
                                        text = currentCard.frontText,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = textPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = currentCard.backText,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = textPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier
                                            .padding(24.dp)
                                            .graphicsLayer { rotationY = 180f } // flip text back
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (isFlipped) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateCard(
                                            currentCard.copy(timesShown = currentCard.timesShown + 1)
                                        )
                                        isFlipped = false
                                        currentIndex++
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Close, "Incorrect")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Again")
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateCard(
                                            currentCard.copy(
                                                timesShown = currentCard.timesShown + 1,
                                                timesCorrect = currentCard.timesCorrect + 1
                                            )
                                        )
                                        isFlipped = false
                                        currentIndex++
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.Check, "Correct")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Got It")
                                }
                            }
                        } else {
                            Text(
                                text = "Tap the card to flip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
