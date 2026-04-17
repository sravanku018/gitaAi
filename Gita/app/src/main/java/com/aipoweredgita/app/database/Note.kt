package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chapter: Int,
    val verse: Int,
    val noteTitle: String,
    val noteContent: String,
    val tags: String = "", // JSON string of tags list
    val mood: String? = null, // joyful, contemplative, reflective, etc.
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPrivate: Boolean = true
)
