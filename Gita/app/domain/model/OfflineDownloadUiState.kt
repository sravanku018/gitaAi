package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.repository.DownloadProgress
import com.aipoweredgita.app.repository.DownloadStatus
import com.aipoweredgita.app.util.GitaConstants

data class OfflineDownloadUiState(
    val downloadProgress: DownloadProgress = DownloadProgress(0, GitaConstants.TOTAL_VERSES, 0, DownloadStatus.IDLE),
    val cachedCount: Int = 0,
    val isFullyCached: Boolean = false,
    val missingVerses: List<Pair<Int, Int>> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseUiState
