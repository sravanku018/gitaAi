package com.aipoweredgita.app.compose

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipoweredgita.app.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart

@Composable
fun TrackOrientation(appViewModel: AppViewModel) {
    val config = LocalConfiguration.current
    val orientation = config.orientation
    // Push initial value
    LaunchedEffect(orientation) {
        appViewModel.setOrientation(orientation)
    }
}

