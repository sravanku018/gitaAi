package com.aipoweredgita.app.util

import android.util.Log

sealed class SafeResult<out T> {
    data class Success<T>(val data: T) : SafeResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : SafeResult<Nothing>()
}

object SafeCall {
    suspend fun <T> run(
        tag: String = "SafeCall",
        operation: String = "",
        block: suspend () -> T
    ): SafeResult<T> {
        return try {
            SafeResult.Success(block())
        } catch (e: Exception) {
            Log.w(tag, "Failed${if (operation.isNotEmpty()) " [$operation]" else ""}: ${e.message}", e)
            SafeResult.Error(e.message ?: "Unknown error", e)
        }
    }
}
