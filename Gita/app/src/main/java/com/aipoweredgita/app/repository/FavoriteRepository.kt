package com.aipoweredgita.app.repository

import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.database.FavoriteVerse
import com.aipoweredgita.app.database.FavoriteVerseDao
import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteVerseDao) {

    val allFavorites: Flow<List<FavoriteVerse>> = favoriteDao.getAllFavorites()

    fun getFavoriteCount(): Flow<Int> = favoriteDao.getFavoriteCount()

    fun isFavorite(chapter: Int, verse: Int): Flow<Boolean> {
        return favoriteDao.isFavorite(chapter, verse)
    }

    suspend fun addFavorite(gitaVerse: GitaVerse): Result<String> {
        return try {
            // Check if already exists
            val existing = favoriteDao.getFavorite(gitaVerse.chapterNo, gitaVerse.verseNo)
            if (existing != null) {
                Result.failure(Exception("Verse already in favorites"))
            } else {
                val favorite = FavoriteVerse(
                    chapterNo = gitaVerse.chapterNo,
                    verseNo = gitaVerse.verseNo,
                    chapterName = gitaVerse.chapterName,
                    verse = gitaVerse.verse,
                    translation = gitaVerse.translation,
                    explanation = gitaVerse.explanation
                )
                val id = favoriteDao.insertFavorite(favorite)
                if (id > 0) {
                    Result.success("Added to favorites")
                } else {
                    Result.failure(Exception("Duplicate verse"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(chapter: Int, verse: Int): Result<String> {
        return try {
            favoriteDao.deleteFavoriteByChapterVerse(chapter, verse)
            Result.success("Removed from favorites")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllFavorites(): Result<String> {
        return try {
            favoriteDao.deleteAllFavorites()
            Result.success("All favorites cleared")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
