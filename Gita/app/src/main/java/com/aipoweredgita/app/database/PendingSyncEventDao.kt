package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingSyncEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PendingSyncEvent)

    @Query("SELECT * FROM pending_sync_events WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getPendingEvents(userId: String): List<PendingSyncEvent>

    @Delete
    suspend fun delete(event: PendingSyncEvent)

    @Query("DELETE FROM pending_sync_events WHERE id = :id")
    suspend fun deleteById(id: Int)
}
