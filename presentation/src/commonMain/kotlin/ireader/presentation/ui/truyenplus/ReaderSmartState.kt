package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*

@Stable class ReaderFollowState {
    var following by mutableStateOf(true); private set
    var returnRequest by mutableIntStateOf(0); private set
    fun onUserDrag() { following=false }
    fun onSpeechMoved() { /* Movement never overrides a user's manual scroll. */ }
    fun resume() { following=true; returnRequest++ }
}
@Stable class ReaderImmersiveState(private val delayMs:Long=5000) {
    var visible by mutableStateOf(true); private set
    var playbackVisible by mutableStateOf(true); private set
    private var lastTouch=0L
    fun touch(now:Long) { lastTouch=now;visible=true;playbackVisible=true }
    fun onUserScroll() { visible=false;playbackVisible=false }
    fun update(now:Long,active:Boolean,settingsOpen:Boolean) {
        if(settingsOpen) { visible=true;playbackVisible=true }
        else if(active && now-lastTouch>=delayMs)visible=false
    }
}
/** Observe scroll without consuming it; smart follow and other programmatic moves stay untouched. */
internal class ReaderUserScrollConnection(private val onUserScroll:()->Unit):NestedScrollConnection {
    override fun onPreScroll(available:Offset,source:NestedScrollSource):Offset {
        if(source==NestedScrollSource.UserInput && available!=Offset.Zero)onUserScroll()
        return Offset.Zero
    }
}

data class ReaderSentenceTarget(val page:Int,val bounds:Rect)

@Composable fun Modifier.readerWakeOnTouch(onTouch:()->Unit):Modifier {
    val callback by rememberUpdatedState(onTouch)
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while(true) {
                val event=awaitPointerEvent(PointerEventPass.Initial)
                if(event.changes.any {it.pressed && !it.previousPressed})callback()
            }
        }
    }
}

/** Screen coordinates, excluding only the actual floating controls. */
internal fun readerFollowScrollDelta(top:Float,bottom:Float,height:Float,topInset:Float,bottomInset:Float):Float? {
    val start=topInset.coerceAtLeast(0f)
    val end=height-bottomInset.coerceAtLeast(0f)
    val available=end-start
    if(available<80f)return null
    return if(top<start+available*.2f || bottom>end-available*.22f)top-(start+available*.28f) else null
}
