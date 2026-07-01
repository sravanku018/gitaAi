package com.aipoweredgita.app.domain.model

import android.content.res.Configuration

sealed class ScreenConfigEvent {
    data class UpdateScreenConfig(val configuration: Configuration? = null) : ScreenConfigEvent()
    data class OnConfigurationChanged(val newConfig: Configuration) : ScreenConfigEvent()
}
