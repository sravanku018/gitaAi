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

    fun log(context: Context, amount: Int, description: String, source: String = "") {
        if (amount == 0) return
        val safeDesc = description.take(120)
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = readJson(prefs)
            val nowMs = System.currentTimeMillis()
            val dateStr = getEntryDateStr(nowMs)
            val normSrc = normalizeSource(source, safeDesc)

            if (normSrc == "checkin" || normSrc == "share") {
                val key = "${normSrc}_${amount}_${dateStr}"
                for (i in 0 until arr.length()) {
                    try {
                        val obj = arr.getJSONObject(i)
                        val objSrc = obj.optString("source", "")
                        val objDesc = obj.optString("description", "")
                        val objAmt = obj.optInt("amount", 0)
                        val objTs = obj.optLong("timestamp", 0L)
                        val objDateStr = getEntryDateStr(objTs)
                        val objNormSrc = normalizeSource(objSrc, objDesc)
                        if ("${objNormSrc}_${objAmt}_${objDateStr}" == key) {
                            return
                        }
                    } catch (_: Exception) { }
                }
            }

            val entry = JSONObject().apply {
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
            src.contains("check") || desc.contains("check") -> "checkin"
            src.contains("share") || desc.contains("share") -> "share"
            src.contains("quiz") || desc.contains("quiz") -> "quiz"
            src.contains("voice") || desc.contains("voice") -> "voice"
            src.contains("chapter") || desc.contains("chapter") -> "chapter"
            src.contains("signup") || src.contains("welcome") || desc.contains("welcome") -> "signup"
            else -> src.ifEmpty { desc.take(20) }
        }
    }

    private fun deduplicateJsonEntries(entries: List<JSONObject>): List<JSONObject> {
        val seen = mutableSetOf<String>()
        val deduplicated = mutableListOf<JSONObject>()
        // Sort newest first to keep the newest entry of duplicates
        val sorted = entries.sortedByDescending { it.optLong("timestamp", 0L) }
        for (obj in sorted) {
            val src = obj.optString("source", "")
            val desc = obj.optString("description", "")
            val amount = obj.optInt("amount", 0)
            val ts = obj.optLong("timestamp", 0L)
            val dateStr = getEntryDateStr(ts)
            val normSrc = normalizeSource(src, desc)
            val key = "${normSrc}_${amount}_${dateStr}"
            if (seen.add(key)) {
                deduplicated.add(obj)
            }
        }
        return deduplicated.sortedBy { it.optLong("timestamp", 0L) }
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
                    put("amount", entry.amount)
                    put("description", entry.description.take(120))
                    val ts = try { com.aipoweredgita.app.ui.screens.coinhistory.parseDateRobust(entry.created_at)?.time ?: System.currentTimeMillis() }
                             catch (_: Exception) { System.currentTimeMillis() }
                    put("timestamp", ts)
                    put("type", entry.type)
                    put("source", entry.source)
                }
            }

            val serverSignatures = mutableMapOf<String, Int>()
            serverJsonEntries.forEach { 
                val src = it.optString("source", "")
                val desc = it.optString("description", "")
                val amount = it.optInt("amount", 0)
                val ts = it.optLong("timestamp", 0L)
                val dateStr = getEntryDateStr(ts)
                val normSrc = normalizeSource(src, desc)
                val sig = "${normSrc}_${amount}_${dateStr}"
                serverSignatures[sig] = serverSignatures.getOrDefault(sig, 0) + 1
            }

            val existing = readJson(prefs)
            val preservedLocal = mutableListOf<JSONObject>()
            for (i in 0 until existing.length()) {
                try {
                    val obj = existing.getJSONObject(i)
                    val ts = obj.optLong("timestamp", Long.MAX_VALUE)
                    if (ts < oldestServerTs) {
                        preservedLocal.add(obj)
                    } else {
                        val src = obj.optString("source", "")
                        val desc = obj.optString("description", "")
                        val amount = obj.optInt("amount", 0)
                        val dateStr = getEntryDateStr(ts)
                        val normSrc = normalizeSource(src, desc)
                        val sig = "${normSrc}_${amount}_${dateStr}"
                        val serverCount = serverSignatures.getOrDefault(sig, 0)
                        if (serverCount > 0) {
                            serverSignatures[sig] = serverCount - 1
                        } else {
                            preservedLocal.add(obj)
                        }
                    }
                } catch (_: Exception) { /* skip corrupted */ }
            }

            val mergedList = mutableListOf<JSONObject>()
            preservedLocal.forEach { mergedList.add(it) }
            serverJsonEntries.forEach { mergedList.add(it) }
            
            val cleanList = deduplicateJsonEntries(mergedList)
            
            val arr = JSONArray()
            cleanList.forEach { arr.put(it) }

            while (arr.length() > MAX) arr.remove(0)
            prefs.edit().putString(KEY, arr.toString()).commit()
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
                result.add(CoinEntry(
                    amount = obj.optInt("amount", 0),
                    description = obj.optString("description", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    type = try { CoinTxType.valueOf(obj.optString("type", CoinTxType.EARN.name)) }
                        catch (_: IllegalArgumentException) { CoinTxType.EARN }
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
