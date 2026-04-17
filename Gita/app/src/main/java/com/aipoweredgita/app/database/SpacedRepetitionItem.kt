package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaced_repetition_items")
data class SpacedRepetitionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chapter: Int,
    val verse: Int,
    val easeFactor: Float = 2.5f, // How easy the item is for the user
    val interval: Int = 1, // Days until next review
    val repetition: Int = 0, // Number of times reviewed
    val lastReviewedAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long = System.currentTimeMillis(),
    val quality: Int = 0, // 0-5 rating from previous review
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
