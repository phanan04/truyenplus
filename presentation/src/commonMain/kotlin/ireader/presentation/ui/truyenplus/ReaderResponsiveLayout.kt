package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
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
fun ReaderListeningDock(optionsOpen: Boolean, options: @Composable () -> Unit, player: @Composable () -> Unit) {
    ReaderPaperTheme { Column(Modifier.widthIn(max=760.dp).fillMaxWidth().heightIn(max=(LocalReaderViewport.current.height-32.dp).coerceAtLeast(48.dp))) {
        if(optionsOpen) Box(Modifier.weight(1f,fill=false)) { options() }
        player()
    } }
}
