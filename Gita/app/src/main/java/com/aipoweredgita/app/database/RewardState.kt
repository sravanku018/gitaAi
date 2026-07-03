package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_state")
data class RewardState(
    @PrimaryKey
    val id: Int = 1, // Single row for global reward state

    // Check-in state
    val checkinDay: Int = 1,
    val checkinWeek: Int = 1,
    val lastCheckinDate: String = "",
    val checkinProtectionUsed: Boolean = false,
    
    // Check-in completion tracking (ISO week-based year + week number)
    val checkinCompletedWeek: String = "",
    val checkinProtectionGranted: Boolean = false,

    // Share state
    val shareDay: Int = 1,
    val shareWeek: Int = 1,
    val lastShareDate: String = "",
    val shareProtectionUsed: Boolean = false,
    
    // Share completion tracking
    val shareCompletedWeek: String = "",
    val shareProtectionGranted: Boolean = false,
    
    // Sync flags
    val isCheckinSynced: Boolean = false,
    val isShareSynced: Boolean = false
)
