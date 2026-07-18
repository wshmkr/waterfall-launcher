package net.wshmkr.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import net.wshmkr.launcher.repository.ActiveMediaSession
import net.wshmkr.launcher.repository.MediaSessionRepository
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    mediaSessionRepository: MediaSessionRepository
) : ViewModel() {

    val activeMediaSession = mediaSessionRepository.activeMediaSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveMediaSession())
}
