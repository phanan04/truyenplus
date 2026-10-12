package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.Composable
import ireader.domain.services.tts_service.v2.TTSViewModelAdapter
import kotlinx.coroutines.flow.StateFlow

data class ListeningAudioState(val supported: Boolean = false, val speechVolume: Float = .8f,
    val musicTitle: String = "", val musicVolume: Float = .2f, val musicEnabled: Boolean = false, val musicError: Boolean = false)

interface ListeningAudio {
    val state: StateFlow<ListeningAudioState>
    fun speechVolume(value: Float)
    fun musicVolume(value: Float)
    fun musicEnabled(value: Boolean)
    fun chooseMusic()
}

@Composable expect fun rememberListeningAudio(adapter: TTSViewModelAdapter): ListeningAudio

internal fun keepListeningMusicPlaying(active: Boolean, state: ireader.domain.services.tts_service.v2.PlaybackState): Boolean = when (state) {
    ireader.domain.services.tts_service.v2.PlaybackState.PLAYING -> true
    ireader.domain.services.tts_service.v2.PlaybackState.LOADING -> active
    else -> false
}
