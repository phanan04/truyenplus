package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*

data class ReaderViewport(val width: Dp = 360.dp, val height: Dp = 800.dp, val fontScale: Float = 1f) {
    val compactControls get() = width.value / fontScale < 360f
    val iconNavigation get() = width.value / fontScale < 300f
}
val LocalReaderViewport = staticCompositionLocalOf { ReaderViewport() }
val LocalReaderDockInset = staticCompositionLocalOf { 185.dp }

/** Recomputed for rotation, split windows and system font changes; no device-name checks. */
@Composable
fun ReaderResponsiveFrame(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = ReaderViewport(maxWidth, maxHeight, LocalDensity.current.fontScale)
        CompositionLocalProvider(LocalReaderViewport provides viewport) { content() }
    }
}

@Composable
fun ReaderContentPane(maxWidth: Dp = 1120.dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max=maxWidth).fillMaxSize()) { content() }
    }
}

/** Keep playback reachable while fixed settings pages fit the remaining height. */
@Composable
fun ReaderListeningDock(optionsOpen: Boolean, options: @Composable () -> Unit, player: @Composable () -> Unit,
    overlay: (@Composable () -> Unit)? = null, visible:Boolean = true) {
    var playerHeight by remember { mutableStateOf(72.dp) }
    val density=LocalDensity.current
    val dock:@Composable ()->Unit={ Column(Modifier.widthIn(max=760.dp).fillMaxWidth().heightIn(max=(LocalReaderViewport.current.height-32.dp).coerceAtLeast(48.dp))) {
        if(optionsOpen) Box(Modifier.weight(1f,fill=false)) { options() }
        Box(Modifier.onSizeChanged {playerHeight=with(density){it.height.toDp()}}) {player()}
    } }
    val animatedDock:@Composable ()->Unit={
        AnimatedVisibility(visible,enter=fadeIn(tween(120))+slideInVertically(tween(120)){it/4},
            exit=fadeOut(tween(120))+slideOutVertically(tween(120)){it/4}) {dock()}
    }
    ReaderPaperTheme {
        if(overlay==null)animatedDock()
        else Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().padding(bottom=playerHeight)) {overlay()}
            Box(Modifier.align(Alignment.BottomCenter)) {animatedDock()}
        }
    }
}
