package com.aipoweredgita.app.domain.model

sealed class ActivityHistoryEvent {
    data class SelectQuizSize(val size: Int?) : ActivityHistoryEvent()
}
