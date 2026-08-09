package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.network.AutoReconcileRequest
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.utils.AuthPreferences

/**
 * Coin reconciliation mechanism
 * Detects and corrects balance mismatches between client and server
 * Uses Groq AI analysis for anomaly detection — with rule-based fallback
 */
class CoinReconciliationManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "CoinReconciliation"
    }

    /**
     * Groq-powered auto-reconciliation.
     * Sends user_id + auth token → backend deletes duplicate rows + corrects balance.
     * Call on login and periodically (e.g. once per session).
     */
    suspend fun autoReconcile(): AutoReconciliationResult {
        val authPrefs = AuthPreferences.getInstance(context)

        if (authPrefs.isGuestUser) {
            return AutoReconciliationResult.Skip("Guest user — using local coins")
        }

        val userId = authPrefs.userId
            ?: return AutoReconciliationResult.Skip("No user ID")
        val token = authPrefs.token
            ?: return AutoReconciliationResult.Skip("No auth token")

        return try {
            val response = CoinApi.retrofitService.autoReconcile(
                token   = "Bearer $token",
                request = AutoReconcileRequest(user_id = userId)
            )

            if (response.delta_applied != 0 || response.rows_deleted > 0) {
                // Sync corrected balance locally
                val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                db.userStatsDao().updateKrishnaCoins(response.corrected_balance)

                val correctionSummary = response.corrections_applied
                    .joinToString("; ") { it.reason }

                Log.w(TAG, "Auto-reconciled: ${response.current_balance} → ${response.corrected_balance}")
                Log.w(TAG, "Engine: ${response.engine}")
                Log.w(TAG, "Anomalies: ${response.anomalies_detected}")
                Log.w(TAG, "Fixes: $correctionSummary")

                AutoReconciliationResult.Corrected(
                    oldBalance  = response.current_balance,
                    newBalance  = response.corrected_balance,
                    anomalies   = response.anomalies.map { "${it.type}: ${it.description}" },
                    groqAnalysis = response.groq_analysis?.analysis,
                    corrections = response.corrections_applied.map { it.reason },
                    engine      = response.engine,
                )
            } else {
                Log.d(TAG, "Balance OK (${response.current_balance}) — engine: ${response.engine}")
                AutoReconciliationResult.OK(
                    balance          = response.current_balance,
                    anomaliesChecked = response.anomalies_detected,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-reconciliation failed: ${e.message}")
            AutoReconciliationResult.Error(e.message ?: "Unknown error")
        }
    }
}

// ─── Result sealed classes ────────────────────────────────────

sealed class AutoReconciliationResult {
    data class OK(
        val balance: Int,
        val anomaliesChecked: Int,
    ) : AutoReconciliationResult()

    data class Corrected(
        val oldBalance:   Int,
        val newBalance:   Int,
        val anomalies:    List<String>,
        val groqAnalysis: String?,
        val corrections:  List<String>,
        val engine:       String,
    ) : AutoReconciliationResult()

    data class Skip(val reason: String)  : AutoReconciliationResult()
    data class Error(val message: String): AutoReconciliationResult()
}
