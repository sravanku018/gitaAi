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

    fun log(context: Context, amount: Int, description: String) {
        if (amount == 0) return
        val safeDesc = description.take(120)
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = readJson(prefs)
            val entry = JSONObject().apply {
                put("amount", amount)
                put("description", safeDesc)
                put("timestamp", System.currentTimeMillis())
                put("type", if (amount > 0) CoinTxType.EARN.name else CoinTxType.SPEND.name)
            }
            arr.put(entry)
            while (arr.length() > MAX) arr.remove(0)
            prefs.edit().putString(KEY, arr.toString()).commit()
        }
    }

    fun syncFromServer(context: Context, serverHistory: List<com.aipoweredgita.app.network.CoinHistoryEntry>) {
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Server stores timestamps in UTC ("yyyy-MM-dd HH:mm:ss" without timezone suffix).
            // We MUST parse with UTC or Java will interpret them as device local time, which
            // shifts timestamps by the device's UTC offset (e.g. IST = +5:30 → 5.5 hr error).
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }

            // Parse server entries (oldest first — server comes newest-first so reverse)
            val serverEntries = serverHistory.reversed().takeLast(MAX)

            // Find the oldest timestamp in the server data
            val oldestServerTs = serverEntries.minOfOrNull { entry ->
                try { fmt.parse(entry.created_at)?.time ?: Long.MAX_VALUE }
                catch (_: Exception) { Long.MAX_VALUE }
            } ?: Long.MAX_VALUE

            // Read existing local entries and keep only those OLDER than the oldest server entry
            // (avoids duplicates while preserving history the server no longer returns)
            // Build server entries JSON objects first
            val serverJsonEntries = serverEntries.map { entry ->
                JSONObject().apply {
                    put("amount", entry.amount)
                    put("description", entry.description.take(120))
                    val ts = try { fmt.parse(entry.created_at)?.time ?: System.currentTimeMillis() }
                             catch (_: Exception) { System.currentTimeMillis() }
                    put("timestamp", ts)
                    put("type", entry.type)
                    put("source", entry.source)
                }
            }

            val serverSignatures = mutableMapOf<String, Int>()
            serverJsonEntries.forEach { 
                val sig = "${it.optInt("amount")}_${it.optString("description").take(20)}"
                serverSignatures[sig] = serverSignatures.getOrDefault(sig, 0) + 1
            }

            val existing = readJson(prefs)
            val preservedLocal = mutableListOf<JSONObject>()
            for (i in 0 until existing.length()) {
                try {
                    val obj = existing.getJSONObject(i)
                    val ts = obj.optLong("timestamp", Long.MAX_VALUE)
                    // Keep if older than server history (fallen off the edge)
                    if (ts < oldestServerTs) {
                        preservedLocal.add(obj)
                    } else {
                        // Keep if it's a recent optimistic event NOT yet in the server history
                        val sig = "${obj.optInt("amount")}_${obj.optString("description").take(20)}"
                        val serverCount = serverSignatures.getOrDefault(sig, 0)
                        if (serverCount > 0) {
                            // Consumed by server history, drop local optimistic duplicate
                            serverSignatures[sig] = serverCount - 1
                        } else {
                            // Not in server history, keep optimistic local
                            preservedLocal.add(obj)
                        }
                    }
                } catch (_: Exception) { /* skip corrupted */ }
            }

            // Build merged array
            val mergedList = mutableListOf<JSONObject>()
            preservedLocal.forEach { mergedList.add(it) }
            serverJsonEntries.forEach { mergedList.add(it) }
            
            // Sort by timestamp (oldest first)
            mergedList.sortBy { it.optLong("timestamp", 0L) }
            
            val arr = JSONArray()
            mergedList.forEach { arr.put(it) }

            // Trim to MAX keeping the newest
            while (arr.length() > MAX) arr.remove(0)
            prefs.edit().putString(KEY, arr.toString()).commit()
        }
    }

    fun getHistory(context: Context): List<CoinEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = readJson(prefs)
        val result = mutableListOf<CoinEntry>()
        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
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
