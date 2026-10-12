package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.*
import ireader.domain.services.tts_service.v2.TTSViewModelAdapter
import kotlinx.coroutines.flow.MutableStateFlow
@Composable actual fun rememberListeningAudio(adapter: TTSViewModelAdapter): ListeningAudio = remember {
    object : ListeningAudio {
        override val state = MutableStateFlow(ListeningAudioState())
        override fun speechVolume(value: Float) {}
        override fun musicVolume(value: Float) {}
        override fun musicEnabled(value: Boolean) {}
        override fun chooseMusic() {}
    }
}
