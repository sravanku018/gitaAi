package com.aipoweredgita.app.repository

/**
 * Represents the current network connectivity state for UI observation.
 */
sealed class NetworkState {
    data object Idle : NetworkState()
    data class Loading(val operation: String) : NetworkState()
    data class Error(val operation: String, val message: String) : NetworkState()
    data object Success : NetworkState()
}