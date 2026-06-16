package com.aipoweredgita.app.domain.model

sealed class NormalModeSideEffect {
    data class ShowMessage(val message: String) : NormalModeSideEffect()
}
