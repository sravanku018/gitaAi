package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.database.VoiceChatMessage

/**
 * UI State for Chat Screen
 * Single source of truth for all chat-related UI state
 */
data class ChatUiState(
    val messages: List<VoiceChatMessage> = emptyList(),
    override val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    override val error: String? = null,
    val coinBalance: Int = 0,
    val canSendMessage: Boolean = true
) : BaseUiState

/**
 * Events that can occur on the Chat screen
 */
sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class DeleteMessage(val id: String) : ChatEvent()
    data object ClearChat : ChatEvent()
    data object StartRecording : ChatEvent()
    data object StopRecording : ChatEvent()
    data object Retry : ChatEvent()
    data object RefreshCoins : ChatEvent()
}

/**
 * One-time side effects for the Chat screen
 */
sealed class ChatSideEffect {
    data class ShowToast(val message: String) : ChatSideEffect()
    data class ShowError(val message: String) : ChatSideEffect()
    data object NavigateToCoinHistory : ChatSideEffect()
    data object InsufficientCoins : ChatSideEffect()
}
