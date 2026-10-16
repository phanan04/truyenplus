package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

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
    fun interact(now:Long) {lastTouch=now}
    fun toggle(now:Long) {if(visible)onUserScroll() else touch(now)}
    fun onUserScroll() { visible=false;playbackVisible=false }
    fun update(now:Long,active:Boolean,settingsOpen:Boolean,listening:Boolean=true) {
        if(settingsOpen) { visible=false;playbackVisible=listening;lastTouch=now }
        else if(active && now-lastTouch>=delayMs) {
            visible=false
            if(!listening)playbackVisible=false
        }
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

/** Children own text/buttons first; only an unclaimed completed tap toggles chrome. */
@Composable fun Modifier.readerWakeOnTouch(onInteraction:()->Unit={},onTouch:()->Unit):Modifier {
    val callback by rememberUpdatedState(onTouch)
    val interaction by rememberUpdatedState(onInteraction)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
            var moved=false
            var claimed=false
            do {
                val event=awaitPointerEvent(PointerEventPass.Final)
                val change=event.changes.firstOrNull {it.id==down.id} ?: break
                moved=moved || (change.position-down.position).getDistance()>viewConfiguration.touchSlop || event.changes.size>1
                claimed=claimed || change.isConsumed
                if(!change.pressed) {
                    interaction()
                    if(!moved && !claimed && change.uptimeMillis-down.uptimeMillis<viewConfiguration.longPressTimeoutMillis)callback()
                    break
                }
            } while(true)
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
