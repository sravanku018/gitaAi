package com.aipoweredgita.app.domain.model

sealed class VoiceChatEvent {
    data class UpdateUserInput(val input: String) : VoiceChatEvent()
    data class SendMessage(
        val text: String? = null,
        val confirmed: Boolean = true
    ) : VoiceChatEvent()
    object DismissCoinConfirmation : VoiceChatEvent()
    object ConfirmAndSendMessage : VoiceChatEvent()
    object ClearChat : VoiceChatEvent()
    object StartListening : VoiceChatEvent()
    object StopListening : VoiceChatEvent()
    object StopAll : VoiceChatEvent()
    object ClearError : VoiceChatEvent()
}
