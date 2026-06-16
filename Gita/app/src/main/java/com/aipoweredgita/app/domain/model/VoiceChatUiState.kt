package com.aipoweredgita.app.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.util.UUID

@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class VoiceChatErrorType {
    MODEL_INIT, LLM_INFERENCE, STT, TTS, NETWORK, CRASH_RECOVERY
}

enum class CoinError { NETWORK_ERROR, UNKNOWN_ERROR }

@Stable
data class VoiceChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isThinking: Boolean = false,
    val liveTranscript: String = "",
    val userInput: String = "",
    val isLlmReady: Boolean = false,
    val error: String? = null,
    val errorType: VoiceChatErrorType? = null,
    val currentModelName: String = "Unknown",
    val coinBalance: Int = 0,
    val coinError: CoinError? = null,
    val showCoinConfirmation: Boolean = false,
    val pendingMessage: String? = null,
    val pendingCost: Int = 0,
    val isBalanceLoaded: Boolean = false
)
