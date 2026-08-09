# Gita App — Sync Architecture Guide

## Root Cause Summary

| Layer | Status |
|-------|--------|
| Server (Turso) | ✅ Always correct |
| Client (Room/SharedPrefs) | ❌ Selectively dropped server response fields |

The app was receiving correct data from the server but only writing `krishna_coins` to local storage, silently discarding `current_day`, `current_week`, `share_day`, `share_week`, `days_active`, `yoga_level` etc.

---

## What Was Fixed This Session

| # | File | Fix |
|---|------|-----|
| 1 | `deno-backend-hono.ts` | Auto-reconcile uses `idempotency_key` instead of date grouping |
| 2 | `deno-backend-hono.ts` | Removed streak revert from `executeCorrectionPlan` |
| 3 | `deno-backend-hono.ts` | Streak date correction (`last_checkin`/`last_share` match UTC transaction dates) |
| 4 | `DailyRewardsTracker.kt` | `syncWithServer`/`syncShareWithServer` — server is source of truth |
| 5 | `StatsRepository.kt` | `getBalance()` now syncs tracker on every balance fetch |

---

## Complete Sync Architecture

### Flow Diagram

```
USER ACTION
    │
    ▼
Repository
    ├── 1. Write optimistic value to Room (instant UI feedback)
    ├── 2. Call API endpoint
    └── 3. On response → overwrite Room with server-authoritative values
              │
              ├── coins         → userStatsDao.upsert()
              ├── yoga_level    → userStatsDao.upsert()
              ├── current_day   → dailyRewardsTracker.syncWithServer()
              └── share_day     → dailyRewardsTracker.syncShareWithServer()

PERIODIC SYNC (every 30 min, network required)
    │
    ▼
SyncWorker
    ├── GET /checkin/status
    ├── Write coins/level → Room
    └── Write day/week   → DailyRewardsTracker
```

---

## Part 1 — Backend: Add `/checkin/status` endpoint

Add to `deno-backend-hono.ts` after the existing `/checkin` route:

```typescript
app.get("/checkin/status", requireAuth, async (c) => {
  const user_id = c.get("userId");
  const today = new Date().toISOString().split("T")[0];

  const [streak, stats] = await Promise.all([
    db.execute({
      sql: `SELECT current_day, current_week, share_day, share_week,
                   last_checkin, last_share
            FROM checkin_streaks WHERE user_id = ?`,
      args: [user_id],
    }),
    db.execute({
      sql: `SELECT krishna_coins, days_active, yoga_level
            FROM user_stats WHERE user_id = ?`,
      args: [user_id],
    }),
  ]);

  if (!streak.rows.length || !stats.rows.length) {
    return c.json({ error: "User not found" }, 404);
  }

  return c.json({
    current_day:      streak.rows[0].current_day,
    current_week:     streak.rows[0].current_week,
    share_day:        streak.rows[0].share_day,
    share_week:       streak.rows[0].share_week,
    checked_in_today: streak.rows[0].last_checkin === today,
    shared_today:     streak.rows[0].last_share === today,
    krishna_coins:    stats.rows[0].krishna_coins,
    days_active:      stats.rows[0].days_active,
    yoga_level:       stats.rows[0].yoga_level,
  });
});
```

---

## Part 2 — Android: Retrofit Interface

```kotlin
@GET("checkin/status")
suspend fun getCheckinStatus(
    @Query("user_id") userId: String,
    @Header("Authorization") token: String
): CheckinStatusResponse

data class CheckinStatusResponse(
    val current_day: Int,
    val current_week: Int,
    val share_day: Int,
    val share_week: Int,
    val checked_in_today: Boolean,
    val shared_today: Boolean,
    val krishna_coins: Int,
    val days_active: Int,
    val yoga_level: Int,
)
```

---

## Part 3 — Android: SyncWorker.kt

