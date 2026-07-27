package com.aipoweredgita.app.coin

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Persists coin transaction history in SharedPreferences.
 * Thread-safe via synchronized. Handles JSON corruption gracefully — silently
 * resets to empty rather than crashing.
 */
object CoinTransactionLogger {
    private const val PREFS_NAME = "coin_tracker"
    private const val KEY = "transactions"
    private const val MAX = 200
    private const val TAG = "CoinTxLogger"

    fun log(context: Context, amount: Int, description: String, source: String = "", eventKey: String? = null, id: String = java.util.UUID.randomUUID().toString()) {
        if (amount == 0) return
        val safeDesc = description.take(120)
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = readJson(prefs)
            val nowMs = System.currentTimeMillis()
            val dateStr = getEntryDateStr(nowMs)
            val normSrc = normalizeSource(source, safeDesc)

            if (normSrc == "checkin_daily" || normSrc == "share_daily") {
                for (i in 0 until arr.length()) {
                    try {
                        val obj = arr.getJSONObject(i)
                        val objSrc = obj.optString("source", "")
                        val objDesc = obj.optString("description", "")
                        val objTs = obj.optLong("timestamp", 0L)
                        val objDateStr = getEntryDateStr(objTs)
                        val objNormSrc = normalizeSource(objSrc, objDesc)
                        if (objNormSrc == normSrc && objDateStr == dateStr) {
                            return
                        }
                    } catch (_: Exception) { }
                }
            }

            val entry = JSONObject().apply {
                put("id", id)
                if (!eventKey.isNullOrEmpty()) put("eventKey", eventKey)
                put("amount", amount)
                put("description", safeDesc)
                put("timestamp", nowMs)
                put("type", if (amount > 0) CoinTxType.EARN.name else CoinTxType.SPEND.name)
                if (source.isNotEmpty()) put("source", source)
            }
            arr.put(entry)
            while (arr.length() > MAX) arr.remove(0)
            prefs.edit().putString(KEY, arr.toString()).commit()
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
            val key = eventKey.ifEmpty { id }
            if (key.isNotEmpty()) {
                if (seenKeys.add(key)) {
                    deduplicated.add(obj)
                }
            } else {
                deduplicated.add(obj)
            }
        }
        return deduplicated
    }

    fun syncFromServer(context: Context, serverHistory: List<com.aipoweredgita.app.network.CoinHistoryEntry>) {
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val serverEntries = serverHistory.reversed().takeLast(MAX)

            val oldestServerTs = serverEntries.minOfOrNull { entry ->
                try { com.aipoweredgita.app.ui.screens.coinhistory.parseDateRobust(entry.created_at)?.time ?: Long.MAX_VALUE }
                catch (_: Exception) { Long.MAX_VALUE }
            } ?: Long.MAX_VALUE

            val serverJsonEntries = serverEntries.map { entry ->
                JSONObject().apply {
                    put("id", entry.id?.toString() ?: java.util.UUID.randomUUID().toString())
                    put("amount", entry.amount)
                    put("description", entry.description.take(120))
                    val ts = try { com.aipoweredgita.app.ui.screens.coinhistory.parseDateRobust(entry.created_at)?.time ?: System.currentTimeMillis() }
                             catch (_: Exception) { System.currentTimeMillis() }
                    put("timestamp", ts)
                    put("type", if (entry.amount > 0) CoinTxType.EARN.name else CoinTxType.SPEND.name)
                    val rawDesc = entry.description
                    val src = when {
                        rawDesc.contains("battle", ignoreCase = true) -> "battle_quiz"
                        rawDesc.contains("quiz", ignoreCase = true) -> "quiz_completion"
                        rawDesc.contains("check", ignoreCase = true) -> "checkin_daily"
                        rawDesc.contains("share", ignoreCase = true) -> "share_daily"
                        else -> "server_sync"
                    }
                    put("source", src)
                }
            }

            val existingArr = readJson(prefs)
            val localOnlyList = mutableListOf<JSONObject>()
            for (i in 0 until existingArr.length()) {
                try {
                    val obj = existingArr.getJSONObject(i)
                    val ts = obj.optLong("timestamp", 0L)
                    if (ts < oldestServerTs) {
                        localOnlyList.add(obj)
                    }
                } catch (_: Exception) { }
            }

            val merged = deduplicateJsonEntries(localOnlyList + serverJsonEntries)
            val finalArr = JSONArray()
            merged.takeLast(MAX).forEach { finalArr.put(it) }
            prefs.edit().putString(KEY, finalArr.toString()).commit()
        }
    }

    fun getHistory(context: Context): List<CoinEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawArr = readJson(prefs)
        val rawList = mutableListOf<JSONObject>()
        for (i in 0 until rawArr.length()) {
            try {
                rawList.add(rawArr.getJSONObject(i))
            } catch (_: JSONException) { }
        }

        val cleanList = deduplicateJsonEntries(rawList)

        if (cleanList.size < rawList.size) {
            synchronized(this) {
                val cleanArr = JSONArray()
                cleanList.forEach { cleanArr.put(it) }
                prefs.edit().putString(KEY, cleanArr.toString()).commit()
            }
        }

        val result = mutableListOf<CoinEntry>()
        for (obj in cleanList) {
            try {
                val idStr = obj.optString("id", "").ifEmpty { java.util.UUID.randomUUID().toString() }
                val eventKeyStr = obj.optString("eventKey", "").ifEmpty { null }
                val srcStr = obj.optString("source", "").ifEmpty { null }
                result.add(CoinEntry(
                    id = idStr,
                    eventKey = eventKeyStr,
                    amount = obj.optInt("amount", 0),
                    description = obj.optString("description", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    type = try { CoinTxType.valueOf(obj.optString("type", CoinTxType.EARN.name)) }
                        catch (_: IllegalArgumentException) { CoinTxType.EARN },
                    source = srcStr
                ))
            } catch (_: JSONException) {
                // skip corrupted entry
            }
        }
        return result.reversed()
    }

    fun clear(context: Context) {
        synchronized(this) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY).commit()
        }
    }

    private fun readJson(prefs: android.content.SharedPreferences): JSONArray {
        val raw = prefs.getString(KEY, null) ?: return JSONArray()
        return try { JSONArray(raw) }
        catch (e: JSONException) {
            Log.w(TAG, "Corrupted transaction log, resetting: ${e.message}")
            prefs.edit().remove(KEY).commit()
            JSONArray()
        }
    }
}
