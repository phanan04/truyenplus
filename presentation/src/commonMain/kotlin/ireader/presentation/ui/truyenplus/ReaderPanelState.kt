package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged

enum class ReaderPanel { None, Appearance, Listening, Voices, SystemVoices }

/** One owner for reader overlays; minimizing playback does not change the TTS session. */
@Stable
class ReaderPanelState {
    var active by mutableStateOf(ReaderPanel.None)
        private set
    var minimized by mutableStateOf(false)
        private set
    private val parents=ArrayDeque<ReaderPanel>()
    fun open(panel:ReaderPanel) {
        if(panel==active)return
        if((panel==ReaderPanel.Voices && active==ReaderPanel.Listening) ||
            (panel==ReaderPanel.SystemVoices && active==ReaderPanel.Voices))parents.addLast(active)
        else parents.clear()
        active=panel
    }
    fun close(panel:ReaderPanel) {
        if(active==panel)active=parents.removeLastOrNull()?:ReaderPanel.None
    }
    fun minimize() { parents.clear();active=ReaderPanel.None;minimized=true }
    fun expand() { parents.clear();active=ReaderPanel.None;minimized=false }
}
val LocalReaderPanels=staticCompositionLocalOf<ReaderPanelState?> { null }

/** Read rapidly changing list positions in a coroutine, outside chapter composition. */
@Composable
fun ReaderTrackScroll(list: LazyListState, chapterId: Long?, restored: Boolean, onPosition: (Int)->Unit) {
    val callback by rememberUpdatedState(onPosition)
    val backdrop=LocalReaderNavigationBackdrop.current
    LaunchedEffect(list,backdrop) {
        snapshotFlow { list.isScrollInProgress }.distinctUntilChanged().collect { moving ->
            backdrop?.samplingEnabled = !moving
        }
    }
    LaunchedEffect(list,chapterId,restored) {
        if(restored && chapterId!=null) snapshotFlow { list.firstVisibleItemIndex }
            .distinctUntilChanged().collect { callback(it) }
    }
    DisposableEffect(backdrop) { onDispose { backdrop?.samplingEnabled=true } }
}
