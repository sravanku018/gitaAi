package com.aipoweredgita.app.quiz.ui

import androidx.viewpager2.widget.ViewPager2

/** Auto-advance a ViewPager2 to the next item. */
fun ViewPager2.autoAdvance(delayMs: Long = 600) {
    val pager = this
    pager.postDelayed({
        val adapter = pager.adapter ?: return@postDelayed
        val last = (adapter.itemCount - 1).coerceAtLeast(0)
        val next = (pager.currentItem + 1).coerceAtMost(last)
        if (next != pager.currentItem) pager.setCurrentItem(next, true)
    }, delayMs)
}

