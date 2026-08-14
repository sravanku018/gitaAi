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
                val previousUserId = authPrefs.userId
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

                try {
                    val tracker = com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(context)
                    val switchedUser = previousUserId != null &&
                        previousUserId.isNotEmpty() &&
                        previousUserId != response.user_id
                    if (switchedUser || wasGuest) {
                        tracker.resetForAccountSwitch()
                    }
                    val dbInstance = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                    val statsRepo = com.aipoweredgita.app.repository.StatsRepository(
                        dbInstance.userStatsDao(),
                        dbInstance.dailyActivityDao(),
                        context
                    )
                    statsRepo.refreshUserState(response.user_id, force = true)
                    NotesServerSync.pullFromServer(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Streak/balance/notes refresh failed after register", e)
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
     * Create a guest account on VPS server & save auth state (matching login/register pattern)
     */
    suspend fun createGuest(): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== CREATE GUEST START ===")
            val response = CoinApi.retrofitService.createGuest()
            Log.d(TAG, "Server response: guest_id=${response.guest_id}, token=${response.token.take(8)}..., coins=${response.coins}")
            val guestId = response.guest_id
            val token = response.token.ifEmpty { guestId }
            Log.d(TAG, "Resolved guestId=$guestId, token=$token")

            if (guestId.isNotEmpty()) {
                // Save as regular user (not guest) — use same code path as signed-in users
                Log.d(TAG, "Saving guest as regular user: guestId=$guestId")
                authPrefs.saveLoginState(
                    userId = guestId,
                    name = "Guest",
                    loginMethod = "guest",
                    token = token,
                    email = "${guestId}@gita.com"
                )
                Log.d(TAG, "After save: authPrefs.userId=${authPrefs.userId}, isGuest=${authPrefs.isGuestUser}")

                // Update Room DB
                val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                db.userStatsDao().updateUserId(guestId)
                db.userStatsDao().updateKrishnaCoins(response.coins.coerceAtLeast(50))

                com.aipoweredgita.app.coin.CoinTransactionLogger.log(
                    context,
                    response.coins.coerceAtLeast(50),
                    "Welcome bonus",
                    source = "signup",
                    userId = guestId
                )

                Log.d(TAG, "=== CREATE GUEST SUCCESS: $guestId ===")
                return@withContext Result.success(
                    AuthResult(
                        userId = guestId,
                        token = token,
                        coins = response.coins
                    )
                )
            } else {
                Log.e(TAG, "Guest creation failed: empty guest_id")
                return@withContext Result.failure(Exception("Failed to create guest user on server"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== CREATE GUEST FAILED: ${e.message} ===", e)
            return@withContext Result.failure(Exception("Network error creating guest: ${e.message}"))
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
                val previousUserId = authPrefs.userId
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

                // Streak strip: only wipe local UI when switching to a *different* account.
                // Same-user re-login keeps local claimed state; force refresh still re-applies server.
                try {
                    val tracker = com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(context)
                    val switchedUser = previousUserId != null &&
                        previousUserId.isNotEmpty() &&
                        previousUserId != response.user_id
                    if (switchedUser || wasGuest) {
                        Log.d(TAG, "resetForAccountSwitch (prev=$previousUserId new=${response.user_id} guest=$wasGuest)")
                        tracker.resetForAccountSwitch()
                    }
                    val dbInstance = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                    val statsRepo = com.aipoweredgita.app.repository.StatsRepository(
                        dbInstance.userStatsDao(),
                        dbInstance.dailyActivityDao(),
                        context
                    )
                    // Always force-pull balance + check-in/share so strip enables after sync
                    statsRepo.refreshUserState(response.user_id, force = true)
                    statsRepo.syncStatsToServer()
                    NotesServerSync.pullFromServer(context)
                    Log.d(TAG, "post-login streak revision=${tracker.revision}")
                } catch (e: Exception) {
                    Log.e(TAG, "Stats/notes/streak sync failed during login", e)
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
        val prevUid = authPrefs.userId
        authPrefs.clearLoginState()
        try {
            // Drop coin history cache so next guest session never shows prior user txs
            prevUid?.let { com.aipoweredgita.app.coin.CoinTransactionLogger.clear(context, it) }
            com.aipoweredgita.app.coin.CoinTransactionLogger.clear(context, "")
            val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
            db.userStatsDao().updateUserId("")
            db.userStatsDao().updateProfile("", "")
            db.userStatsDao().insertStats(com.aipoweredgita.app.database.UserStats(id = 1, userId = "", krishnaCoins = 0, serverUpdatedAt = ""))
            db.chatSummaryDao().deleteSummary("krishna-guest-${java.time.LocalDate.now()}")
            db.voiceChatMessageDao().deleteAllMessages()
            com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(context).resetForAccountSwitch()
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
