package com.aipoweredgita.app.domain.model

sealed class UiConfigEvent {
    data class UpdateFromSize(val widthDp: Int, val heightDp: Int) : UiConfigEvent()
}
