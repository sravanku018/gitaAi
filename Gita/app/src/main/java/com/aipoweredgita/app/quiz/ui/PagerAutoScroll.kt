package com.aipoweredgita.app.quiz.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * Auto-advance a Compose Pager to the next page when [trigger] changes.
 * Call from a quiz screen after submitting an answer.
 */
@Composable
fun AutoAdvancePagerEffect(
    pagerState: PagerState,
    trigger: Any?,
    delayMs: Long = 600,
) {
    LaunchedEffect(trigger) {
        val lastIndex = pagerState.pageCount - 1
        val next = pagerState.currentPage + 1
        if (next <= lastIndex) {
            if (delayMs > 0) delay(delayMs)
            pagerState.animateScrollToPage(next)
        }
    }
}

