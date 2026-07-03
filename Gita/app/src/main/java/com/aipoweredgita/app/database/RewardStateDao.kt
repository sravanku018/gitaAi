package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardStateDao {
    @Query("SELECT * FROM reward_state WHERE id = 1 LIMIT 1")
    fun getRewardStateFlow(): Flow<RewardState?>

    @Query("SELECT * FROM reward_state WHERE id = 1 LIMIT 1")
    fun getRewardStateSync(): RewardState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(state: RewardState)

    @Update
    fun update(state: RewardState)
}
