package com.aipoweredgita.app.coin

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.utils.AuthPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Persists coin transaction history in SharedPreferences, scoped per user.
 *
 * Each profile/guest has its own key (`tx_<userId>`) so history never mixes
 * across logins. Legacy unscoped `transactions` key is discarded on access.
 *
 * Thread-safe via synchronized. Handles JSON corruption gracefully — silently
 * resets to empty rather than crashing.
 */
object CoinTransactionLogger {
    private const val PREFS_NAME = "coin_tracker"
    private const val KEY_LEGACY = "transactions"
    /** Stable store for all guest sessions — guest UUIDs change, history must not vanish. */
    private const val GUEST_STORE_UID = "GUEST_SESSION"
    private const val MAX = 200
    private const val TAG = "CoinTxLogger"

    private fun resolveUserId(context: Context, explicitUserId: String? = null): String {
        val explicit = explicitUserId?.trim().orEmpty()
        if (explicit.isNotEmpty()) return explicit
        return try {
            AuthPreferences.getInstance(context).userId?.trim().orEmpty()
        } catch (_: Exception) {
            ""
        }.ifEmpty { "anonymous" }
    }

    private fun isGuestContext(context: Context): Boolean {
        return try {
            AuthPreferences.getInstance(context).isGuestUser
        } catch (_: Exception) {
            false
        }
    }

    /** Map guest UUIDs / guest sessions onto one stable SharedPreferences key. */
    private fun storageUid(context: Context, explicitUserId: String? = null): String {
        val uid = resolveUserId(context, explicitUserId)
        if (uid == GUEST_STORE_UID) return GUEST_STORE_UID
        if (uid.startsWith("guest_")) return GUEST_STORE_UID
        if (isGuestContext(context)) return GUEST_STORE_UID
        return uid
    }

    private fun keyFor(userId: String): String = "tx_$userId"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Drop pre-per-user shared history so old multi-profile mixes cannot leak. */
    private fun dropLegacySharedHistory(prefs: android.content.SharedPreferences) {
        if (prefs.contains(KEY_LEGACY)) {
            prefs.edit().remove(KEY_LEGACY).commit()
            Log.i(TAG, "Removed legacy shared coin history (was not user-scoped)")
        }
    }

    fun log(
        context: Context,
        amount: Int,
        description: String,
        source: String = "",
        eventKey: String? = null,
        id: String = java.util.UUID.randomUUID().toString(),
        userId: String? = null
    ) {
        val safeDesc = description.take(120)
        val normSrc = normalizeSource(source, safeDesc)
        if (amount == 0 && normSrc != "checkin_daily" && normSrc != "share_daily") return
        synchronized(this) {
            val logicalUid = resolveUserId(context, userId)
            val uid = storageUid(context, userId)
            val prefs = prefs(context)
            dropLegacySharedHistory(prefs)
            val key = keyFor(uid)
            Log.d(TAG, "log amount=$amount src=$normSrc logical=$logicalUid store=$uid")
            val arr = readJson(prefs, key)
            val nowMs = System.currentTimeMillis()
            val dateStr = getEntryDateStr(nowMs)

            if (normSrc == "checkin_daily" || normSrc == "share_daily") {
                for (i in 0 until arr.length()) {
                    try {
                        val obj = arr.getJSONObject(i)
                        val objSrc = obj.optString("source", "")
                        val objDesc = obj.optString("description", "")
                        val objTs = obj.optLong("timestamp", 0L)
                        val objDateStr = getEntryDateStr(objTs)
                        val objNormSrc = normalizeSource(objSrc, objDesc)
                        // Same-day source dedupe within this storage bucket (guest uses stable key)
                        if (objNormSrc == normSrc && objDateStr == dateStr) {
                            return
                        }
                    } catch (_: Exception) { }
                }
            }

            val entry = JSONObject().apply {
                put("id", id)
                put("user_id", logicalUid)
                if (!eventKey.isNullOrEmpty()) put("eventKey", eventKey)
                put("amount", amount)
                put("description", safeDesc)
                put("timestamp", nowMs)
                put("type", if (amount > 0) CoinTxType.EARN.name else CoinTxType.SPEND.name)
                if (source.isNotEmpty()) put("source", source)
            }
            arr.put(entry)
            while (arr.length() > MAX) arr.remove(0)
            prefs.edit().putString(key, arr.toString()).commit()
        }
    }

    private fun getEntryDateStr(ts: Long): String {
        return try {
            java.time.Instant.ofEpochMilli(ts)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        } catch (_: Exception) { "" }
    }

