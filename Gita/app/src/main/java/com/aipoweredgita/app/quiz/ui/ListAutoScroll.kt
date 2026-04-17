package com.aipoweredgita.app.quiz.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * Auto-advance a LazyList (LazyRow/LazyColumn) to the next item when [trigger] changes.
 */
@Composable
fun AutoAdvanceListEffect(
    listState: LazyListState,
    trigger: Any?,
    delayMs: Long = 600,
) {
    LaunchedEffect(trigger) {
        val nextIndex = listState.firstVisibleItemIndex + 1
        if (nextIndex < listState.layoutInfo.totalItemsCount) {
            if (delayMs > 0) delay(delayMs)
            listState.animateScrollToItem(nextIndex)
        }
    }
}

