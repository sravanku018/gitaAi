package com.aipoweredgita.app.database
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface UserPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prefs: UserPreferences)
    @Update
    suspend fun update(prefs: UserPreferences)
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getPreferences(userId: Int): Flow<UserPreferences?>
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getPreferencesSync(userId: Int): UserPreferences?
}
