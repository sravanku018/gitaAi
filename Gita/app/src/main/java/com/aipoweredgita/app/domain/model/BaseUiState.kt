package com.aipoweredgita.app.domain.model

/**
 * Base interface for all UI states
 * Provides common properties for loading and error states
 */
interface BaseUiState {
    val isLoading: Boolean
    val error: String?
}

/**
 * Extension function to check if the state has an error
 */
fun BaseUiState.hasError(): Boolean = error != null

/**
 * Extension function to check if the state is loading
 */
fun BaseUiState.isIdle(): Boolean = !isLoading && !hasError()
