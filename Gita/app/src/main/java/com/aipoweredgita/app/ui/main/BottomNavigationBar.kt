package com.aipoweredgita.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aipoweredgita.app.R
import com.aipoweredgita.app.navigation.Screen
import com.aipoweredgita.app.navigation.NavGraph
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.utils.AuthPreferences
import androidx.compose.foundation.border
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit
) {
    val springSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)
    val springColor = spring<Color>(dampingRatio = 0.7f, stiffness = 400f)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
        tonalElevation = 4.dp
    ) {
        val navItems = listOf(
            Screen.Home.route to Icons.Filled.Home to "Home",
            Screen.ChapterSelection.route to Icons.AutoMirrored.Filled.MenuBook to "Read",
            Screen.QuizSection.route to Icons.Filled.School to "Quiz",
            Screen.VoiceStudio.route to Icons.Filled.Mic to "Voice",
            "more" to Icons.Filled.AddCircle to "More"
        )
        
        navItems.forEach { (routeAndIcon, label) ->
            val route = routeAndIcon.first
            val icon = routeAndIcon.second
            val isSelected = when (route) {
                Screen.Home.route -> currentRoute == Screen.Home.route
                Screen.ChapterSelection.route -> currentRoute == Screen.ChapterSelection.route || currentRoute?.startsWith("normal_mode") == true
                Screen.QuizSection.route -> currentRoute == Screen.QuizSection.route || currentRoute == Screen.QuizConfig.route || currentRoute == Screen.QuizMode.route
                Screen.VoiceStudio.route -> currentRoute == Screen.VoiceStudio.route
                "more" -> false
                else -> false
            }

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.9f,
                animationSpec = springSpec,
                label = "nav_scale_$label"
            )
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) GoldSpark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                animationSpec = springColor,
                label = "nav_color_$label"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) GoldSpark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                animationSpec = springColor,
                label = "nav_text_$label"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Box(modifier = Modifier.scale(iconScale)) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconColor
                        )
                    }
                },
                label = {
                    Text(
                        label,
                        color = textColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            )
        }
    }
}