    private fun normalizeSource(source: String, description: String): String {
        val src = source.lowercase()
        val desc = description.lowercase()
        return when {
            src == "checkin_day7_bonus" || desc.contains("7-day check-in") || desc.contains("day 7 check-in") -> "checkin_day7_bonus"
            src == "share_day7_bonus" || desc.contains("7-day share") || desc.contains("day 7 share") -> "share_day7_bonus"
            src.contains("check") || desc.contains("check") -> "checkin_daily"
            src.contains("share") || desc.contains("share") -> "share_daily"
            src.contains("quiz") || desc.contains("quiz") -> "quiz"
            src.contains("voice") || desc.contains("voice") -> "voice"
            src.contains("chapter") || desc.contains("chapter") -> "chapter"
            src.contains("signup") || src.contains("welcome") || desc.contains("welcome") -> "signup"
            else -> src.ifEmpty { desc.take(20) }
        }
    }

    /** Server numeric ids vs local UUID (contains '-'). */
    private fun isLocalUuidId(id: String): Boolean = id.contains("-")

    /**
     * Drop optimistic local rows once the same earn exists on the server.
     * Local quiz log uses "5base + 6acc = 11"; server uses "Quiz (general): 9/10"
     * and may multiply by yoga — old dedupe keys never matched → double lines.
     */
    private fun isLocalRowCoveredByServer(local: JSONObject, serverRows: List<JSONObject>): Boolean {
        val localId = local.optString("id", "")
        if (!isLocalUuidId(localId)) return false
        val localDesc = local.optString("description", "")
        val localSrc = normalizeSource(local.optString("source", ""), localDesc)
        val localTs = local.optLong("timestamp", 0L)
        val localAmt = kotlin.math.abs(local.optInt("amount", 0))
        val windowMs = 30 * 60 * 1000L // 30 min — local log then SyncWorker award

        return serverRows.any { server ->
            val sid = server.optString("id", "")
            if (isLocalUuidId(sid)) return@any false
            val sDesc = server.optString("description", "")
            val sSrc = normalizeSource(server.optString("source", ""), sDesc)
            if (sSrc != localSrc) return@any false
            val sTs = server.optLong("timestamp", 0L)
            if (kotlin.math.abs(sTs - localTs) > windowMs) return@any false
            val sAmt = kotlin.math.abs(server.optInt("amount", 0))
            // Same amount, or server applied yoga multiplier to local base
            localAmt == sAmt ||
                (localAmt > 0 && sAmt % localAmt == 0) ||
                (sAmt > 0 && localAmt % sAmt == 0) ||
                // quiz/battle/chapter: same-source near time is enough (desc/amount always differ)
                localSrc in setOf("quiz", "chapter", "battle_quiz", "battle")
        }
    }

    private fun deduplicateJsonEntries(entries: List<JSONObject>): List<JSONObject> {
        val sorted = entries.sortedBy { it.optLong("timestamp", 0L) }
        val serverRows = sorted.filter { !isLocalUuidId(it.optString("id", "")) }
        val seenKeys = mutableSetOf<String>()
        val deduplicated = mutableListOf<JSONObject>()

        // Prefer server rows first so fuzzy keys prefer them
        val ordered = serverRows + sorted.filter { isLocalUuidId(it.optString("id", "")) }

        for (obj in ordered) {
            val id = obj.optString("id", "")
            val eventKey = obj.optString("eventKey", "")
            val desc = obj.optString("description", "")
            val amt = obj.optInt("amount", 0)
            val src = obj.optString("source", "")
            val normSrc = normalizeSource(src, desc)
            val ts = obj.optLong("timestamp", 0L)
            val dateStr = getEntryDateStr(ts)
            val uid = obj.optString("user_id", "")

            // Drop local optimistic earn once server counterpart exists
            if (isLocalUuidId(id) && isLocalRowCoveredByServer(obj, serverRows)) {
                continue
            }

            val key = when {
                eventKey.isNotEmpty() -> "${uid}_$eventKey"
                id.isNotEmpty() && !isLocalUuidId(id) -> "${uid}_server_id_$id"
                // Same day + source for local previews (without amount/desc) after server pass
                isLocalUuidId(id) -> "${uid}_${dateStr}_${normSrc}_local_${amt}_${desc.take(30)}"
                else -> "${uid}_${dateStr}_${normSrc}_${amt}_${desc.take(30)}"
            }

            if (seenKeys.add(key)) {
                deduplicated.add(obj)
            }
        }
        return deduplicated.sortedBy { it.optLong("timestamp", 0L) }
    }

