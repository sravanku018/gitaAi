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
            prefs.edit().putString(KEY, arr.toString()).apply()
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
                .edit().remove(KEY).apply()
        }
    }

    private fun readJson(prefs: android.content.SharedPreferences): JSONArray {
        val raw = prefs.getString(KEY, null) ?: return JSONArray()
        return try { JSONArray(raw) }
        catch (e: JSONException) {
            Log.w(TAG, "Corrupted transaction log, resetting: ${e.message}")
            prefs.edit().remove(KEY).apply()
            JSONArray()
        }
    }
}
