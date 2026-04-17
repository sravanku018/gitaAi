package com.aipoweredgita.app.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBookmarkType(value: BookmarkType): String {
        return value.name
    }

    @TypeConverter
    fun toBookmarkType(value: String): BookmarkType {
        return BookmarkType.valueOf(value)
    }
}
