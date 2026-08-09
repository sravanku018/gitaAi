package com.aipoweredgita.app.domain.model

sealed class OfflineDownloadEvent {
    object StartDownload : OfflineDownloadEvent()
    object ClearCache : OfflineDownloadEvent()
    object CheckMissingVerses : OfflineDownloadEvent()
}
