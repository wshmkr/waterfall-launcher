package net.wshmkr.launcher.viewmodel

import android.media.session.MediaController
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.wshmkr.launcher.model.MediaInfo
import net.wshmkr.launcher.repository.MediaSessionRepository
import javax.inject.Inject

// Wrapping the raw MediaController lets us mark it @Immutable and keep composable params stable.
@Immutable
data class MediaControllerRef(val controller: MediaController?)

@HiltViewModel
class MediaViewModel @Inject constructor(
    mediaSessionRepository: MediaSessionRepository
) : ViewModel() {

    val mediaInfo: StateFlow<MediaInfo?> = mediaSessionRepository.mediaInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPlaying: StateFlow<Boolean> = mediaSessionRepository.isPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val canSkipNext: StateFlow<Boolean> = mediaSessionRepository.canSkipNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val canSkipPrevious: StateFlow<Boolean> = mediaSessionRepository.canSkipPrevious
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val controllerRef: StateFlow<MediaControllerRef> = mediaSessionRepository.controller
        .map { MediaControllerRef(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaControllerRef(null))
}
