package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteVerseDao {

    @Query("SELECT * FROM favorite_verses ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteVerse>>

    @Query("SELECT * FROM favorite_verses WHERE chapterNo = :chapter AND verseNo = :verse LIMIT 1")
    suspend fun getFavorite(chapter: Int, verse: Int): FavoriteVerse?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_verses WHERE chapterNo = :chapter AND verseNo = :verse)")
    fun isFavorite(chapter: Int, verse: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(favorite: FavoriteVerse): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteVerse)

    @Query("DELETE FROM favorite_verses WHERE chapterNo = :chapter AND verseNo = :verse")
    suspend fun deleteFavoriteByChapterVerse(chapter: Int, verse: Int)

    @Query("DELETE FROM favorite_verses")
    suspend fun deleteAllFavorites()

    @Query("SELECT COUNT(*) FROM favorite_verses")
    fun getFavoriteCount(): Flow<Int>
}
