package com.aipoweredgita.app.domain.model

sealed class VoiceChatSideEffect {
    data class ShowToast(val message: String) : VoiceChatSideEffect()
}
