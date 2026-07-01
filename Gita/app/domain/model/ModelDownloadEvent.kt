package com.aipoweredgita.app.domain.model

sealed class ModelDownloadEvent {
    object ClearError : ModelDownloadEvent()
    object RefreshModelStatus : ModelDownloadEvent()
    object StartDownload : ModelDownloadEvent()
    object StartManagerDownload : ModelDownloadEvent()
    data class StartSingleModelDownload(val modelName: String) : ModelDownloadEvent()
    object CancelDownload : ModelDownloadEvent()
}
