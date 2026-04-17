package com.aipoweredgita.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    /**
     * Returns a Flow that emits network connectivity status.
     * Properly registers and unregisters NetworkCallback.
     *
     * @param context Application context
     * @return Flow<Boolean> - true when connected, false when disconnected
     */
    fun networkStatusFlow(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Send initial state immediately
        trySend(isNetworkAvailable(context))

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                trySend(true)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                Log.d(TAG, "Capabilities changed - Internet: $hasInternet, Validated: $hasValidated")
                trySend(hasInternet && hasValidated)
            }

            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                trySend(false)
            }
        }

        // Register callback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ - register default network callback
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                // Android 6.0 and below - use NetworkRequest
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            }
            Log.d(TAG, "Network callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            // Send false on registration failure
            trySend(false)
        }

        // Unregister callback when flow is cancelled
        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                Log.d(TAG, "Network callback unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
    }.distinctUntilChanged() // Only emit when status actually changes

    /**
     * Check current network availability synchronously.
     * Use this for one-time checks, not for monitoring.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // Android 5.x and below (deprecated but still needed for older devices)
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected == true
        }
    }

    /**
     * Check if connected to unmetered network (WiFi, Ethernet).
     * Useful for download tasks that should only run on WiFi.
     */
    fun isUnmeteredNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.type == ConnectivityManager.TYPE_WIFI ||
            @Suppress("DEPRECATION")
            networkInfo?.type == ConnectivityManager.TYPE_ETHERNET
        }
    }

    /**
     * Get detailed network type information.
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return NetworkType.NONE

            return when (networkInfo.type) {
                @Suppress("DEPRECATION")
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                @Suppress("DEPRECATION")
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                @Suppress("DEPRECATION")
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        }
    }

    /**
     * Get cellular generation (4G/LTE, 5G) when on mobile data.
     * Returns null if not on cellular network.
     */
    fun getCellularGeneration(context: Context): CellularGeneration? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            if (networkInfo?.type != ConnectivityManager.TYPE_MOBILE) return null

            @Suppress("DEPRECATION")
            return when (networkInfo.subtype) {
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
                android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0,
                android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A -> CellularGeneration.FOUR_G
                else -> null
            }
        }

        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return null
        }

        // Check for 5G capability
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            // Not metered means it's likely unmetered cellular (rare, but possible)
        }

        // Use telephony manager for precise generation detection
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                ?: return CellularGeneration.FOUR_G // Default to 4G if we can't determine

            val dataNetworkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                telephonyManager.dataNetworkType
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.networkType
            }

            return when (dataNetworkType) {
                android.telephony.TelephonyManager.NETWORK_TYPE_NR,
                20, // NETWORK_TYPE_NR (API 29+)
                21 -> CellularGeneration.FIVE_G // NETWORK_TYPE_NR_NEW

                android.telephony.TelephonyManager.NETWORK_TYPE_LTE,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
                android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0,
                android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A,
                android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD -> CellularGeneration.FOUR_G

                else -> CellularGeneration.FOUR_G // Default to 4G for unknown types
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect cellular generation: ${e.message}")
            return null
        }
    }

    enum class CellularGeneration {
        FOUR_G,
        FIVE_G
    }

    enum class NetworkType {
        NONE,
        WIFI,
        CELLULAR,
        ETHERNET,
        OTHER
    }
}
