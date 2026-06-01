package com.aipoweredgita.app.database

import androidx.room.TypeConverter
<<<<<<< HEAD
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
=======

class Converters {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    @TypeConverter
    fun fromBookmarkType(value: BookmarkType): String {
        return value.name
    }

    @TypeConverter
    fun toBookmarkType(value: String): BookmarkType {
        return BookmarkType.valueOf(value)
    }
}
