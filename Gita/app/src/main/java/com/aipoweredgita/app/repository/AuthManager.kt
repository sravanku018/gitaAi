package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.network.AuthLoginRequest
import com.aipoweredgita.app.network.AuthRegisterRequest
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.utils.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages authentication operations.
 */
class AuthManager(private val context: Context) {

    private val authPrefs = AuthPreferences.getInstance(context)
    private val guestSyncManager = GuestSyncManager.getInstance(context)

    companion object {
        private const val TAG = "AuthManager"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Register a new user
     */
    suspend fun register(
        userId: String,
        password: String,
        name: String = "",
        email: String = ""
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val response = CoinApi.retrofitService.register(
                AuthRegisterRequest(userId, password, name, email)
            )

            if (response.success) {
                val wasGuest = authPrefs.isGuest || !authPrefs.isLoggedIn
                val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                val previousCoins = db.userStatsDao().getUserStatsOnce()?.krishnaCoins ?: 0

                // Save auth state
                authPrefs.saveLoginState(
                    userId = response.user_id,
                    name = name,
                    loginMethod = "password",
                    token = response.token,
                    email = email
                )

                // Update Room DB with new user info
                db.userStatsDao().updateUserId(response.user_id)
                db.userStatsDao().updateProfile(name = name.ifEmpty { "Gita Seeker" }, dob = "")

                if (wasGuest) {
                    val guestConversionCoins = previousCoins + response.coins
                    db.userStatsDao().updateKrishnaCoins(guestConversionCoins)
                    com.aipoweredgita.app.coin.CoinTransactionLogger.log(context, response.coins, "Guest to User conversion bonus")
                } else {
                    db.userStatsDao().updateKrishnaCoins(response.coins.coerceAtLeast(0))
                }

                // Sync guest data if exists
                if (guestSyncManager.hasGuestDataToSync()) {
                    guestSyncManager.syncGuestData(response.user_id, name, email)
                }

                Log.d(TAG, "Registration successful")
                return@withContext Result.success(
                    AuthResult(
                        userId = response.user_id,
                        token = response.token,
                        coins = response.coins
                    )
                )
            } else {
                Log.e(TAG, "Registration failed: ${response.error}")
                return@withContext Result.failure(Exception(response.error ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Login with existing credentials
     */
    suspend fun login(
        userId: String,
        password: String
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val response = CoinApi.retrofitService.login(
                AuthLoginRequest(userId, password)
            )

if (response.success) {
                val wasGuest = authPrefs.hasGuestSession()
                val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                val previousCoins = db.userStatsDao().getUserStatsOnce()?.krishnaCoins ?: 0

                // Extract a display name from email/username
                val displayName = userId.substringBefore("@").replaceFirstChar { it.uppercase() }

                // Save auth state
                authPrefs.saveLoginState(
                    userId = response.user_id,
                    name = displayName,
                    loginMethod = "password",
                    token = response.token,
                    email = userId
                )

                // Update Room DB with new user info
                db.userStatsDao().updateUserId(response.user_id)
                
                val existingStats = db.userStatsDao().getUserStatsOnce()
                val currentName = existingStats?.userName ?: ""
                val newName = if (currentName.isEmpty() || currentName == "Gita Seeker" || currentName == "Arjuna") {
                    displayName
                } else {
                    currentName
                }
                db.userStatsDao().updateProfile(name = newName, dob = existingStats?.dateOfBirth ?: "")

                // Sync coins from server response (not hardcoded 200)
                // Server returns the user's actual balance including welcome bonus
                if (wasGuest) {
                    val guestConversionCoins = previousCoins + response.coins
                    db.userStatsDao().updateKrishnaCoins(guestConversionCoins)
                    com.aipoweredgita.app.coin.CoinTransactionLogger.log(context, response.coins, "Guest to User conversion bonus")
                } else {
                    db.userStatsDao().updateKrishnaCoins(response.coins.coerceAtLeast(0))
                }

                // Sync guest data if exists
                if (guestSyncManager.hasGuestDataToSync()) {
                    guestSyncManager.syncGuestData(response.user_id)
                }

                // Run auto-reconciliation after login to detect any discrepancies
                try {
                    val reconciliationManager = CoinReconciliationManager(context)
                    val result = reconciliationManager.autoReconcile()
                    when (result) {
                        is AutoReconciliationResult.Corrected -> {
                            Log.w(TAG, "Auto-reconciliation corrected after login: ${result.oldBalance} → ${result.newBalance}")
                        }
                        is AutoReconciliationResult.Error -> {
                            Log.e(TAG, "Auto-reconciliation failed after login: ${result.message}")
                        }
                        else -> { /* OK or Skip */ }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-reconciliation failed during login", e)
                }

                // Pull server state into local DB (streak, quizzes, verses, DailyRewardsTracker)
                try {
                    val dbInstance = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                    val statsRepo = com.aipoweredgita.app.repository.StatsRepository(
                        dbInstance.userStatsDao(),
                        dbInstance.dailyActivityDao(),
                        context
                    )
                    statsRepo.refreshUserState(response.user_id)
                    statsRepo.syncStatsToServer()
                } catch (e: Exception) {
                    Log.e(TAG, "Stats sync failed during login", e)
                }

                Log.d(TAG, "Login successful")
                return@withContext Result.success(
                    AuthResult(
                        userId = response.user_id,
                        token = response.token,
                        coins = response.coins
                    )
                )
            } else {
                Log.e(TAG, "Login failed: ${response.error}")
                return@withContext Result.failure(Exception(response.error ?: "Login failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Logout current user
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        val token = authPrefs.token
        if (token != null) {
            try {
                CoinApi.retrofitService.logout("Bearer $token")
            } catch (e: Exception) {
                Log.e(TAG, "Logout API error: ${e.message}")
            }
        }
        authPrefs.clearLoginState()
        try {
            val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
            db.userStatsDao().updateUserId("")
            db.userStatsDao().updateProfile("", "")
            db.userStatsDao().insertStats(com.aipoweredgita.app.database.UserStats(id = 1, userId = "", krishnaCoins = 0, serverUpdatedAt = ""))
            db.chatSummaryDao().deleteSummary("krishna-guest-${java.time.LocalDate.now()}")
            db.voiceChatMessageDao().deleteAllMessages()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset Room database on logout", e)
        }
    }

    /**
     * Delete user account from server and clear all local data.
     * Returns Result.success(Unit) if server deletion succeeded, or Result.failure(Exception) if remote deletion failed.
     */
    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val token = authPrefs.token
        if (token != null) {
            try {
                CoinApi.retrofitService.deleteAccount("Bearer $token")
                Log.d(TAG, "Account deleted from server")
            } catch (e: Exception) {
                Log.e(TAG, "Delete account API error: ${e.message}")
                return@withContext Result.failure(e)
            }
        }
        // Clear ALL local data including credentials on successful server deletion
        authPrefs.clearAll()
        Result.success(Unit)
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return authPrefs.isLoggedIn && authPrefs.token != null
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return authPrefs.userId
    }

    /**
     * Get current auth token
     */
    fun getToken(): String? {
        return authPrefs.token
    }
}

/**
 * Result of an authentication operation
 */
data class AuthResult(
    val userId: String,
    val token: String,
    val coins: Int
)
