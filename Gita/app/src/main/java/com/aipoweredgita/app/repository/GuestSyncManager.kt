package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.network.ClaimGuestRequest
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.utils.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages guest data synchronization when user logs in.
 * Merges guest account data (coins, streaks, etc.) with real account.
 */
class GuestSyncManager(private val context: Context) {

    private val authPrefs = AuthPreferences.getInstance(context)

    companion object {
        private const val TAG = "GuestSyncManager"

        @Volatile
        private var INSTANCE: GuestSyncManager? = null

        fun getInstance(context: Context): GuestSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GuestSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Sync guest data with real account after login.
     * Call this after successful authentication.
     * 
     * @param realUserId The authenticated user's ID
     * @param name User's name
     * @param email User's email (optional)
     * @return true if sync was successful
     */
    suspend fun syncGuestData(
        realUserId: String,
        name: String = "",
        email: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val guestId = authPrefs.guestId
        
        if (guestId == null) {
            Log.d(TAG, "No guest session to sync")
            return@withContext true
        }

        try {
            Log.d(TAG, "Syncing guest data")
            
            val response = CoinApi.retrofitService.claimGuest(
                ClaimGuestRequest(
                    guest_id = guestId,
                    real_user_id = realUserId,
                    name = name,
                    email = email
                ),
                authPrefs.guestToken?.let { "Bearer $it" }
            )

            if (response.success) {
                Log.d(TAG, "Guest data synced successfully. Sync bonus: ${response.sync_bonus}")
                // Clear guest state after successful sync
                authPrefs.guestId = null
                authPrefs.isGuest = false
                authPrefs.guestToken = null
                return@withContext true
            } else {
                Log.e(TAG, "Guest sync failed: ${response.error}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Guest sync error: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Check if there's guest data to sync
     */
    fun hasGuestDataToSync(): Boolean {
        return authPrefs.hasGuestSession()
    }
}
