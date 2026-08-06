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
            val uid = resolveUserId(context, userId)
            val prefs = prefs(context)
            dropLegacySharedHistory(prefs)
            val key = keyFor(uid)
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
                        val objUser = obj.optString("user_id", uid)
                        if (objUser == uid && objNormSrc == normSrc && objDateStr == dateStr) {
                            return
                        }
                    } catch (_: Exception) { }
                }
            }

            val entry = JSONObject().apply {
                put("id", id)
                put("user_id", uid)
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

    private fun deduplicateJsonEntries(entries: List<JSONObject>): List<JSONObject> {
        val seenKeys = mutableSetOf<String>()
        val deduplicated = mutableListOf<JSONObject>()
        val sorted = entries.sortedBy { it.optLong("timestamp", 0L) }
        for (obj in sorted) {
            val id = obj.optString("id", "")
            val eventKey = obj.optString("eventKey", "")
            val desc = obj.optString("description", "")
            val amt = obj.optInt("amount", 0)
            val src = obj.optString("source", "")
            val normSrc = normalizeSource(src, desc)
            val ts = obj.optLong("timestamp", 0L)
            val dateStr = getEntryDateStr(ts)
            val uid = obj.optString("user_id", "")

            val key = when {
                eventKey.isNotEmpty() -> "${uid}_$eventKey"
                id.isNotEmpty() && !id.contains("-") -> "${uid}_server_id_$id"
                else -> "${uid}_${dateStr}_${normSrc}_${amt}_${desc.take(30)}"
            }

            if (seenKeys.add(key)) {
                deduplicated.add(obj)
            }
        }
        return deduplicated
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
            val uid = resolveUserId(context, userId)
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

            // Only keep local rows that belong to this user and look local-only
            // (UUID ids), so we don't reintroduce another account's leftovers.
            val existingArr = readJson(prefs, key)
            val localOnly = mutableListOf<JSONObject>()
            for (i in 0 until existingArr.length()) {
                try {
                    val obj = existingArr.getJSONObject(i)
                    val objUser = obj.optString("user_id", uid)
                    if (objUser != uid && objUser.isNotEmpty()) continue
                    val id = obj.optString("id", "")
                    val isLocalOnly = id.contains("-") // UUID from local log
                    if (isLocalOnly) {
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

    fun getHistory(context: Context, userId: String? = null): List<CoinEntry> {
        val uid = resolveUserId(context, userId)
        val prefs = prefs(context)
        dropLegacySharedHistory(prefs)
        val rawArr = readJson(prefs, keyFor(uid))
        val rawList = mutableListOf<JSONObject>()
        for (i in 0 until rawArr.length()) {
            try {
                val obj = rawArr.getJSONObject(i)
                val objUser = obj.optString("user_id", uid)
                // Strict filter: drop rows tagged for another user
                if (objUser.isNotEmpty() && objUser != uid) continue
                rawList.add(obj)
            } catch (_: JSONException) { }
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
            val uid = resolveUserId(context, userId)
            val prefs = prefs(context)
            dropLegacySharedHistory(prefs)
            prefs.edit().remove(keyFor(uid)).commit()
        }
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
