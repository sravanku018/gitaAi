package com.aipoweredgita.app.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMap(value: Map<String, Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMap(value: String): Map<String, Int> {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    @TypeConverter
    fun fromBookmarkType(value: BookmarkType): String {
        return value.name
    }

    @TypeConverter
    fun toBookmarkType(value: String): BookmarkType {
        return BookmarkType.valueOf(value)
    }
}
