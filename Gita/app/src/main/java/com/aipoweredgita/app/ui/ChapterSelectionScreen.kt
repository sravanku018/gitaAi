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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aipoweredgita.app.ui.components.ChapterCard
import com.aipoweredgita.app.ui.components.ChapterInfo
import kotlin.math.absoluteValue
import com.aipoweredgita.app.database.GitaDatabase
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterSelectionScreen(
    onChapterSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val chapters = remember {
        listOf(
            ChapterInfo(1, "Arjuna Vishada Yoga", "అర్జున విషాద యోగము", "Arjuna's Crisis of Compassion", 47, listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
            ChapterInfo(2, "Sankhya Yoga", "సాంఖ్య యోగము", "The Yoga of Knowledge", 72, listOf(Color(0xFFEC4899), Color(0xFFF43F5E))),
            ChapterInfo(3, "Karma Yoga", "కర్మ యోగము", "The Yoga of Action", 43, listOf(Color(0xFF10B981), Color(0xFF059669))),
            ChapterInfo(4, "Jnana Karma Sanyasa Yoga", "జ్ఞాన కర్మ సన్యాస యోగము", "The Yoga of Wisdom", 42, listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
            ChapterInfo(5, "Karma Sanyasa Yoga", "కర్మ సన్యాస యోగము", "The Yoga of Renunciation", 29, listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))),
            ChapterInfo(6, "Dhyana Yoga", "ధ్యాన యోగము", "The Yoga of Meditation", 47, listOf(Color(0xFF06B6D4), Color(0xFF10B981))),
            ChapterInfo(7, "Jnana Vijnana Yoga", "జ్ఞాన విజ్ఞాన యోగము", "The Yoga of Discernment", 30, listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))),
            ChapterInfo(8, "Akshara Brahma Yoga", "అక్షర బ్రహ్మ యోగము", "The Yoga of Imperishable Brahman", 28, listOf(Color(0xFFEC4899), Color(0xFFD946EF))),
            ChapterInfo(9, "Raja Vidya Raja Guhya Yoga", "రాజ విద్యా రాజ గుహ్య యోగము", "The Yoga of Royal Secret", 34, listOf(Color(0xFF10B981), Color(0xFFF59E0B))),
            ChapterInfo(10, "Vibhuti Yoga", "విభూతి యోగము", "The Yoga of Divine Splendors", 42, listOf(Color(0xFFFF6B35), Color(0xFFFF9F43))),
            ChapterInfo(11, "Vishwarupa Darshana Yoga", "విశ్వరూప దర్శన యోగము", "The Yoga of Universal Form", 55, listOf(Color(0xFF6366F1), Color(0xFFEC4899))),
            ChapterInfo(12, "Bhakti Yoga", "భక్తి యోగము", "The Yoga of Devotion", 20, listOf(Color(0xFFF43F5E), Color(0xFF8B5CF6))),
            ChapterInfo(13, "Kshetra Kshetrajna Vibhaga Yoga", "క్షేత్ర క్షేత్రజ్ఞ విభాగ యోగము", "The Field and the Knower", 34, listOf(Color(0xFF059669), Color(0xFF06B6D4))),
            ChapterInfo(14, "Gunatraya Vibhaga Yoga", "గుణత్రయ విభాగ యోగము", "The Three Gunas", 27, listOf(Color(0xFFEF4444), Color(0xFF3B82F6))),
            ChapterInfo(15, "Purushottama Yoga", "పురుషోత్తమ యోగము", "The Supreme Being", 20, listOf(Color(0xFFD946EF), Color(0xFF10B981))),
            ChapterInfo(16, "Daivasura Sampad Vibhaga Yoga", "దైవాసుర సంపద్ విభాగ యోగము", "Divine and Demonic Natures", 24, listOf(Color(0xFF60A5FA), Color(0xFFFF6B35))),
            ChapterInfo(17, "Shraddhatraya Vibhaga Yoga", "శ్రద్ధాత్రయ విభాగ యోగము", "The Threefold Faith", 28, listOf(Color(0xFF8B5CF6), Color(0xFFFF9F43))),
            ChapterInfo(18, "Moksha Sanyasa Yoga", "మోక్ష సన్యాస యోగము", "The Yoga of Liberation", 78, listOf(Color(0xFF10B981), Color(0xFF6366F1)))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sacred Chapters", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Flip through the wisdom of Gita",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
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
                    onClick = { /* Previous */ },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                }
                
                Text(
                    "Chapter ${pagerState.currentPage + 1} of 18",
                    style = MaterialTheme.typography.labelLarge
                )
                
                IconButton(
                    onClick = { /* Next */ },
                    enabled = pagerState.currentPage < (chapters.size - 1)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                }
            }
        }
    }
}