    /**
     * Merge server history into this user's local store only.
     * Never touches other users' keys.
     */
    fun syncFromServer(
        context: Context,
        serverHistory: List<com.aipoweredgita.app.network.CoinHistoryEntry>,
        userId: String? = null
    ) {
        synchronized(this) {
            val uid = storageUid(context, userId)
            // Never pull server history into the guest bucket
            if (uid == GUEST_STORE_UID) return
            val prefs = prefs(context)
            dropLegacySharedHistory(prefs)
            val key = keyFor(uid)
            val serverEntries = serverHistory.reversed().takeLast(MAX)

            val serverJsonEntries = serverEntries.map { entry ->
                val isSpendEntry = entry.type.equals("SPEND", ignoreCase = true) || entry.amount < 0
                val signedAmt = if (isSpendEntry) -kotlin.math.abs(entry.amount) else kotlin.math.abs(entry.amount)
                val txType = if (isSpendEntry) CoinTxType.SPEND.name else CoinTxType.EARN.name

                JSONObject().apply {
                    put("id", entry.id.toString())
                    put("user_id", uid)
                    put("amount", signedAmt)
                    put("description", entry.description.take(120))
                    val ts = try {
                        com.aipoweredgita.app.ui.screens.coinhistory.parseDateRobust(entry.created_at)?.time
                            ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    put("timestamp", ts)
                    put("type", txType)
                    val rawDesc = entry.description
                    val src = entry.source.ifEmpty {
                        when {
                            rawDesc.contains("battle", ignoreCase = true) -> "battle_quiz"
                            rawDesc.contains("quiz", ignoreCase = true) -> "quiz_completion"
                            rawDesc.contains("check", ignoreCase = true) -> "checkin_daily"
                            rawDesc.contains("share", ignoreCase = true) -> "share_daily"
                            rawDesc.contains("voice", ignoreCase = true) ||
                                rawDesc.contains("asked", ignoreCase = true) -> "voice_chat"
                            else -> "server_sync"
                        }
                    }
                    put("source", src)
                }
            }

            // Prefer dropping covered local previews, then store merge (legacy path).
            val existingArr = readJson(prefs, key)
            val localOnly = mutableListOf<JSONObject>()
            for (i in 0 until existingArr.length()) {
                try {
                    val obj = existingArr.getJSONObject(i)
                    val objUser = obj.optString("user_id", uid)
                    if (objUser != uid && objUser.isNotEmpty()) continue
                    val id = obj.optString("id", "")
                    if (isLocalUuidId(id) && !isLocalRowCoveredByServer(obj, serverJsonEntries)) {
                        obj.put("user_id", uid)
                        localOnly.add(obj)
                    }
                } catch (_: Exception) { }
            }

            val merged = deduplicateJsonEntries(localOnly + serverJsonEntries)
            val finalArr = JSONArray()
            merged.takeLast(MAX).forEach { finalArr.put(it) }
            prefs.edit().putString(key, finalArr.toString()).commit()
        }
    }

    /**
     * Signed-in source of truth: replace local cache with server history only.
     * Prevents double lines (local optimistic + server award) for one quiz/battle.
     */
    fun replaceWithServerHistory(
        context: Context,
        serverHistory: List<com.aipoweredgita.app.network.CoinHistoryEntry>,
        userId: String? = null
    ) {
        synchronized(this) {
            val uid = storageUid(context, userId)
            if (uid == GUEST_STORE_UID) return
            val prefs = prefs(context)
            dropLegacySharedHistory(prefs)
            val key = keyFor(uid)
            val finalArr = JSONArray()
            serverHistory
                .distinctBy { if (it.id != 0) it.id else "${it.created_at}_${it.amount}_${it.description}" }
                .reversed()
                .takeLast(MAX)
                .forEach { entry ->
                    val isSpendEntry = entry.type.equals("SPEND", ignoreCase = true) || entry.amount < 0
                    val signedAmt = if (isSpendEntry) -kotlin.math.abs(entry.amount) else kotlin.math.abs(entry.amount)
                    finalArr.put(JSONObject().apply {
                        put("id", entry.id.toString())
                        put("user_id", uid)
                        put("amount", signedAmt)
                        put("description", entry.description.take(120))
                        put(
                            "timestamp",
                            try {
                                com.aipoweredgita.app.ui.screens.coinhistory.parseDateRobust(entry.created_at)?.time
                                    ?: System.currentTimeMillis()
                            } catch (_: Exception) {
                                System.currentTimeMillis()
                            }
                        )
                        put("type", if (isSpendEntry) CoinTxType.SPEND.name else CoinTxType.EARN.name)
                        put("source", entry.source.ifEmpty { "server_sync" })
                    })
                }
            prefs.edit().putString(key, finalArr.toString()).commit()
        }
    }

    fun getHistory(context: Context, userId: String? = null): List<CoinEntry> {
        val logicalUid = resolveUserId(context, userId)
        val uid = storageUid(context, userId)
        val prefs = prefs(context)
        dropLegacySharedHistory(prefs)
        val rawArr = readJson(prefs, keyFor(uid))
        val rawList = mutableListOf<JSONObject>()
        for (i in 0 until rawArr.length()) {
            try {
                val obj = rawArr.getJSONObject(i)
                rawList.add(obj)
            } catch (_: JSONException) { }
        }
        // Recover orphaned guest UUID buckets (older builds wrote tx_guest_<uuid>)
        if (uid == GUEST_STORE_UID) {
            for (storedKey in prefs.all.keys) {
                if (!storedKey.startsWith("tx_guest_")) continue
                val orphan = readJson(prefs, storedKey)
                for (i in 0 until orphan.length()) {
                    try { rawList.add(orphan.getJSONObject(i)) } catch (_: JSONException) { }
                }
            }
            // Also migrate into stable key if we found orphans and stable was empty
            if (rawArr.length() == 0 && rawList.isNotEmpty()) {
                synchronized(this) {
                    val merged = JSONArray()
                    rawList.forEach { merged.put(it) }
                    prefs.edit().putString(keyFor(GUEST_STORE_UID), merged.toString()).commit()
                    Log.i(TAG, "Migrated ${rawList.size} orphan guest txs → $GUEST_STORE_UID")
                }
            }
        } else {
            // Strict filter for signed-in: drop rows tagged for another user
            val filtered = rawList.filter { obj ->
                val objUser = obj.optString("user_id", logicalUid)
                objUser.isEmpty() || objUser == logicalUid || objUser == uid
            }
            rawList.clear()
            rawList.addAll(filtered)
        }

        val cleanList = deduplicateJsonEntries(rawList)

        if (cleanList.size != rawList.size || rawArr.length() != cleanList.size) {
            synchronized(this) {
                val cleanArr = JSONArray()
                cleanList.forEach { cleanArr.put(it) }
                prefs.edit().putString(keyFor(uid), cleanArr.toString()).commit()
            }
        }

        val result = mutableListOf<CoinEntry>()
        for (obj in cleanList) {
            try {
                val idStr = obj.optString("id", "").ifEmpty { java.util.UUID.randomUUID().toString() }
                val eventKeyStr = obj.optString("eventKey", "").ifEmpty { null }
                val srcStr = obj.optString("source", "").ifEmpty { null }
                result.add(
                    CoinEntry(
                        id = idStr,
                        eventKey = eventKeyStr,
                        amount = obj.optInt("amount", 0),
                        description = obj.optString("description", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        type = try {
                            CoinTxType.valueOf(obj.optString("type", CoinTxType.EARN.name))
                        } catch (_: IllegalArgumentException) {
                            CoinTxType.EARN
                        },
                        source = srcStr
                    )
                )
            } catch (_: JSONException) {
                // skip corrupted entry
            }
        }
        return result.reversed()
    }

    fun clear(context: Context, userId: String? = null) {
        synchronized(this) {
            val logical = resolveUserId(context, userId)
            val uid = storageUid(context, userId)
            val prefs = prefs(context)
            dropLegacySharedHistory(prefs)
            val ed = prefs.edit().remove(keyFor(uid))
            // Clearing a guest also wipes stable bucket + any orphan guest_* keys
            if (uid == GUEST_STORE_UID || logical.startsWith("guest_") || isGuestContext(context)) {
                ed.remove(keyFor(GUEST_STORE_UID))
                for (storedKey in prefs.all.keys.toList()) {
                    if (storedKey.startsWith("tx_guest_")) ed.remove(storedKey)
                }
            }
            if (logical.isNotEmpty() && logical != uid) {
                ed.remove(keyFor(logical))
            }
            ed.commit()
            Log.d(TAG, "clear store=$uid logical=$logical")
        }
    }

    /** Ensure guest has at least a welcome line; returns true if anything was written. */
    fun ensureGuestWelcome(context: Context, amount: Int = 50, userId: String? = null): Boolean {
        val store = storageUid(context, userId)
        if (store != GUEST_STORE_UID && !resolveUserId(context, userId).startsWith("guest_")) {
            // Force guest store if caller knows this is guest
            if (!isGuestContext(context)) return false
        }
        if (getHistory(context, userId).isNotEmpty()) return false
        log(
            context,
            amount.coerceAtLeast(1),
            "Welcome bonus (guest)",
            source = "signup",
            userId = userId ?: GUEST_STORE_UID
        )
        return true
    }

    private fun readJson(prefs: android.content.SharedPreferences, key: String): JSONArray {
        val raw = prefs.getString(key, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (e: JSONException) {
            Log.w(TAG, "Corrupted transaction log for $key, resetting: ${e.message}")
            prefs.edit().remove(key).commit()
            JSONArray()
        }
    }
}
