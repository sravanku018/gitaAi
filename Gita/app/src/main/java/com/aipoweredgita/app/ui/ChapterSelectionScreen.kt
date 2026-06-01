package com.aipoweredgita.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aipoweredgita.app.ui.components.ChapterCard
import com.aipoweredgita.app.ui.components.ChapterInfo
import com.aipoweredgita.app.ui.components.createChapterInfo
import kotlin.math.absoluteValue
import com.aipoweredgita.app.database.GitaDatabase
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterSelectionScreen(
    onChapterSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val chapters: List<ChapterInfo> = remember {
        listOf(
            createChapterInfo(1, "Arjuna Vishada Yoga", "అర్జున విషాద యోగము", "Arjuna's Crisis of Compassion", 47),
            createChapterInfo(2, "Sankhya Yoga", "సాంఖ్య యోగము", "The Yoga of Knowledge", 72),
            createChapterInfo(3, "Karma Yoga", "కర్మ యోగము", "The Yoga of Action", 43),
            createChapterInfo(4, "Jnana Karma Sanyasa Yoga", "జ్ఞాన కర్మ సన్యాస యోగము", "The Yoga of Wisdom", 42),
            createChapterInfo(5, "Karma Sanyasa Yoga", "కర్మ సన్యాస యోగము", "The Yoga of Renunciation", 29),
            createChapterInfo(6, "Dhyana Yoga", "ధ్యాన యోగము", "The Yoga of Meditation", 47),
            createChapterInfo(7, "Jnana Vijnana Yoga", "జ్ఞాన విజ్ఞాన యోగము", "The Yoga of Discernment", 30),
            createChapterInfo(8, "Akshara Brahma Yoga", "అక్షర బ్రహ్మ యోగము", "The Yoga of Imperishable Brahman", 28),
            createChapterInfo(9, "Raja Vidya Raja Guhya Yoga", "రాజ విద్యా రాజ గుహ్య యోగము", "The Yoga of Royal Secret", 34),
            createChapterInfo(10, "Vibhuti Yoga", "విభూతి యోగము", "The Yoga of Divine Splendors", 42),
            createChapterInfo(11, "Vishwarupa Darshana Yoga", "విశ్వరూప దర్శన యోగము", "The Yoga of Universal Form", 55),
            createChapterInfo(12, "Bhakti Yoga", "భక్తి యోగము", "The Yoga of Devotion", 20),
            createChapterInfo(13, "Kshetra Kshetrajna Vibhaga Yoga", "క్షేత్ర క్షేత్రజ్ఞ విభాగ యోగము", "The Field and the Knower", 34),
            createChapterInfo(14, "Gunatraya Vibhaga Yoga", "గుణత్రయ విభాగ యోగము", "The Three Gunas", 27),
            createChapterInfo(15, "Purushottama Yoga", "పురుషోత్తమ యోగము", "The Supreme Being", 20),
            createChapterInfo(16, "Daivasura Sampad Vibhaga Yoga", "దైవాసుర సంపద్ విభాగ యోగము", "Divine and Demonic Natures", 24),
            createChapterInfo(17, "Shraddhatraya Vibhaga Yoga", "శ్రద్ధాత్రయ విభాగ యోగము", "The Threefold Faith", 28),
            createChapterInfo(18, "Moksha Sanyasa Yoga", "మోక్ష సన్యాస యోగము", "The Yoga of Liberation", 78)
        )
    }
    
    val context = LocalContext.current
    var chapterProgressMap by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    
    LaunchedEffect(Unit) {
        val db = GitaDatabase.getDatabase(context)
        val progressMap = mutableMapOf<Int, Int>()
        withContext(Dispatchers.IO) {
            chapters.forEach { chapter ->
                val count = db.readVerseDao().getReadVersesCountByChapter(chapter.number)
                progressMap[chapter.number] = count
            }
        }
        chapterProgressMap = progressMap
    }

    val pagerState = rememberPagerState(pageCount = { chapters.size })
    val coroutineScope = rememberCoroutineScope()

    val isDark = isSystemInDarkTheme()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) com.aipoweredgita.app.ui.theme.GoldSpark else com.aipoweredgita.app.ui.theme.Saffron

    com.aipoweredgita.app.ui.background.AppBackground(
        pattern = com.aipoweredgita.app.ui.background.BgPattern.AMBIENT_ORBS,
        intensity = 0.3f,
        isDark = isDark
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Sacred Chapters", fontWeight = FontWeight.Bold, color = textPrimary) },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Flip through the wisdom of Gita",
                    style = MaterialTheme.typography.titleMedium,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                        val alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        
                        val scale = lerp(
                            start = 0.8f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )

                        ChapterCard(
                            chapter = chapters[page],
                            readCount = chapterProgressMap[chapters[page].number] ?: 0,
                            onClick = { onChapterSelected(chapters[page].number) },
                            modifier = Modifier
                                .graphicsLayer {
                                    this.alpha = alpha
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    // 3D Book Flip effect
                                    rotationY = lerp(
                                        start = 0f,
                                        stop = 40f,
                                        fraction = pageOffset.coerceIn(-1f, 1f)
                                    ) * (if (page < pagerState.currentPage) 1f else -1f)
                                }
                        )
                    }
                }

                // Page Indicator
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(chapters.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) com.aipoweredgita.app.ui.theme.GoldSpark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 10.dp else 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = if (pagerState.currentPage > 0) com.aipoweredgita.app.ui.theme.GoldSpark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    
                    Text(
                        text = "Chapter ${pagerState.currentPage + 1} of 18",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        enabled = pagerState.currentPage < (chapters.size - 1)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = if (pagerState.currentPage < (chapters.size - 1)) com.aipoweredgita.app.ui.theme.GoldSpark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