```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = dataStore.userId.first() ?: return Result.success()
            val token  = dataStore.token.first()  ?: return Result.success()

            // Single call — gets coins + streak state together
            val status = CoinApi.retrofitService.getCheckinStatus(
                userId = userId,
                token  = "Bearer $token"
            )

            // Write coins + level to Room
            userStatsDao.upsert(
                UserStatsEntity(
                    userId       = userId,
                    krishnaCoins = status.krishna_coins,
                    daysActive   = status.days_active,
                    yogaLevel    = status.yoga_level,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )

            // Write streak state to DailyRewardsTracker (SharedPrefs)
            dailyRewardsTracker.syncWithServer(
                serverDay  = status.current_day,
                serverWeek = status.current_week
            )
            dailyRewardsTracker.syncShareWithServer(
                serverDay  = status.share_day,
                serverWeek = status.share_week
            )

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry() // WorkManager retries with backoff automatically
        }
    }
}
```

---

## Part 4 — Android: Register WorkManager

In `Application.kt` or wherever you initialise WorkManager:

```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
    repeatInterval = 30,
    repeatIntervalTimeUnit = TimeUnit.MINUTES
)
.setConstraints(
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
)
.build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "gita_sync",
    ExistingPeriodicWorkPolicy.KEEP,
    syncRequest
)
```

---

## Part 5 — Android: DTO → Entity Mapper

One mapper used everywhere — never write individual field setters:

```kotlin
fun BalanceResponse.toEntity(userId: String) = UserStatsEntity(
    userId       = userId,
    krishnaCoins = krishna_coins,
    daysActive   = days_active,
    yogaLevel    = yoga_level,
    multiplier   = multiplier,
    lastSyncedAt = System.currentTimeMillis()
)
```

Use like this in every call site:

```kotlin
val balance = api.getBalance(userId)
userStatsDao.upsert(balance.toEntity(userId))
```

---

## Part 6 — Android: refreshUserState() — Single Sync Entry Point

```kotlin
// StatsRepository.kt
suspend fun refreshUserState(userId: String, token: String) {
    val status = api.getCheckinStatus(userId, "Bearer $token")

    userStatsDao.upsert(
        UserStatsEntity(
            userId       = userId,
            krishnaCoins = status.krishna_coins,
            daysActive   = status.days_active,
            yogaLevel    = status.yoga_level,
            lastSyncedAt = System.currentTimeMillis()
        )
    )

    dailyRewardsTracker.syncWithServer(
        serverDay  = status.current_day,
        serverWeek = status.current_week
    )
    dailyRewardsTracker.syncShareWithServer(
        serverDay  = status.share_day,
        serverWeek = status.share_week
    )
}
```

Call `refreshUserState()` from:
- Login success
- Guest claim success
- App foreground (via `DefaultLifecycleObserver`)
- After checkin/share response
- Inside `SyncWorker.doWork()`

---

## Trigger Map — When to Call What

| Trigger | Call |
|---------|------|
| Login / Register | `refreshUserState()` |
| Guest claim | `refreshUserState()` |
| App comes to foreground | `refreshUserState()` |
| After `/checkin` response | `dailyRewardsTracker.syncWithServer(response.day, response.week)` + `userStatsDao.updateCoins(response.total_coins)` |
| After `/share` response | `dailyRewardsTracker.syncShareWithServer(response.share_day, response.share_week)` |
| After `/coins/spend` response | `userStatsDao.updateCoins(response.remaining_balance)` |
| Periodic background (30 min) | `SyncWorker` → `refreshUserState()` |

---

## Golden Rules Going Forward

1. **Server response is always authoritative** — never trust accumulated local math for coins/streaks
2. **One mapper per DTO** — `toEntity()` extension, used everywhere, updated in one place
3. **One sync entry point** — `refreshUserState()` called from all triggers above
4. **Never individual field setters** — always full object `upsert()`
5. **UTC everywhere** — append `'Z'` to SQLite timestamps before parsing on both client and server
6. **Room for data, DataStore for preferences** — streak/coins go in Room, settings/token go in DataStore

---

## Current Server State (confirmed working)

```
checkin_day  = 2  ✅
share_day    = 2  ✅
last_checkin = 2026-06-13  ✅
last_share   = 2026-06-13  ✅
```
