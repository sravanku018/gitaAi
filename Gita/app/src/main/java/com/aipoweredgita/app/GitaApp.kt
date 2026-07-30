package com.aipoweredgita.app

import android.app.Application
import android.util.Log
import com.aipoweredgita.app.repository.CoinReconciliationManager
import com.aipoweredgita.app.utils.AuthPreferences
import com.aipoweredgita.app.utils.DeviceTierDetector
import com.aipoweredgita.app.utils.DeviceProfile
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class GitaApp : Application() {

    lateinit var deviceProfile: DeviceProfile
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize with default fallback profile and compute actual tier off main thread
        deviceProfile = DeviceProfile.from(com.aipoweredgita.app.utils.DeviceTier.MID)

        applicationScope.launch {
            val tier = DeviceTierDetector.detectAsync(this@GitaApp)
            deviceProfile = DeviceProfile.from(tier)
            Log.d("GitaApp", "Device tier=$tier profile=$deviceProfile")
        }

        // Run auto-reconciliation on app startup for logged-in users
        runAutoReconciliation()
    }

    /**
     * Run AI-powered auto-reconciliation on app startup
     * This detects and corrects any coin discrepancies
     */
    private fun runAutoReconciliation() {
        applicationScope.launch {
            try {
                val authPrefs = AuthPreferences.getInstance(this@GitaApp)
                if (authPrefs.isGuestUser || authPrefs.userId == null) {
                    return@launch
                }

                val reconciliationManager = CoinReconciliationManager(this@GitaApp)

                val result = reconciliationManager.autoReconcile()
                when (result) {
                    is com.aipoweredgita.app.repository.AutoReconciliationResult.Corrected -> {
                        Log.w("GitaApp", "AI corrected: ${result.oldBalance} → ${result.newBalance}")
                        Log.w("GitaApp", "Anomalies: ${result.anomalies.joinToString(", ")}")
                        Log.w("GitaApp", "Corrections: ${result.corrections.joinToString(", ")}")
                    }
                    is com.aipoweredgita.app.repository.AutoReconciliationResult.Error -> {
                        Log.e("GitaApp", "AI reconciliation failed: ${result.message}")
                    }
                    is com.aipoweredgita.app.repository.AutoReconciliationResult.Skip -> {
                        Log.d("GitaApp", "AI reconciliation skipped: ${result.reason}")
                    }
                    is com.aipoweredgita.app.repository.AutoReconciliationResult.OK -> {
                        Log.d("GitaApp", "Balance OK (${result.balance})")
                    }
                }
            } catch (e: Exception) {
                Log.e("GitaApp", "Auto-reconciliation error: ${e.message}")
            }
        }
    }

    companion object {
        lateinit var instance: GitaApp
            private set
    }
}
